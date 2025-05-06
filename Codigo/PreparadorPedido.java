
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class PreparadorPedido implements Runnable {
    // ... (campos sin cambios)
    private final RegistroPedidos registro;
    private final MatrizCasilleros matriz;
    private final AtomicBoolean running;
    private final int demoraPreparador;
    private final int variacionDemoraMs;
    private final int totalPedidosAGenerar; // Límite superior

    public PreparadorPedido(RegistroPedidos registro, MatrizCasilleros matriz, int demoraPreparador, int variacionDemoraMs, int totalPedidosAGenerar, AtomicBoolean running) {
        this.registro = registro;
        this.matriz = matriz;
        this.demoraPreparador = demoraPreparador;
        this.variacionDemoraMs = variacionDemoraMs;
        this.totalPedidosAGenerar = totalPedidosAGenerar;
        this.running = running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            // La condición ahora usa el contador total de generados
            while (running.get() && registro.getTotalPedidosGenerados() < totalPedidosAGenerar) {
                procesarPedido(); // Intentará crear y agregar un pedido
                aplicarDemora();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " interrumpido.");
        }
        // Añadir un mensaje si termina por alcanzar el límite
        if (registro.getTotalPedidosGenerados() >= totalPedidosAGenerar) {
            System.out.println(Thread.currentThread().getName() + " terminó porque se alcanzó el límite de pedidos.");
        } else {
            System.out.println(Thread.currentThread().getName() + " terminado.");
        }
    }

    private void procesarPedido() {
        // Doble chequeo: verificar el límite antes de intentar ocupar casillero
        if (registro.getTotalPedidosGenerados() >= totalPedidosAGenerar) {
            return; // No generar más si ya se alcanzó el límite
        }

        int casilleroId = matriz.ocuparCasilleroAleatorio();
        if (casilleroId != -1) {
            // Solo crear y procesar si se obtuvo casillero y aún no se ha superado el límite
            // (otra verificación por si acaso otro hilo incrementó justo ahora)
            if (registro.getTotalPedidosGenerados() < totalPedidosAGenerar) {
                // Crear nuevo pedido y asignar casillero
                Pedido nuevoPedido = new Pedido();
                nuevoPedido.asignarCasillero(casilleroId);

                // No es necesario lock/unlock aquí (eliminar comentarios)
                try {
                    // Usar el método generalizado con el estado PREPARACION
                    registro.agregarPedido(nuevoPedido, EstadoPedido.PREPARACION);
                    // Incrementar el contador total de generados *después* de agregarlo exitosamente
                    registro.incrementarTotalGenerados();
                    System.out.println(Thread.currentThread().getName() + " preparó " + nuevoPedido + " en casillero " + casilleroId + " (Total generados: " + registro.getTotalPedidosGenerados() + ")");
                } finally {
                    // No es necesario unlock aquí (eliminar comentario)
                }
            } else {
                // Si se alcanzó el límite justo después de ocupar el casillero, hay que liberarlo.
                System.out.println(Thread.currentThread().getName() + " ocupó casillero " + casilleroId + " pero se alcanzó el límite antes de crear el pedido. Liberando...");
                matriz.liberarCasillero(casilleroId);
            }
        } else {
            System.out.println(Thread.currentThread().getName() + " no encontró casillero disponible.");
            // Considerar una pequeña demora aquí para evitar spin-wait si la matriz está llena a menudo
            try {
                Thread.sleep(50); // Pequeña pausa si no se encontró casillero
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void aplicarDemora() throws InterruptedException {
        // ... (sin cambios)
        int variacion = (variacionDemoraMs > 0)
                ? ThreadLocalRandom.current().nextInt(-variacionDemoraMs, variacionDemoraMs + 1)
                : 0;
        Thread.sleep(Math.max(0, demoraPreparador + variacion));
    }
}