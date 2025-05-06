import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
// import EstadoPedido; //<- Probablemente innecesario si está en el mismo paquete

public class Main {

    // --- Parámetros de Simulación (Ejemplo) ---
    private static final int NUM_PREPARADORES = 3;
    private static final int NUM_DESPACHADORES = 2;
    private static final int NUM_ENTREGADORES = 3;
    private static final int NUM_VERIFICADORES = 2;
    private static final int TOTAL_PEDIDOS_A_GENERAR = 500; // Límite de pedidos

    private static final int FILAS_MATRIZ = 10;
    private static final int COLUMNAS_MATRIZ = 20;

    private static final int DEMORA_BASE_PREP = 100;
    private static final int DEMORA_VAR_PREP = 20;
    private static final int DEMORA_BASE_DESP = 80;
    private static final int DEMORA_VAR_DESP = 15;
    private static final int DEMORA_BASE_ENT = 120;
    private static final int DEMORA_VAR_ENT = 30;
    private static final int DEMORA_BASE_VER = 50;
    private static final int DEMORA_VAR_VER = 10;

    private static final double PROBABILIDAD_FALLO_ENTREGA = 0.05; // 5%
    private static final double PROBABILIDAD_FALLO_CASILLERO = 0.02; // 2%

    private static final long INTERVALO_LOG_MS = 1000; // Log cada segundo
    private static final String LOG_FILE_PATH = "simulacion_logistica.log";
    // -----------------------------------------


    public static void main(String[] args) {
        System.out.println("Iniciando simulación de logística...");
        long startTime = System.currentTimeMillis();

        // 1. Crear componentes compartidos
        AtomicBoolean running = new AtomicBoolean(true); // Flag para detener hilos
        MatrizCasilleros matriz = new MatrizCasilleros(FILAS_MATRIZ, COLUMNAS_MATRIZ);
        RegistroPedidos registro = new RegistroPedidos(); // La creación no cambia
        LoggerSistema logger = new LoggerSistema(LOG_FILE_PATH, registro, matriz);

        // 2. Crear lista para guardar referencias a los Runnables
        List<Runnable> tareas = new ArrayList<>();
        List<Thread> hilos = new ArrayList<>(); //

        // 3. Crear Runnables (trabajadores)
        for (int i = 0; i < NUM_PREPARADORES; i++) {
            tareas.add(new PreparadorPedido(registro, matriz, DEMORA_BASE_PREP, DEMORA_VAR_PREP, TOTAL_PEDIDOS_A_GENERAR, running));
        }
        for (int i = 0; i < NUM_DESPACHADORES; i++) {
            tareas.add(new DespachadorPedido(registro, matriz, DEMORA_BASE_DESP, DEMORA_VAR_DESP, running));
        }
        for (int i = 0; i < NUM_ENTREGADORES; i++) {
            tareas.add(new EntregadorPedido(registro, DEMORA_BASE_ENT, DEMORA_VAR_ENT, PROBABILIDAD_FALLO_ENTREGA, running));
        }
        for (int i = 0; i < NUM_VERIFICADORES; i++) {
            // Pasar la matriz también al verificador
            tareas.add(new VerificadorPedido(registro, matriz, DEMORA_BASE_VER, DEMORA_VAR_VER, PROBABILIDAD_FALLO_CASILLERO, running));
        }


        // 4. Iniciar Logger
        logger.iniciarLogPeriodico(INTERVALO_LOG_MS);

        // 5. Crear e iniciar Hilos usando un ExecutorService (más moderno)
        ExecutorService executor = Executors.newFixedThreadPool(
                NUM_PREPARADORES + NUM_DESPACHADORES + NUM_ENTREGADORES + NUM_VERIFICADORES);

        for (Runnable tarea : tareas) {
            executor.submit(tarea); // Envía la tarea al pool de hilos
        }


        // 6. Lógica de espera y terminación
        System.out.println("Esperando finalización de la generación y procesamiento...");
        while (running.get()) {
            boolean preparacionCompleta = registro.getTotalPedidosGenerados() >= TOTAL_PEDIDOS_A_GENERAR;
            // Asegúrate que EstadoPedido está accesible (mismo paquete o importado)
            boolean colasIntermediasVacias = registro.getCantidad(EstadoPedido.PREPARACION) == 0 &&
                    registro.getCantidad(EstadoPedido.TRANSITO) == 0 &&
                    registro.getCantidad(EstadoPedido.ENTREGADO) == 0;

            if (preparacionCompleta && colasIntermediasVacias) {
                running.set(false);
                System.out.println("Condición de parada alcanzada. Señalando a hilos para terminar...");
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running.set(false);
                    System.err.println("Hilo principal interrumpido. Terminando simulación.");
                }
            }
        }


        // 7. Detener ExecutorService y esperar a que los hilos terminen
        System.out.println("Deteniendo ExecutorService...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("Los hilos no terminaron a tiempo. Forzando detención...");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 8. Detener Logger
        logger.detenerLogPeriodico();

        // 9. Calcular tiempo y mostrar resultados finales
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("\n--- Simulación Finalizada ---");
        System.out.printf("Tiempo total de ejecución: %.2f segundos\n", duration / 1000.0);
        System.out.println("--- Estadísticas Finales de Pedidos ---");

        System.out.println("Pedidos generados totales: " + registro.getTotalPedidosGenerados());
        // Asegúrate que EstadoPedido está accesible (mismo paquete o importado)
        System.out.println("Pedidos en Preparación (al final): " + registro.getCantidad(EstadoPedido.PREPARACION));
        System.out.println("Pedidos en Tránsito (al final): " + registro.getCantidad(EstadoPedido.TRANSITO));
        System.out.println("Pedidos Entregados (pendientes de verificación): " + registro.getCantidad(EstadoPedido.ENTREGADO));
        System.out.println("Pedidos Verificados exitosamente: " + registro.getCantidad(EstadoPedido.VERIFICADO));
        System.out.println("Pedidos con entrega Fallida: " + registro.getCantidad(EstadoPedido.FALLIDO));
        System.out.println("--- Estadísticas Finales de Casilleros ---");
        System.out.println("Casilleros Fuera de Servicio: " + matriz.getSizeFueraDeServicio());


        logger.logFinal(duration);
        System.out.println("Log final guardado en: " + LOG_FILE_PATH);
    }
}