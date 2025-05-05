// En PreparadorPedido.java
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class PreparadorPedido implements Runnable {
    private final RegistroPedidos registro;
    private final MatrizCasilleros matriz;
    private final int demoraBaseMs;
    private final int variacionDemoraMs;
    private final Random random = new Random();
    private final int totalPedidosAGenerar;
    private final AtomicBoolean running;
    private final AtomicBoolean stoppedDueToNoLockers; // Flag para informar al Main

    // Actualizar constructor
    public PreparadorPedido(RegistroPedidos reg, MatrizCasilleros mat, int demoraBase, int variacion, int totalPedidos, AtomicBoolean running, AtomicBoolean stoppedDueToNoLockers) {
        this.registro = reg;
        this.matriz = mat;
        this.demoraBaseMs = demoraBase;
        this.variacionDemoraMs = variacion;
        this.totalPedidosAGenerar = totalPedidos;
        this.running = running;
        this.stoppedDueToNoLockers = stoppedDueToNoLockers; // Guardar referencia
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            while (running.get() && registro.getCantidadPreparados() < totalPedidosAGenerar) {
                int casilleroId = -1;
                try {
                    while (running.get() && casilleroId == -1) {
                        // Solo intentar si aún no hemos generado todos los pedidos
                        if (registro.getCantidadPreparados() >= totalPedidosAGenerar) break;

                        // *** LLAMADA CRÍTICA ***
                        casilleroId = matriz.ocuparCasilleroAleatorio(); // Puede lanzar NoAvailableLockersException

                        if (casilleroId == -1) {
                            // No hay casillero VACIO temporalmente, esperar y reintentar
                            //System.out.println(Thread.currentThread().getName() + " no encontró casillero vacío, reintentando...");
                            Thread.sleep(100); // Espera un poco más larga si falló
                        }
                    }
                } catch (NoHayCasilleros e) {
                    // *** MANEJO DE LA CONDICIÓN IRRECUPERABLE ***
                    System.err.println(Thread.currentThread().getName() + ": " + e.getMessage());
                    System.err.println(Thread.currentThread().getName() + ": Señalizando detención de la simulación.");
                    stoppedDueToNoLockers.set(true); // Informar al Main la causa
                    running.set(false); // Señalizar a todos los hilos que paren
                    break; // Salir del bucle while principal de este hilo
                }

                // Si salimos del while interno porque ya se generaron todos, terminar
                if (registro.getCantidadPreparados() >= totalPedidosAGenerar) break;
                // Si salimos porque se detuvo la simulación (running=false) o no se obtuvo casillero (error)
                if (!running.get() || casilleroId == -1) break;

                // ----- Si llegamos aquí, tenemos un casilleroId válido -----

                Pedido nuevoPedido = new Pedido();
                nuevoPedido.asignarCasillero(casilleroId);

                nuevoPedido.lock();
                try {
                    registro.agregarAPreparacion(nuevoPedido);
                    int preparados = registro.incrementarPreparados();
                     // System.out.println(Thread.currentThread().getName() + " preparó " + nuevoPedido + ". Total preparados: " + preparados);
                } finally {
                    nuevoPedido.unlock();
                }

                dormir(); // Simular demora de preparación
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
             // Solo imprimir si no fue por falta de casilleros (para evitar doble mensaje)
            if (!stoppedDueToNoLockers.get()) {
                 System.out.println(Thread.currentThread().getName() + " interrumpido.");
            }
        } finally {
            // Mensaje final del hilo
            if (stoppedDueToNoLockers.get() && !Thread.currentThread().isInterrupted()) {
                 System.out.println(Thread.currentThread().getName() + " terminado debido a falta de casilleros.");
            } else if (!running.get() && !stoppedDueToNoLockers.get()){
                 System.out.println(Thread.currentThread().getName() + " terminado por señal de detención.");
            } else if (registro.getCantidadPreparados() >= totalPedidosAGenerar) {
                 System.out.println(Thread.currentThread().getName() + " terminado. Objetivo de pedidos alcanzado.");
            } else {
                 System.out.println(Thread.currentThread().getName() + " terminado (estado final: " + registro.getCantidadPreparados() + " preparados).");
            }
        }
    }

    private void dormir() throws InterruptedException {
        int demora = demoraBaseMs + (variacionDemoraMs > 0 ? random.nextInt(variacionDemoraMs * 2 + 1) - variacionDemoraMs : 0);
        Thread.sleep(Math.max(0, demora));
    }
}