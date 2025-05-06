
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;


public class EntregadorPedido implements Runnable {
    // ... (campos sin cambios)
    private final RegistroPedidos registro;
    private final AtomicBoolean running;
    private final int demoraBaseMs;
    private final int variacionDemoraMs;
    private final double probabilidadFallo; // Ejemplo: 0.1 para 10% de fallo

    public EntregadorPedido(RegistroPedidos registro, int demoraBaseMs, int variacionDemoraMs, double probabilidadFallo, AtomicBoolean running) {
        this.registro = registro;
        this.demoraBaseMs = demoraBaseMs;
        this.variacionDemoraMs = variacionDemoraMs;
        this.probabilidadFallo = probabilidadFallo;
        this.running = running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            // Bucle principal: sigue mientras 'running' sea true O mientras queden pedidos por entregar
            while (running.get() || registro.getCantidad(EstadoPedido.TRANSITO) > 0) {
                // Si !running.get(), solo procesamos los restantes, si no hay, salimos.
                if (!running.get() && registro.getCantidad(EstadoPedido.TRANSITO) == 0) {
                    break;
                }

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
        // Obtener un pedido aleatorio de TRANSITO (no lo remueve aún)
        Pedido pedido = registro.obtenerPedidoAleatorio(EstadoPedido.TRANSITO);

        if (pedido != null) {
            // Bloquear el pedido específico para procesarlo
            pedido.lock();
            try {
                // Intentar remover el pedido específico de TRANSITO.
                boolean removido = registro.removerPedido(pedido, EstadoPedido.TRANSITO); // <-- MODIFICADO: Usar método de RegistroPedidos

                if (removido) {
                    // El pedido fue exitosamente "adquirido" por este hilo Entregador.
                    // Simular intento de entrega
                    boolean entregaExitosa = ThreadLocalRandom.current().nextDouble() >= probabilidadFallo;

                    if (entregaExitosa) {
                        // Mover a ENTREGADO
                        registro.agregarPedido(pedido, EstadoPedido.ENTREGADO);
                        System.out.println(Thread.currentThread().getName() + " entregó exitosamente " + pedido);
                    } else {
                        // Mover a FALLIDO
                        registro.agregarPedido(pedido, EstadoPedido.FALLIDO);
                        System.out.println(Thread.currentThread().getName() + " falló la entrega de " + pedido);
                    }
                }
                // Si removido es false, otro hilo Entregador ya procesó este pedido. No hacemos nada.

            } finally {
                // Siempre liberar el lock del pedido
                pedido.unlock();
            }
        }
        // Si pedido es null, no hay nada en TRANSITO para entregar en este momento.
    }


    private void aplicarDemora() throws InterruptedException {
        // ... (igual que en las otras clases)
        int variacion = (variacionDemoraMs > 0)
                ? ThreadLocalRandom.current().nextInt(-variacionDemoraMs, variacionDemoraMs + 1)
                : 0;
        Thread.sleep(Math.max(0, demoraBaseMs + variacion));
    }
}