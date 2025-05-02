// RegistroPedidos.java
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock; // Necesario para la selección aleatoria
import java.util.List;
import java.util.ArrayList;
import java.util.Collections; // Para shuffle o random index
import java.util.Random;

public class RegistroPedidos {
    // Colas concurrentes
    final ConcurrentLinkedQueue<Pedido> pedidosEnPreparacion = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<Pedido> pedidosEnTransito = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<Pedido> pedidosEntregados = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<Pedido> pedidosFallidos = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<Pedido> pedidosVerificados = new ConcurrentLinkedQueue<>();

    // Contadores atómicos
    final AtomicInteger fallidosCount = new AtomicInteger(0);
    final AtomicInteger verificadosCount = new AtomicInteger(0);
    final AtomicInteger preparadosCount = new AtomicInteger(0);

    private final Random random = new Random();

    // --- Locks para selección aleatoria ---
    private final ReentrantLock preparacionLock = new ReentrantLock();
    private final ReentrantLock transitoLock = new ReentrantLock();
    private final ReentrantLock entregadosLock = new ReentrantLock();

    // Métodos para añadir (no necesitan lock externo)
    public void agregarAPreparacion(Pedido p) { pedidosEnPreparacion.offer(p); }
    public void agregarATransito(Pedido p) { pedidosEnTransito.offer(p); }
    public void agregarAEntregados(Pedido p) { pedidosEntregados.offer(p); }
    public void agregarAFallidos(Pedido p) {
        pedidosFallidos.offer(p);
        fallidosCount.incrementAndGet();
    }
    public void agregarAVerificados(Pedido p) {
        pedidosVerificados.offer(p);
        verificadosCount.incrementAndGet();
    }

    // --- Métodos para TOMAR ALEATORIAMENTE (CORREGIDOS) ---

    /**
     * Toma un pedido aleatorio de la cola de preparación.
     * Reemplaza drainTo con un bucle poll().
     * @return Un pedido aleatorio, o null si la cola está vacía.
     */
    public Pedido tomarDePreparacionAleatorio() {
        preparacionLock.lock();
        try {
            List<Pedido> tempList = new ArrayList<>();
            // *** CORRECCIÓN: Usar bucle poll() en lugar de drainTo ***
            while (true) {
                Pedido item = pedidosEnPreparacion.poll();
                if (item == null) {
                    break; // La cola está vacía
                }
                tempList.add(item);
            }
            // *** FIN CORRECCIÓN ***

            if (tempList.isEmpty()) {
                return null;
            }

            int randomIndex = random.nextInt(tempList.size());
            Pedido pedidoSeleccionado = tempList.remove(randomIndex);

            // Volver a añadir los elementos restantes a la cola concurrente
            // addAll es seguro aquí porque la cola fue vaciada bajo lock
            pedidosEnPreparacion.addAll(tempList);

            return pedidoSeleccionado;

        } finally {
            preparacionLock.unlock();
        }
    }

    /**
     * Toma un pedido aleatorio de la cola de tránsito.
     * Reemplaza drainTo con un bucle poll().
     * @return Un pedido aleatorio, o null si la cola está vacía.
     */
    public Pedido tomarDeTransitoAleatorio() {
        transitoLock.lock();
        try {
            List<Pedido> tempList = new ArrayList<>();
            // *** CORRECCIÓN: Usar bucle poll() en lugar de drainTo ***
             while (true) {
                Pedido item = pedidosEnTransito.poll();
                if (item == null) {
                    break;
                }
                tempList.add(item);
            }
            // *** FIN CORRECCIÓN ***


            if (tempList.isEmpty()) {
                return null;
            }

            int randomIndex = random.nextInt(tempList.size());
            Pedido pedidoSeleccionado = tempList.remove(randomIndex);
            pedidosEnTransito.addAll(tempList); // Re-agregar los restantes

            return pedidoSeleccionado;
        } finally {
            transitoLock.unlock();
        }
    }

    /**
     * Toma un pedido aleatorio de la cola de entregados.
     * Reemplaza drainTo con un bucle poll().
     * @return Un pedido aleatorio, o null si la cola está vacía.
     */
    public Pedido tomarDeEntregadosAleatorio() {
        entregadosLock.lock();
        try {
            List<Pedido> tempList = new ArrayList<>();
            // *** CORRECCIÓN: Usar bucle poll() en lugar de drainTo ***
            while (true) {
                Pedido item = pedidosEntregados.poll();
                if (item == null) {
                    break;
                }
                tempList.add(item);
            }
             // *** FIN CORRECCIÓN ***


            if (tempList.isEmpty()) {
                return null;
            }

            int randomIndex = random.nextInt(tempList.size());
            Pedido pedidoSeleccionado = tempList.remove(randomIndex);
            pedidosEntregados.addAll(tempList); // Re-agregar los restantes

            return pedidoSeleccionado;
        } finally {
            entregadosLock.unlock();
        }
    }

    // Métodos para obtener contadores
    public int getCantidadFallidos() { return fallidosCount.get(); }
    public int getCantidadVerificados() { return verificadosCount.get(); }
    public int getCantidadEnPreparacion() { return pedidosEnPreparacion.size(); } // O(N)
    public int getCantidadEnTransito() { return pedidosEnTransito.size(); } // O(N)
    public int getCantidadEntregados() { return pedidosEntregados.size(); } // O(N)

    // Control de generación
    public int incrementarPreparados() { return preparadosCount.incrementAndGet(); }
    public int getCantidadPreparados() { return preparadosCount.get(); }

     // Verifica si todas las colas de procesamiento están vacías
    public boolean todasLasColasProcesamientoVacias() {
        // isEmpty() es preferible a size() == 0 para rendimiento O(1)
        return pedidosEnPreparacion.isEmpty() &&
               pedidosEnTransito.isEmpty() &&
               pedidosEntregados.isEmpty();
    }
}