// Codigo/Casillero.java
public class Casillero {

    private EstadoCasillero estado;
    private int vecesOcupado;

    public Casillero() {
        this.estado = EstadoCasillero.VACIO;
        this.vecesOcupado = 0;
    }

    // Sincronizar para proteger el estado interno del casillero
    public synchronized void ocupar() throws IllegalStateException {
        if (estado != EstadoCasillero.VACIO) {
            throw new IllegalStateException("No se puede ocupar un casillero que no está vacío");
        }
        estado = EstadoCasillero.OCUPADO;
        vecesOcupado++;
    }

    public synchronized void liberar() throws IllegalStateException{
        if (estado != EstadoCasillero.OCUPADO) {
            throw new IllegalStateException("No se puede liberar un casillero que no está ocupado");
        }
        estado = EstadoCasillero.VACIO;
    }

    public synchronized void marcarFueraDeServicio() throws IllegalStateException {
        // Se podría permitir marcar fuera de servicio un casillero VACIO también.
        // Por ahora, mantenemos la lógica original.
        if (estado != EstadoCasillero.OCUPADO) {
            // Considera si esta lógica es la deseada o si un casillero VACIO también podría ponerse FUERA_DE_SERVICIO.
            // Si la lógica es que sólo se pone fuera de servicio tras un fallo de DESPACHO, entonces OCUPADO es correcto.
            throw new IllegalStateException("No se puede marcar como fuera de servicio un casillero que no estaba ocupado (estaba " + estado + ")");
        }
        estado = EstadoCasillero.FUERA_DE_SERVICIO;
    }

    public synchronized EstadoCasillero getEstado() {
        return estado;
    }

    public synchronized int getVecesOcupado() {
        return vecesOcupado;
    }
}