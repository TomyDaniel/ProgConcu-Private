@startuml Diagrama de Secuencia - Flujo Exitoso Pedido

title Flujo Exitoso de un Pedido a través del Sistema

actor Preparador as Prep <<Thread>>
participant MatrizCasilleros as Matriz
participant Pedido as P <<Object>>
participant RegistroPedidos as Registro
actor Despachador as Desp <<Thread>>
actor Entregador as Ent <<Thread>>
actor Verificador as Verif <<Thread>>

' == Preparación ==
activate Prep
Prep -> Matriz : ocuparCasilleroAleatorio()
activate Matriz
Matriz --> Prep : casilleroId
deactivate Matriz

Prep -> P : create()
activate P
Prep -> P : asignarCasillero(casilleroId)
Prep -> P : lock()
note right of P : Bloqueo adquirido
deactivate P

Prep -> Registro : agregarAPreparacion(P)
activate Registro
Registro --> Prep
deactivate Registro

Prep -> Registro : incrementarPreparados()
activate Registro
Registro --> Prep
deactivate Registro

activate P
Prep -> P : unlock()
note right of P : Bloqueo liberado
deactivate P
deactivate Prep
'... Preparador aplica demora y continua ...'

' == Despacho ==
activate Desp
Desp -> Registro : obtenerPedidoPreparacionAleatorio()
activate Registro
Registro --> Desp : P
deactivate Registro

Desp -> P : lock()
activate P
note right of P : Bloqueo adquirido
deactivate P

Desp -> Registro : removerDePreparacion(P)
activate Registro
Registro --> Desp
deactivate Registro

' Simulacion de éxito (85%)
Desp -> Matriz : liberarCasillero(casilleroId)
activate Matriz
Matriz --> Desp
deactivate Matriz

Desp -> Registro : agregarATransito(P)
activate Registro
Registro --> Desp
deactivate Registro

activate P
Desp -> P : unlock()
note right of P : Bloqueo liberado
deactivate P
deactivate Desp
'... Despachador aplica demora y continua ...'

' == Entrega ==
activate Ent
Ent -> Registro : obtenerPedidoTransitoAleatorio()
activate Registro
Registro --> Ent : P
deactivate Registro

Ent -> P : lock()
activate P
note right of P : Bloqueo adquirido
deactivate P

Ent -> Registro : removerDeTransito(P)
activate Registro
Registro --> Ent
deactivate Registro

' Simulacion de éxito (90%)
Ent -> Registro : agregarAEntregados(P)
activate Registro
Registro --> Ent
deactivate Registro

activate P
Ent -> P : unlock()
note right of P : Bloqueo liberado
deactivate P
deactivate Ent
'... Entregador aplica demora y continua ...'

' == Verificación ==
activate Verif
Verif -> Registro : obtenerPedidoEntregadoAleatorio()
activate Registro
Registro --> Verif : P
deactivate Registro

Verif -> P : lock()
activate P
note right of P : Bloqueo adquirido
deactivate P

Verif -> Registro : removerDeEntregados(P)
activate Registro
Registro --> Verif
deactivate Registro

' Simulacion de éxito (95%)
Verif -> Registro : agregarAVerificados(P)
activate Registro
Registro --> Verif
deactivate Registro

activate P
Verif -> P : unlock()
note right of P : Bloqueo liberado
deactivate P
deactivate Verif
'... Verificador aplica demora y continua ...'

@enduml