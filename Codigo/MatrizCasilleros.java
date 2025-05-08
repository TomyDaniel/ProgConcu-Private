// MatrizCasilleros.java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MatrizCasilleros {
    private final Casillero[][] matriz;
    private final int filas;
    private final int columnas;

    public MatrizCasilleros(int filas, int columnas) {
        this.filas = filas;
        this.columnas = columnas;
        matriz = new Casillero[filas][columnas];

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                matriz[i][j] = new Casillero();
            }
        }
    }

    public int ocuparCasilleroAleatorio() {
        // La creación y mezcla de posiciones no necesita bloqueo
        List<Integer> posiciones = new ArrayList<>(filas * columnas);
        for (int i = 0; i < filas * columnas; i++) {
            posiciones.add(i);
        }
        Collections.shuffle(posiciones);

        for (int pos : posiciones) {
            int fila = pos / columnas;
            int col = pos % columnas;
            Casillero casilleroActual = matriz[fila][col];

            // No necesitamos un lock de matriz aquí.
            // Intentamos ocupar el casillero individual
            if (casilleroActual.intentarOcupar()) {
                return pos; // Se ocupó exitosamente
            }
            // Si intentarOcupar() devuelve false, significa que no estaba VACIO
            // o no se pudo ocupar. Simplemente continuamos al siguiente.
        }
        return -1; // No se encontró casillero disponible y ocupable
    }

    public void liberarCasillero(int casilleroId) {
        if (casilleroId < 0 || casilleroId >= filas * columnas) {
            // System.err.println("ID de casillero inválido para liberar: " + casilleroId);
            return; // o lanzar una excepción si prefieres
        }
        int fila = casilleroId / columnas;
        int col = casilleroId % columnas;
        Casillero casilleroActual = matriz[fila][col];

        // El casillero individual maneja su propio bloqueo
        if (!casilleroActual.intentarLiberar()) {
            // System.err.println("Intento de liberar casillero " + casilleroId + " que no estaba ocupado.");
        }
    }

    public void marcarFueraDeServicio(int casilleroId) {
        if (casilleroId < 0 || casilleroId >= filas * columnas) {
            // System.err.println("ID de casillero inválido para marcar fuera de servicio: " + casilleroId);
            return; // o lanzar una excepción
        }
        int fila = casilleroId / columnas;
        int col = casilleroId % columnas;
        Casillero casilleroActual = matriz[fila][col];

        // El casillero individual maneja su propio bloqueo
        casilleroActual.marcarFueraDeServicioConLock();
    }

    public int getSizeFueraDeServicio() {
        // Para leer el estado de múltiples casilleros, si cada uno tiene su lock,
        // obtener el estado de cada uno es una operación bloqueante individual.
        // Esto es aceptable, pero es menos eficiente que un readLock global
        // si esta operación se llama muy frecuentemente Y hay mucha contención en los locks individuales.
        // Sin embargo, permite que otros hilos modifiquen OTROS casilleros mientras este cuenta.
        int size = 0;
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                // getEstado() ahora bloquea y desbloquea el lock del Casillero individual
                if (matriz[i][j].getEstado() == EstadoCasillero.FUERA_DE_SERVICIO) {
                    size++;
                }
            }
        }
        return size;
    }

    public void verificarEstadoCritico() throws MatrizLlenaException {
        // Ya no se necesita readLock aquí porque getSizeFueraDeServicio iterará
        // sobre casilleros que manejan su propia sincronización para getEstado().
        int contador = getSizeFueraDeServicio();
        int limitePermitido = filas * columnas;

        if (contador >= limitePermitido) {
            throw new MatrizLlenaException("Error: Se detectaron " + contador +
                    " casilleros fuera de servicio de un total de " +
                    (filas * columnas) + ". El sistema no puede continuar.");
        }
    }
}

class MatrizLlenaException extends RuntimeException { // Definición de la excepción
    public MatrizLlenaException(String mensaje) {
        super(mensaje);
    }
}