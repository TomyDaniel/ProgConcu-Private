import java.util.Random; // Aunque no se use directamente, Random sí se usa en RegistroPedidos. ThreadLocalRandom sí se usa aquí.
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
// Eliminar el import estático de EstadoPedido, no es necesario
// import static tp1.ProgConcu.Codigo.RegistroPedidos.EstadoPedido; // Ajusta la ruta <-- ELIMINAR

// No necesitas importar MatrizLlenaException, Pedido, RegistroPedidos, MatrizCasilleros si están en el mismo paquete.
// Asegúrate de que EstadoPedido también esté en el mismo paquete (parece que sí).

public class VerificadorPedido implements Runnable {
    // ... (campos sin cambios)
    private final RegistroPedidos registro;
    private final MatrizCasilleros matriz; // Necesaria para marcar fuera de servicio
    private final AtomicBoolean running;
    private final int demoraBaseMs;
    private final int variacionDemoraMs;
    private final double probFalloCasillero; // Probabilidad de marcar casillero como malo

    public VerificadorPedido(RegistroPedidos registro, MatrizCasilleros matriz, int demoraBaseMs, int variacionDemoraMs, double probFalloCasillero, AtomicBoolean running) {
        this.registro = registro;
        this.matriz = matriz;
        this.demoraBaseMs = demoraBaseMs;
        this.variacionDemoraMs = variacionDemoraMs;
        this.probFalloCasillero = probFalloCasillero;
        this.running = running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            // Bucle principal: sigue mientras 'running' sea true O mientras queden pedidos por verificar
            while (running.get() || registro.getCantidad(EstadoPedido.ENTREGADO) > 0) {
                // Si !running.get(), solo procesamos los restantes, si no hay, salimos.
                if (!running.get() && registro.getCantidad(EstadoPedido.ENTREGADO) == 0) {
                    break;
                }

                procesarPedido(); // Puede lanzar MatrizLlenaException
                aplicarDemora();

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " interrumpido.");
        } catch (MatrizLlenaException e) { // Capturar la excepción si la matriz se llena
            System.err.println(Thread.currentThread().getName() + ": " + e.getMessage());
            running.set(false); // Detener la simulación si la matriz está llena
        }
        System.out.println(Thread.currentThread().getName() + " terminado.");
    }

    private void procesarPedido() throws MatrizLlenaException { // Propagar excepción
        // Obtener un pedido aleatorio del estado ENTREGADO
        // OJO: obtenerPedidoAleatorio NO lo saca de la lista. Hay que intentar removerlo después.
        Pedido pedido = registro.obtenerPedidoAleatorio(EstadoPedido.ENTREGADO);

        if (pedido != null) {
            // Bloquear el pedido específico para procesarlo y evitar condiciones de carrera
            // si otro Verificador lo eligiera al mismo tiempo.
            pedido.lock();
            try {
                // Intentar remover el pedido específico de la lista ENTREGADO.
                // Es posible que otro hilo lo haya removido entre obtenerPedidoAleatorio y aquí.
                boolean removido = registro.removerPedido(pedido, EstadoPedido.ENTREGADO); // <-- MODIFICADO: Usar método de RegistroPedidos

                if (removido) {
                    // El pedido fue exitosamente "adquirido" por este hilo Verificador.
                    boolean casilleroOk = ThreadLocalRandom.current().nextDouble() >= probFalloCasillero;
                    int casilleroId = pedido.getCasilleroId(); // Obtener ID antes de decidir qué hacer

                    if (casilleroOk) {
                        // Casillero OK, mover a VERIFICADO
                        registro.agregarPedido(pedido, EstadoPedido.VERIFICADO);
                        System.out.println(Thread.currentThread().getName() + " verificó OK " + pedido + " (Casillero: " + casilleroId + ")");
                    } else {
                        // Casillero FALLÓ la verificación
                        if (casilleroId != -1) {
                            // Si tenía casillero, marcarlo como fuera de servicio
                            matriz.marcarFueraDeServicio(casilleroId);
                            // El pedido fallido no se re-agrega a ninguna lista (queda "perdido" o implícitamente descartado)
                            System.out.println(Thread.currentThread().getName() + " verificó y marcó casillero " + casilleroId + " FUERA DE SERVICIO para " + pedido);
                            // Verificar si la matriz está llena después de marcar
                            matriz.verificarEstadoCritico(); // Puede lanzar MatrizLlenaException
                        } else {
                            // Caso raro: estaba ENTREGADO pero sin casillero asignado?
                            System.out.println(Thread.currentThread().getName() + " intentó verificar " + pedido + " (fallo casillero) pero no tenía casillero asignado.");
                            // Decisión: ¿Qué hacer con este pedido? Lo movemos a VERIFICADO igual para sacarlo del flujo? O a FALLIDO?
                            // Moviéndolo a VERIFICADO como antes, aunque podría ser confuso.
                            registro.agregarPedido(pedido, EstadoPedido.VERIFICADO);
                        }
                    }
                }
                // Si removido es false, significa que otro hilo Verificador ya procesó este pedido. No hacemos nada.

            } finally {
                // Siempre liberar el lock del pedido, incluso si hubo excepciones (excepto MatrizLlenaException que se captura fuera)
                pedido.unlock();
            }
        }
        // Si pedido es null, no hay nada en ENTREGADO para verificar en este momento.
    }


    private void aplicarDemora() throws InterruptedException {
        // ... (sin cambios)
        int variacion = (variacionDemoraMs > 0)
                ? ThreadLocalRandom.current().nextInt(-variacionDemoraMs, variacionDemoraMs + 1)
                : 0;
        Thread.sleep(Math.max(0, demoraBaseMs + variacion));
    }
}