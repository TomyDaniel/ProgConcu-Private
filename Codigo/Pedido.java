// Pedido.java
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;

public class Pedido {
    private static final AtomicInteger idCounter = new AtomicInteger(0);
    private final int id;
    private volatile int casilleroIdAsignado = -1; // ID del casillero mientras está en preparación
    private final ReentrantLock lock = new ReentrantLock(); // Lock por pedido es CRUCIAL

    public Pedido() {
        this.id = idCounter.incrementAndGet();
    }

    public int getId() { return id; }
    public int getCasilleroIdAsignado() { return casilleroIdAsignado; }
    public void asignarCasillero(int casilleroId) { this.casilleroIdAsignado = casilleroId; }
    public void liberarCasillero() { this.casilleroIdAsignado = -1; }

    // Métodos para bloquear/desbloquear el pedido específico
    public void lock() { lock.lock(); }
    public void unlock() { lock.unlock(); }

    @Override
    public String toString() {
        return "Pedido [id=" + id + ", casilleroId=" + casilleroIdAsignado + "]";
    }
     @Override
    public int hashCode() { // Necesario si se usan en HashMaps/Sets
        return Integer.hashCode(id);
    }
     @Override
    public boolean equals(Object obj) { // Necesario si se usan en HashMaps/Sets
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Pedido other = (Pedido) obj;
        return id == other.id;
    }
}

