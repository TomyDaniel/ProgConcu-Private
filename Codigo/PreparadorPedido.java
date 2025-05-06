
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class PreparadorPedido implements Runnable {
    private final Random random = new Random();
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
        // Doble chequeo
        if (registro.getTotalPedidosGenerados() >= totalPedidosAGenerar) {
            return; // No generar más si ya se alcanzó el límite
        }

        int casilleroId = matriz.ocuparCasilleroAleatorio();
        if (casilleroId != -1) {
            if (registro.getTotalPedidosGenerados() < totalPedidosAGenerar) {
                // Crear nuevo pedido y asignar casillero
                Pedido nuevoPedido = new Pedido();
                nuevoPedido.asignarCasillero(casilleroId);
                // El pedido debe pasar al estado Preparacion
                registro.agregarPedido(nuevoPedido, EstadoPedido.PREPARACION);
                // Incrementar el contador total de generados
                registro.incrementarTotalGenerados();
                System.out.println(Thread.currentThread().getName() + " preparó " + nuevoPedido + " en casillero " + casilleroId + " (Total generados: " + registro.getTotalPedidosGenerados() + ")");

            } else {
                // Si se alcanzó el límite justo después de ocupar el casillero, hay que liberarlo.
                System.out.println(Thread.currentThread().getName() + " ocupó casillero " + casilleroId + " pero se alcanzó el límite antes de crear el pedido. Liberando...");
                matriz.liberarCasillero(casilleroId);
            }
        } else {
            System.out.println(Thread.currentThread().getName() + " no encontró casillero disponible.");
            try {
                Thread.sleep(50); // Pequeña pausa si no se encontró casillero
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void aplicarDemora() throws InterruptedException {
        int variacion = 0;
        if (variacionDemoraMs > 0) {
            variacion = random.nextInt(variacionDemoraMs * 2 + 1) - variacionDemoraMs;
        }
        int demora = Math.max(0, demoraPreparador + variacion);
        Thread.sleep(demora);
    }
}