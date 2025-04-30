// --- Clases de Gestión de Recursos Compartidos ---
// MatrizCasilleros.java
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class MatrizCasilleros {
    private final Casillero[] casilleros;
    private final int totalCasilleros;
    private final ReentrantLock matrizLock = new ReentrantLock(); // Lock para operaciones sobre la matriz/casilleros
    private final Random random = new Random(); // Para selección aleatoria

    public MatrizCasilleros(int numCasilleros) {
        this.totalCasilleros = numCasilleros;
        this.casilleros = new Casillero[numCasilleros];
        for (int i = 0; i < numCasilleros; i++) {
            casilleros[i] = new Casillero(i);
        }
    }

    // Busca un casillero vacío aleatorio y lo ocupa. Devuelve el ID o -1 si no hay.
    public int ocuparCasilleroAleatorio() {
        matrizLock.lock();
        try {
            List<Integer> vacios = new ArrayList<>();
            for (int i = 0; i < totalCasilleros; i++) {
                if (casilleros[i].getEstado() == EstadoCasillero.VACIO) {
                    vacios.add(i);
                }
            }

            if (vacios.isEmpty()) {
                return -1; // No hay casilleros vacíos
            }

            int indiceAleatorio = random.nextInt(vacios.size());
            int casilleroId = vacios.get(indiceAleatorio);
            casilleros[casilleroId].ocupar();
            //System.out.println(Thread.currentThread().getName() + " ocupó casillero " + casilleroId);
            return casilleroId;
        } finally {
            matrizLock.unlock();
        }
    }

    // Libera un casillero específico
    public void liberarCasillero(int casilleroId) {
        if (casilleroId < 0 || casilleroId >= totalCasilleros) return;
        matrizLock.lock();
        try {
            if (casilleros[casilleroId].getEstado() == EstadoCasillero.OCUPADO) {
                 casilleros[casilleroId].liberar();
                 //System.out.println("Casillero " + casilleroId + " liberado.");
            }
        } finally {
            matrizLock.unlock();
        }
    }

    // Pone un casillero específico fuera de servicio
    public void ponerFueraDeServicio(int casilleroId) {
         if (casilleroId < 0 || casilleroId >= totalCasilleros) return;
        matrizLock.lock();
        try {
            if (casilleros[casilleroId].getEstado() == EstadoCasillero.OCUPADO) {
                casilleros[casilleroId].ponerFueraDeServicio();
                //System.out.println("Casillero " + casilleroId + " fuera de servicio.");
            }
        } finally {
            matrizLock.unlock();
        }
    }

    // Obtiene estadísticas finales
    public String getEstadisticas() {
        matrizLock.lock();
        try {
            long vacios = 0;
            long ocupados = 0;
            long fueraServicio = 0;
            long totalUsos = 0;

            for (Casillero c : casilleros) {
                totalUsos += c.getContadorUso();
                switch (c.getEstado()) {
                    case VACIO: vacios++; break;
                    case OCUPADO: ocupados++; break; // No debería haber ocupados al final
                    case FUERA_DE_SERVICIO: fueraServicio++; break;
                }
            }
            return String.format("Estado Final Casilleros: Vacíos=%d, Ocupados=%d, FueraDeServicio=%d | Usos Totales Registrados=%d",
                                 vacios, ocupados, fueraServicio, totalUsos);
        } finally {
            matrizLock.unlock();
        }
    }
}