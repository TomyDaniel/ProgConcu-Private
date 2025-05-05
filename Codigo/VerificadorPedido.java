import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class VerificadorPedido implements Runnable {
    private final Random random = new Random();
    private final RegistroPedidos registro;
    private final AtomicBoolean running;
    private final int demoraVerificador;
    private final int variacionDemoraMs;
    private static final int PROBABILIDAD_EXITO = 95; // 95% de éxito

    public VerificadorPedido(RegistroPedidos registro, int demoraVerificador, int variacionDemoraMs, AtomicBoolean running) {
        this.registro = registro;
        this.demoraVerificador = demoraVerificador;
        this.variacionDemoraMs = variacionDemoraMs;
        this.running = running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            while (running.get() || registro.getCantidadEntregados()>0) {
                Pedido pedido = registro.obtenerPedidoEntregadoAleatorio();
                if (pedido != null) {
                    verificarPedido(pedido);
                }
                aplicarDemora();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " interrumpido.");
        }
        System.out.println(Thread.currentThread().getName() + " terminado.");
    }

    private void verificarPedido(Pedido pedido) {
        pedido.lock();
        try {
            // Verificar con 95% de éxito
            boolean exitoso = random.nextInt(100) < PROBABILIDAD_EXITO;

            // Remover de entregados primero
            registro.removerDeEntregados(pedido);

            if (exitoso) {
                registro.agregarAVerificados(pedido);
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
        int demora = Math.max(0, demoraVerificador + variacion);
        Thread.sleep(demora);
    }
}