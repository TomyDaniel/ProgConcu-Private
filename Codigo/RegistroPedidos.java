import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class RegistroPedidos {
    private final List<Pedido> pedidosPreparacion = new CopyOnWriteArrayList<>();
    private final List<Pedido> pedidosTransito = new CopyOnWriteArrayList<>();
    private final List<Pedido> pedidosEntregados = new CopyOnWriteArrayList<>();
    private final List<Pedido> pedidosFallidos = new CopyOnWriteArrayList<>();
    private final List<Pedido> pedidosVerificados = new CopyOnWriteArrayList<>();

    private final AtomicInteger preparadosCount = new AtomicInteger(0);
    private final Random random = new Random();

    // Métodos para agregar pedidos a las listas
    public void agregarAPreparacion(Pedido pedido) {
        pedidosPreparacion.add(pedido);
    }

    public void agregarATransito(Pedido pedido) {
        pedidosTransito.add(pedido);
    }

    public void agregarAEntregados(Pedido pedido) {
        pedidosEntregados.add(pedido);
    }

    public void agregarAFallidos(Pedido pedido) {
        pedidosFallidos.add(pedido);
    }

    public void agregarAVerificados(Pedido pedido) {
        pedidosVerificados.add(pedido);
    }

    // Métodos para remover pedidos de las listas
    public void removerDePreparacion(Pedido pedido) {
        pedidosPreparacion.remove(pedido);
    }

    public void removerDeTransito(Pedido pedido) {
        pedidosTransito.remove(pedido);
    }

    public void removerDeEntregados(Pedido pedido) {
        pedidosEntregados.remove(pedido);
    }

    // Métodos para obtener pedidos aleatorios
    public Pedido obtenerPedidoPreparacionAleatorio() {
        return obtenerPedidoAleatorio(pedidosPreparacion);
    }

    public Pedido obtenerPedidoTransitoAleatorio() {
        return obtenerPedidoAleatorio(pedidosTransito);
    }

    public Pedido obtenerPedidoEntregadoAleatorio() {
        return obtenerPedidoAleatorio(pedidosEntregados);
    }

    //Agregar una sincronizacion extra en pedido aleatorio
    private  Pedido obtenerPedidoAleatorio(List<Pedido> lista) {
        if (lista.isEmpty()) {
            return null;
        }
        int index = random.nextInt(lista.size());
        return lista.get(index);
    }

    // Contador de pedidos preparados
    public void incrementarPreparados() {
        preparadosCount.incrementAndGet();
    }

    public int getCantidadPreparados() {
        return preparadosCount.get();
    }

    // Métodos para obtener estadísticas
    public int getCantidadEnPreparacion() {
        return pedidosPreparacion.size();
    }

    public int getCantidadEnTransito() {
        return pedidosTransito.size();
    }

    public int getCantidadEntregados() {
        return pedidosEntregados.size();
    }

    public int getCantidadFallidos() {
        return pedidosFallidos.size();
    }

    public int getCantidadVerificados() {
        return pedidosVerificados.size();
    }
}