// Codigo/RegistroPedidos.java
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class RegistroPedidos {

    private final Map<EstadoPedido, List<Pedido>> pedidosPorEstado = new HashMap<>();
    private volatile int totalPedidosGenerados = 0; // Volatile para visibilidad
    private final Random random = new Random();

    public RegistroPedidos() {
        for (EstadoPedido estado : EstadoPedido.values()) {
            pedidosPorEstado.put(estado, new ArrayList<>());
        }
    }

    public boolean colasVacias() {
        return this.getCantidad(EstadoPedido.PREPARACION) == 0 &&
               this.getCantidad(EstadoPedido.TRANSITO) == 0 &&
               this.getCantidad(EstadoPedido.ENTREGADO) == 0;
    }

    public void agregarPedido(Pedido pedido, EstadoPedido estado) {
        List<Pedido> lista = pedidosPorEstado.get(estado);
        // Sincronizamos sobre la lista específica para evitar bloquear el MAPA completo
        synchronized (lista) {
            lista.add(pedido);
        }
    }

    public Pedido obtenerYRemoverPedidoAleatorio(EstadoPedido estado) {
        List<Pedido> lista = pedidosPorEstado.get(estado);
        if (lista == null) {
            return null;
        }
        synchronized (lista) {
            if (lista.isEmpty()) {
                return null;
            }
            int index = random.nextInt(lista.size());
            return lista.remove(index);
        }
    }


    public synchronized void incrementarTotalGenerados() {
        totalPedidosGenerados++;
    }

    public int getTotalPedidosGenerados() {
        return totalPedidosGenerados;
    }

    public int getCantidad(EstadoPedido estado) {
        List<Pedido> lista = pedidosPorEstado.get(estado);
        if (lista == null) return 0;
        synchronized (lista) {
            return lista.size();
        }
    }
}