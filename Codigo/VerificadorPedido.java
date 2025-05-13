import java.util.Random;
public class VerificadorPedido implements Runnable {
    private final Random random = new Random();
    private final RegistroPedidos registro;
    private static volatile boolean running;
    private final int demoraVerificador;
    private static final int PROBABILIDAD_EXITO = 95; //95% por consigna

    public VerificadorPedido(RegistroPedidos registro, int demoraBaseMs, boolean running) {
        this.registro = registro;
        this.demoraVerificador = demoraBaseMs;
        VerificadorPedido.running = running;
    }

    private void procesarPedido() { // Propagar excepción
        // Obtener un pedido aleatorio del estado ENTREGADO
        Pedido pedido = registro.obtenerPedidoAleatorio(EstadoPedido.ENTREGADO);

        if (pedido == null) {
            return;
        }

        // Intentar remover el pedido específico de la lista ENTREGADO.
        boolean removido = registro.removerPedido(pedido, EstadoPedido.ENTREGADO);
        if (!removido) {
            return; //Esto significa que el pedido que estaba verificando ya fue removido anteriormente por otro verificador
        }

        // El pedido fue adquirido por este hilo Verificador.
        boolean exitoso = random.nextInt(100) < PROBABILIDAD_EXITO;
        int casilleroId = pedido.getCasilleroId(); // Obtener ID del casillero

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
            // Bucle principal
            while (isRunning() || registro.getCantidad(EstadoPedido.ENTREGADO) > 0) {
                procesarPedido(); // Puede lanzar MatrizLlenaException
                aplicarDemora();

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " interrumpido.");
        }
        System.out.println(Thread.currentThread().getName() + " terminado.");
    }
}