import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class VerificadorPedido implements Runnable {
    private final Random random = new Random();
    private final RegistroPedidos registro;
    private final AtomicBoolean running;
    private final int demoraVerificador;
    private final int variacionDemoraMs;
    private static final int PROBABILIDAD_EXITO = 95; //95% por consigna

    public VerificadorPedido(RegistroPedidos registro, int demoraBaseMs, int variacionDemoraMs, AtomicBoolean running) {
        this.registro = registro;
        this.demoraVerificador = demoraBaseMs;
        this.variacionDemoraMs = variacionDemoraMs;
        this.running = running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            // Bucle principal: sigue mientras 'running' sea true O mientras queden pedidos por verificar
            while (running.get() || registro.getCantidad(EstadoPedido.ENTREGADO) > 0) {
                //Verificacion por si algun hilo lo vuelve cero en el momento que entra
                if (!running.get() && registro.getCantidad(EstadoPedido.ENTREGADO) == 0) {
                    break;
                }
                procesarPedido(); // Puede lanzar MatrizLlenaException
                aplicarDemora();

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " interrumpido.");
        }
        System.out.println(Thread.currentThread().getName() + " terminado.");
    }

    private void procesarPedido() throws MatrizLlenaException { // Propagar excepción
        // Obtener un pedido aleatorio del estado ENTREGADO
        Pedido pedido = registro.obtenerPedidoAleatorio(EstadoPedido.ENTREGADO);

        if (pedido != null) {
            pedido.lock();
            try {
                // Intentar remover el pedido específico de la lista ENTREGADO.
                boolean removido = registro.removerPedido(pedido, EstadoPedido.ENTREGADO);

                if (removido) {
                    // El pedido fue exitosamente "adquirido" por este hilo Verificador.
                    boolean exitoso = random.nextInt(100) < PROBABILIDAD_EXITO;
                    int casilleroId = pedido.getCasilleroId(); // Obtener ID antes de decidir qué hacer

                    if (exitoso) {
                        // Casillero OK, mover a VERIFICADO
                        registro.agregarPedido(pedido, EstadoPedido.VERIFICADO);
                        System.out.println(Thread.currentThread().getName() + " verificó OK " + pedido + " (Casillero: " + casilleroId + ")");
                    } else {
                        // Casillero FAIL, mover a FALLIDO
                        registro.agregarPedido(pedido, EstadoPedido.FALLIDO);
                        System.out.println(Thread.currentThread().getName() + " verificó FAIL " + pedido + " (Casillero: " + casilleroId + ")");

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
        int demora = Math.max(0, demoraVerificador + variacion);
        Thread.sleep(demora);
    }
}