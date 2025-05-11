public class Pedido {

    private static int proximoId = 0;
    private final int id; //Representa el número de pedido
    private int casilleroId = -1; //Representa la posición del casillero seleccionado para ese pedido

    public Pedido() {this.id = generarId();
    }

    private static synchronized int generarId() {return proximoId++;
    }
    public void asignarCasillero(int casilleroId) {
        this.casilleroId = casilleroId;
    }

    public int getId() {return id;
    }

    public int getCasilleroId() {return casilleroId;
    }

    @Override
    public String toString() {
        return "Pedido #" + getId() + " (casillero: " + getCasilleroId() + ")";
    }
}