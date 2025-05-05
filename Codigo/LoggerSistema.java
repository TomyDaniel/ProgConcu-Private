import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gestiona el logging periódico y final de la simulación.
 * Escribe los logs en un archivo especificado.
 */
public class LoggerSistema {
    private final RegistroPedidos registro;
    private final String archivoLog;
    private ScheduledExecutorService scheduler;
    private PrintWriter writer;
    // Formato de timestamp para los logs
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Constructor del LoggerSistema.
     * @param registro El objeto RegistroPedidos para obtener contadores.
     * @param archivoLog El nombre del archivo donde se guardarán los logs.
     */
    public LoggerSistema(RegistroPedidos registro, String archivoLog) {
        this.registro = registro;
        this.archivoLog = archivoLog;
    }

    /**
     * Inicia el servicio de logging periódico.
     * Crea el archivo de log (o añade si ya existe) y programa una tarea
     * que escribe el estado de los contadores principales a intervalos regulares.
     * @param intervaloMs El intervalo en milisegundos entre cada escritura de log.
     */
    public void iniciarLogPeriodico(long intervaloMs) {
        try {
            // PrintWriter con append=true y autoflush=true
            writer = new PrintWriter(new FileWriter(archivoLog, true), true);
            // Scheduler para ejecutar la tarea periódicamente
            scheduler = Executors.newSingleThreadScheduledExecutor();

            // Tarea a ejecutar periódicamente
            Runnable tareaLog = () -> {
                // Evita NullPointerException si el registro no está listo (poco probable aquí)
                if (registro == null) return;

                String timestamp = dtf.format(LocalDateTime.now());
                int fallidos = registro.getCantidadFallidos();
                int verificados = registro.getCantidadVerificados();
                // Añadir estado de colas intermedias para más detalle
                int prepQ = registro.getCantidadEnPreparacion();
                int tranQ = registro.getCantidadEnTransito();
                int entrQ = registro.getCantidadEntregados();
                int generados = registro.getCantidadPreparados(); // Total generados hasta ahora

                // Escribir línea de log formateada
                writer.println(String.format("%s - Generados:%d | Verificados:%d | Fallidos:%d",
                                timestamp, generados, verificados, fallidos));
            };

            // Programar la tarea para que se ejecute ahora y luego cada intervaloMs
            scheduler.scheduleAtFixedRate(tareaLog, 0, intervaloMs, TimeUnit.MILLISECONDS);
            logMensaje("Logger periódico iniciado. Escribiendo en: " + archivoLog + " cada " + intervaloMs + " ms.");

        } catch (IOException e) {
            System.err.println("Error Crítico al iniciar el logger de archivo: " + e.getMessage());
            System.err.println("Los logs periódicos NO se guardarán en archivo.");
            if (writer != null) {
                writer.close(); // Intentar cerrar si se abrió parcialmente
            }
             writer = null; // Asegurar que no se intente usar
             scheduler = null; // No iniciar si falla el archivo
        } catch (Exception e) {
             System.err.println("Error inesperado al iniciar el logger periódico: " + e.getMessage());
             scheduler = null;
             if (writer != null) writer.close();
             writer = null;
        }
    }

    /**
     * Escribe un mensaje puntual en el archivo de log (si está disponible)
     * o en la consola como fallback.
     * @param mensaje El mensaje a loguear.
     */
    public void logMensaje(String mensaje) {
        if (writer != null) {
             String timestamp = dtf.format(LocalDateTime.now());
             // Escribe el mensaje precedido del timestamp
             writer.println(timestamp + " - " + mensaje);
             // El autoflush debería manejarlo, pero un flush explícito puede ser útil
             // writer.flush();
        } else {
            // Si el writer no está inicializado (error en inicio), loguear a consola
            System.out.println("LOG (consola): " + mensaje);
        }
    }

    /**
     * Detiene el servicio de logging periódico.
     * Intenta esperar un poco a que termine la tarea actual antes de forzar la detención.
     */
    public void detenerLogPeriodico() {
        if (scheduler != null && !scheduler.isShutdown()) {
             logMensaje("Deteniendo logger periódico...");
            scheduler.shutdown(); // Inicia la secuencia de apagado
            try {
                // Espera un tiempo corto para que la tarea actual (si la hay) termine
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                     logMensaje("Forzando detención del logger periódico...");
                    scheduler.shutdownNow(); // Interrumpe la tarea si sigue corriendo
                }
                 logMensaje("Logger periódico detenido.");
            } catch (InterruptedException e) {
                 logMensaje("Interrumpido mientras se esperaba la detención del logger periódico. Forzando...");
                scheduler.shutdownNow();
                Thread.currentThread().interrupt(); // Restablece el estado de interrupción
            }
        } else {
             logMensaje("Logger periódico no estaba corriendo o ya fue detenido previamente.");
        }
         // No cerramos el writer aquí, se cierra en logFinal
    }

    /**
     * Escribe el informe final de la simulación en el archivo de log y cierra el writer.
     * Incluye estadísticas totales, estado de los casilleros y un mensaje
     * indicando si la simulación se detuvo prematuramente.
     *
     * @param matriz La instancia de MatrizCasilleros para obtener sus estadísticas.
     * @param tiempoTotalMs El tiempo total de ejecución de la simulación en milisegundos.
     * @param stoppedPrematurely Indica si la simulación fue detenida por falta de casilleros.
     */
    public void logFinal(MatrizCasilleros matriz, long tiempoTotalMs, boolean stoppedPrematurely) {
        // Asegurarse de que el writer esté disponible, si no, loguear a consola
        boolean logToFile = writer != null;

        String logPrefix = logToFile ? "" : "LOG (consola): ";

        printlnFinal(logPrefix + "--- INFORME FINAL ---");

        // *** Mensaje condicional sobre la causa de la detención ***
        if (stoppedPrematurely) {
            printlnFinal(logPrefix + "!!! ATENCIÓN: La simulación se detuvo prematuramente por falta de casilleros disponibles !!!");
        } else {
            printlnFinal(logPrefix + "Simulación completada según condiciones normales de parada.");
        }

        printlnFinal(logPrefix + "Tiempo total de ejecución: " + tiempoTotalMs + " ms (" + String.format("%.3f", tiempoTotalMs / 1000.0) + " segundos)");

        // Estadísticas de Pedidos
        int generados = registro.getCantidadPreparados();
        int verificados = registro.getCantidadVerificados();
        int fallidos = registro.getCantidadFallidos();
        int procFinal = verificados + fallidos;
        int qPrep = registro.getCantidadEnPreparacion();
        int qTran = registro.getCantidadEnTransito();
        int qEntr = registro.getCantidadEntregados();
        int qRestantes = qPrep + qTran + qEntr;

        printlnFinal(logPrefix + "Total Pedidos Generados: " + generados);
        printlnFinal(logPrefix + "Total Pedidos Verificados: " + verificados);
        printlnFinal(logPrefix + "Total Pedidos Fallidos: " + fallidos);
        printlnFinal(logPrefix + "Total Pedidos Finalizados (Verificados + Fallidos): " + procFinal);

        // Estadísticas de Casilleros (si matriz no es null)
        if (matriz != null) {
             printlnFinal(logPrefix + matriz.getEstadisticas());
        } else {
             printlnFinal(logPrefix + "Estadísticas de matriz no disponibles (matriz es null).");
        }

        // Estado final de las colas
        printlnFinal(logPrefix + "Pedidos restantes en Cola Preparación: " + qPrep);
        printlnFinal(logPrefix + "Pedidos restantes en Cola Tránsito: " + qTran);
        printlnFinal(logPrefix + "Pedidos restantes en Cola Entregados: " + qEntr);
        printlnFinal(logPrefix + "Total Pedidos Restantes en Colas: " + qRestantes);


        // Verificación de consistencia
        if (generados != procFinal + qRestantes) {
             printlnFinal(String.format(logPrefix + "ADVERTENCIA EN LOG: Inconsistencia detectada. Generados (%d) != Finalizados (%d) + Restantes (%d). Diferencia: %d",
                                     generados, procFinal, qRestantes, (generados - (procFinal + qRestantes))));
        } else {
             printlnFinal(logPrefix + "Verificación de consistencia: OK (Generados = Finalizados + Restantes)");
        }

        printlnFinal(logPrefix + "--- FIN INFORME ---");

        // Cerrar el PrintWriter si se estaba usando
        if (writer != null) {
            writer.flush(); // Asegura que todo se escriba
            writer.close(); // Cierra el archivo
            // Verificar si hubo errores al cerrar
            if(writer.checkError()){
                System.err.println("Error al cerrar el archivo de log.");
            }
            writer = null; // Indica que ya no está disponible
        }
    }

    /**
     * Método auxiliar para escribir una línea en el log final,
     * manejando si el writer está disponible o no.
     * @param line La línea a escribir.
     */
    private void printlnFinal(String line) {
        if (writer != null) {
            writer.println(line);
        } else {
            System.out.println(line); // Fallback a consola
        }
    }
}