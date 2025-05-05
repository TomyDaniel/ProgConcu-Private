import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
public class Main {
    // Configuración
    private static final int MATRIZ_FILAS = 20;
    private static final int MATRIZ_COLUMNAS = 10;
    private static final int TOTAL_PEDIDOS = 500;

    // Tiempos de demora base (milisegundos)
    private static final int DEMORA_PREPARADOR = 150;
    private static final int DEMORA_DESPACHADOR = 100;
    private static final int DEMORA_ENTREGADOR = 150;
    private static final int DEMORA_VERIFICADOR = 100;

    // Variación de demora (milisegundos +/-)
    private static final int VARIACION_DEMORA = 100;

    public static void main(String[] args) {
        long startTime = System.nanoTime();
        // Inicializar componentes
        AtomicBoolean running = new AtomicBoolean(true);
        MatrizCasilleros matriz = new MatrizCasilleros(MATRIZ_FILAS, MATRIZ_COLUMNAS);
        RegistroPedidos registro = new RegistroPedidos();

        // Inicializar el logger
        LoggerSistema logger = new LoggerSistema(registro, "simulacion_logistica.log");
        logger.iniciarLogPeriodico(5000); // Log cada 5 segundos


        // Crear hilos
        List<Thread> hilos = new ArrayList<>();

        // 3 hilos preparadores
        for (int i = 0; i < 3; i++) {
            Thread hilo = new Thread(new PreparadorPedido(registro, matriz,
                    DEMORA_PREPARADOR, VARIACION_DEMORA,
                    TOTAL_PEDIDOS, running));
            hilo.setName("Preparador-" + i);
            hilos.add(hilo);
        }

        // 2 hilos despachadores
        for (int i = 0; i < 2; i++) {
            Thread hilo = new Thread(new DespachadorPedido(registro, matriz,
                    DEMORA_DESPACHADOR, VARIACION_DEMORA, running));
            hilo.setName("Despachador-" + i);
            hilos.add(hilo);
        }

        // 3 hilos entregadores
        for (int i = 0; i < 3; i++) {
            Thread hilo = new Thread(new EntregadorPedido(registro,
                    DEMORA_ENTREGADOR, VARIACION_DEMORA, running));
            hilo.setName("Entregador-" + i);
            hilos.add(hilo);
        }

        // 2 hilos verificadores
        for (int i = 0; i < 2; i++) {
            Thread hilo = new Thread(new VerificadorPedido(registro,
                    DEMORA_VERIFICADOR, VARIACION_DEMORA, running));
            hilo.setName("Verificador-" + i);
            hilos.add(hilo);
        }

        // Iniciar hilos
        for (Thread hilo : hilos) {
            hilo.start();
        }

        // Monitor de progreso
        new Thread(() -> {
            try {
                while (running.get()) {
                    imprimirEstadisticas(registro);
                    Thread.sleep(2000);

                    // Criterio de finalización: todos los pedidos procesados
                    if (registro.getCantidadPreparados() >= TOTAL_PEDIDOS &&
                            registro.getCantidadEnPreparacion() == 0 &&
                            registro.getCantidadEnTransito() == 0 &&
                            (registro.getCantidadEntregados() + registro.getCantidadVerificados() +
                                    registro.getCantidadFallidos() >= TOTAL_PEDIDOS)) {
                        running.set(false);
                        System.out.println("Todos los pedidos procesados. Finalizando...");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Monitor").start();

        // Esperar a que todos los hilos terminen
        for (Thread hilo : hilos) {
            try {
                hilo.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Resultados finales
        System.out.println("\n--- RESULTADOS FINALES ---");
        imprimirEstadisticas(registro);

        // 7. Finalizar y Reportar
        long endTime = System.nanoTime();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        System.out.println("Simulación completada.");
        logger.detenerLogPeriodico();
        logger.logFinal(matriz, durationMs);
    }

    private static void imprimirEstadisticas(RegistroPedidos registro) {
        System.out.println("\n--- ESTADO ACTUAL ---");
        System.out.println("Pedidos preparados: " + registro.getCantidadPreparados());
        System.out.println("En preparación: " + registro.getCantidadEnPreparacion());
        System.out.println("En tránsito: " + registro.getCantidadEnTransito());
        System.out.println("Entregados: " + registro.getCantidadEntregados());
        System.out.println("Verificados: " + registro.getCantidadVerificados());
        System.out.println("Fallidos: " + registro.getCantidadFallidos());
        System.out.println("Total completados: " + (registro.getCantidadEntregados() + registro.getCantidadVerificados() + registro.getCantidadFallidos()));
    }
}