import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
// Asegúrate de tener también las clases Casillero, EstadoCasillero y NoAvailableLockersException

public class MatrizCasilleros {
    private final Casillero[][] casilleros; // Cambio a matriz 2D
    private final int numRows;
    private final int numCols;
    private final int totalCasilleros;
    private final ReentrantLock matrizLock = new ReentrantLock();
    private final Random random = new Random();

    /**
     * Constructor para una matriz de casilleros bidimensional.
     * @param numRows Número de filas.
     * @param numCols Número de columnas.
     */
    public MatrizCasilleros(int numRows, int numCols) {
        if (numRows <= 0 || numCols <= 0) {
            throw new IllegalArgumentException("El número de filas y columnas debe ser positivo.");
        }
        this.numRows = numRows;
        this.numCols = numCols;
        this.totalCasilleros = numRows * numCols;
        this.casilleros = new Casillero[numRows][numCols];

        // Inicializar la matriz 2D con casilleros y IDs lineales
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                // Calculamos un ID único lineal (0 a totalCasilleros - 1)
                int id = r * numCols + c;
                casilleros[r][c] = new Casillero(id);
            }
        }
        System.out.println("Matriz de casilleros creada: " + numRows + "x" + numCols + " (Total: " + totalCasilleros + ")");
    }

    /**
     * Busca un casillero vacío aleatorio en la matriz 2D y lo ocupa.
     * Devuelve el ID lineal del casillero ocupado o -1 si no hay vacíos temporalmente.
     * Lanza NoAvailableLockersException si no quedan casilleros usables permanentemente.
     * @return ID del casillero ocupado, -1 si no hay vacíos temporalmente.
     * @throws NoAvailableLockersException Si todos los casilleros están FUERA_DE_SERVICIO.
     */
    public int ocuparCasilleroAleatorio() throws NoHayCasilleros {
        matrizLock.lock();
        try {
            // Almacenamos las coordenadas (fila, columna) de los casilleros vacíos
            List<int[]> vaciosCoords = new ArrayList<>();
            boolean hayOcupados = false;

            // Iterar sobre la matriz 2D
            for (int r = 0; r < numRows; r++) {
                for (int c = 0; c < numCols; c++) {
                    EstadoCasillero estado = casilleros[r][c].getEstado();
                    if (estado == EstadoCasillero.VACIO) {
                        vaciosCoords.add(new int[]{r, c}); // Guardar [fila, columna]
                    } else if (estado == EstadoCasillero.OCUPADO) {
                        hayOcupados = true;
                    }
                    // Si es FUERA_DE_SERVICIO, no lo añadimos a vacíos
                }
            }

            if (vaciosCoords.isEmpty()) {
                // No hay VACIOS
                if (!hayOcupados) {
                    // Si TAMPOCO hay OCUPADOS, todos están FUERA_DE_SERVICIO
                    throw new NoHayCasilleros("¡Todos los casilleros ("+ totalCasilleros +") están Fuera de Servicio! No se pueden preparar más pedidos.");
                } else {
                    // No hay VACIOS ahora, pero podrían liberarse OCUPADOS
                    return -1; // Fallo temporal
                }
            }

            // Seleccionar coordenadas aleatorias de un casillero vacío
            int randomIndex = random.nextInt(vaciosCoords.size());
            int[] selectedCoords = vaciosCoords.get(randomIndex);
            int r = selectedCoords[0];
            int c = selectedCoords[1];

            // Ocupar el casillero en esas coordenadas
            casilleros[r][c].ocupar();

            // Devolver el ID lineal correspondiente a esas coordenadas
            int casilleroId = r * numCols + c;
            // System.out.println(Thread.currentThread().getName() + " ocupó casillero ID " + casilleroId + " en [" + r + "," + c + "]");
            return casilleroId;

        } finally {
            matrizLock.unlock();
        }
    }

    /**
     * Libera un casillero específico usando su ID lineal.
     * @param casilleroId El ID lineal del casillero a liberar.
     */
    public void liberarCasillero(int casilleroId) {
        // Convertir ID lineal a coordenadas [fila, columna]
        int[] coords = getCoordsFromId(casilleroId);
        if (coords == null) return; // ID inválido

        int r = coords[0];
        int c = coords[1];

        matrizLock.lock();
        try {
            if (casilleros[r][c].getEstado() == EstadoCasillero.OCUPADO) {
                casilleros[r][c].liberar();
                // System.out.println("Casillero ID " + casilleroId + " en [" + r + "," + c + "] liberado.");
            }
        } finally {
            matrizLock.unlock();
        }
    }

    /**
     * Pone un casillero específico fuera de servicio usando su ID lineal.
     * @param casilleroId El ID lineal del casillero a poner fuera de servicio.
     */
    public void ponerFueraDeServicio(int casilleroId) {
        int[] coords = getCoordsFromId(casilleroId);
        if (coords == null) return; // ID inválido

        int r = coords[0];
        int c = coords[1];

        matrizLock.lock();
        try {
            // Solo poner fuera de servicio si está OCUPADO (o VACIO, si se quiere permitir)
            if (casilleros[r][c].getEstado() == EstadoCasillero.OCUPADO) {
                casilleros[r][c].ponerFueraDeServicio();
                // System.out.println("Casillero ID " + casilleroId + " en [" + r + "," + c + "] fuera de servicio.");
            }
            // Considerar si se puede poner fuera de servicio un casillero ya VACIO:
            // else if (casilleros[r][c].getEstado() == EstadoCasillero.VACIO) {
            //     casilleros[r][c].ponerFueraDeServicio();
            // }
        } finally {
            matrizLock.unlock();
        }
    }

    /**
     * Obtiene estadísticas finales recorriendo la matriz 2D.
     * @return Un string con el resumen del estado y uso de los casilleros.
     */
    public String getEstadisticas() {
        matrizLock.lock();
        try {
            long vacios = 0;
            long ocupados = 0;
            long fueraServicio = 0;
            long totalUsos = 0;

            // Iterar sobre la matriz 2D
            for (int r = 0; r < numRows; r++) {
                for (int c = 0; c < numCols; c++) {
                    Casillero casillero = casilleros[r][c];
                    totalUsos += casillero.getContadorUso();
                    switch (casillero.getEstado()) {
                        case VACIO: vacios++; break;
                        case OCUPADO: ocupados++; break;
                        case FUERA_DE_SERVICIO: fueraServicio++; break;
                    }
                }
            }
            return String.format("Estado Final Casilleros (%dx%d): Vacíos=%d, Ocupados=%d, FueraDeServicio=%d | Usos Totales Registrados=%d",
                                 numRows, numCols, vacios, ocupados, fueraServicio, totalUsos);
        } finally {
            matrizLock.unlock();
        }
    }

    /**
     * Convierte un ID lineal (0 a totalCasilleros-1) a coordenadas [fila, columna].
     * @param casilleroId El ID lineal.
     * @return Un array de int `[fila, columna]` o `null` si el ID es inválido.
     */
    private int[] getCoordsFromId(int casilleroId) {
        if (casilleroId < 0 || casilleroId >= totalCasilleros) {
             System.err.println("Error: Intento de acceso a casillero con ID inválido: " + casilleroId);
            return null; // ID fuera de rango
        }
        int r = casilleroId / numCols; // División entera da la fila
        int c = casilleroId % numCols; // Módulo da la columna
        return new int[]{r, c};
    }

    // --- Métodos Adicionales (Opcionales) ---

    /**
     * Obtiene el número total de casilleros.
     * @return El total de casilleros (filas * columnas).
     */
    public int getTotalCasilleros() {
        return totalCasilleros;
    }

     /**
     * Obtiene el casillero en una coordenada específica (si necesitas acceso directo).
     * ¡Precaución! Devolver el objeto directamente podría permitir manipulación externa sin lock.
     * Es mejor exponer solo el estado si es necesario.
     * @param row Fila.
     * @param col Columna.
     * @return El objeto Casillero o null si las coordenadas son inválidas.
     */
     /*
    public Casillero getCasilleroAt(int row, int col) {
        if (row >= 0 && row < numRows && col >= 0 && col < numCols) {
            return casilleros[row][col];
        }
        return null;
    }
    */

     /**
      * Obtiene el estado de un casillero por ID.
      * @param casilleroId ID lineal del casillero.
      * @return El EstadoCasillero o null si el ID es inválido.
      */
     public EstadoCasillero getEstadoCasillero(int casilleroId) {
         int[] coords = getCoordsFromId(casilleroId);
         if (coords == null) return null;
         // No necesita lock porque el estado es volatile y solo leemos
         return casilleros[coords[0]][coords[1]].getEstado();
     }
}