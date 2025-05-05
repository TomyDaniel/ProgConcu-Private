import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
public class MatrizCasilleros {
    private final Casillero[][] matriz;
    private final int filas;
    private final int columnas;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    public MatrizCasilleros(int filas, int columnas) {
        this.filas = filas;
        this.columnas = columnas;
        matriz = new Casillero[filas][columnas];

        // Inicializar todos los casilleros como vacíos
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                matriz[i][j] = new Casillero();
            }
        }
    }

    public int ocuparCasilleroAleatorio() {
        readLock.lock();
        try {
            // Crear lista de posiciones a intentar (orden aleatorio)
            List<Integer> posiciones = new ArrayList<>(filas * columnas);
            for (int i = 0; i < filas * columnas; i++) {
                posiciones.add(i);
            }
            Collections.shuffle(posiciones);  //Randomiza la coleccion

            // Probar cada posición hasta encontrar una disponible
            for (int pos : posiciones) {
                int fila = pos / columnas;
                int col = pos % columnas;

                // Upgrade a write lock solo si encontramos casillero disponible
                readLock.unlock();
                writeLock.lock();
                try {
                    if (matriz[fila][col].getEstado()==EstadoCasillero.VACIO) { //Verifico que el casillero anterior este libre
                        matriz[fila][col].ocupar(); //Cambio el estado del casillero a OCUPADO
                        return pos;
                    }
                }
                catch (IllegalStateException e) {}
                finally {
                    // Downgrade de nuevo a read lock
                    readLock.lock();
                    writeLock.unlock();
                }
            }
            // No se encontró casillero disponible
            return -1;
        } finally {
            readLock.unlock();
        }
    }

    public void liberarCasillero(int casilleroId) {
        int fila = casilleroId / columnas;
        int col = casilleroId % columnas;

        writeLock.lock();
        try {
            matriz[fila][col].liberar();
        }
        catch (IllegalStateException e) {}
        finally {
            writeLock.unlock();
        }
    }

    public void marcarFueraDeServicio(int casilleroId) {
        int fila = casilleroId / columnas;
        int col = casilleroId % columnas;

        writeLock.lock();
        try {
            matriz[fila][col].marcarFueraDeServicio();
        } finally {
            writeLock.unlock();
        }
    }
    public int getSizeFueraDeServicio() {
        int size = 0;
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                if (matriz[i][j].getEstado() == EstadoCasillero.FUERA_DE_SERVICIO) {
                    size++;
                }
            }
        }
        return size;
    }

    public void verificarEstadoCritico() throws MatrizLlenaException{
        readLock.lock();
        try {
            int contador = getSizeFueraDeServicio();
            int limitePermitido = filas * columnas; // Puedes ajustar este umbral

            if (contador >= limitePermitido) {
                throw new MatrizLlenaException("Error: Se detectaron " + contador +
                        " casilleros fuera de servicio de un total de " +
                        (filas * columnas) + ". El sistema no puede continuar.");
            }
        } finally {
            readLock.unlock();
        }
    }


}

class MatrizLlenaException extends RuntimeException {
    public MatrizLlenaException(String mensaje) {
        super(mensaje);
    }
}
