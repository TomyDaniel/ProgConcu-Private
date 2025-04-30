
// --- Clase de Logging ---
// LoggerSistema.java
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerSistema {
    private final RegistroPedidos registro;
    private final String archivoLog;
    private ScheduledExecutorService scheduler;
    private PrintWriter writer;
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public LoggerSistema(RegistroPedidos registro, String archivoLog) {
        this.registro = registro;
        this.archivoLog = archivoLog;
    }

    public void iniciarLogPeriodico(long intervaloMs) {
        try {
            // Append mode true
            writer = new PrintWriter(new FileWriter(archivoLog, true), true); // Aut flush true
            scheduler = Executors.newSingleThreadScheduledExecutor();

            Runnable tareaLog = () -> {
                String timestamp = dtf.format(LocalDateTime.now());
                int fallidos = registro.getCantidadFallidos();
                int verificados = registro.getCantidadVerificados();
                writer.println(String.format("%s - Fallidos: %d, Verificados: %d", timestamp, fallidos, verificados));
            };

            scheduler.scheduleAtFixedRate(tareaLog, 0, intervaloMs, TimeUnit.MILLISECONDS);
             logMensaje("Logger periódico iniciado. Escribiendo en: " + archivoLog);

        } catch (IOException e) {
            System.err.println("Error al iniciar el logger: " + e.getMessage());
            if (writer != null) writer.close();
             scheduler = null; // No iniciar si falla el archivo
        }
    }

    public void logMensaje(String mensaje) {
        if (writer != null) {
             String timestamp = dtf.format(LocalDateTime.now());
             writer.println(timestamp + " - " + mensaje);
             writer.flush(); // Añadir flush explícito
        } else {
            System.out.println("LOG (consola): " + mensaje); // Fallback a consola
        }
    }


    public void detenerLogPeriodico() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
         logMensaje("Logger periódico detenido.");
    }

     public void logFinal(MatrizCasilleros matriz, long tiempoTotalMs) {
        logMensaje("--- INFORME FINAL ---");
        logMensaje("Tiempo total de ejecución: " + tiempoTotalMs + " ms (" + tiempoTotalMs / 1000.0 + " segundos)");
        logMensaje("Total Pedidos Fallidos: " + registro.getCantidadFallidos());
        logMensaje("Total Pedidos Verificados: " + registro.getCantidadVerificados());
        logMensaje(matriz.getEstadisticas());
        logMensaje("Pedidos restantes en Preparación: " + registro.getCantidadEnPreparacion());
        logMensaje("Pedidos restantes en Tránsito: " + registro.getCantidadEnTransito());
        logMensaje("Pedidos restantes Entregados: " + registro.getCantidadEntregados());
        int totalFinal = registro.getCantidadFallidos() + registro.getCantidadVerificados();
        logMensaje("Total Pedidos Procesados (Fallidos + Verificados): " + totalFinal);
        logMensaje("--- FIN INFORME ---");

        if (writer != null) {
            writer.flush(); // Añadir flush explícito antes de cerrar
            writer.close();
        }
    }
}
