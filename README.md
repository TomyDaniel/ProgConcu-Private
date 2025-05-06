# Prueba TP1 Concurrente

Este proyecto implementa una simulación de un sistema de procesamiento logístico utilizando concurrencia en Java. Modela el flujo de pedidos a través de múltiples etapas (preparación, despacho, entrega, verificación) gestionadas por diferentes tipos de trabajadores (hilos). La simulación incluye la gestión de recursos compartidos como casilleros y utiliza mecanismos de sincronización para garantizar la correcta ejecución concurrente.

## Características Principales

*   **Flujo de Trabajo Simulado:** Modela el ciclo de vida de un pedido desde su creación hasta su verificación final o fallo.
*   **Procesamiento Concurrente:** Utiliza múltiples hilos (`PreparadorPedido`, `DespachadorPedido`, `EntregadorPedido`, `VerificadorPedido`) gestionados por un `ExecutorService` para simular trabajadores paralelos.
*   **Gestión de Recursos Compartidos:** Simula una `MatrizCasilleros` donde los pedidos se almacenan temporalmente durante la preparación. Incluye lógica para ocupar, liberar y marcar casilleros como fuera de servicio.
*   **Sincronización:** Emplea primitivas de concurrencia de Java como `ReentrantLock`, `ConcurrentLinkedQueue`, `AtomicInteger`, `AtomicBoolean` y `volatile` para manejar el acceso seguro a datos compartidos y coordinar los hilos.
*   **Aleatoriedad:** Incorpora tiempos de procesamiento variables y probabilidades de éxito/fallo en las etapas de despacho, entrega y verificación.
*   **Selección Aleatoria:** Implementa una estrategia para que los trabajadores tomen pedidos de las colas de forma aleatoria (en lugar de estrictamente FIFO).
*   **Logging Detallado:** Genera un archivo `simulacion_logistica.log` con actualizaciones periódicas del estado y un informe final detallado con estadísticas de la simulación (tiempo total, pedidos procesados, estado final de los casilleros, etc.).
*   **Configurable:** Los parámetros clave de la simulación (número de trabajadores, casilleros, pedidos, tiempos de demora, probabilidades) se pueden ajustar fácilmente a través de constantes en `Main.java`.

## Requisitos

*   **Java Development Kit (JDK):** Se recomienda JDK 8 o superior.
*   Un entorno para compilar y ejecutar código Java (como una terminal o un IDE como IntelliJ IDEA, Eclipse, VS Code con extensiones Java).

## Cómo Compilar y Ejecutar

1.  **Navega al directorio del código:**
    ```bash
    cd ProgConcuTest/Codigo
    ```

2.  **Compila los archivos Java:**
    ```bash
    javac *.java
    ```
    *Nota: Si tienes una estructura de paquetes más compleja, ajusta el comando de compilación.*

3.  **Ejecuta la simulación:**
    ```bash
    java Main
    ```

4.  **Observa la salida:** La consola mostrará mensajes de inicio y fin, y posiblemente algunos mensajes de estado. El registro detallado se escribirá en el archivo `../simulacion_logistica.log`.

## Configuración

Los principales parámetros de la simulación se pueden modificar directamente en el archivo `Codigo/Main.java`. Busca las constantes `static final` al principio de la clase, como:

*   `NUM_CASILLEROS`: Número total de casilleros disponibles.
*   `TOTAL_PEDIDOS_A_PROCESAR`: Número total de pedidos que se intentarán procesar.
*   `NUM_PREPARADORES`, `NUM_DESPACHADORES`, etc.: Número de hilos para cada tipo de trabajador.
*   `DEMORA_BASE_*`: Tiempo base (en ms) para cada operación.
*   `VARIACION_DEMORA`: Rango de variación aleatoria sobre el tiempo base.
*   `PROB_*_OK`: Probabilidad de éxito (entre 0.0 y 1.0) para las operaciones de despacho, entrega y verificación.
*   `INTERVALO_LOG_MS`: Frecuencia con la que se escribe el estado en el archivo de log.
*   `ARCHIVO_LOG`: Nombre del archivo de log.

Después de modificar los valores, recompila el código antes de ejecutarlo de nuevo.

## Estructura del Proyecto

```
ProgConcu/
|-- Codigo/ # Código fuente Java de la simulación (.java)
|-- Graficos/ # Diagramas de diseño (UML, secuencia, etc.) (.md)
|-- Img/ # Imagenes de los diagramas de secuencia y diseño (jpeg)
|-- TXT/ # Archivos de texto (posiblemente versiones antiguas o notas)
|-- README.md # Este archivo de descripción
|-- generate_sumary.py # Script Python para generar un resumen del proyecto
|-- simulacion_logistica.log # Archivo de log generado por la ejecución
```

## Conceptos de Diseño y Concurrencia

*   **Modelo Productor-Consumidor:** Las diferentes etapas (preparador -> despachador -> entregador -> verificador) siguen este patrón, utilizando `ConcurrentLinkedQueue` como buffers intermedios.
*   **Bloqueo a Nivel de Pedido:** Cada `Pedido` tiene su propio `ReentrantLock` para evitar que múltiples hilos operen sobre el mismo pedido simultáneamente en etapas conflictivas.
*   **Bloqueo de Recursos Globales:** La `MatrizCasilleros` utiliza un único `ReentrantLock` para proteger el estado de todos los casilleros durante las operaciones de búsqueda y modificación.
*   **Variables Atómicas:** Se usan `AtomicInteger` y `AtomicBoolean` para contadores y flags que necesitan ser actualizados de forma segura por múltiples hilos.
*   **Visibilidad de Memoria:** Se usa `volatile` para variables de estado (como `Casillero.estado`) para asegurar que los cambios sean visibles entre hilos.
*   **Gestión de Hilos:** Se utiliza `ExecutorService` para una gestión más robusta y flexible de los hilos trabajadores.
*   **Finalización Coordinada:** Se usa un `AtomicBoolean running` y la lógica de espera en `Main` para asegurar que la simulación termine ordenadamente una vez procesados todos los pedidos.

## Posibles Mejoras

*   **Optimizar Selección Aleatoria:** La estrategia actual de vaciar y rellenar la cola en `RegistroPedidos` para seleccionar aleatoriamente puede ser ineficiente y causar alta contención. Investigar alternativas.
*   **Bloqueo Granular en Matriz:** El bloqueo único en `MatrizCasilleros` puede ser un cuello de botella. Explorar bloqueos más finos si la contención es un problema.
*   **Manejo de Errores/Reintentos:** Implementar políticas más sofisticadas para pedidos fallidos (p. ej., reintentos limitados).