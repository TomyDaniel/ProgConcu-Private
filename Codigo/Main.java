import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

public class Main {

    //Parametros de simulacion
    private static final int NUM_PREPARADORES = 3;
    private static final int NUM_DESPACHADORES = 2;
    private static final int NUM_ENTREGADORES = 3;
    private static final int NUM_VERIFICADORES = 2;
    private static final int TOTAL_PEDIDOS_A_GENERAR = 500; // Límite de pedidos

    //Tamaño de la matriz de casilleros 10x20=200 casilleros
    private static final int FILAS_MATRIZ = 10;
    private static final int COLUMNAS_MATRIZ = 20;

    //Tiempos de demora expresado en milisegundos
    private static final int DEMORA_PREPARADOR = 100;
    private static final int DEMORA_DESPACHADOR = 80;
    private static final int DEMORA_ENTREGADOR = 120;
    private static final int DEMORA_VERIFICADOR = 50;

    private static final long INTERVALO_LOG_MS = 200; // Log cada 200ms
    private static final String LOG_FILE_PATH = "simulacion_logistica.log";
    // -----------------------------------------

    private static void finalizarHilos() {
        PreparadorPedido.setRunning(false);
        DespachadorPedido.setRunning(false);
        EntregadorPedido.setRunning(false);
        VerificadorPedido.setRunning(false);
    }
    public static void main(String[] args) {
        System.out.println("Iniciando simulación de logística...");
        long startTime = System.currentTimeMillis();

        // Inicializar componentes
        boolean running=true;
        MatrizCasilleros matriz = new MatrizCasilleros(FILAS_MATRIZ, COLUMNAS_MATRIZ);
        RegistroPedidos registro = new RegistroPedidos();

        //Inicializar el logger
        LoggerSistema logger = new LoggerSistema(LOG_FILE_PATH, registro, matriz);
        logger.iniciarLogPeriodico(INTERVALO_LOG_MS);

        //Crear hilos
        List<Thread> hilos = new ArrayList<>();

        // 3 hilos preparadores
        for (int i = 0; i < NUM_PREPARADORES; i++) {
            Thread hilo = new Thread(new PreparadorPedido(registro, matriz, DEMORA_PREPARADOR, TOTAL_PEDIDOS_A_GENERAR, running));
            hilo.setName("Preparador-" + i);
            hilos.add(hilo);
        }

        // 2 hilos despachadores
        for (int i = 0; i < NUM_DESPACHADORES; i++) {
            Thread hilo = new Thread(new DespachadorPedido(registro, matriz, DEMORA_DESPACHADOR, running));
            hilo.setName("Despachador-" + i);
            hilos.add(hilo);
        }

        //3 hilos entregadores
        for (int i = 0; i < NUM_ENTREGADORES; i++) {
            Thread hilo = new Thread(new EntregadorPedido(registro, DEMORA_ENTREGADOR, running));
            hilo.setName("Entregador-" + i);
            hilos.add(hilo);
        }

        //2 hilos verificadores
        for (int i = 0; i < NUM_VERIFICADORES; i++) {
            Thread hilo = new Thread(new VerificadorPedido(registro,DEMORA_VERIFICADOR, running));
            hilo.setName("Verificador-" + i);
            hilos.add(hilo);
        }

        //Iniciar hilos
        for (Thread hilo : hilos) {
            hilo.start();
        }

        //Lógica Finalizacion
        System.out.println("Esperando finalización de la generación y procesamiento...");
        try{
            while (running) {
                boolean preparacionCompleta = registro.getTotalPedidosGenerados() >= TOTAL_PEDIDOS_A_GENERAR;
                boolean colasIntermediasVacias = registro.colasVacias();
                Thread.sleep(500);
                matriz.verificarEstadoCritico();
                if (preparacionCompleta && colasIntermediasVacias) {
                    System.out.println("Condición de parada alcanzada. Señalando a hilos para terminar...");
                    running=false;
                    finalizarHilos();
                }}


            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Hilo principal interrumpido. Terminando simulación.");
                finalizarHilos();
        } catch (MatrizLlenaException e){
                System.out.println("Programa finalizado: todos los casilleros estan fuera de servicio");
                finalizarHilos();
        }

        //Esperar a que todos los hilos terminen
        for (Thread hilo : hilos) {
            try{
                hilo.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // Detener Logger
        logger.detenerLogPeriodico();

        // Calcular tiempo y mostrar resultados finales
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("\n--- Simulación Finalizada ---");
        System.out.printf("Tiempo total de ejecución: %.2f segundos\n", duration / 1000.0);
        System.out.println("--- Estadísticas Finales de Pedidos ---");
        System.out.println("Pedidos generados totales: " + registro.getTotalPedidosGenerados());
        System.out.println("Pedidos en Preparación (al final): " + registro.getCantidad(EstadoPedido.PREPARACION));
        System.out.println("Pedidos en Tránsito (al final): " + registro.getCantidad(EstadoPedido.TRANSITO));
        System.out.println("Pedidos Entregados (pendientes de verificación): " + registro.getCantidad(EstadoPedido.ENTREGADO));
        System.out.println("Pedidos Verificados exitosamente: " + registro.getCantidad(EstadoPedido.VERIFICADO));
        System.out.println("Pedidos con entrega Fallida: " + registro.getCantidad(EstadoPedido.FALLIDO));
        System.out.println("--- Estadísticas Finales de Casilleros ---");


        //logger.logFinal(duration);
        System.out.println("Log final guardado en: " + LOG_FILE_PATH);
    }
}