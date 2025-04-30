// --- Clase Principal ---
// Main.java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    // --- Configuración ---
    private static final int NUM_CASILLEROS = 200;
    private static final int TOTAL_PEDIDOS_A_PROCESAR = 500;

    private static final int NUM_PREPARADORES = 3;
    private static final int NUM_DESPACHADORES = 2;
    private static final int NUM_ENTREGADORES = 3;
    private static final int NUM_VERIFICADORES = 2;

    // Tiempos de demora base (ms) - AJUSTAR PARA OBTENER 15-30 SEGUNDOS
    private static final int DEMORA_BASE_PREPARADOR = 25; // Más rápido para generar carga
    private static final int DEMORA_BASE_DESPACHADOR = 45;
    private static final int DEMORA_BASE_ENTREGADOR = 35;
    private static final int DEMORA_BASE_VERIFICADOR = 55; // Potencial cuello de botella

    // Variación aleatoria de la demora (+/- ms)
    private static final int VARIACION_DEMORA = 10;

    // Probabilidades
    private static final double PROB_DESPACHO_OK = 0.85;
    private static final double PROB_ENTREGA_OK = 0.90;
    private static final double PROB_VERIFICACION_OK = 0.95;

    // Logger
    private static final long INTERVALO_LOG_MS = 200;
    private static final String ARCHIVO_LOG = "simulacion_logistica.log";

    public static void main(String[] args) {
        System.out.println("Iniciando simulación logística...");
        long startTime = System.nanoTime();

        // 1. Crear Recursos Compartidos
        MatrizCasilleros matriz = new MatrizCasilleros(NUM_CASILLEROS);
        RegistroPedidos registro = new RegistroPedidos();
        LoggerSistema logger = new LoggerSistema(registro, ARCHIVO_LOG);

        // 2. Iniciar Logger Periódico
        logger.iniciarLogPeriodico(INTERVALO_LOG_MS);

        // 3. Crear y Ejecutar Hilos usando ExecutorService
        //    Usamos un flag atómico para señalar cuándo detenerse.
        AtomicBoolean running = new AtomicBoolean(true);
        // Usamos un ExecutorService para gestionar los hilos más fácilmente
        ExecutorService executor = Executors.newCachedThreadPool(); // O newFixedThreadPool si prefieres limitar

        // Lanzar Hilos Preparadores
        for (int i = 0; i < NUM_PREPARADORES; i++) {
            executor.submit(new PreparadorPedido(registro, matriz, DEMORA_BASE_PREPARADOR, VARIACION_DEMORA, TOTAL_PEDIDOS_A_PROCESAR, running));
        }
        // Lanzar Hilos Despachadores
        for (int i = 0; i < NUM_DESPACHADORES; i++) {
             executor.submit(new DespachadorPedido(registro, matriz, DEMORA_BASE_DESPACHADOR, VARIACION_DEMORA, PROB_DESPACHO_OK, running));
        }
        // Lanzar Hilos Entregadores
        for (int i = 0; i < NUM_ENTREGADORES; i++) {
            executor.submit(new EntregadorPedido(registro, DEMORA_BASE_ENTREGADOR, VARIACION_DEMORA, PROB_ENTREGA_OK, running));
        }
        // Lanzar Hilos Verificadores
        for (int i = 0; i < NUM_VERIFICADORES; i++) {
            executor.submit(new VerificadorPedido(registro, DEMORA_BASE_VERIFICADOR, VARIACION_DEMORA, PROB_VERIFICACION_OK, running));
        }

        // 4. Lógica de Espera y Detención
        // Esperar hasta que todos los pedidos hayan sido generados y procesados (llegado a Fallido o Verificado)
        while (registro.getCantidadPreparados() < TOTAL_PEDIDOS_A_PROCESAR || !registro.todasLasColasProcesamientoVacias()) {
            try {
                 // Comprobación periódica para no consumir CPU excesivamente
                Thread.sleep(100);
                // Opcional: Añadir un tiempo máximo de espera para evitar bloqueos infinitos
                 // System.out.printf("Estado: Preparados=%d, PrepQ=%d, TranQ=%d, EntrQ=%d, Fall=%d, Verif=%d%n",
                 //      registro.getCantidadPreparados(), registro.getCantidadEnPreparacion(),
                 //      registro.getCantidadEnTransito(), registro.getCantidadEntregados(),
                 //      registro.getCantidadFallidos(), registro.getCantidadVerificados());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Hilo principal interrumpido durante espera.");
                running.set(false); // Señalizar detención si el main se interrumpe
                break;
            }
        }

        // 5. Señalizar a los hilos que deben detenerse (los que dependen de 'running')
        System.out.println("Todos los pedidos generados y colas intermedias vacías. Señalizando detención...");
        running.set(false);

        // 6. Detener el ExecutorService y esperar finalización de tareas en curso
        executor.shutdown(); // No acepta nuevas tareas, permite finalizar las actuales
        try {
            // Esperar un tiempo prudencial para que los hilos terminen limpiamente
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) { // Espera hasta 1 minuto
                System.err.println("Los hilos no terminaron en el tiempo esperado, forzando detención...");
                executor.shutdownNow(); // Intenta interrumpir los hilos en ejecución
            }
        } catch (InterruptedException e) {
            System.err.println("Interrumpido mientras se esperaba la finalización de los hilos.");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 7. Finalizar y Reportar
        long endTime = System.nanoTime();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        System.out.println("Simulación completada.");
        logger.detenerLogPeriodico();
        logger.logFinal(matriz, durationMs);

        System.out.println("Log guardado en: " + ARCHIVO_LOG);
         // Verificar si el total coincide (debug)
        int totalFinal = registro.getCantidadFallidos() + registro.getCantidadVerificados();
        if (totalFinal != TOTAL_PEDIDOS_A_PROCESAR) {
             System.err.printf("ADVERTENCIA: El número total de pedidos procesados (%d) no coincide con el objetivo (%d)!%n",
                                totalFinal, TOTAL_PEDIDOS_A_PROCESAR);
        } else {
             System.out.printf("Confirmación: %d pedidos procesados correctamente.%n", totalFinal);
        }
    }
}