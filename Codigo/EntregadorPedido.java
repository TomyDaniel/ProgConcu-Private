import java.util.Random;

public class EntregadorPedido implements Runnable {
    private final Random random = new Random();
    private final RegistroPedidos registro;
    private static volatile boolean running;        //Siendo volatile es visible para todos los hilos
    private final int demoraEntregador;
    private static final int PROBABILIDAD_EXITO = 90; //90% por consigna

    public EntregadorPedido(RegistroPedidos registro, int demoraBaseMs, boolean estadoInicial) {
        this.registro = registro;
        this.demoraEntregador = demoraBaseMs;
        EntregadorPedido.running = estadoInicial;       //Estado inicial de simulación
    }

    private void procesarPedido() {
        // Obtener un pedido aleatorio de TRANSITO (no lo remueve aún)
        Pedido pedido = registro.obtenerPedidoAleatorio(EstadoPedido.TRANSITO);

        if (pedido==null) {
            return; //La lista está vacia, por lo que no se puede obtener un pedido aleatorio. O otro hilo se lo llevó
        }

        // Intentar remover el pedido específico de TRANSITO.
        boolean removido = registro.removerPedido(pedido, EstadoPedido.TRANSITO);

        if (!removido) {
            return; //Esto significa que otro hilo se "llevó" el pedido del hilo actual
        }
        // Bloquear el pedido específico para procesarlo

        // El pedido fue adquirido por este hilo Entregador.
        boolean exitoso = random.nextInt(100) < PROBABILIDAD_EXITO;

        if (exitoso) {
            // Mover a ENTREGADO
            registro.agregarPedido(pedido, EstadoPedido.ENTREGADO);
            System.out.println(Thread.currentThread().getName() + " entregó exitosamente " + pedido);
        } else {
            // Mover a FALLIDO
            registro.agregarPedido(pedido, EstadoPedido.FALLIDO);
            System.out.println(Thread.currentThread().getName() + " falló la entrega de " + pedido);
        }

    }


    private void aplicarDemora() throws InterruptedException {
        int variacion = random.nextInt(0, demoraEntregador/2)+demoraEntregador;
        Thread.sleep(variacion);
    }

    public static void setRunning(boolean nuevoEstado) {
        EntregadorPedido.running= nuevoEstado;
    }

    public boolean isRunning() {
        return EntregadorPedido.running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            // Bucle principal
            while (isRunning() || registro.getCantidad(EstadoPedido.TRANSITO) > 0) {
                procesarPedido();
                aplicarDemora();
            }
        } catch (InterruptedException e) { //Un hilo lo despertó mientras estaba durmiendo
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " interrumpido.");
        }
        System.out.println(Thread.currentThread().getName() + " terminado.");
    }
}