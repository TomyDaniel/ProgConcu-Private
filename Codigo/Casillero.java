// --- Clases de Datos ---
// Casillero.java
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock; // Aunque no lo usemos activamente aquí si bloqueamos en Matriz

public class Casillero {
    private final int id;
    private volatile EstadoCasillero estado; // volatile para visibilidad entre hilos
    private final AtomicInteger contadorUso = new AtomicInteger(0);
    // Lock individual podría ser útil para operaciones atómicas complejas,
    // pero por simplicidad, bloquearemos en MatrizCasilleros.
    // private final ReentrantLock lock = new ReentrantLock();

    public Casillero(int id) {
        this.id = id;
        this.estado = EstadoCasillero.VACIO;
    }

    public int getId() { return id; }
    public EstadoCasillero getEstado() { return estado; }
    public int getContadorUso() { return contadorUso.get(); }

    // Las actualizaciones de estado y contador se harán desde MatrizCasilleros
    // bajo el bloqueo de la matriz para asegurar atomicidad en las operaciones
    // que involucran búsqueda y actualización.
    void ocupar() {
        this.estado = EstadoCasillero.OCUPADO;
        this.contadorUso.incrementAndGet();
    }

    void liberar() {
        this.estado = EstadoCasillero.VACIO;
    }

    void ponerFueraDeServicio() {
        this.estado = EstadoCasillero.FUERA_DE_SERVICIO;
    }

    @Override
    public String toString() {
        return "Casillero [id=" + id + ", estado=" + estado + ", usos=" + contadorUso.get() + "]";
    }
}