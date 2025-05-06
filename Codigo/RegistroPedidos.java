import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;

public class RegistroPedidos {

    // Mapa para almacenar las listas de pedidos por estado
    private final Map<EstadoPedido, List<Pedido>> pedidosPorEstado = new ConcurrentHashMap<>();
    // Contador para el número total de pedidos que se han iniciado a preparar
    private final AtomicInteger totalPedidosGenerados = new AtomicInteger(0);
    private final Random random = new Random();

    public RegistroPedidos() {
        // Inicializar una lista para cada estado definido en el enum
        for (EstadoPedido estado : EstadoPedido.values()) {
            pedidosPorEstado.put(estado, new CopyOnWriteArrayList<>());
        }
    }

    public boolean colasVacias(){
        //Comprobamos que TODOS los pedidos ya no esten en las colas intermedias y solo esten en VERIFICADOS y FALLIDOS
        return this.getCantidad(EstadoPedido.PREPARACION) == 0 &&
                this.getCantidad(EstadoPedido.TRANSITO) == 0 &&
                this.getCantidad(EstadoPedido.ENTREGADO) == 0;
    }

    public void agregarPedido(Pedido pedido, EstadoPedido estado) {
        pedidosPorEstado.get(estado).add(pedido);
    }

    public boolean removerPedido(Pedido pedido, EstadoPedido estado) { // Cambiado a boolean
        List<Pedido> lista = pedidosPorEstado.get(estado);
        if (lista != null) {
            // remove() en CopyOnWriteArrayList devuelve boolean
            return lista.remove(pedido);
        }
        return false; // No se pudo remover (lista no encontrada o pedido no presente)
    }

    public Pedido obtenerPedidoAleatorio(EstadoPedido estado) {
        List<Pedido> lista = pedidosPorEstado.get(estado);
        if (lista == null || lista.isEmpty()) {
            return null;
        }
        int index = random.nextInt(lista.size());
        try {
            return lista.get(index);
        } catch (IndexOutOfBoundsException e) {
            // Puede ocurrir en raras condiciones de concurrencia si la lista se vacía
            // entre la comprobación isEmpty/size() y el get().
            return null;
        }
    }

    public int getCantidad(EstadoPedido estado) {
        List<Pedido> lista = pedidosPorEstado.get(estado);
        // Las listas se inicializan, así que no deberían ser null
        return (lista != null) ? lista.size() : 0;
    }

    public void incrementarTotalGenerados() {
        totalPedidosGenerados.incrementAndGet();
    }

    public int getTotalPedidosGenerados() {
        return totalPedidosGenerados.get();
    }
}