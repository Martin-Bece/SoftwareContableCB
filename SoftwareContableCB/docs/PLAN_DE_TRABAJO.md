# Plan de trabajo – Entrega del 12/12/2026

Basado en la **segunda reunión con Caro (25/11/2026)**. Incluye **todo** lo pedido para esta entrega.

- **Martín:** backend (`config`, `dao`, `service`).
- **Hermano:** frontend (`view`, `view.paneles`).
- **Fecha de entrega:** sábado 12/12/2026. **Congelamiento de código: jueves 10/12 a la noche.**
  El viernes 11 es solo para pruebas, empaquetado y manual.

**Queda fuera de esta entrega por pedido de Caro** (no es un recorte nuestro):
- **Impuestos** (IVA, Ganancias, Monotributo): para la entrega siguiente. Lo que ya está hecho de IVA queda como base.
- **Inventario:** para el futuro.
- **API de ARCA / facturación con RECE:** en esta entrega solo hay que **averiguarlo** (tarea A2).

---

## 1. Alcance completo del 12/12

### 1.1 Navegación y pantallas
- [ ] **Pantalla inicial antes de entrar:** ABMC de clientes. Se elige un cliente y recién ahí se entra a trabajar.
- [ ] Sacar "Clientes" del menú del módulo de trabajo.
- [ ] **Clientes del cliente** como acceso directo dentro de **Ventas**, con su ABMC.
- [ ] **Proveedores** como acceso directo dentro de **Compras**, con su ABMC.
- [ ] **Reportes de ventas** dentro de Ventas y **reportes de compras** dentro de Compras. Se saca el módulo Reportes.

### 1.2 Ventas
- [ ] Al cargar una factura con un **CUIT que no existe**, se crea la contraparte por **CUIT** (no por nombre).
  *Ya existe el alta automática. Falta lo que sigue:*
- [ ] **Consumidor final:** identificarlo por **DNI**, y un **"Consumidor final" genérico** si no hay DNI.
- [ ] La factura nace **abierta, con saldo a cobrar**, salvo que se indique cómo se cobró.
- [ ] **Estado:** saldo > 0 = **impaga**; saldo = 0 = **pagada**.
- [ ] **Filtros** por todos los campos no numéricos: fecha, estado, cliente, tipo, punto de venta, número.
- [ ] Sacar la columna **origen** de la tabla.
- [ ] Separar "comprobante" en **punto de venta** y **número**, con filtro para cada uno.
- [ ] Las ventas **no tienen percepciones, tienen retenciones**. No se muestran en la tabla.
- [ ] **Marcar como cobrada** una venta abierta, indicando la forma: efectivo, MP, cuenta bancaria, cheque,
  retenciones o saldo a favor.
- [ ] **Nota de crédito vinculada** a una venta: reduce el saldo o genera **saldo a favor**.
- [ ] **Importar el archivo mensual** de ventas. **Caro pasa el formato.**
- [ ] **Asiento contable automático** por cada operación (ver 1.4).

### 1.3 Compras
- [ ] **Los mismos cambios que en Ventas**, del lado de proveedores: estado y saldo a pagar, marcar como pagada
  con su forma de pago, notas de crédito vinculadas, filtros, PV y número separados, sin origen, alta
  automática de proveedores por CUIT, reportes dentro de Compras y asientos automáticos.

### 1.4 Módulo contable (nuevo)
- [ ] **Plan de cuentas.** Caro lo pasa. ABM de cuentas y opción de importarlo.
- [ ] **Asientos automáticos** desde ventas, compras, cobros, pagos, notas de crédito, movimientos de dinero y gastos.
- [ ] **Numeración automática** de asientos, **sin duplicados**, que **vuelve a 1 cada año**.
- [ ] **Ver asientos** (libro diario) y **crear asientos manuales**.
- [ ] Reportes: **balance de sumas y saldos**, **mayores** y **estados contables** (Caro pasa el formato).
- [ ] **Estados contables por líneas:** cada línea del estado busca las cuentas que le corresponden.

### 1.5 Módulo de finanzas (nuevo)
- [ ] **ABMC de cuentas bancarias**, cada una **obligatoriamente asociada a una cuenta contable**. Si la cuenta
  contable no existe, se puede crear desde ahí.
- [ ] **Saldos** de cada cuenta bancaria.
- [ ] **Todo movimiento de dinero** (entrada, salida o transferencia entre cuentas) queda registrado y visible acá.
- [ ] **Gastos** sin comprobante dentro de cada cuenta, imputados a una cuenta contable.
- [ ] **Conciliación bancaria.**

---

## 2. Insumos que tiene que pasar Caro (BLOQUEANTES)

| # | Qué | Lo necesita | Pedir para | Si no llega a tiempo |
|---|---|---|---|---|
| C1 | **Plan de cuentas** | B3, B4, todo lo contable | **vie 27/11** | Arrancamos con un plan base estándar, configurable, y después importamos el de ella |
| C2 | **Qué cuentas usa cada operación** (venta, compra, cobro, pago, NC, gasto, transferencia) | B5 y asientos automáticos | **lun 30/11** | Usamos cuentas por defecto configurables por cliente; ella las ajusta en una pantalla |
| C3 | **Formato del archivo mensual de ventas** (archivo de ejemplo) | B13 | **jue 3/12** | Queda listo con el lector genérico de CSV/Excel y se ajusta al llegar |
| C4 | **Formato de los estados contables** (qué líneas y qué cuentas suma cada una) | B12 | **jue 3/12** | El motor de estados por líneas queda listo y se carga el formato al llegar |
| C5 | **Respuestas a las dudas de la sección 3** | Diseño | **vie 27/11** | Seguimos con los supuestos de la sección 3 |
| C6 | **Datos reales de un cliente** (un mes) para probar | A4 | **mar 8/12** | Probamos con datos inventados |

> Mandarle a Caro **hoy mismo** la lista C1–C6 con las fechas.

---

## 3. Decisiones de diseño (supuestos a confirmar con Caro)

Seguimos con estos supuestos para no frenar. Si Caro dice otra cosa, se ajusta.

1. **Cada cliente es una contabilidad separada:** su propio plan de cuentas (copiado de una plantilla), sus asientos
   y sus cuentas bancarias.
2. **Numeración de asientos por cliente y por año:** 1, 2, 3… y vuelve a 1 el 1° de enero. Única en la base.
3. **Partida doble estricta:** un asiento no se guarda si el debe no es igual al haber.
4. **Los asientos automáticos no se editan ni se borran a mano.** Si se corrige la operación, se anula con un
   contra-asiento y se genera uno nuevo. Así la numeración nunca tiene huecos raros ni duplicados.
5. **Cobros y pagos parciales:** una factura puede tener varios cobros, y **un cobro puede cancelar varias
   facturas** (imputación).
6. **El saldo de una factura se calcula:** total − cobros/pagos imputados − notas de crédito vinculadas.
   El estado sale del saldo.
7. **Las retenciones que le hacen al cliente al cobrar son una forma de cobro:** bajan el saldo y quedan
   registradas como retención sufrida.
8. **Moneda:** solo pesos en esta entrega.
9. **Consumidor final:** la contraparte acepta CUIT (11 dígitos) o DNI (7 u 8 dígitos). Cada cliente tiene un
   "Consumidor final" genérico precargado.
10. **Saldo a favor:** se guarda por contraparte y se puede usar como forma de cobro o pago.

---

## 4. Contratos primero (cómo trabajamos en paralelo)

Igual que hasta ahora: **el frontend trabaja contra interfaces de `view.Servicios` con un mock**, y el backend
implementa esas mismas interfaces. Por eso **lo primero (B1) es acordar las interfaces nuevas juntos**.
Así el hermano no espera al backend y Martín no espera las pantallas.

Interfaces nuevas o que cambian:

| Interfaz | Para qué |
|---|---|
| `ClienteService` | Igual. Se usa desde la pantalla inicial |
| `ComprobanteService` | + estado, saldo, filtros nuevos y documento CUIT/DNI/CF |
| `CobroPagoService` (nueva) | Registrar cobros y pagos con formas, imputar a facturas, saldo a favor |
| `NotaCreditoService` (o dentro de `ComprobanteService`) | Vincular una NC a una factura |
| `PlanCuentasService` (nueva) | ABM e importación del plan de cuentas, cuentas por defecto |
| `AsientoService` (nueva) | Listar, ver, crear manual, anular |
| `ReporteContableService` (nueva) | Diario, mayor, sumas y saldos, estados contables |
| `FinanzasService` (nueva) | Cuentas bancarias, movimientos, transferencias, gastos, saldos, conciliación |
| `ReporteService` | Igual, pero se muestra dentro de Ventas y Compras |
| `ContraparteService` | + DNI y consumidor final |

**Modelos nuevos** en `Modelos`: `CuentaContable`, `Asiento`, `RenglonAsiento`, `CobroPago`, `Imputacion`,
`FormaPago` (enum), `CuentaBancaria`, `MovimientoFinanciero`, `Gasto`, `LineaEstadoContable`, y en
`Comprobante`: `estado`, `saldo`, `idComprobanteVinculado`.

---

## 5. Tareas

**Est.** = días de trabajo estimados. **Dep.** = de qué depende.

### Backend – Martín

| ID | Tarea | Est. | Fechas | Dep. | Listo cuando… |
|---|---|---|---|---|---|
| B1 | **Contratos:** interfaces y modelos nuevos de la sección 4 (con el hermano) | 1 | 25–26/11 | — | Compila; el mock los implementa y el front puede usarlos |
| B2 | **Esquema v3 + migración:** cuentas, asientos, renglones, cobros/pagos, imputaciones, cuentas bancarias, movimientos, gastos, conciliación; estado y vínculo en comprobante; documento DNI/CF | 1 | 26–27/11 | B1 | Migra desde v2 sin perder datos (test); las facturas existentes quedan abiertas con saldo = total |
| B3 | **Plan de cuentas:** ABM, jerarquía (rubro/cuenta), cuentas imputables, plantilla base, importación desde Excel/CSV | 1,5 | 27–28/11 | B2, C1 | Se carga el plan de Caro desde su archivo |
| B4 | **Motor de asientos:** alta validada (debe = haber, cuentas imputables), numeración por cliente/año sin duplicados, anulación por contra-asiento, asientos manuales, libro diario | 2 | 29/11–1/12 | B3 | Tests de numeración (incluido el cambio de año), rechazo de desbalanceados y concurrencia |
| B5 | **Cuentas por defecto** por cliente para los asientos automáticos (ventas, IVA DF, deudores, compras, IVA CF, proveedores, caja, retenciones, etc.) | 0,5 | 1/12 | B4, C2 | Configurables; cada operación sabe qué cuentas usar |
| B6 | **Ventas y compras v2:** estado y saldo, filtros nuevos (estado, PV, número, tipo, contraparte, fecha), retenciones en ventas, contraparte por CUIT/DNI y consumidor final genérico, **asiento automático** al cargar o importar | 1,5 | 1–2/12 | B5 | Al cargar una factura se genera su asiento numerado; los filtros andan |
| B7 | **Cobros y pagos:** formas (efectivo, MP, cuenta bancaria, cheque, retenciones, saldo a favor), parciales, imputación a varias facturas, saldo a favor, asiento automático | 2 | 2–4/12 | B6 | El saldo y el estado se actualizan; cada cobro genera su asiento y su movimiento financiero |
| B8 | **Notas de crédito vinculadas:** reducen el saldo o generan saldo a favor; asiento automático | 1 | 4/12 | B7 | Una NC mayor que el saldo genera saldo a favor |
| B9 | **Finanzas:** cuentas bancarias (asociadas a una cuenta contable, con alta de cuenta desde ahí), movimientos, transferencias, gastos, saldos, todo con asiento | 1,5 | 5–7/12 | B4, B7 | Todos los cobros y pagos aparecen como movimientos; la transferencia genera un solo asiento |
| B10 | **Conciliación bancaria:** importar el extracto (CSV/Excel), emparejar con los movimientos (automático por fecha e importe, y manual) y marcar conciliados | 1 | 7–8/12 | B9 | Muestra saldo del banco vs. saldo del sistema y las diferencias |
| B11 | **Reportes contables:** libro diario, mayor por cuenta (con saldo acumulado), balance de sumas y saldos por período | 1 | 8–9/12 | B4 | Sumas y saldos balancea (total debe = total haber) |
| B12 | **Estados contables por líneas:** cada línea define qué cuentas (o rangos) suma, con subtotales; formato configurable e importable | 1 | 9/12 | B11, C4 | Se carga el formato de Caro y el estado sale de los asientos |
| B13 | **Importación del archivo mensual de ventas** con el formato de Caro (reutiliza `service.importacion`), con asientos | 0,5 | 10/12 | B6, C3 | Se importa un mes real sin duplicar |

### Frontend – Hermano

| ID | Tarea | Est. | Fechas | Dep. | Listo cuando… |
|---|---|---|---|---|---|
| F1 | **Pantalla inicial:** ABMC de clientes, elegir uno y entrar; desde adentro, poder volver a cambiar de cliente | 1 | 26–27/11 | B1 | Arranca en esa pantalla; "Clientes" ya no está en el menú |
| F2 | **Nueva navegación:** sacar Clientes y Reportes del menú; agregar Contable y Finanzas | 0,5 | 27/11 | F1 | Menú nuevo funcionando con el mock |
| F3 | **Tablas de ventas y compras:** PV y número separados, sin origen, sin percepciones en ventas, columnas estado y saldo, **filtros** (fecha, estado, contraparte, tipo, PV, número) | 1,5 | 28–30/11 | B1 | Filtros combinables; estado impaga/pagada visible |
| F4 | **Clientes del cliente** dentro de Ventas y **proveedores** dentro de Compras (acceso directo con ABMC), y **reportes** dentro de cada uno | 1 | 30/11–1/12 | F3 | Mismo comportamiento que hoy, en el lugar nuevo |
| F5 | **Carga de factura v2:** documento CUIT/DNI/consumidor final, retenciones en ventas, "cobrada/pagada al contado" con forma de pago | 1 | 1–2/12 | F3 | Con consumidor final no pide CUIT |
| F6 | **Diálogo de cobro/pago:** forma de pago, cuenta bancaria, importe parcial, elegir varias facturas, usar saldo a favor; **vincular nota de crédito** | 1,5 | 2–4/12 | F5 | El saldo de la tabla cambia al cobrar |
| F7 | **Módulo contable:** plan de cuentas (árbol, ABM, importar), cuentas por defecto, libro diario, detalle de asiento, **alta de asiento manual** (renglones, control debe = haber en pantalla) | 2 | 4–7/12 | B1 | Se puede cargar un asiento manual y verlo en el diario |
| F8 | **Módulo de finanzas:** cuentas bancarias (con alta de cuenta contable), saldos, movimientos, transferencia, gasto, **conciliación** (importar extracto y emparejar) | 1,5 | 7–8/12 | B1 | Todo navegable con el mock y después con el backend |
| F9 | **Reportes contables:** mayor (con selector de cuenta), sumas y saldos, estados contables, totales visibles | 1 | 9/12 | B1 | Muestran totales y respetan el período |
| F10 | **Importación mensual de ventas** (reutiliza `DialogoImportacion`) | 0,5 | 10/12 | F3 | Previsualiza y confirma |

### Los dos

| ID | Tarea | Quién | Fechas | Listo cuando… |
|---|---|---|---|---|
| A1 | Mandarle a Caro la lista de insumos C1–C6 y las dudas de la sección 3 | Martín | **hoy 25/11** | Mail o mensaje enviado con fechas |
| A2 | **Averiguar ARCA:** web services (WSAA + WSFEv1), facturación electrónica RECE desde el soft, certificados, homologación, costo y esfuerzo | Cualquiera, 1 día | antes del 4/12 | Informe de 1 página para Caro |
| A3 | **Integración diaria:** a medida que cada parte del backend está lista, el front pasa del mock a la real | Los dos | todos los días | `mvn test` en verde y la app abre en modo real |
| A4 | **Prueba integral con datos reales** de un mes (C6): cargar, cobrar, conciliar y sacar los reportes | Los dos | 10–11/12 | Sumas y saldos balancea; los saldos bancarios coinciden |
| A5 | **Empaquetado y manual:** `.jar` ejecutable o instalador, manual corto de uso y cómo hacer backup de la base | Los dos | 11/12 | Se instala en la PC de Caro desde cero |
| A6 | **Entrega** | Los dos | **sáb 12/12** | — |

---

## 6. Calendario

| Día | Backend (Martín) | Frontend (Hermano) | Hito |
|---|---|---|---|
| mié 25/11 | A1, B1 | B1 (con Martín) | Insumos pedidos a Caro |
| jue 26/11 | B1, B2 | F1 | **Contratos cerrados** |
| vie 27/11 | B2, B3 | F1, F2 | Llega el plan de cuentas (C1) |
| sáb 28 – dom 29/11 | B3, B4 | F3 | |
| lun 30/11 | B4 | F3, F4 | Llegan las cuentas por operación (C2) |
| mar 1/12 | B4, B5, B6 | F4, F5 | **Motor contable andando** |
| mié 2/12 | B6, B7 | F5, F6 | |
| jue 3/12 | B7 | F6 | Llegan los formatos (C3, C4) |
| vie 4/12 | B7, B8 | F6, F7 | **Ventas y compras con cobros, NC y asientos** · informe ARCA (A2) |
| sáb 5 – lun 7/12 | B9, B10 | F7, F8 | |
| mar 8/12 | B10, B11 | F8 | **Finanzas andando** · llegan datos reales (C6) |
| mié 9/12 | B11, B12 | F9 | |
| jue 10/12 | B13 + correcciones | F10 + correcciones | **Congelamiento de código (noche)** |
| vie 11/12 | A4, A5 | A4, A5 | Pruebas con datos reales, empaquetado, manual |
| **sáb 12/12** | | | **ENTREGA** |

**Camino crítico:** B1 → B2 → B3 → B4 → B6 → B7 → B9. Si algo de esto se atrasa, se atrasa todo.
Lo que más puede atrasarlo es que **el plan de cuentas (C1) no llegue el 27/11**: por eso arrancamos con una
plantilla base y el de Caro se importa cuando llegue.

---

## 7. Riesgos y cómo los manejamos

| Riesgo | Qué hacemos |
|---|---|
| Caro tarda en pasar los insumos | Pedirlos hoy con fecha; trabajar con plantillas y formatos configurables; recordárselo el día anterior |
| El backend tiene más trabajo que el front | El front va con mock, así que no espera. Si el front termina antes, el hermano puede tomar B11 o B12 (son consultas sobre asientos ya hechos) |
| Romper lo que ya funciona | `mvn test` antes de cada commit; tests nuevos para cada tarea B; migración testeada |
| Problemas de integración al final | Integrar todos los días (A3), no el 10/12 |
| Errores contables (asientos mal armados) | Partida doble validada en el backend; sumas y saldos como control; prueba con datos reales (A4) |
| Choques en git entre los dos | `git pull` antes de empezar; commits chicos; cada uno toca sus paquetes; los contratos (`Servicios`, `Modelos`) se cambian de a dos |

---

## 8. Forma de trabajo

- **Todos los días, 10 minutos:** qué hice, qué hago hoy, en qué estoy trabado. Se tacha en este archivo lo terminado.
- **Antes de cada commit:** `mvn test` en verde y la app abre.
- **Cada tarea B lleva sus tests**, igual que las anteriores (base temporal, nunca la base real).
- **Cambios en `Servicios` o `Modelos`** se acuerdan entre los dos antes de commitearlos.
- **Al terminar cada fase**, actualizar el README (reglas de negocio, esquema, tests).
