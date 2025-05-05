// --- Clases de Lógica de Procesos (Runnables) ---
// ProcesoLogistica (Interfaz o Clase base si hay comportamiento común)
// Por simplicidad, haremos que cada proceso implemente Runnable directamente.

// PreparadorPedido.java
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class PreparadorPedido implements Runnable {
    private final Random random = new Random();
    private final RegistroPedidos registro;
    private final MatrizCasilleros matriz;
    private final AtomicBoolean running;
    private final int demoraBaseMs;
    private final int variacionDemoraMs;
    private final int totalPedidosAGenerar;

    public PreparadorPedido(RegistroPedidos registro, MatrizCasilleros matriz, int demoraBaseMs,
                            int variacionDemoraMs, int totalPedidosAGenerar, AtomicBoolean running) {
        this.registro = registro;
        this.matriz = matriz;
        this.demoraBaseMs = demoraBaseMs;
        this.variacionDemoraMs = variacionDemoraMs;
        this.totalPedidosAGenerar = totalPedidosAGenerar;
        this.running = running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            while (running.get() && registro.getCantidadPreparados() < totalPedidosAGenerar) {
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
        int casilleroId = matriz.ocuparCasilleroAleatorio();
        if (casilleroId != -1) {
            // Crear nuevo pedido y asignar casillero
            Pedido nuevoPedido = new Pedido();
            nuevoPedido.asignarCasillero(casilleroId);

            // Bloquear pedido para procesarlo
            nuevoPedido.lock();
            try {
                registro.agregarAPreparacion(nuevoPedido);
                registro.incrementarPreparados();
            } finally {
                nuevoPedido.unlock();
            }
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