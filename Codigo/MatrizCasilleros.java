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

        // Inicializar todos los casilleros como vacíos
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                matriz[i][j] = new Casillero();
            }
        }
    }

    public int ocuparCasilleroAleatorio() {
            // Crear lista de posiciones a intentar (orden aleatorio)
            List<Integer> posiciones = new ArrayList<>(filas * columnas);   //contiene las id de todos los casilleros
            for (int i = 0; i < filas * columnas; i++) {
                posiciones.add(i);
            }
            Collections.shuffle(posiciones);  //Randomiza la coleccion

            // Probar cada posición hasta encontrar una disponible
            for (int pos : posiciones) {
                int fila = pos / columnas;
                int col = pos % columnas;

                try {
                    if (matriz[fila][col].getEstado()==EstadoCasillero.VACIO) { //Verifico que el casillero anterior este libre
                        matriz[fila][col].ocupar(); //Cambio el estado del casillero a OCUPADO
                        return pos;
                    }
                }
                catch (IllegalStateException e) {}

            }
            // No se encontró casillero disponible
            return -1;
    }


    public void liberarCasillero(int casilleroId) {
        int fila = casilleroId / columnas;
        int col = casilleroId % columnas;

        try {
            matriz[fila][col].liberar();
        }
        catch (IllegalStateException e) {}

    }

    public void marcarFueraDeServicio(int casilleroId) {
        int fila = casilleroId / columnas;
        int col = casilleroId % columnas;

        try {
            matriz[fila][col].marcarFueraDeServicio();
        } catch (IllegalStateException e) { }
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
        try {
            int contador = getSizeFueraDeServicio();
            int limitePermitido = filas * columnas; // Puedes ajustar este umbral

            if (contador >= limitePermitido) {
                throw new MatrizLlenaException("Error: Se detectaron " + contador +
                        " casilleros fuera de servicio de un total de " +
                        (filas * columnas) + ". El sistema no puede continuar.");
            }
        } catch (Exception e) {}
    }


}

class MatrizLlenaException extends RuntimeException {
    public MatrizLlenaException(String mensaje) {
        super(mensaje);
    }
}
