import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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

    private static final long INTERVALO_LOG_MS = 1000; // Log cada segundo
    private static final String LOG_FILE_PATH = "simulacion_logistica.log";
    // -----------------------------------------


    public static void main(String[] args) {
        System.out.println("Iniciando simulación de logística...");
        long startTime = System.currentTimeMillis();

        // Inicializar componentes
        AtomicBoolean running = new AtomicBoolean(true); // Flag para detener hilos
        MatrizCasilleros matriz = new MatrizCasilleros(FILAS_MATRIZ, COLUMNAS_MATRIZ);
        RegistroPedidos registro = new RegistroPedidos(); // La creación no cambia

        //Inicializar el logger
        LoggerSistema logger = new LoggerSistema(LOG_FILE_PATH, registro, matriz);
        logger.iniciarLogPeriodico(INTERVALO_LOG_MS);

        //Crear hilos
        List<Thread> hilos = new ArrayList<>();

        // 3 hilos preparadores
        for (int i = 0; i < NUM_PREPARADORES; i++) {
            Thread hilo = new Thread(new PreparadorPedido(registro, matriz, DEMORA_BASE_PREP, DEMORA_VAR_PREP, TOTAL_PEDIDOS_A_GENERAR, running));
            hilo.setName("Preparador-" + i);
            hilos.add(hilo);
        }

        // 2 hilos despachadores
        for (int i = 0; i < NUM_DESPACHADORES; i++) {
            Thread hilo = new Thread(new DespachadorPedido(registro, matriz, DEMORA_BASE_DESP, DEMORA_VAR_DESP, running));
            hilo.setName("Despachador-" + i);
            hilos.add(hilo);
        }

        //3 hilos entregadores
        for (int i = 0; i < NUM_ENTREGADORES; i++) {
            Thread hilo = new Thread(new EntregadorPedido(registro, DEMORA_BASE_ENT, DEMORA_VAR_ENT, running));
            hilo.setName("Entregador-" + i);
            hilos.add(hilo);
        }

        //2 hilos verificadores
        for (int i = 0; i < NUM_VERIFICADORES; i++) {
            Thread hilo = new Thread(new VerificadorPedido(registro,DEMORA_BASE_VER, DEMORA_VAR_VER, running));
            hilo.setName("Verificador-" + i);
            hilos.add(hilo);
        }

        //Iniciar hilos
        for (Thread hilo : hilos) {
            hilo.start();
        }

        // 6. Lógica de espera y terminación
        System.out.println("Esperando finalización de la generación y procesamiento...");
        try{
            while (running.get()) {
                boolean preparacionCompleta = registro.getTotalPedidosGenerados() >= TOTAL_PEDIDOS_A_GENERAR;
                boolean colasIntermediasVacias = registro.colasVacias();
                Thread.sleep(500);
                matriz.verificarEstadoCritico();
                if (preparacionCompleta && colasIntermediasVacias) {
                    running.set(false);
                    System.out.println("Condición de parada alcanzada. Señalando a hilos para terminar...");
                }}


            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running.set(false);
                System.err.println("Hilo principal interrumpido. Terminando simulación.");
            } catch (MatrizLlenaException e){
            System.out.println("Programa finalizado: todos los casilleros estan fuera de servicio");
            running.set(false);
        }


        //Esperar a que todos los hilos terminen
        for (Thread hilo : hilos) {
            try{
                hilo.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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


        logger.logFinal(duration);
        System.out.println("Log final guardado en: " + LOG_FILE_PATH);
    }
}