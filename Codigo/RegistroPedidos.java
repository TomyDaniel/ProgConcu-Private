import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;
// Eliminar import java.util.concurrent.locks.Lock; (ya no se usa)

public class RegistroPedidos {

    // Mapa para almacenar las listas de pedidos por estado
    private final Map<EstadoPedido, List<Pedido>> pedidosPorEstado = new ConcurrentHashMap<>();
    // Contador para el número total de pedidos que se han iniciado a preparar
    private final AtomicInteger totalPedidosGenerados = new AtomicInteger(0);
    private final Random random = new Random();
    // Eliminar el campo lock: private final Lock lock;

    public RegistroPedidos() {
        // Inicializar una lista para cada estado definido en el enum
        for (EstadoPedido estado : EstadoPedido.values()) {
            pedidosPorEstado.put(estado, new CopyOnWriteArrayList<>());
        }
    }

    // Método tomarPedido modificado para no usar el lock externo
    public Pedido tomarPedido(EstadoPedido estado) {
        // Ya no se usa lock.lock() / lock.unlock()
        List<Pedido> lista = pedidosPorEstado.get(estado);
        if (lista != null && !lista.isEmpty()) {
            // Advertencia: remove(0) en CopyOnWriteArrayList es ineficiente si se llama a menudo.
            // Considerar una estructura diferente si este es un cuello de botella.
            // O si se necesita un elemento aleatorio, usar obtenerPedidoAleatorio y luego removerPedido.
            try {
                // Tomamos el primero por simplicidad, como estaba antes.
                return lista.remove(0);
            } catch (IndexOutOfBoundsException e) {
                // Puede ocurrir si la lista se vacía concurrentemente.
                return null;
            }
        }
        return null; // No hay pedidos en ese estado
    }


    /**
     * Agrega un pedido a la lista del estado especificado.
     * @param pedido El pedido a agregar.
     * @param estado El estado al que pertenece el pedido.
     */
    public void agregarPedido(Pedido pedido, EstadoPedido estado) {
        pedidosPorEstado.get(estado).add(pedido);
    }

    /**
     * Remueve un pedido de la lista del estado especificado.
     * @param pedido El pedido a remover.
     * @param estado El estado del que se debe remover el pedido.
     * @return true si el pedido fue removido, false en caso contrario.
     */
    public boolean removerPedido(Pedido pedido, EstadoPedido estado) { // Cambiado a boolean
        List<Pedido> lista = pedidosPorEstado.get(estado);
        if (lista != null) {
            // remove() en CopyOnWriteArrayList devuelve boolean
            return lista.remove(pedido);
        }
        return false; // No se pudo remover (lista no encontrada o pedido no presente)
    }

    /**
     * Obtiene un pedido aleatorio de la lista del estado especificado.
     * ¡Importante! Este método NO remueve el pedido de la lista.
     * @param estado El estado del cual obtener un pedido.
     * @return Un pedido aleatorio de esa lista, o null si la lista está vacía.
     */
    public Pedido obtenerPedidoAleatorio(EstadoPedido estado) {
        List<Pedido> lista = pedidosPorEstado.get(estado);
        if (lista == null || lista.isEmpty()) {
            return null;
        }
        // nextInt es exclusivo en el límite superior, por lo que size() es correcto.
        int index = random.nextInt(lista.size());
        try {
            return lista.get(index);
        } catch (IndexOutOfBoundsException e) {
            // Puede ocurrir en raras condiciones de concurrencia si la lista se vacía
            // entre la comprobación isEmpty/size() y el get().
            return null;
        }
    }

    /**
     * Obtiene la cantidad de pedidos en un estado específico.
     * @param estado El estado del cual consultar la cantidad.
     * @return El número de pedidos en ese estado.
     */
    public int getCantidad(EstadoPedido estado) {
        List<Pedido> lista = pedidosPorEstado.get(estado);
        // Las listas se inicializan, así que no deberían ser null.
        return (lista != null) ? lista.size() : 0;
    }

    /**
     * Incrementa el contador total de pedidos generados.
     * Debe ser llamado por el Preparador cuando crea un nuevo pedido.
     */
    public void incrementarTotalGenerados() {
        totalPedidosGenerados.incrementAndGet();
    }

    /**
     * Obtiene el número total de pedidos que han sido generados (iniciados).
     * Usado para controlar el fin de la simulación.
     * @return El contador total de pedidos generados.
     */
    public int getTotalPedidosGenerados() {
        return totalPedidosGenerados.get();
    }
}