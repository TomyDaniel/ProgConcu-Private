```mermaid
sequenceDiagram
    participant User as "Usuario/E-commerce"
    participant hPrep1 as "hPrep1:ProcesoPreparacion"
    participant hPrep2 as "hPrep2:ProcesoPreparacion"
    participant MatrizC as "mc:MatrizCasilleros"
    participant RegP as "rp:RegistroPedidos"
    participant PedidoActual as "pActual:Pedido"
    participant hDesp1 as "hDesp1:ProcesoDespacho"
    participant hDesp2 as "hDesp2:ProcesoDespacho"
    participant hEnt1 as "hEnt1:ProcesoEntrega"
    participant hEnt2 as "hEnt2:ProcesoEntrega"
    participant hVerif1 as "hVerif1:ProcesoVerificacion"
    participant hVerif2 as "hVerif2:ProcesoVerificacion"
    participant SysLog as "log:Log"

    User->>hPrep1: Nuevo Pedido (datos)
    activate hPrep1
    hPrep1->>hPrep1: pNuevo = crear Objeto Pedido()

    par hPrep1_busca_casillero
        hPrep1->>MatrizC: mc.getLock().lock()
        activate MatrizC
        hPrep1->>MatrizC: casillero = obtenerCasilleroAleatorioDisponible()
        opt casillero existe
            hPrep1->>MatrizC: marcarOcupado(casillero, pNuevo)
            hPrep1->>MatrizC: incrementarContadorCasillero(casillero)
            hPrep1->>MatrizC: mc.getLock().unlock()
            deactivate MatrizC
            hPrep1->>pNuevo: setCasilleroAsociado(casillero)
            hPrep1->>pNuevo: setEstado(EN_PREPARACION)

            hPrep1->>RegP: rp.getLockPreparacion().lock()
            activate RegP
            hPrep1->>RegP: agregarAPreparacion(pNuevo)
            hPrep1->>RegP: rp.getLockPreparacion().unlock()
            deactivate RegP
            note right of hPrep1: Pedido en preparación
        else casillero NO existe
            hPrep1->>MatrizC: mc.getLock().unlock()
            deactivate MatrizC
            hPrep1->>hPrep1: Esperar/Reintentar búsqueda
            note right of hPrep1: No hay casilleros vacíos
        end
   
        activate hPrep2
        hPrep2->>MatrizC: mc.getLock().lock()
        note right of hPrep2: Otro hilo de preparación (hPrep2)\nintenta acceder a la matriz de casilleros
        hPrep2->>MatrizC: (operaciones similares...)
        hPrep2->>MatrizC: mc.getLock().unlock()
        deactivate hPrep2
    end
    deactivate hPrep1

    activate hDesp1
    par hDesp1_busca_pedido
        hDesp1->>RegP: rp.getLockPreparacion().lock()
        activate RegP
        hDesp1->>RegP: pActual = tomarDePreparacionAleatorio()
        opt pActual existe
            hDesp1->>RegP: eliminarDePreparacion(pActual)
        end
        hDesp1->>RegP: rp.getLockPreparacion().unlock()
        deactivate RegP

        opt pActual existe
            hDesp1->>pActual: pActual.getLock().lock()
            activate pActual
            hDesp1->>hDesp1: verificarDatosPedido() // Probabilidad 85% éxito
            alt Verificación Correcta (85%)
                hDesp1->>MatrizC: mc.getLock().lock()
                activate MatrizC
                hDesp1->>MatrizC: marcarVacio(pActual.getCasilleroAsociado())
                hDesp1->>MatrizC: mc.getLock().unlock()
                deactivate MatrizC
                
                hDesp1->>pActual: setEstado(EN_TRANSITO)
                hDesp1->>RegP: rp.getLockTransito().lock()
                activate RegP
                hDesp1->>RegP: agregarATransito(pActual)
                hDesp1->>RegP: rp.getLockTransito().unlock()
                deactivate RegP
                note right of hDesp1: Pedido en tránsito
            else Verificación Incorrecta (15%)
                hDesp1->>MatrizC: mc.getLock().lock()
                activate MatrizC
                hDesp1->>MatrizC: marcarFueraDeServicio(pActual.getCasilleroAsociado())
                hDesp1->>MatrizC: mc.getLock().unlock()
                deactivate MatrizC

                hDesp1->>pActual: setEstado(FALLIDO)
                hDesp1->>RegP: rp.getLockFallidos().lock()
                activate RegP
                hDesp1->>RegP: agregarAFallidos(pActual)
                hDesp1->>RegP: rp.getLockFallidos().unlock()
                deactivate RegP
                note right of hDesp1: Pedido fallido (despacho)
            end
            hDesp1->>pActual: pActual.getLock().unlock()
            deactivate pActual
        else pActual NO existe
             hDesp1->>hDesp1: Esperar/Reintentar tomar pedido
             note right of hDesp1: No hay pedidos en preparación
        end
        activate hDesp2
        hDesp2->>RegP: rp.getLockPreparacion().lock()
        note right of hDesp2: Otro hilo de despacho (hDesp2)\nintenta acceder a pedidos en preparación
        hDesp2->>RegP: (operaciones similares...)
        hDesp2->>RegP: rp.getLockPreparacion().unlock()
        deactivate hDesp2
    end
    deactivate hDesp1

    activate hEnt1
    par hEnt1_busca_pedido_transito
        hEnt1->>RegP: rp.getLockTransito().lock()
        activate RegP
        hEnt1->>RegP: pActual = tomarDeTransitoAleatorio()
        opt pActual existe
            hEnt1->>RegP: eliminarDeTransito(pActual)
        end
        hEnt1->>RegP: rp.getLockTransito().unlock()
        deactivate RegP

        opt pActual existe
            hEnt1->>pActual: pActual.getLock().lock()
            activate pActual
            hEnt1->>hEnt1: confirmarEntrega() // Probabilidad 90% éxito
            alt Entrega Confirmada (90%)
                hEnt1->>pActual: setEstado(ENTREGADO)
                hEnt1->>RegP: rp.getLockEntregados().lock()
                activate RegP
                hEnt1->>RegP: agregarAEntregados(pActual)
                hEnt1->>RegP: rp.getLockEntregados().unlock()
                deactivate RegP
                note right of hEnt1: Pedido entregado
            else Entrega No Confirmada (10%)
                hEnt1->>pActual: setEstado(FALLIDO)
                hEnt1->>RegP: rp.getLockFallidos().lock()
                activate RegP
                hEnt1->>RegP: agregarAFallidos(pActual)
                hEnt1->>RegP: rp.getLockFallidos().unlock()
                deactivate RegP
                note right of hEnt1: Pedido fallido (entrega)
            end
            hEnt1->>pActual: pActual.getLock().unlock()
            deactivate pActual
        else pActual NO existe
             hEnt1->>hEnt1: Esperar/Reintentar tomar pedido
             note right of hEnt1: No hay pedidos en tránsito
        end

        activate hEnt2
        hEnt2->>RegP: rp.getLockTransito().lock()
        note right of hEnt2: Otro hilo de entrega (hEnt2)\nintenta acceder a pedidos en tránsito
        hEnt2->>RegP: (operaciones similares...)
        hEnt2->>RegP: rp.getLockTransito().unlock()
        deactivate hEnt2
    end
    deactivate hEnt1

    activate hVerif1
    par hVerif1_busca_pedido_entregado
        hVerif1->>RegP: rp.getLockEntregados().lock()
        activate RegP
        hVerif1->>RegP: pActual = tomarDeEntregadosAleatorio()
        opt pActual existe
            hVerif1->>RegP: eliminarDeEntregados(pActual)
        end
        hVerif1->>RegP: rp.getLockEntregados().unlock()
        deactivate RegP

        opt pActual existe
            hVerif1->>pActual: pActual.getLock().lock()
            activate pActual
            hVerif1->>hVerif1: verificarPedidoFinal() // Probabilidad 95% éxito
            alt Verificación Exitosa (95%)
                hVerif1->>pActual: setEstado(VERIFICADO)
                hVerif1->>RegP: rp.getLockVerificados().lock()
                activate RegP
                hVerif1->>RegP: agregarAVerificados(pActual)
                hVerif1->>RegP: rp.getLockVerificados().unlock()
                deactivate RegP
                note right of hVerif1: Pedido verificado
            else Verificación Fallida (5%)
                hVerif1->>pActual: setEstado(FALLIDO)
                hVerif1->>RegP: rp.getLockFallidos().lock()
                activate RegP
                hVerif1->>RegP: agregarAFallidos(pActual)
                hVerif1->>RegP: rp.getLockFallidos().unlock()
                deactivate RegP
                note right of hVerif1: Pedido fallido (verificación)
            end
            hVerif1->>pActual: pActual.getLock().unlock()
            deactivate pActual
        else pActual NO existe
             hVerif1->>hVerif1: Esperar/Reintentar tomar pedido
             note right of hVerif1: No hay pedidos entregados para verificar
        end
    
        activate hVerif2
        hVerif2->>RegP: rp.getLockEntregados().lock()
        note right of hVerif2: Otro hilo de verificación (hVerif2)\nintenta acceder a pedidos entregados
        hVerif2->>RegP: (operaciones similares...)
        hVerif2->>RegP: rp.getLockEntregados().unlock()
        deactivate hVerif2
    end
    deactivate hVerif1

    loop Cada 200ms
        SysLog->>RegP: rp.getLockFallidos().lock()
        activate RegP
        SysLog->>RegP: numFallidos = getCantidadFallidos()
        SysLog->>RegP: rp.getLockFallidos().unlock()
        deactivate RegP

        SysLog->>RegP: rp.getLockVerificados().lock()
        activate RegP
        SysLog->>RegP: numVerificados = getCantidadVerificados()
        SysLog->>RegP: rp.getLockVerificados().unlock()
        deactivate RegP
        
        SysLog->>SysLog: escribirEnArchivo(numFallidos, numVerificados)
    end

``` 