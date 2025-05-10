import java.util.Random;

public class EntregadorPedido implements Runnable {
    private final Random random = new Random();
    private final RegistroPedidos registro;
    private static volatile boolean running;        //Siendo volatile es visible para todos los hilos
    private final int demoraEntregador;
    private final int variacionDemoraMs;
    private static final int PROBABILIDAD_EXITO = 90; //90% por consigna

    public EntregadorPedido(RegistroPedidos registro, int demoraBaseMs, int variacionDemoraMs, boolean estadoInicial) {
        this.registro = registro;
        this.demoraEntregador = demoraBaseMs;
        this.variacionDemoraMs = variacionDemoraMs;
        EntregadorPedido.running = estadoInicial;       //Estado inicial de simulación
    }

    private void procesarPedido() {
        // Obtener un pedido aleatorio de TRANSITO (no lo remueve aún)
        Pedido pedido = registro.obtenerPedidoAleatorio(EstadoPedido.TRANSITO);

        if (pedido != null) {
            // Bloquear el pedido específico para procesarlo
            try {
                // Intentar remover el pedido específico de TRANSITO.
                boolean removido = registro.removerPedido(pedido, EstadoPedido.TRANSITO);

                if (removido) {
                    // El pedido fue exitosamente "adquirido" por este hilo Entregador.
                    // Verificar con 95% de éxito
                    boolean exitoso = random.nextInt(100) < PROBABILIDAD_EXITO;

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

            } catch (Exception e) {

            }
        }
    }


    private void aplicarDemora() throws InterruptedException {
        int variacion = 0;
        if (variacionDemoraMs > 0) {
            variacion = random.nextInt(variacionDemoraMs * 2 + 1) - variacionDemoraMs;
        }
        int demora = Math.max(0, demoraEntregador + variacion);
        Thread.sleep(demora);
    }

    public static void setRunning(boolean nuevoEstado) {
        EntregadorPedido.running= nuevoEstado;
    }

    public boolean isRunning() {
        return EntregadorPedido.running;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " iniciado.");
        try {
            // Bucle principal
            while (isRunning() || registro.getCantidad(EstadoPedido.TRANSITO) > 0) {
                procesarPedido();
                aplicarDemora();
            }
        } catch (InterruptedException e) { //Un hilo lo despertó mientras estaba durmiendo
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " interrumpido.");
        }
        System.out.println(Thread.currentThread().getName() + " terminado.");
    }
}