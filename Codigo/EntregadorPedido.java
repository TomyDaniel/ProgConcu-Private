
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;


public class EntregadorPedido implements Runnable {
    // ... (campos sin cambios)
    private final RegistroPedidos registro;
    private final AtomicBoolean running;
    private final int demoraBaseMs;
    private final int variacionDemoraMs;
    private static final int PROBABILIDAD_EXITO = 30; //85% por consigna

    public EntregadorPedido(RegistroPedidos registro, int demoraBaseMs, int variacionDemoraMs, AtomicBoolean running) {
        this.registro = registro;
        this.demoraBaseMs = demoraBaseMs;
        this.variacionDemoraMs = variacionDemoraMs;
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
                    boolean exitoso= ThreadLocalRandom.current().nextInt(0, 100) <= PROBABILIDAD_EXITO;

                    if (exitoso) {
                        // Mover a ENTREGADO
                        registro.agregarPedido(pedido, EstadoPedido.ENTREGADO);
                        System.out.println(Thread.currentThread().getName() + " entregó exitosamente " + pedido);
                    } else {
                        // Mover a FALLIDO
                        registro.agregarPedido(pedido, EstadoPedido.FALLIDO);
                        System.out.println(Thread.currentThread().getName() + " falló la entrega de " + pedido);
                    }
                }

            } finally {
                // Siempre liberar el lock del pedido
                pedido.unlock();
            }
        }
    }


    private void aplicarDemora() throws InterruptedException {
        // ... (igual que en las otras clases)
        int variacion = (variacionDemoraMs > 0)
                ? ThreadLocalRandom.current().nextInt(-variacionDemoraMs, variacionDemoraMs + 1)
                : 0;
        Thread.sleep(Math.max(0, demoraBaseMs + variacion));
    }
}