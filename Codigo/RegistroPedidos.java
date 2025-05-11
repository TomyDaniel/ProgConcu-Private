import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class RegistroPedidos {

    // Mapa para almacenar las listas de pedidos por estado
    private final Map<EstadoPedido, List<Pedido>> pedidosPorEstado = new HashMap<>();
    // Contador para el número total de pedidos que se han iniciado a preparar
    private int totalPedidosGenerados = 0;
    private final Random random = new Random();

    public RegistroPedidos() {
        // Inicializar una lista para cada estado definido en el enum
        for (EstadoPedido estado : EstadoPedido.values()) {
            pedidosPorEstado.put(estado, new ArrayList<>());
        }
    }

    public boolean colasVacias(){
        //Comprobamos que TODOS los pedidos ya no esten en las colas intermedias y solo esten en VERIFICADOS y FALLIDOS
        return this.getCantidad(EstadoPedido.PREPARACION) == 0 &&
                this.getCantidad(EstadoPedido.TRANSITO) == 0 &&
                this.getCantidad(EstadoPedido.ENTREGADO) == 0;
    }

    public synchronized void agregarPedido(Pedido pedido, EstadoPedido estado) {
        pedidosPorEstado.get(estado).add(pedido);
    }

    public synchronized boolean removerPedido(Pedido pedido, EstadoPedido estado) {
        List<Pedido> lista = pedidosPorEstado.get(estado);
        if (lista != null) {
            return lista.remove(pedido);
        }
        return false; // No se pudo remover (el pedido no existe)
    }

    public synchronized void incrementarTotalGenerados() {
        totalPedidosGenerados++;
    }

    public int getTotalPedidosGenerados() {
        return totalPedidosGenerados;
    }

    public synchronized Pedido obtenerPedidoAleatorio(EstadoPedido estado) {
        List<Pedido> lista = pedidosPorEstado.get(estado);
        if (lista == null || lista.isEmpty()) {
            return null; //Todavia no existe ningun pedido
        }
        int index = random.nextInt(lista.size());   //Elegimos una posicion aleatoria de la lista
        return lista.get(index);

    }

    public synchronized int getCantidad(EstadoPedido estado) {
        List<Pedido> lista = pedidosPorEstado.get(estado);
        // Las listas se inicializan, así que no deberían ser null
        return (lista != null) ? lista.size() : 0;
    }


}