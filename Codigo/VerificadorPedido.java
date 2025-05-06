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
    private final AtomicBoolean running;
    private final int demoraBaseMs;
    private final int variacionDemoraMs;
    private static final int PROBABILIDAD_EXITO = 30; //95% por consigna

    public VerificadorPedido(RegistroPedidos registro, int demoraBaseMs, int variacionDemoraMs, AtomicBoolean running) {
        this.registro = registro;
        this.demoraBaseMs = demoraBaseMs;
        this.variacionDemoraMs = variacionDemoraMs;
        this.running = running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            // Bucle principal: sigue mientras 'running' sea true O mientras queden pedidos por verificar
            while (running.get() || registro.getCantidad(EstadoPedido.ENTREGADO) > 0) {
                //Verificacion por si algun hilo lo vuelve cero en el momento que entra
                if (!running.get() && registro.getCantidad(EstadoPedido.ENTREGADO) == 0) {
                    break;
                }
                procesarPedido(); // Puede lanzar MatrizLlenaException
                aplicarDemora();

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " interrumpido.");
        }
        System.out.println(Thread.currentThread().getName() + " terminado.");
    }

    private void procesarPedido() throws MatrizLlenaException { // Propagar excepción
        // Obtener un pedido aleatorio del estado ENTREGADO
        Pedido pedido = registro.obtenerPedidoAleatorio(EstadoPedido.ENTREGADO);

        if (pedido != null) {
            pedido.lock();
            try {
                // Intentar remover el pedido específico de la lista ENTREGADO.
                boolean removido = registro.removerPedido(pedido, EstadoPedido.ENTREGADO);

                if (removido) {
                    // El pedido fue exitosamente "adquirido" por este hilo Verificador.
                    boolean exitoso= ThreadLocalRandom.current().nextInt(0, 100) <= PROBABILIDAD_EXITO;
                    int casilleroId = pedido.getCasilleroId(); // Obtener ID antes de decidir qué hacer

                    if (exitoso) {
                        // Casillero OK, mover a VERIFICADO
                        registro.agregarPedido(pedido, EstadoPedido.VERIFICADO);
                        System.out.println(Thread.currentThread().getName() + " verificó OK " + pedido + " (Casillero: " + casilleroId + ")");
                    } else {
                        // Casillero FAIL, mover a FALLIDO
                        registro.agregarPedido(pedido, EstadoPedido.FALLIDO);
                        System.out.println(Thread.currentThread().getName() + " verificó FAIL " + pedido + " (Casillero: " + casilleroId + ")");

                    }
                }

            } finally {
                pedido.unlock();
            }
        }
    }


    private void aplicarDemora() throws InterruptedException {
        int variacion = (variacionDemoraMs > 0)
                ? ThreadLocalRandom.current().nextInt(-variacionDemoraMs, variacionDemoraMs + 1)
                : 0;
        Thread.sleep(Math.max(0, demoraBaseMs + variacion));
    }
}