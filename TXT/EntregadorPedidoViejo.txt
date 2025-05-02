// EntregadorPedido.java
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class EntregadorPedido implements Runnable {
    private final RegistroPedidos registro;
    private final int demoraBaseMs;
    private final int variacionDemoraMs;
    private final double probExito;
    private final Random random = new Random();
    private final AtomicBoolean running;

    public EntregadorPedido(RegistroPedidos reg, int demoraBase, int variacion, double probExito, AtomicBoolean running) {
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
            while (running.get() || !registro.pedidosEnTransito.isEmpty()) {
                Pedido pedido = registro.tomarDeTransito();
                if (pedido != null) {
                    pedido.lock();
                    try {
                        boolean exito = random.nextDouble() < probExito;
                        if (exito) {
                            registro.agregarAEntregados(pedido);
                           // System.out.println(Thread.currentThread().getName() + " entregó con éxito " + pedido);
                        } else {
                            registro.agregarAFallidos(pedido);
                            //System.out.println(Thread.currentThread().getName() + " falló entrega de " + pedido);
                        }
                    } finally {
                        pedido.unlock();
                    }
                    dormir();
                } else if (running.get()){
                     Thread.sleep(20);
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