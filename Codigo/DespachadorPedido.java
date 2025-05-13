import java.util.Random;

public class DespachadorPedido implements Runnable {

    private final Random random = new Random();
    private final RegistroPedidos registro;
    private final MatrizCasilleros matriz;
    private static volatile boolean running;
    private final int demoraDespachador;
    private static final int PROBABILIDAD_EXITO = 85;

    public DespachadorPedido(RegistroPedidos registro, MatrizCasilleros matriz, int demoraBaseMs, boolean running) {
        this.registro = registro;
        this.matriz = matriz;
        this.demoraDespachador = demoraBaseMs;
        DespachadorPedido.running = running;
    }

    private void procesarPedido() {
        // Obtener Y REMOVER un pedido aleatorio que tenga un estado de PREPARACION
        // Esta operación es ahora atómica dentro de RegistroPedidos
        Pedido pedido = registro.obtenerYRemoverPedidoAleatorio(EstadoPedido.PREPARACION);

        if (pedido == null) {
            // No hay pedidos en PREPARACION o fue tomado por otro despachador
            return;
        }

        // El pedido fue adquirido y removido de PREPARACION por este hilo Despachador.
        try {
            int casilleroId = pedido.getCasilleroId();
            boolean exitoso = random.nextInt(100) < PROBABILIDAD_EXITO;
            if (exitoso) {
                matriz.liberarCasillero(casilleroId); // Casillero.liberar() es synchronized
                System.out.println(Thread.currentThread().getName() + " liberó casillero " + casilleroId + " para " + pedido);
                registro.agregarPedido(pedido, EstadoPedido.TRANSITO);
                System.out.println(Thread.currentThread().getName() + " despachó " + pedido + " a tránsito.");
            } else {
                matriz.marcarFueraDeServicio(casilleroId); // Casillero.marcarFueraDeServicio() es synchronized
                System.out.println(Thread.currentThread().getName() + " marco casillero " + casilleroId + " como FUERA DE SERVICIO " + pedido);
                registro.agregarPedido(pedido, EstadoPedido.FALLIDO);
                System.out.println(Thread.currentThread().getName() + " envió " + pedido + " a fallido.");
            }
        } catch (IllegalStateException e) {
            // Esto podría ocurrir si el casillero ya no está en el estado esperado
            // (ej. si se intentó liberar un casillero no ocupado).
            // La sincronización en Casillero ayuda a prevenir inconsistencias,
            // pero la lógica de estados debe ser robusta.
            System.err.println(Thread.currentThread().getName() + " Error de estado al procesar " + pedido + " en casillero " + pedido.getCasilleroId() + ": " + e.getMessage());
            // Decidir qué hacer con el pedido, quizás mover a FALLIDO si no se hizo ya.
            // Por ahora, solo logueamos. Podría ser necesario re-intentar o mover a fallidos.
            registro.agregarPedido(pedido, EstadoPedido.FALLIDO); // Asegurar que el pedido vaya a fallidos
             System.out.println(Thread.currentThread().getName() + " envió " + pedido + " a fallido debido a error de estado del casillero.");
        } catch (Exception e) {
            System.err.println(Thread.currentThread().getName() + " Excepción inesperada procesando " + pedido + ": " + e.getMessage());
            e.printStackTrace();
            registro.agregarPedido(pedido, EstadoPedido.FALLIDO);
            System.out.println(Thread.currentThread().getName() + " envió " + pedido + " a fallido debido a excepción inesperada.");
        }
    }

    // ... resto de la clase DespachadorPedido (aplicarDemora, setRunning, isRunning, run) sin cambios ...
    private void aplicarDemora() throws InterruptedException {
        int variacion = random.nextInt(0, demoraDespachador/2)+demoraDespachador;
        Thread.sleep(variacion);
    }

    public static void setRunning(boolean nuevoEstado) {
        DespachadorPedido.running= nuevoEstado;
    }

    public boolean isRunning() {
        return DespachadorPedido.running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            while (isRunning() || registro.getCantidad(EstadoPedido.PREPARACION) > 0) {
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