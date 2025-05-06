
@startuml Diagrama de Clases - Simulacion Logistica

' --- Estilos (Opcional) ---
skinparam classAttributeIconSize 0
hide empty members

' --- Clases y Enums ---
enum EstadoCasillero {
  VACIO
  OCUPADO
  FUERA_DE_SERVICIO
}

class Casillero {
  - estado : EstadoCasillero
  - vecesOcupado : int
  + ocupar()
  + liberar()
  + marcarFueraDeServicio()
  + getEstado() : EstadoCasillero
  + getVecesOcupado() : int
}

class Pedido {
  - {static} ID_GENERATOR : AtomicInteger
  - id : int
  - casilleroId : int
  - lock : ReentrantLock
  + Pedido()
  + asignarCasillero(int)
  + lock()
  + unlock()
  + getId() : int
  + getCasilleroId() : int
}

class MatrizCasilleros {
  - matriz : Casillero[][]
  - filas : int
  - columnas : int
  - rwLock : ReentrantReadWriteLock
  + MatrizCasilleros(int, int)
  + ocuparCasilleroAleatorio() : int
  + liberarCasillero(int)
  + marcarFueraDeServicio(int)
  + getSizeFueraDeServicio() : int
  + verificarEstadoCritico()
}

class MatrizLlenaException extends RuntimeException {
  + MatrizLlenaException(String)
}

class RegistroPedidos {
  - pedidosPreparacion : List<Pedido>
  - pedidosTransito : List<Pedido>
  - pedidosEntregados : List<Pedido>
  - pedidosFallidos : List<Pedido>
  - pedidosVerificados : List<Pedido>
  - preparadosCount : AtomicInteger
  + agregarAPreparacion(Pedido)
  + agregarATransito(Pedido)
  + agregarAEntregados(Pedido)
  + agregarAFallidos(Pedido)
  + agregarAVerificados(Pedido)
  + removerDePreparacion(Pedido)
  + removerDeTransito(Pedido)
  + removerDeEntregados(Pedido)
  + obtenerPedidoPreparacionAleatorio() : Pedido
  + obtenerPedidoTransitoAleatorio() : Pedido
  + obtenerPedidoEntregadoAleatorio() : Pedido
  + incrementarPreparados()
  + getCantidad...() : int
}
note right of RegistroPedidos : Usa CopyOnWriteArrayList para concurrencia

interface Runnable <<interface>>

abstract class TrabajadorLogistica implements Runnable {
 # registro : RegistroPedidos
 # running : AtomicBoolean
 # demoraBaseMs : int
 # variacionDemoraMs : int
 # random : Random
 + run()
 # procesarPedido(Pedido)
 # aplicarDemora()
}

class PreparadorPedido {
  - matriz : MatrizCasilleros
  - totalPedidosAGenerar : int
  + run()
  - procesarPedido() ' Sobrescribe lógica
}
note right of PreparadorPedido : Crea Pedidos

class DespachadorPedido {
  - matriz : MatrizCasilleros
  + run()
  - procesarPedido(Pedido) ' Sobrescribe lógica
}

class EntregadorPedido {
  + run()
  - procesarPedido(Pedido) ' Sobrescribe lógica
}

class VerificadorPedido {
  + run()
  - procesarPedido(Pedido) ' Sobrescribe lógica
}

class LoggerSistema {
  - registro : RegistroPedidos
  - archivoLog : String
  - scheduler : ScheduledExecutorService
  + iniciarLogPeriodico(long)
  + logMensaje(String)
  + detenerLogPeriodico()
  + logFinal(MatrizCasilleros, long)
}

class Main {
  - {static} MATRIZ_FILAS : int
  - {static} MATRIZ_COLUMNAS : int
  - {static} TOTAL_PEDIDOS : int
  - {static} DEMORA_... : int
  - {static} VARIACION_DEMORA : int
  + {static} main(String[])
  - {static} imprimirEstadisticas(RegistroPedidos)
}


' --- Relaciones ---
Main ..> MatrizCasilleros : crea y usa >
Main ..> RegistroPedidos : crea y usa >
Main ..> LoggerSistema : crea y usa >
Main ..> PreparadorPedido : crea e inicia >
Main ..> DespachadorPedido : crea e inicia >
Main ..> EntregadorPedido : crea e inicia >
Main ..> VerificadorPedido : crea e inicia >
Main ..> AtomicBoolean : usa >
Main ..> Thread : usa >

MatrizCasilleros "1" o-- "*" Casillero : contiene >
MatrizCasilleros ..> MatrizLlenaException : lanza >
MatrizCasilleros ..> EstadoCasillero : usa

Casillero *-- EstadoCasillero : tiene un

RegistroPedidos "1" o-- "*" Pedido : gestiona >

Runnable <|.. PreparadorPedido
Runnable <|.. DespachadorPedido
Runnable <|.. EntregadorPedido
Runnable <|.. VerificadorPedido
' Alternativa: Mostrar herencia de una clase base abstracta (si se usa)
' TrabajadorLogistica <|-- PreparadorPedido
' TrabajadorLogistica <|-- DespachadorPedido
' TrabajadorLogistica <|-- EntregadorPedido
' TrabajadorLogistica <|-- VerificadorPedido
' Runnable <|.. TrabajadorLogistica


PreparadorPedido ..> RegistroPedidos : usa
PreparadorPedido ..> MatrizCasilleros : usa
PreparadorPedido ..> Pedido : crea
PreparadorPedido ..> AtomicBoolean : usa

DespachadorPedido ..> RegistroPedidos : usa
DespachadorPedido ..> MatrizCasilleros : usa
DespachadorPedido ..> Pedido : usa
DespachadorPedido ..> AtomicBoolean : usa

EntregadorPedido ..> RegistroPedidos : usa
EntregadorPedido ..> Pedido : usa
EntregadorPedido ..> AtomicBoolean : usa

VerificadorPedido ..> RegistroPedidos : usa
VerificadorPedido ..> Pedido : usa
VerificadorPedido ..> AtomicBoolean : usa

LoggerSistema ..> RegistroPedidos : usa
LoggerSistema ..> MatrizCasilleros : usa (en logFinal)


@enduml