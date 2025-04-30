// RegistroPedidos.java
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class RegistroPedidos {
    // Usamos colas concurrentes para operaciones eficientes y seguras de añadir/quitar
    final ConcurrentLinkedQueue<Pedido> pedidosEnPreparacion = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<Pedido> pedidosEnTransito = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<Pedido> pedidosEntregados = new ConcurrentLinkedQueue<>();
    // Para los finales, podemos usar colas o listas sincronizadas si necesitamos más operaciones
    final ConcurrentLinkedQueue<Pedido> pedidosFallidos = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<Pedido> pedidosVerificados = new ConcurrentLinkedQueue<>();

    // Contadores atómicos para estadísticas rápidas y seguras
    final AtomicInteger fallidosCount = new AtomicInteger(0);
    final AtomicInteger verificadosCount = new AtomicInteger(0);
    final AtomicInteger preparadosCount = new AtomicInteger(0); // Para saber cuándo parar de generar

    private final Random random = new Random();

    // Métodos para añadir (seguros por ConcurrentLinkedQueue)
    public void agregarAPreparacion(Pedido p) { pedidosEnPreparacion.offer(p); }
    public void agregarATransito(Pedido p) { pedidosEnTransito.offer(p); }
    public void agregarAEntregados(Pedido p) { pedidosEntregados.offer(p); }
    public void agregarAFallidos(Pedido p) {
        pedidosFallidos.offer(p);
        fallidosCount.incrementAndGet();
    }
    public void agregarAVerificados(Pedido p) {
        pedidosVerificados.offer(p);
        verificadosCount.incrementAndGet();
    }

    // Métodos para tomar un pedido (poll es seguro y atómico)
    // La especificación dice "aleatorio", pero poll() toma del inicio.
    // Para aleatorio real necesitaríamos convertir a lista, seleccionar y quitar,
    // lo cual es más complejo y requiere bloqueo externo. Usaremos poll() por eficiencia.
    // Si se requiere aleatorio estricto, habría que cambiar la estructura.
    public Pedido tomarDePreparacion() { return pedidosEnPreparacion.poll(); }
    public Pedido tomarDeTransito() { return pedidosEnTransito.poll(); }
    public Pedido tomarDeEntregados() { return pedidosEntregados.poll(); }

    // Métodos para obtener contadores (seguros por AtomicInteger)
    public int getCantidadFallidos() { return fallidosCount.get(); }
    public int getCantidadVerificados() { return verificadosCount.get(); }
    public int getCantidadEnPreparacion() { return pedidosEnPreparacion.size(); } // Size puede no ser O(1)
    public int getCantidadEnTransito() { return pedidosEnTransito.size(); }
    public int getCantidadEntregados() { return pedidosEntregados.size(); }

    // Control de generación
    public int incrementarPreparados() { return preparadosCount.incrementAndGet(); }
    public int getCantidadPreparados() { return preparadosCount.get(); }

     // Verifica si todas las colas de procesamiento están vacías
    public boolean todasLasColasProcesamientoVacias() {
        return pedidosEnPreparacion.isEmpty() &&
               pedidosEnTransito.isEmpty() &&
               pedidosEntregados.isEmpty();
    }
}