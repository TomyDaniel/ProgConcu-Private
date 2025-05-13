import java.util.Random;

public class VerificadorPedido implements Runnable {
    private final Random random = new Random();
    private final RegistroPedidos registro;
    private static volatile boolean running;
    private final int demoraVerificador;
    private static final int PROBABILIDAD_EXITO = 95;

    public VerificadorPedido(RegistroPedidos registro, int demoraBaseMs, boolean running) {
        this.registro = registro;
        this.demoraVerificador = demoraBaseMs;
        VerificadorPedido.running = running;
    }

    private void procesarPedido() {
        // Obtener Y REMOVER un pedido aleatorio del estado ENTREGADO
        Pedido pedido = registro.obtenerYRemoverPedidoAleatorio(EstadoPedido.ENTREGADO);

        if (pedido == null) {
            // No hay pedidos en ENTREGADO o fue tomado por otro verificador
            return;
        }

        // El pedido fue adquirido y removido de ENTREGADO por este hilo Verificador.
        boolean exitoso = random.nextInt(100) < PROBABILIDAD_EXITO;
        int casilleroId = pedido.getCasilleroId();

        if (exitoso) {
            registro.agregarPedido(pedido, EstadoPedido.VERIFICADO);
            System.out.println(Thread.currentThread().getName() + " verificó OK " + pedido + " (Casillero: " + casilleroId + ")");
        } else {
            registro.agregarPedido(pedido, EstadoPedido.FALLIDO);
            System.out.println(Thread.currentThread().getName() + " verificó FAIL " + pedido + " (Casillero: " + casilleroId + ")");
        }
    }

    // ... resto de la clase VerificadorPedido (aplicarDemora, setRunning, isRunning, run) sin cambios ...
    private void aplicarDemora() throws InterruptedException {
        int variacion = random.nextInt(0, demoraVerificador/2)+demoraVerificador;
        Thread.sleep(variacion);
    }

    public static void setRunning(boolean nuevoEstado) {
        VerificadorPedido.running= nuevoEstado;
    }

    public boolean isRunning() {
        return VerificadorPedido.running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            while (isRunning() || registro.getCantidad(EstadoPedido.ENTREGADO) > 0) {
                procesarPedido();
                aplicarDemora();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " interrumpido.");
        }
        System.out.println(Thread.currentThread().getName() + " terminado.");
    }
}