# Informe del Proyecto: Simulación Logística Concurrente

## 1. Resumen General

Este proyecto implementa una simulación de un sistema logístico utilizando concurrencia en Java. El sistema modela el flujo de pedidos a través de varias etapas: preparación, despacho, entrega y verificación. Se utilizan múltiples hilos para representar a los trabajadores en cada etapa, interactuando con recursos compartidos como una matriz de casilleros y un registro central de pedidos. La simulación incorpora elementos de aleatoriedad, como tiempos de procesamiento variables y probabilidades de éxito/fallo en ciertas etapas, lo que afecta el estado de los pedidos y los casilleros. Se incluye un sistema de logging para registrar el progreso y las estadísticas finales de la simulación.

## 2. Estructura del Proyecto

La estructura del proyecto proporcionada es la siguiente:

```
ProgConcu/
|-- README.md
|-- generate_sumary.py
|-- simulacion_logistica.log
TXT/
|-- DespachadorPedidosViejo.txt
|-- EntregadorPedidoViejo.txt
|-- RegistroPedidosViejo.txt
|-- VerificadorPedidosViejo.txt
Codigo/
|-- Casillero.java
|-- DespachadorPedido.java
|-- EntregadorPedido.java
|-- EstadoCasillero.java
|-- LoggerSistema.java
|-- Main.java
|-- MatrizCasilleros.java
|-- Pedido.java
|-- PreparadorPedido.java
|-- RegistroPedidos.java
|-- VerificadorPedido.java
Graficos/
|-- Diagramas/
|-- DiagramaDeClases.md
|-- diagramaDeSequencia.md
```

*   **`ProgConcu/`**: Directorio raíz del proyecto.
    *   `README.md`: Archivo de descripción del proyecto.
    *   `generate_sumary.py`: Script Python para generar este resumen.
    *   `simulacion_logistica.log`: Archivo de log generado por la simulación.
*   **`Codigo/`**: Contiene el código fuente Java de la simulación.
*   **`Graficos/Diagramas/`**: Contiene diagramas del sistema.

## 3. Componentes Principales (Código Java)

### 3.1. Clases de Datos y Estado

*   **`Pedido.java`**: Representa una orden individual.
    *   Tiene un ID único generado automáticamente (`AtomicInteger`).
    *   Mantiene una referencia al ID del casillero asignado (`casilleroIdAsignado`, `volatile`).
    *   **Crucial**: Incluye un `ReentrantLock` propio (`lock`) para permitir el bloqueo a nivel de pedido, evitando condiciones de carrera cuando múltiples hilos intentan procesar el mismo pedido.
*   **`Casillero.java`**: Representa un casillero físico en la matriz.
    *   Tiene un ID único.
    *   Mantiene su estado actual (`EstadoCasillero`, `volatile` para visibilidad entre hilos).
    *   Cuenta cuántas veces ha sido utilizado (`AtomicInteger contadorUso`).
    *   Las operaciones de cambio de estado (`ocupar`, `liberar`, `ponerFueraDeServicio`) son llamadas desde `MatrizCasilleros`.
*   **`EstadoCasillero.java`**: Enum simple que define los posibles estados de un casillero: `VACIO`, `OCUPADO`, `FUERA_DE_SERVICIO`.

### 3.2. Recursos Compartidos

*   **`MatrizCasilleros.java`**: Gestiona la colección de todos los `Casillero`.
    *   Almacena los casilleros en un array (`Casillero[]`).
    *   Utiliza un **único `ReentrantLock` (`matrizLock`)** para proteger el acceso concurrente a la estructura de casilleros (búsqueda, ocupación, liberación, cambio de estado). Esto simplifica la sincronización pero puede ser un cuello de botella si hay mucha contención por los casilleros.
    *   Implementa la lógica para `ocuparCasilleroAleatorio()`, `liberarCasillero()`, y `ponerFueraDeServicio()`. La selección aleatoria de casilleros vacíos ayuda a distribuir la carga.
    *   Proporciona un método `getEstadisticas()` para el informe final.
*   **`RegistroPedidos.java`**: Centraliza el seguimiento de los pedidos en sus diferentes etapas.
    *   Utiliza múltiples `ConcurrentLinkedQueue` para almacenar pedidos en cada estado (Preparación, Tránsito, Entregados, Fallidos, Verificados), lo cual es adecuado para escenarios productor-consumidor.
    *   Usa `AtomicInteger` para contar los pedidos que llegan a estados finales (Fallidos, Verificados) y el total de preparados.
    *   **Implementación de Selección Aleatoria**: Los métodos `tomarDe...Aleatorio()` (para Preparación, Tránsito, Entregados) utilizan un `ReentrantLock` específico para cada cola y una estrategia de:
        1.  Bloquear el acceso a la cola.
        2.  Vaciar *toda* la `ConcurrentLinkedQueue` en una `ArrayList` temporal usando `poll()` en bucle.
        3.  Seleccionar un elemento aleatorio de la `ArrayList`.
        4.  Reinsertar los elementos restantes de la `ArrayList` en la `ConcurrentLinkedQueue`.
        5.  Desbloquear.
        *Observación*: Aunque funcionalmente correcto para obtener un elemento aleatorio, este enfoque puede ser **ineficiente** y causar alta contención, especialmente si las colas son grandes, ya que bloquea toda la cola y realiza muchas operaciones.

### 3.3. Procesos (Runnables)

Cada una de estas clases implementa `Runnable` y representa un tipo de trabajador en la simulación. Son gestionadas por un `ExecutorService` en `Main`. Comparten un `AtomicBoolean running` para permitir una detención coordinada.

*   **`PreparadorPedido.java`**:
    *   Genera nuevos `Pedido`.
    *   Intenta ocupar un `Casillero` aleatorio a través de `MatrizCasilleros`. Si no hay disponibles, reintenta tras una pausa.
    *   Asigna el casillero al pedido.
    *   Añade el pedido a la cola `pedidosEnPreparacion` en `RegistroPedidos`.
    *   Incrementa el contador `preparadosCount`.
    *   Simula un tiempo de trabajo (`dormir()`).
    *   Se detiene si la señal `running` es falsa o si se alcanza el `TOTAL_PEDIDOS_A_PROCESAR`.
*   **`DespachadorPedido.java`**:
    *   Toma un pedido *aleatorio* de la cola `pedidosEnPreparacion`.
    *   Bloquea el `Pedido` específico.
    *   Simula un intento de despacho con una probabilidad de éxito (`PROB_DESPACHO_OK`).
    *   **Éxito**: Libera el `Casillero` (`matriz.liberarCasillero`), desasigna el casillero del pedido, y mueve el pedido a `pedidosEnTransito`.
    *   **Fallo**: Pone el `Casillero` fuera de servicio (`matriz.ponerFueraDeServicio`), desasigna el casillero del pedido, y mueve el pedido a `pedidosFallidos`.
    *   Desbloquea el `Pedido`.
    *   Simula tiempo de trabajo.
    *   Se detiene si `running` es falso y la cola de entrada está vacía.
*   **`EntregadorPedido.java`**:
    *   Toma un pedido *aleatorio* de la cola `pedidosEnTransito`.
    *   Bloquea el `Pedido`.
    *   Simula un intento de entrega (`PROB_ENTREGA_OK`).
    *   **Éxito**: Mueve el pedido a `pedidosEntregados`.
    *   **Fallo**: Mueve el pedido a `pedidosFallidos`.
    *   Desbloquea el `Pedido`.
    *   Simula tiempo de trabajo.
    *   Se detiene si `running` es falso y la cola de entrada está vacía.
*   **`VerificadorPedido.java`**:
    *   Toma un pedido *aleatorio* de la cola `pedidosEntregados`.
    *   Bloquea el `Pedido`.
    *   Simula un intento de verificación (`PROB_VERIFICACION_OK`).
    *   **Éxito**: Mueve el pedido a `pedidosVerificados` (estado final exitoso).
    *   **Fallo**: Mueve el pedido a `pedidosFallidos` (estado final fallido).
    *   Desbloquea el `Pedido`.
    *   Simula tiempo de trabajo.
    *   Se detiene si `running` es falso y la cola de entrada está vacía.

### 3.4. Orquestación y Utilidades

*   **`Main.java`**:
    *   Punto de entrada de la aplicación.
    *   Define constantes de configuración (número de casilleros, pedidos, hilos por tipo, demoras base/variación, probabilidades).
    *   Inicializa los recursos compartidos (`MatrizCasilleros`, `RegistroPedidos`, `LoggerSistema`).
    *   Crea un `ExecutorService` (cached thread pool) para gestionar los hilos de los procesos.
    *   Inicia todos los hilos (Preparadores, Despachadores, Entregadores, Verificadores).
    *   Implementa la lógica de espera principal: continúa mientras no se hayan preparado todos los pedidos (`TOTAL_PEDIDOS_A_PROCESAR`) *y* las colas intermedias (Preparación, Tránsito, Entregados) no estén vacías.
    *   Señaliza la detención a los hilos estableciendo `running.set(false)`.
    *   Realiza una parada ordenada del `ExecutorService` (`shutdown` y `awaitTermination`, con `shutdownNow` como fallback).
    *   Registra el informe final a través del `LoggerSistema`.
    *   Realiza una comprobación final para verificar si el número total de pedidos procesados (Fallidos + Verificados) coincide con el objetivo.
*   **`LoggerSistema.java`**:
    *   Gestiona el logging de la simulación en un archivo (`simulacion_logistica.log`).
    *   Utiliza un `ScheduledExecutorService` para registrar periódicamente el estado (pedidos fallidos y verificados).
    *   Permite registrar mensajes puntuales.
    *   Escribe un informe final detallado con estadísticas de pedidos, estado de casilleros y tiempo total.
    *   Usa `PrintWriter` con auto-flush y modo append.

## 4. Flujo de Trabajo y Lógica de Negocio

1.  **Inicio**: `Main` configura e inicializa todos los componentes y lanza los hilos trabajadores.
2.  **Preparación**: Los `PreparadorPedido` generan pedidos, buscan y ocupan un `Casillero` aleatorio, y añaden el pedido a la cola de `Preparación`.
3.  **Despacho**: Los `DespachadorPedido` toman pedidos aleatorios de `Preparación`. Si tienen éxito, liberan el casillero y mueven el pedido a `Tránsito`. Si fallan, ponen el casillero `FUERA_DE_SERVICIO` y mueven el pedido a `Fallidos`.
4.  **Entrega**: Los `EntregadorPedido` toman pedidos aleatorios de `Tránsito`. Si tienen éxito, mueven el pedido a `Entregados`. Si fallan, lo mueven a `Fallidos`.
5.  **Verificación**: Los `VerificadorPedido` toman pedidos aleatorios de `Entregados`. Si tienen éxito, mueven el pedido a `Verificados`. Si fallan, lo mueven a `Fallidos`.
6.  **Finalización**: La simulación termina cuando se han generado todos los pedidos iniciales y las colas de `Preparación`, `Tránsito` y `Entregados` están vacías. `Main` señaliza la detención, espera a que los hilos terminen y genera el informe final.

## 5. Mecanismos de Concurrencia y Sincronización

*   **Gestión de Hilos**: `java.util.concurrent.ExecutorService` (específicamente `Executors.newCachedThreadPool`) gestiona el ciclo de vida de los hilos trabajadores.
*   **Sincronización**:
    *   **`ReentrantLock`**:
        *   Usado en `Pedido` para asegurar exclusión mutua al procesar un pedido específico por diferentes etapas.
        *   Usado en `MatrizCasilleros` para proteger el acceso al array de casilleros (potencial cuello de botella).
        *   Usado en `RegistroPedidos` para implementar la (ineficiente) selección aleatoria de las colas.
    *   **`ConcurrentLinkedQueue`**: Colecciones thread-safe usadas en `RegistroPedidos` para las colas de pedidos, permitiendo adiciones y extracciones concurrentes (aunque la extracción aleatoria introduce su propio bloqueo).
    *   **`AtomicInteger`**: Usado para contadores (`Pedido.idCounter`, `Casillero.contadorUso`, contadores en `RegistroPedidos`) garantizando operaciones atómicas sin necesidad de locks explícitos.
    *   **`volatile`**: Usado para `Casillero.estado` y `Pedido.casilleroIdAsignado` para asegurar que los cambios realizados por un hilo sean visibles para otros hilos.
    *   **`AtomicBoolean`**: Usado para la bandera `running` que señaliza de forma segura la solicitud de detención a los hilos trabajadores.
*   **Coordinación**: La lógica en `Main` y las condiciones de bucle en los `Runnable` (verificando `running` y el estado de las colas) coordinan el inicio y la finalización ordenada de la simulación.

## 6. Observaciones y Posibles Mejoras

*   **Eficiencia de Selección Aleatoria**: La implementación de `tomarDe...Aleatorio()` en `RegistroPedidos` es un punto crítico. Bloquear, vaciar, seleccionar y rellenar puede ser muy costoso y limitar el rendimiento bajo alta concurrencia. Alternativas podrían incluir estructuras de datos más complejas o aceptar una selección pseudo-aleatoria o simplemente FIFO (`poll()`).
*   **Bloqueo de MatrizCasilleros**: El uso de un único lock para toda la matriz de casilleros puede limitar la escalabilidad, ya que cualquier operación sobre un casillero bloquea el acceso a todos los demás. Podría explorarse un bloqueo más granular (ej. por rangos de casilleros o locks individuales si las operaciones fueran más complejas), aunque aumentaría la complejidad.
*   **Manejo de Fallos**: El sistema maneja fallos moviendo pedidos a una cola `Fallidos` y poniendo casilleros fuera de servicio. No hay reintentos explícitos para los pedidos fallidos.
*   **Logging**: El `LoggerSistema` proporciona buena visibilidad. El problema reportado con `simulacion_logistica.log` ("No se pudo leer") podría deberse a un cierre incorrecto en una ejecución anterior, corrupción o problemas de permisos/codificación no manejados por el script `generate_sumary.py`.
*   **Diseño General**: El uso de `ExecutorService`, `Atomic*`, `Concurrent*` collections y `ReentrantLock` demuestra una buena comprensión de las herramientas de concurrencia de Java. El bloqueo a nivel de `Pedido` es una decisión acertada.

## 7. Conclusión

El proyecto implementa con éxito una simulación concurrente de un sistema logístico. Utiliza adecuadamente primitivas de concurrencia de Java para gestionar el acceso a recursos compartidos y coordinar el trabajo entre múltiples hilos que representan diferentes etapas del proceso. Si bien existen áreas de mejora potencial en términos de eficiencia (principalmente la selección aleatoria de colas y el bloqueo global de la matriz), el diseño general es sólido y funcional para el propósito de la simulación.