// DespachadorPedido.java
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class DespachadorPedido implements Runnable {
    private final RegistroPedidos registro;
    private final MatrizCasilleros matriz;
    private final int demoraBaseMs;
    private final int variacionDemoraMs;
    private final double probExito;
    private final Random random = new Random();
    private final AtomicBoolean running;

    public DespachadorPedido(RegistroPedidos reg, MatrizCasilleros mat, int demoraBase, int variacion, double probExito, AtomicBoolean running) {
        this.registro = reg;
        this.matriz = mat;
        this.demoraBaseMs = demoraBase;
        this.variacionDemoraMs = variacion;
        this.probExito = probExito;
        this.running = running;
    }

    @Override
    public void run() {
         System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            // Continuar mientras esté corriendo o haya pedidos por procesar en la cola anterior
            while (running.get() || !registro.pedidosEnPreparacion.isEmpty()) {
                Pedido pedido = registro.tomarDePreparacion();
                if (pedido != null) {
                    // Bloquear el pedido específico
                    pedido.lock();
                    try {
                        boolean exito = random.nextDouble() < probExito;
                        int casilleroId = pedido.getCasilleroIdAsignado();

                        if (exito) {
                            // Éxito: Liberar casillero, mover a tránsito
                            matriz.liberarCasillero(casilleroId);
                            pedido.liberarCasillero(); // Quitar referencia interna
                            registro.agregarATransito(pedido);
                            //System.out.println(Thread.currentThread().getName() + " despachó con éxito " + pedido);
                        } else {
                            // Fallo: Poner casillero fuera de servicio, mover a fallidos
                            matriz.ponerFueraDeServicio(casilleroId);
                            pedido.liberarCasillero();
                            registro.agregarAFallidos(pedido);
                             //System.out.println(Thread.currentThread().getName() + " falló despacho de " + pedido + ". Casillero " + casilleroId + " fuera de servicio.");
                        }
                    } finally {
                        pedido.unlock(); // Siempre liberar el lock del pedido
                    }
                    dormir(); // Dormir después de procesar un pedido
                } else if (running.get()) {
                    // Si no hay pedidos y la simulación sigue, esperar un poco
                    Thread.sleep(20);
                }
                 // Si !running.get() y la cola está vacía, el bucle terminará
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