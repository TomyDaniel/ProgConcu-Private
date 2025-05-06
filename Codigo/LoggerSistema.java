import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LoggerSistema {

    private final String logFilePath;
    private final RegistroPedidos registro;
    private final MatrizCasilleros matriz; // Para estadísticas de casilleros
    private final ScheduledExecutorService scheduler;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public LoggerSistema(String logFilePath, RegistroPedidos registro, MatrizCasilleros matriz) {
        this.logFilePath = logFilePath;
        this.registro = registro;
        this.matriz = matriz;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        // Crear/Limpiar archivo al inicio
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, false))) {
            writer.println("--- Inicio del Log de Simulación ---");
        } catch (IOException e) {
            System.err.println("Error al inicializar el archivo de log: " + e.getMessage());
        }
    }

    public void iniciarLogPeriodico(long intervaloMs) {
        scheduler.scheduleAtFixedRate(this::logEstadoActual, intervaloMs, intervaloMs, TimeUnit.MILLISECONDS);
        System.out.println("Logger periódico iniciado cada " + intervaloMs + " ms.");
    }

    public void detenerLogPeriodico() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Logger periódico detenido.");
        logEstadoActual(); // Log final antes de terminar
    }

    public void logEstadoActual() {
        String timestamp = LocalDateTime.now().format(formatter);
        // Usar el método getCantidad generalizado
        String estado = String.format("[%s] Estado: Preparación=%d, Tránsito=%d, Entregados=%d, Verificados=%d, Fallidos=%d | Total Generados=%d | Casilleros Malos=%d",
                timestamp,
                registro.getCantidad(EstadoPedido.PREPARACION),
                registro.getCantidad(EstadoPedido.TRANSITO),
                registro.getCantidad(EstadoPedido.ENTREGADO),
                registro.getCantidad(EstadoPedido.VERIFICADO),
                registro.getCantidad(EstadoPedido.FALLIDO),
                registro.getTotalPedidosGenerados(),
                matriz.getSizeFueraDeServicio() // Asume que MatrizCasilleros tiene este método
        );
        escribirArchivo(estado);
        System.out.println(estado); // También mostrar en consola
    }

    public void logMensaje(String mensaje) {
        String timestamp = LocalDateTime.now().format(formatter);
        escribirArchivo(String.format("[%s] Mensaje: %s", timestamp, mensaje));
        System.out.println(String.format("[%s] %s", timestamp, mensaje)); // Opcional: mostrar en consola
    }


    public void logFinal(long tiempoTotalMs) {
        String timestamp = LocalDateTime.now().format(formatter);
        escribirArchivo(String.format("[%s] --- Fin de la Simulación ---", timestamp));
        escribirArchivo(String.format("[%s] Tiempo total de ejecución: %.2f segundos", timestamp, tiempoTotalMs / 1000.0));
        logEstadoActual(); // Log del estado final detallado
        escribirArchivo(String.format("[%s] --- Reporte Final Generado ---", timestamp));
    }

    private synchronized void escribirArchivo(String mensaje) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, true))) {
            writer.println(mensaje);
        } catch (IOException e) {
            System.err.println("Error al escribir en el archivo de log: " + e.getMessage());
        }
    }
}