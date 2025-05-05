import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

// Asegúrate de tener las otras clases (Pedido, Casillero, MatrizCasilleros, etc.)
// y la excepción personalizada NoAvailableLockersException en tu proyecto.

public class Main {

    // --- Configuración ---
    private static final int NUM_FILAS_CASILLEROS = 10;
    private static final int NUM_COLS_CASILLEROS = 20;
    private static final int TOTAL_PEDIDOS_A_PROCESAR = 500;

    private static final int NUM_PREPARADORES = 3;
    private static final int NUM_DESPACHADORES = 2;
    private static final int NUM_ENTREGADORES = 3;
    private static final int NUM_VERIFICADORES = 2;

    // Tiempos de demora base (ms)
    private static final int DEMORA_BASE_PREPARADOR = 25;
    private static final int DEMORA_BASE_DESPACHADOR = 65;
    private static final int DEMORA_BASE_ENTREGADOR = 45;
    private static final int DEMORA_BASE_VERIFICADOR = 75;

    // Variación aleatoria de la demora (+/- ms)
    private static final int VARIACION_DEMORA = 10;

    // Probabilidades
    // *** AJUSTA ESTA PROBABILIDAD A UN VALOR BAJO (ej: 0.1) PARA PROBAR EL CASO DE FALLO DE CASILLEROS ***
    private static final double PROB_DESPACHO_OK = 0.85; // Ejemplo: 0.1 para forzar fallos
    private static final double PROB_ENTREGA_OK = 0.90;
    private static final double PROB_VERIFICACION_OK = 0.95;

    // Logger
    private static final long INTERVALO_LOG_MS = 200;
    private static final String ARCHIVO_LOG = "simulacion_logistica.log";

    public static void main(String[] args) {
        System.out.println("Iniciando simulación logística...");
        long startTime = System.nanoTime();

        // 1. Crear Recursos Compartidos
        // Asegúrate de que MatrizCasilleros maneje la NoAvailableLockersException
        MatrizCasilleros matriz = new MatrizCasilleros(NUM_FILAS_CASILLEROS, NUM_COLS_CASILLEROS);
        RegistroPedidos registro = new RegistroPedidos();
        // Asegúrate de que LoggerSistema tenga el método logFinal modificado
        LoggerSistema logger = new LoggerSistema(registro, ARCHIVO_LOG);

        // 2. Iniciar Logger Periódico
        logger.iniciarLogPeriodico(INTERVALO_LOG_MS);

        // 3. Crear y Ejecutar Hilos usando ExecutorService
        AtomicBoolean running = new AtomicBoolean(true);
        // *** NUEVO FLAG: Indica si la detención fue causada por falta de casilleros ***
        AtomicBoolean stoppedDueToNoLockers = new AtomicBoolean(false);
        // Usamos un ExecutorService para gestionar los hilos
        ExecutorService executor = Executors.newCachedThreadPool();

        // Lanzar Hilos Preparadores
        // *** Pasar el nuevo flag al constructor de PreparadorPedido ***
        for (int i = 0; i < NUM_PREPARADORES; i++) {
            executor.submit(new PreparadorPedido(registro, matriz, DEMORA_BASE_PREPARADOR, VARIACION_DEMORA, TOTAL_PEDIDOS_A_PROCESAR, running, stoppedDueToNoLockers));
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
        // Esperar mientras la simulación esté 'running' (no detenida por falta de casilleros o interrupción)
        // Y (no se hayan generado todos los pedidos O aún queden pedidos en las colas intermedias)
        // *** Condición del bucle modificada para incluir running.get() ***
        while (running.get() && (registro.getCantidadPreparados() < TOTAL_PEDIDOS_A_PROCESAR || !registro.todasLasColasProcesamientoVacias())) {
            try {
                 // Comprobación periódica para no consumir CPU excesivamente
                Thread.sleep(100); // Aumentado ligeramente el sleep para reducir carga
                // Opcional: Añadir un tiempo máximo de espera para evitar bloqueos infinitos
                /*
                 System.out.printf("Estado: Run=%s, Prep=%d/%d, PrepQ=%d, TranQ=%d, EntrQ=%d, Fall=%d, Verif=%d%n",
                      running.get(), registro.getCantidadPreparados(), TOTAL_PEDIDOS_A_PROCESAR,
                      registro.getCantidadEnPreparacion(), registro.getCantidadEnTransito(), registro.getCantidadEntregados(),
                      registro.getCantidadFallidos(), registro.getCantidadVerificados());
                */
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Hilo principal interrumpido durante espera.");
                running.set(false); // Señalizar detención si el main se interrumpe
                break; // Salir del bucle de espera
            }
        }

        // 5. Señalizar a los hilos que deben detenerse (si no lo hizo ya el Preparador)
        // Determinar por qué salimos del bucle while
        if (stoppedDueToNoLockers.get()) {
             System.out.println("Detención señalizada debido a la falta de casilleros disponibles.");
             // 'running' ya fue puesto a false por el PreparadorPedido
        } else if (!running.get()) {
             // 'running' fue puesto a false por otra razón (ej: interrupción del main)
             System.out.println("Detención señalizada por interrupción u otra causa externa.");
        } else {
            // El bucle terminó porque se cumplió la condición de finalización normal
             System.out.println("Objetivo de pedidos alcanzado o colas intermedias vacías. Señalizando detención normal...");
             running.set(false); // Asegura que todos los hilos que dependen de 'running' terminen
        }

        // 6. Detener el ExecutorService y esperar finalización de tareas en curso
        System.out.println("Iniciando apagado del ExecutorService...");
        executor.shutdown(); // No acepta nuevas tareas, permite finalizar las actuales
        try {
            // Esperar un tiempo prudencial para que los hilos terminen limpiamente
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) { // Espera hasta 1 minuto
                System.err.println("Los hilos no terminaron en el tiempo esperado (60s), forzando detención...");
                executor.shutdownNow(); // Intenta interrumpir los hilos en ejecución
                 if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                     System.err.println("ExecutorService no pudo ser terminado forzosamente.");
                 }
            } else {
                System.out.println("Todos los hilos han terminado correctamente.");
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
        logger.detenerLogPeriodico(); // Detener el log periódico antes del log final
        // *** Pasar el flag 'stoppedDueToNoLockers' al método logFinal ***
        logger.logFinal(matriz, durationMs, stoppedDueToNoLockers.get());

        System.out.println("Log guardado en: " + ARCHIVO_LOG);

        // Reporte final en consola para rápida verificación
        int totalVerificados = registro.getCantidadVerificados();
        int totalFallidos = registro.getCantidadFallidos();
        int totalGenerados = registro.getCantidadPreparados(); // Cuántos se intentaron crear
        int totalFinalProcesados = totalVerificados + totalFallidos;
        int enPreparacion = registro.getCantidadEnPreparacion();
        int enTransito = registro.getCantidadEnTransito();
        int enEntregados = registro.getCantidadEntregados();
        int restantesEnColas = enPreparacion + enTransito + enEntregados;

        System.out.printf("--- Resumen Final Consola ---%n");
        System.out.printf("Pedidos Generados Inicialmente: %d%n", totalGenerados);
        System.out.printf("Pedidos Verificados con Éxito: %d%n", totalVerificados);
        System.out.printf("Pedidos Fallidos: %d%n", totalFallidos);
        System.out.printf("Total Finalizados (Verificados + Fallidos): %d%n", totalFinalProcesados);
        System.out.printf("Pedidos restantes en colas (Prep: %d, Tran: %d, Entr: %d): %d%n",
                           enPreparacion, enTransito, enEntregados, restantesEnColas);

        // Verificar si el total coincide (debug)
        if (totalGenerados != totalFinalProcesados + restantesEnColas) {
             System.err.printf("ADVERTENCIA: El balance de pedidos no cuadra! Generados (%d) != Finalizados (%d) + Restantes (%d). Suma = %d%n",
                                totalGenerados, totalFinalProcesados, restantesEnColas, (totalFinalProcesados + restantesEnColas));
        } else {
             System.out.printf("Confirmación: Balance de pedidos correcto (%d generados = %d finalizados + %d restantes).%n",
                                totalGenerados, totalFinalProcesados, restantesEnColas);
        }
         System.out.printf("Tiempo Total Ejecución: %.3f segundos%n", durationMs / 1000.0);
         System.out.printf("----------------------------%n");

         if (stoppedDueToNoLockers.get()){
             System.out.println("*** La simulación terminó prematuramente por falta de casilleros. ***");
         }
    }
}