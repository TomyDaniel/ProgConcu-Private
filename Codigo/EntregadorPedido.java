import java.util.Random;

public class EntregadorPedido implements Runnable {
    private final Random random = new Random();
    private final RegistroPedidos registro;
    private static volatile boolean running;
    private final int demoraEntregador;
    private static final int PROBABILIDAD_EXITO = 90;

    public EntregadorPedido(RegistroPedidos registro, int demoraBaseMs, boolean estadoInicial) {
        this.registro = registro;
        this.demoraEntregador = demoraBaseMs;
        EntregadorPedido.running = estadoInicial;
    }

    private void procesarPedido() {
        // Obtener Y REMOVER un pedido aleatorio de TRANSITO
        Pedido pedido = registro.obtenerYRemoverPedidoAleatorio(EstadoPedido.TRANSITO);

        if (pedido == null) {
            // No hay pedidos en TRANSITO o fue tomado por otro entregador
            return;
        }

        // El pedido fue adquirido y removido de TRANSITO por este hilo Entregador.
        boolean exitoso = random.nextInt(100) < PROBABILIDAD_EXITO;

        if (exitoso) {
            registro.agregarPedido(pedido, EstadoPedido.ENTREGADO);
            System.out.println(Thread.currentThread().getName() + " entregó exitosamente " + pedido);
        } else {
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
            while (isRunning() || registro.getCantidad(EstadoPedido.TRANSITO) > 0) {
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