// --- Clases de Lógica de Procesos (Runnables) ---
// ProcesoLogistica (Interfaz o Clase base si hay comportamiento común)
// Por simplicidad, haremos que cada proceso implemente Runnable directamente.

// PreparadorPedido.java
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class PreparadorPedido implements Runnable {
    private final RegistroPedidos registro;
    private final MatrizCasilleros matriz;
    private final int demoraBaseMs;
    private final int variacionDemoraMs;
    private final Random random = new Random();
    private final int totalPedidosAGenerar;
    private final AtomicBoolean running; // Para detener el hilo externamente

    public PreparadorPedido(RegistroPedidos reg, MatrizCasilleros mat, int demoraBase, int variacion, int totalPedidos, AtomicBoolean running) {
        this.registro = reg;
        this.matriz = mat;
        this.demoraBaseMs = demoraBase;
        this.variacionDemoraMs = variacion;
        this.totalPedidosAGenerar = totalPedidos;
        this.running = running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            while (running.get() && registro.getCantidadPreparados() < totalPedidosAGenerar) {
                // 1. Intentar generar y asignar casillero
                int casilleroId = -1;
                while (running.get() && casilleroId == -1) {
                     // Solo intentar si aún no hemos generado todos los pedidos
                     if (registro.getCantidadPreparados() >= totalPedidosAGenerar) break;

                    casilleroId = matriz.ocuparCasilleroAleatorio();
                    if (casilleroId == -1) {
                        //System.out.println(Thread.currentThread().getName() + " no encontró casillero vacío, reintentando...");
                        Thread.sleep(50); // Espera corta si no hay casillero
                    }
                }
                 // Si salimos del while porque ya se generaron todos, terminar
                if (registro.getCantidadPreparados() >= totalPedidosAGenerar) break;
                // Si salimos porque se detuvo la simulación
                if (!running.get()) break;


                // 2. Crear pedido y asignarle casillero
                Pedido nuevoPedido = new Pedido();
                nuevoPedido.asignarCasillero(casilleroId);

                // 3. Bloquear el pedido (buena práctica aunque aquí es el primer uso)
                nuevoPedido.lock();
                try {
                    // 4. Registrar en preparación
                    registro.agregarAPreparacion(nuevoPedido);
                    int preparados = registro.incrementarPreparados();
                    // System.out.println(Thread.currentThread().getName() + " preparó " + nuevoPedido + ". Total preparados: " + preparados);

                } finally {
                    nuevoPedido.unlock();
                }

                // 5. Simular demora
                dormir();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " interrumpido.");
        }
        System.out.println(Thread.currentThread().getName() + " terminado. Pedidos generados: " + registro.getCantidadPreparados());
    }

    private void dormir() throws InterruptedException {
        int demora = demoraBaseMs + (variacionDemoraMs > 0 ? random.nextInt(variacionDemoraMs * 2 + 1) - variacionDemoraMs : 0);
        Thread.sleep(Math.max(0, demora)); // Asegura no dormir negativo
    }
}
