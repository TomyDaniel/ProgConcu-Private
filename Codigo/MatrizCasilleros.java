
// --- Clases de Gestión de Recursos Compartidos ---
// MatrizCasilleros.java
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger; // <--- IMPORTADO
import java.util.stream.Collectors;

// Asegúrate de que las otras clases necesarias estén accesibles
// import Codigo.Casillero;
// import Codigo.EstadoCasillero;


public class MatrizCasilleros {
    private final Casillero[] casilleros;
    private final int totalCasilleros;
    private final ReentrantLock matrizLock = new ReentrantLock(); // Lock para operaciones sobre la matriz/casilleros
    private final Random random = new Random(); // Para selección aleatoria

    // --- NUEVOS CAMPOS PARA EL LÍMITE ---
    private final AtomicInteger fueraDeServicioCount = new AtomicInteger(0); // Contador thread-safe
    private final int maxFueraDeServicio; // Límite máximo permitido
    // --- FIN NUEVOS CAMPOS ---

    public MatrizCasilleros(int numCasilleros) {
        this.totalCasilleros = numCasilleros;
        this.casilleros = new Casillero[numCasilleros];
        for (int i = 0; i < numCasilleros; i++) {
            casilleros[i] = new Casillero(i);
        }
        // --- INICIALIZACIÓN DEL LÍMITE ---
        // Calculamos el límite (ej: 50% del total, pero al menos 1)
        this.maxFueraDeServicio = Math.max(1, numCasilleros / 2);
        System.out.println("[MatrizCasilleros] Inicializada con " + numCasilleros + " casilleros.");
        System.out.println("[MatrizCasilleros] Límite máximo de casilleros FueraDeServicio establecido en: " + this.maxFueraDeServicio);
        // --- FIN INICIALIZACIÓN ---
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

    // Libera un casillero específico (SIN CAMBIOS)
    public void liberarCasillero(int casilleroId) {
        if (casilleroId < 0 || casilleroId >= totalCasilleros) return;
        matrizLock.lock();
        try {
            // Solo liberar si está OCUPADO o FUERA_DE_SERVICIO (aunque FdS no debería pasar por aquí normalmente)
            EstadoCasillero estadoActual = casilleros[casilleroId].getEstado();
            if (estadoActual == EstadoCasillero.OCUPADO) {
                 casilleros[casilleroId].liberar();
                 //System.out.println("Casillero " + casilleroId + " liberado.");
            } else if (estadoActual == EstadoCasillero.FUERA_DE_SERVICIO) {
                // Opcional: Podrías decidir si 'liberar' también repara un FdS,
                // pero la lógica actual asume que FdS es (casi) permanente.
                // No hacemos nada aquí para mantener la consistencia con FdS.
                 // System.out.println("Intento de liberar casillero " + casilleroId + " que está FdS. Ignorado.");
            }
        } finally {
            matrizLock.unlock();
        }
    }

    // Pone un casillero específico fuera de servicio, PERO CON LÍMITE
    public void ponerFueraDeServicio(int casilleroId) {
         if (casilleroId < 0 || casilleroId >= totalCasilleros) return;
        matrizLock.lock(); // Asegura exclusión mutua para leer contador y estado
        try {
            // Verificar si el casillero está realmente en un estado desde el cual puede pasar a FdS (OCUPADO)
            if (casilleros[casilleroId].getEstado() == EstadoCasillero.OCUPADO) {

                // --- LÓGICA DEL LÍMITE ---
                if (fueraDeServicioCount.get() < maxFueraDeServicio) {
                    // Límite NO alcanzado: Marcar como FdS e incrementar contador
                    casilleros[casilleroId].ponerFueraDeServicio();
                    fueraDeServicioCount.incrementAndGet(); // Incremento atómico
                    // System.out.println("Casillero " + casilleroId + " puesto fuera de servicio. Total FdS: " + fueraDeServicioCount.get());
                } else {
                    // Límite ALCANZADO: Liberar el casillero en lugar de marcarlo FdS
                    casilleros[casilleroId].liberar(); // Lo hacemos VACIO para que pueda reutilizarse
                    System.out.println("[MatrizCasilleros] Límite FdS (" + maxFueraDeServicio + ") alcanzado. Casillero " + casilleroId + " liberado en su lugar.");
                }
                // --- FIN LÓGICA DEL LÍMITE ---

            } else {
                 // Si no estaba OCUPADO, no hacemos nada (ya está VACIO o FdS)
                 // System.out.println("Intento de poner FdS casillero " + casilleroId + ", pero no estaba OCUPADO (estado=" + casilleros[casilleroId].getEstado() + "). Ignorando.");
            }
        } finally {
            matrizLock.unlock();
        }
    }

    // Obtiene estadísticas finales (ACTUALIZADO PARA MOSTRAR CONTADOR Y LÍMITE)
    public String getEstadisticas() {
        matrizLock.lock(); // Bloquear para obtener una foto consistente
        try {
            long vacios = 0;
            long ocupados = 0;
            long fueraServicio = 0;
            long totalUsos = 0;

            for (Casillero c : casilleros) {
                totalUsos += c.getContadorUso();
                switch (c.getEstado()) {
                    case VACIO: vacios++; break;
                    case OCUPADO: ocupados++; break; // No debería haber ocupados al final si todo terminó bien
                    case FUERA_DE_SERVICIO: fueraServicio++; break;
                }
            }
             // Devolvemos la cadena formateada incluyendo el contador y el límite
            return String.format(
                "Estado Final Casilleros: Vacíos=%d, Ocupados=%d, FueraDeServicio=%d (Contador FdS: %d, Límite: %d) | Usos Totales Registrados=%d",
                vacios, ocupados, fueraServicio, fueraDeServicioCount.get(), this.maxFueraDeServicio, totalUsos
            );
        } finally {
            matrizLock.unlock();
        }
    }

    // --- Método Opcional (No usado en el flujo actual, pero podría ser útil) ---
    // Podrías añadir un método para "reparar" casilleros FdS si quisieras
    /*
    public void repararCasillero(int casilleroId) {
        if (casilleroId < 0 || casilleroId >= totalCasilleros) return;
        matrizLock.lock();
        try {
            if (casilleros[casilleroId].getEstado() == EstadoCasillero.FUERA_DE_SERVICIO) {
                casilleros[casilleroId].liberar(); // Lo pone VACIO
                fueraDeServicioCount.decrementAndGet(); // Decrementa el contador
                System.out.println("Casillero " + casilleroId + " reparado y puesto como VACIO. Total FdS: " + fueraDeServicioCount.get());
            }
        } finally {
            matrizLock.unlock();
        }
    }
    */
}