import java.util.Random;

public class DespachadorPedido implements Runnable {

    private final Random random = new Random();
    private final RegistroPedidos registro;
    private final MatrizCasilleros matriz; // Necesaria para liberar casillero
    private static volatile boolean running;
    private final int demoraDespachador;
    private static final int PROBABILIDAD_EXITO = 85; //85% por consigna

    public DespachadorPedido(RegistroPedidos registro, MatrizCasilleros matriz, int demoraBaseMs, boolean running) {
        this.registro = registro;
        this.matriz = matriz;
        this.demoraDespachador = demoraBaseMs;
        DespachadorPedido.running = running;
    }

    private void procesarPedido() {
        // Obtener un pedido aleatorio que tenga un estado de PREPARACION
        Pedido pedido = registro.obtenerPedidoAleatorio(EstadoPedido.PREPARACION);

        if (pedido==null) {
            return; //La lista está vacia, por lo que no se puede obtener un pedido aleatorio. O otro hilo se lo llevó
        }

        //remover el pedido específico de PREPARACION.
        boolean removido = registro.removerPedido(pedido, EstadoPedido.PREPARACION); //Verifico que pueda ser removido

        if (!removido ) {
            return; //Esto significa que otro hilo se "llevó" el pedido del hilo actual
        }

        try {
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
        } catch (Exception e) {}
    }


    private void aplicarDemora() throws InterruptedException {
        int variacion = random.nextInt(0, demoraDespachador/2)+demoraDespachador;
        Thread.sleep(variacion);
    }

    public static void setRunning(boolean nuevoEstado) {
        DespachadorPedido.running= nuevoEstado;
    }

    public boolean isRunning() {
        return DespachadorPedido.running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            // Bucle principal
            while (isRunning() || registro.getCantidad(EstadoPedido.PREPARACION) > 0) {
                procesarPedido();
                aplicarDemora();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " interrumpido.");
        }
        System.out.println(Thread.currentThread().getName() + " terminado.");
    }
}