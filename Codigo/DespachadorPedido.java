// DespachadorPedido.java
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class DespachadorPedido implements Runnable {
    private final Random random = new Random();
    private final RegistroPedidos registro;
    private final MatrizCasilleros matriz;
    private final AtomicBoolean running;
    private final int demoraDespachador;
    private final int variacionDemoraMs;
    private static final int PROBABILIDAD_EXITO = 85; // 85% de éxito

    public DespachadorPedido(RegistroPedidos registro, MatrizCasilleros matriz,
                             int demoraDespachador, int variacionDemoraMs, AtomicBoolean running) {
        this.registro = registro;
        this.matriz = matriz;
        this.demoraDespachador = demoraDespachador;
        this.variacionDemoraMs = variacionDemoraMs;
        this.running = running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            while (running.get() || registro.getCantidadEnPreparacion()>0) {
                Pedido pedido = registro.obtenerPedidoPreparacionAleatorio();
                if (pedido != null) {
                    despacharPedido(pedido);
                }
                sleepRandom();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " interrumpido.");
        }
        System.out.println(Thread.currentThread().getName() + " terminado.");
    }

    private void despacharPedido(Pedido pedido) {
        pedido.lock();
        try {
            int casilleroId = pedido.getCasilleroId();
            boolean exitoso = random.nextInt(100) < PROBABILIDAD_EXITO;

            // Remover de preparación primero
            registro.removerDePreparacion(pedido);

            if (exitoso) {
                // Caso exitoso
                matriz.liberarCasillero(casilleroId);
                registro.agregarATransito(pedido);
            } else {
                // Caso fallido
                matriz.marcarFueraDeServicio(casilleroId);
                registro.agregarAFallidos(pedido);
            }
        } finally {
            pedido.unlock();
        }
    }

    private void sleepRandom() throws InterruptedException {
        int variacion = 0;
        if (variacionDemoraMs > 0) {
            variacion = random.nextInt(variacionDemoraMs * 2 + 1) - variacionDemoraMs;
        }
        int demora = Math.max(0, demoraDespachador+variacion);
        Thread.sleep(demora); //Puede lanzar InterruptedException
    }
}