import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class DespachadorPedido implements Runnable {
    private final Random random = new Random();
    private final RegistroPedidos registro;
    private final MatrizCasilleros matriz; // Necesaria para liberar casillero
    private final AtomicBoolean running;
    private final int demoraDespachador;
    private final int variacionDemoraMs;
    private static final int PROBABILIDAD_EXITO = 85; //85% por consigna

    public DespachadorPedido(RegistroPedidos registro, MatrizCasilleros matriz, int demoraBaseMs, int variacionDemoraMs, AtomicBoolean running) {
        this.registro = registro;
        this.matriz = matriz;
        this.demoraDespachador = demoraBaseMs;
        this.variacionDemoraMs = variacionDemoraMs;
        this.running = running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            // Bucle principal: sigue mientras 'running' sea true O mientras queden pedidos por despachar
            while (running.get() || registro.getCantidad(EstadoPedido.PREPARACION) > 0) {
                // Condición de control por si otro hilo lo hace cero en el momento que entra.
                if (!running.get() && registro.getCantidad(EstadoPedido.PREPARACION) == 0) {
                    break;
                }
                procesarPedido();
                aplicarDemora();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " interrumpido.");
        }
        System.out.println(Thread.currentThread().getName() + " terminado.");
    }

    private void procesarPedido() {
        // Obtener un pedido aleatorio que tenga un estado de PREPARACION
        Pedido pedido = registro.obtenerPedidoAleatorio(EstadoPedido.PREPARACION);

        if (pedido != null) {
            // Bloquear el pedido específico para procesarlo
            pedido.lock();
            try {
                // Intentar remover el pedido específico de PREPARACION.
                // Es posible que otro hilo lo haya removido entre obtenerPedidoAleatorio y aquí.
                boolean removido = registro.removerPedido(pedido, EstadoPedido.PREPARACION); //Verifico que pueda ser removido

                if (removido ) {
                    // El pedido fue adquirido por este hilo Despachador.
                    int casilleroId = pedido.getCasilleroId();
                    boolean exitoso = random.nextInt(100) < PROBABILIDAD_EXITO;
                    if (exitoso) {
                        // Lo sacamos del estado ocupado
                        matriz.liberarCasillero(casilleroId);
                        System.out.println(Thread.currentThread().getName() + " liberó casillero " + casilleroId + " para " + pedido);
                        // Lo movemos a pedidos en transito
                        registro.agregarPedido(pedido, EstadoPedido.TRANSITO);
                        System.out.println(Thread.currentThread().getName() + " despachó " + pedido + " a tránsito.");
                    }
                    else{
                        matriz.marcarFueraDeServicio(casilleroId);
                        System.out.println(Thread.currentThread().getName() + " marco casillero " + casilleroId + " como FUERA DE SERVICIO " + pedido);
                        registro.agregarPedido(pedido, EstadoPedido.FALLIDO);
                        System.out.println(Thread.currentThread().getName() + " envió " + pedido + " a fallido.");
                    }
                }

            } finally {
                pedido.unlock();
            }
        }
    }


    private void aplicarDemora() throws InterruptedException {
        int variacion = 0;
        if (variacionDemoraMs > 0) {
            variacion = random.nextInt(variacionDemoraMs * 2 + 1) - variacionDemoraMs;
        }
        int demora = Math.max(0, demoraDespachador + variacion);
        Thread.sleep(demora);
    }
}