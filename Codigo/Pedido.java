import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
public class Pedido {
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    private final int id;
    private int casilleroId = -1;
    private final ReentrantLock lock = new ReentrantLock(); // Lock por pedido es CRUCIAL

    public Pedido() {
        this.id = ID_GENERATOR.incrementAndGet();
    }

    public void asignarCasillero(int casilleroId) {
        this.casilleroId = casilleroId;
    }
    // Métodos para bloquear/desbloquear el pedido específico
    public void lock() { lock.lock(); }
    public void unlock() { lock.unlock(); }

    public int getId() {
        return id;
    }

    public int getCasilleroId() {
        return casilleroId;
    }

    @Override
    public String toString() {
        return "Pedido #" + id + " (casillero: " + casilleroId + ")";
    }
}