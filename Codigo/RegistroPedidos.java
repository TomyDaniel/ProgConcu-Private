// Codigo/RegistroPedidos.java
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class RegistroPedidos {

    private final Map<EstadoPedido, List<Pedido>> pedidosPorEstado = new HashMap<>();
    private volatile int totalPedidosGenerados = 0; // Volatile para visibilidad
    private final Object totalGeneradosLock = new Object(); // Lock para el contador
    private final Random random = new Random();

    public RegistroPedidos() {
        for (EstadoPedido estado : EstadoPedido.values()) {
            // Usamos ArrayList estándar, la sincronización será externa.
            pedidosPorEstado.put(estado, new ArrayList<>());
        }
    }

    public boolean colasVacias() {
        // Esta operación lee tamaños. Para una consistencia perfecta,
        // necesitaría adquirir todos los locks, lo cual es complejo y propenso a deadlocks.
        // Para una verificación de "finalización", una lectura no estrictamente atómica
        // de todas las colas suele ser aceptable si la condición se verifica repetidamente.
        // Si se necesita atomicidad estricta aquí, la estrategia sería más compleja.
        return this.getCantidad(EstadoPedido.PREPARACION) == 0 &&
               this.getCantidad(EstadoPedido.TRANSITO) == 0 &&
               this.getCantidad(EstadoPedido.ENTREGADO) == 0;
    }

    public void agregarPedido(Pedido pedido, EstadoPedido estado) {
        List<Pedido> lista = pedidosPorEstado.get(estado);
        // Sincronizamos sobre la lista específica
        synchronized (lista) {
            lista.add(pedido);
        }
    }

    // Nuevo método atómico para obtener y remover un pedido aleatorio
    public Pedido obtenerYRemoverPedidoAleatorio(EstadoPedido estado) {
        List<Pedido> lista = pedidosPorEstado.get(estado);
        if (lista == null) { // No debería ocurrir con la inicialización actual
            return null;
        }
        // Sincronizamos sobre la lista específica
        synchronized (lista) {
            if (lista.isEmpty()) {
                return null;
            }
            int index = random.nextInt(lista.size());
            return lista.remove(index); // remove() devuelve el elemento eliminado
        }
    }


    public void incrementarTotalGenerados() {
        synchronized (totalGeneradosLock) {
            totalPedidosGenerados++;
        }
    }

    public int getTotalPedidosGenerados() {
        return totalPedidosGenerados; // La lectura de volatile es atómica
    }

    public int getCantidad(EstadoPedido estado) {
        List<Pedido> lista = pedidosPorEstado.get(estado);
        if (lista == null) return 0; // No debería ocurrir
        // Sincronizamos sobre la lista específica para leer su tamaño consistentemente
        synchronized (lista) {
            return lista.size();
        }
    }
}