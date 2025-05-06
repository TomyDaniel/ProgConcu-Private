import java.util.Random; // Usado indirectamente o potencialmente por ThreadLocalRandom
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
// No se necesita import para EstadoPedido, Pedido, RegistroPedidos, MatrizCasilleros si están en el mismo paquete

public class DespachadorPedido implements Runnable {
    // ... (campos sin cambios)
    private final RegistroPedidos registro;
    private final MatrizCasilleros matriz; // Necesaria para liberar casillero
    private final AtomicBoolean running;
    private final int demoraBaseMs;
    private final int variacionDemoraMs;

    public DespachadorPedido(RegistroPedidos registro, MatrizCasilleros matriz, int demoraBaseMs, int variacionDemoraMs, AtomicBoolean running) {
        this.registro = registro;
        this.matriz = matriz;
        this.demoraBaseMs = demoraBaseMs;
        this.variacionDemoraMs = variacionDemoraMs;
        this.running = running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            // Bucle principal: sigue mientras 'running' sea true O mientras queden pedidos por despachar
            while (running.get() || registro.getCantidad(EstadoPedido.PREPARACION) > 0) {
                // Si !running.get(), solo procesamos los restantes, si no hay, salimos.
                if (!running.get() && registro.getCantidad(EstadoPedido.PREPARACION) == 0) {
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
        // Obtener un pedido aleatorio de PREPARACION (no lo remueve aún)
        Pedido pedido = registro.obtenerPedidoAleatorio(EstadoPedido.PREPARACION);

        if (pedido != null) {
            // Bloquear el pedido específico para procesarlo
            pedido.lock();
            try {
                // Intentar remover el pedido específico de PREPARACION.
                // Es posible que otro hilo lo haya removido entre obtenerPedidoAleatorio y aquí.
                boolean removido = registro.removerPedido(pedido, EstadoPedido.PREPARACION); // <-- MODIFICADO: Usar método de RegistroPedidos

                if (removido) {
                    // El pedido fue exitosamente "adquirido" por este hilo Despachador.
                    int casilleroId = pedido.getCasilleroId();
                    // Liberar el casillero asociado al pedido
                    if (casilleroId != -1) {
                        matriz.liberarCasillero(casilleroId);
                        System.out.println(Thread.currentThread().getName() + " liberó casillero " + casilleroId + " para " + pedido);
                        // Considerar si se debe quitar el ID del casillero del pedido ahora que está libre
                        // pedido.asignarCasillero(-1); // Opcional, depende de si se reutiliza el campo
                    } else {
                        // Esto no debería ocurrir si los Preparadores siempre asignan casillero
                        System.out.println(Thread.currentThread().getName() + " despachó " + pedido + " pero no tenía casillero asignado (?)");
                    }
                    // Mover a tránsito
                    registro.agregarPedido(pedido, EstadoPedido.TRANSITO);
                    System.out.println(Thread.currentThread().getName() + " despachó " + pedido + " a tránsito.");
                }
                // Si removido es false, otro hilo Despachador ya procesó este pedido. No hacemos nada.

            } finally {
                // Siempre liberar el lock del pedido
                pedido.unlock();
            }
        }
        // Si pedido es null, no hay nada en PREPARACION para despachar en este momento.
    }


    private void aplicarDemora() throws InterruptedException {
        // ... (igual que en PreparadorPedido)
        int variacion = (variacionDemoraMs > 0)
                ? ThreadLocalRandom.current().nextInt(-variacionDemoraMs, variacionDemoraMs + 1)
                : 0;
        Thread.sleep(Math.max(0, demoraBaseMs + variacion));
    }
}