import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class EntregadorPedido implements Runnable {
    private final Random random = new Random();
    private final RegistroPedidos registro;
    private final AtomicBoolean running;
    private final int demoraBaseMs;
    private final int variacionDemoraMs;
    private static final int PROBABILIDAD_EXITO = 90; // 90% de éxito

    public EntregadorPedido(RegistroPedidos registro, int demoraBaseMs,
                            int variacionDemoraMs, AtomicBoolean running) {
        this.registro = registro;
        this.demoraBaseMs = demoraBaseMs;
        this.variacionDemoraMs = variacionDemoraMs;
        this.running = running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            while (running.get() || registro.getCantidadEnTransito()>0) {
                Pedido pedido = registro.obtenerPedidoTransitoAleatorio();
                if (pedido != null) {
                    procesarPedido(pedido);
                }
                aplicarDemora();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " interrumpido.");
        }
        System.out.println(Thread.currentThread().getName() + " terminado.");
    }

    private void procesarPedido(Pedido pedido) {
        pedido.lock();
        try {
            // Procesar con 90% de éxito
            boolean exitoso = random.nextInt(100) < PROBABILIDAD_EXITO;

            // Remover de tránsito primero
            registro.removerDeTransito(pedido);

            if (exitoso) {
                registro.agregarAEntregados(pedido);
            } else {
                registro.agregarAFallidos(pedido);
            }
        } finally {
            pedido.unlock();
        }
    }

    private void aplicarDemora() throws InterruptedException {
        int variacion = 0;
        if (variacionDemoraMs > 0) {
            variacion = random.nextInt(variacionDemoraMs * 2 + 1) - variacionDemoraMs;
        }
        int demora = Math.max(0, demoraBaseMs + variacion);
        Thread.sleep(demora);
    }
}