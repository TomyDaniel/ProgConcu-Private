// VerificadorPedido.java
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class VerificadorPedido implements Runnable {
    private final RegistroPedidos registro;
    private final int demoraBaseMs;
    private final int variacionDemoraMs;
    private final double probExito;
    private final Random random = new Random();
     private final AtomicBoolean running;

    public VerificadorPedido(RegistroPedidos reg, int demoraBase, int variacion, double probExito, AtomicBoolean running) {
        this.registro = reg;
        this.demoraBaseMs = demoraBase;
        this.variacionDemoraMs = variacion;
        this.probExito = probExito;
        this.running = running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
             // isEmpty() es más eficiente que size()
             while (running.get() || !registro.pedidosEntregados.isEmpty()) {
                 // *** CAMBIO PRINCIPAL: Usar selección aleatoria ***
                Pedido pedido = registro.tomarDeEntregadosAleatorio();

                if (pedido != null) {
                    pedido.lock();
                    try {
                        boolean exito = random.nextDouble() < probExito;
                        if (exito) {
                            registro.agregarAVerificados(pedido);
                           // System.out.println(Thread.currentThread().getName() + " verificó con éxito " + pedido);
                        } else {
                            registro.agregarAFallidos(pedido);
                           // System.out.println(Thread.currentThread().getName() + " falló verificación de " + pedido);
                        }
                    } finally {
                        pedido.unlock();
                    }
                    dormir();
                } else if (running.get()){
                     // Si no hay pedidos y la simulación sigue, esperar un poco más
                     Thread.sleep(30); // Espera aumentada
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " interrumpido.");
        }
         System.out.println(Thread.currentThread().getName() + " terminado.");
    }

      private void dormir() throws InterruptedException {
        int demora = demoraBaseMs + (variacionDemoraMs > 0 ? random.nextInt(variacionDemoraMs * 2 + 1) - variacionDemoraMs : 0);
        Thread.sleep(Math.max(0, demora));
    }
}