import java.util.Random;

public class PreparadorPedido implements Runnable {
    private final Random random = new Random();
    private final RegistroPedidos registro;
    private final MatrizCasilleros matriz;
    private static volatile boolean running;
    private final int demoraPreparador;
    private final int totalPedidosAGenerar;

    public PreparadorPedido(RegistroPedidos registro, MatrizCasilleros matriz, int demoraPreparador, int totalPedidosAGenerar,boolean  running) {
        this.registro = registro;
        this.matriz = matriz;
        this.demoraPreparador = demoraPreparador;
        this.totalPedidosAGenerar = totalPedidosAGenerar;
        PreparadorPedido.running = running;
    }

    private void aplicarDemora() throws InterruptedException {
        int variacion = random.nextInt(0, demoraPreparador/2)+demoraPreparador;
        Thread.sleep(variacion);
    }

    private void procesarPedido() {
        if (registro.getTotalPedidosGenerados() >= totalPedidosAGenerar) {
            return; // No generar más si ya se alcanzó el límite
        }

        int casilleroId = matriz.ocuparCasilleroAleatorio();//Casilleroid toma un valor aleatorio disponible entre 0 y 200

        if (casilleroId == -1) {
            System.out.println(Thread.currentThread().getName() + " no encontró casillero disponible.");
            return;
        }

        // Luego de ocupar un casillero se alcanzó el límite de pedidos, por lo tanto, lo debe liberar
        if (registro.getTotalPedidosGenerados() >= totalPedidosAGenerar) {
            System.out.println(Thread.currentThread().getName() + " ocupó casillero " + casilleroId + " pero se alcanzó el límite antes de crear el pedido. Liberando...");
            matriz.liberarCasillero(casilleroId);
            return;
        }

        Pedido nuevoPedido = new Pedido();
        nuevoPedido.asignarCasillero(casilleroId);
        registro.agregarPedido(nuevoPedido, EstadoPedido.PREPARACION);
        registro.incrementarTotalGenerados(); //
        System.out.println(Thread.currentThread().getName() + " preparó " + nuevoPedido + " en casillero " + casilleroId + " (Total generados: " + registro.getTotalPedidosGenerados() + ")");

    }



    public static void setRunning(boolean nuevoEstado) {
        PreparadorPedido.running= nuevoEstado;
    }

    public boolean isRunning() {
        return PreparadorPedido.running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");

        if (registro.getTotalPedidosGenerados() >= totalPedidosAGenerar) {
            System.out.println(Thread.currentThread().getName() + " terminó porque se alcanzó el límite de pedidos.");
        } else {
            System.out.println(Thread.currentThread().getName() + " terminado.");
        }

        try {
            while (isRunning() && registro.getTotalPedidosGenerados() < totalPedidosAGenerar) {
                procesarPedido(); // Crear y agregar un pedido
                aplicarDemora();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " interrumpido.");
        }

    }
}