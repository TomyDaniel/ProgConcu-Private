// Casillero.java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Casillero {

    private EstadoCasillero estado;
    private int vecesOcupado;
    private final Lock lock = new ReentrantLock(); // Lock individual para este casillero

    public Casillero() {
        this.estado = EstadoCasillero.VACIO;
        this.vecesOcupado = 0;
    }

    // El método ocupar ahora manejará su propio bloqueo
    public boolean intentarOcupar() { // Cambiado para devolver boolean y no lanzar excepción aquí
        lock.lock();
        try {
            if (estado == EstadoCasillero.VACIO) {
                estado = EstadoCasillero.OCUPADO;
                vecesOcupado++;
                return true; // Ocupación exitosa
            }
            return false; // No se pudo ocupar (ya estaba ocupado, etc.)
        } finally {
            lock.unlock();
        }
    }

    // El método liberar también manejará su propio bloqueo
    public boolean intentarLiberar() { // Cambiado para devolver boolean
        lock.lock();
        try {
            if (estado == EstadoCasillero.OCUPADO) {
                estado = EstadoCasillero.VACIO;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    // Marcar fuera de servicio también con su lock
    public void marcarFueraDeServicioConLock() { // Renombrado para claridad si mantienes el otro
        lock.lock();
        try {
            estado = EstadoCasillero.FUERA_DE_SERVICIO;
        } finally {
            lock.unlock();
        }
    }

    public EstadoCasillero getEstado() {
        // Para lecturas simples del estado, podrías considerar si el lock es estrictamente
        // necesario si el estado se actualiza siempre bajo lock.
        // Por consistencia y si hay múltiples campos que leer, es más seguro con lock.
        lock.lock();
        try {
            return estado;
        } finally {
            lock.unlock();
        }
    }

    public int getVecesOcupado() {
        lock.lock();
        try {
            return vecesOcupado;
        } finally {
            lock.unlock();
        }
    }
}