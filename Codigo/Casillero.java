
public class Casillero {

    private EstadoCasillero estado;
    private int vecesOcupado;

    public Casillero() {
        this.estado = EstadoCasillero.VACIO;
        this.vecesOcupado = 0;
    }

    public void ocupar() throws IllegalStateException {
        if (estado != EstadoCasillero.VACIO) {
            throw new IllegalStateException("No se puede ocupar un casillero que no está vacío");
        }
        estado = EstadoCasillero.OCUPADO;
        vecesOcupado++;
    }

    public void liberar() throws IllegalStateException{
        if (estado != EstadoCasillero.OCUPADO) {
            throw new IllegalStateException("No se puede liberar un casillero que no está ocupado");
        }
        estado = EstadoCasillero.VACIO;
    }

    public void marcarFueraDeServicio() {
        estado = EstadoCasillero.FUERA_DE_SERVICIO;
    }

    public EstadoCasillero getEstado() {
        return estado;
    }

    public int getVecesOcupado() {
        return vecesOcupado;
    }
}