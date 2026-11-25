# Software Contable – Carolina Becerra

Aplicación de escritorio (Java 24 + Swing/FlatLaf + SQLite) para que la contadora gestione a sus clientes:
ABM de clientes, carga de ventas y compras (manual o desde archivo), percepciones y retenciones,
determinación de impuestos (IVA, Ganancias, SUSS) y reportes.

- **Frontend** (paquetes `view` y `view.paneles`): todas las pantallas.
- **Backend** (paquetes `config`, `dao`, `service`, `util`): base de datos y reglas de negocio.

> 📋 **Plan de la entrega del 12/12/2026** (módulo contable, cobros y pagos, finanzas, nueva navegación):
> [docs/PLAN_DE_TRABAJO.md](docs/PLAN_DE_TRABAJO.md)

---

## 1. Cómo correrlo

### Desde NetBeans
Abrir el proyecto y darle **Run**. La clase principal es `view.App` (configurada en el `pom.xml`).

### Desde la terminal
Requisitos: **JDK 24 o superior** (en esta PC: `C:\Program Files\Java\jdk-25.0.3`) y Maven
(NetBeans trae uno en `C:\netbeans-29\netbeans\java\maven\bin\mvn.cmd`).

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.3"
& "C:\netbeans-29\netbeans\java\maven\bin\mvn.cmd" compile exec:java
```

### Modos de ejecución

| Modo | Cómo | Qué hace |
|---|---|---|
| **Real** (por defecto) | Run normal | Todo se guarda en SQLite. |
| **Demo** | agregar `-Dcontabilidad.demo=true` a la VM | Todo en memoria con datos de ejemplo. Útil para mostrar pantallas o trabajar en el front sin base. |

En NetBeans: *Properties → Run → VM Options*.

### Dónde está la base
`%USERPROFILE%\.contabilidad_app\contabilidad.db` (se crea sola la primera vez).
Se puede usar otra con `-Dcontabilidad.db=C:\ruta\otra.db`.
Para empezar de cero: cerrar la app y borrar ese archivo.

---

## 2. Arquitectura

```
view (pantallas Swing)
  │  usa SOLO las interfaces de view.Servicios  (ClienteService, ComprobanteService, ...)
  ▼
service (implementaciones reales)      ← validan datos y lanzan ServicioException con mensajes para el usuario
  │
  ▼
dao (SQL puro)                          ← propagan SQLException, no validan, no muestran nada
  │
  ▼
config.Conexion (SQLite)               ← conexión compartida, esquema versionado, transacciones
```

**El contrato entre front y back es [`view/Servicios.java`](src/main/java/view/Servicios.java).**
El front no sabe si del otro lado hay un mock o la base real. Para pasar un módulo del mock a la base,
se escribe una clase que implemente su interfaz y se la conecta en [`view/App.java`](src/main/java/view/App.java)
(método `contextoReal()`).

### Paquetes

| Paquete | Contenido |
|---|---|
| `config` | `Conexion`: abre la base, crea/migra el esquema, `enTransaccion(...)`. |
| `dao` | `ClienteDAO`, `ComprobanteDAO`, `PercepcionDAO`, `ContraparteDAO` y el ayudante interno `Sql`. |
| `service` | Una implementación por interfaz de `Servicios` (`*ServiceImpl`) y el ayudante interno `Comun`. |
| `service.importacion` | Lectura de archivos CSV y Excel: `LectorComprobantes`, `LectorPercepciones` y sus piezas internas. |
| `util` | `Cuit.normalizar(...)`. |
| `view` | Ventana, diálogos, `Modelos` (el modelo de datos), `Servicios` (las interfaces), `ServiciosMock`. |
| `view.paneles` | Una pantalla por sección del menú. |

### Modelo de datos único
Se usa **un solo modelo**: las clases de [`view/Modelos.java`](src/main/java/view/Modelos.java)
(`Cliente`, `Actividad`, `Comprobante`, `PercepcionRetencion`, etc.). El paquete viejo `model`
(`cliente`, `comprobante`, `contacto`) se eliminó porque se quedaba corto: guardaba la plata en `double`,
no tenía punto de venta ni número, ni el detalle de IVA por alícuota.

> Pendiente a coordinar: `Modelos` vive en el paquete `view` y el backend lo importa desde ahí.
> Lo prolijo sería moverlo a un paquete `model`, pero eso toca casi todos los archivos del front,
> así que conviene hacerlo cuando ninguno tenga cambios del front sin commitear.

### Reglas para escribir DAOs
1. **Nunca cerrar la conexión.** Usar `Connection conn = Conexion.getConexion();` fuera del `try`, y en el
   `try (...)` poner solo `PreparedStatement` y `ResultSet`. (Cerrarla fue el bug original:
   después del primer uso todo fallaba con *"database connection closed"*.)
2. Siempre `PreparedStatement` con `?`, nunca concatenar datos del usuario en el SQL.
3. Si una operación toca varias tablas, envolverla en `Conexion.enTransaccion(conn -> { ... })`.
4. La plata **siempre** es `BigDecimal`, nunca `double`.

---

## 3. Qué está implementado y qué no

| Módulo (interfaz) | Implementación | Estado |
|---|---|---|
| `ClienteService` | `service.ClienteServiceImpl` | ✅ Real |
| `ComprobanteService` | `service.ComprobanteServiceImpl` | ✅ Real. Importación: CSV y Excel (PDF no) |
| `PercepcionService` | `service.PercepcionServiceImpl` | ✅ Real. Importación: CSV y Excel (PDF no) |
| `ContraparteService` (clientes del cliente / proveedores) | `service.ContraparteServiceImpl` | ✅ Real, un padrón por cliente |
| `ImpuestoService` | `service.ImpuestoServiceImpl` | ⚠️ IVA real. **Ganancias y SUSS son fórmulas provisorias** (ver §8) |
| `ReporteService` | `service.ReporteServiceImpl` | ✅ Real |

`ServiciosMock` se usa únicamente en el modo demo.

### Reglas de negocio implementadas

**Clientes**
- El CUIT es la clave. Se guarda siempre como `XX-XXXXXXXX-X`. Se acepta con o sin guiones y tiene que tener 11 dígitos.
  *No se valida todavía el dígito verificador.*
- La razón social es obligatoria.
- No se puede dar de alta un CUIT repetido.
- **No se puede dar de baja un cliente que tiene comprobantes o percepciones/retenciones** (para no borrar
  historia contable por error). Su padrón de clientes y proveedores sí se borra con él.
- Actividades: lista ordenada de código + descripción (tabla `actividad`).
- Impuestos activos: texto separado por comas, como pide el requerimiento (`"IVA, Ganancias, SUSS"`).
- La búsqueda ignora mayúsculas y busca en razón social, nombre de fantasía y CUIT.

**Comprobantes**
- Obligatorios: operación (venta/compra), fecha, tipo, número, CUIT (11 dígitos) y razón social de la contraparte.
  Punto de venta por defecto: `0001`. Los importes vacíos valen 0.
- **No se permiten duplicados**: el mismo cliente no puede tener dos comprobantes con igual
  operación + tipo + punto de venta + número + CUIT de la contraparte.
- La lista por período incluye del día 1 al último día del mes.
- Las notas de crédito restan en IVA y reportes (lógica de `Modelos.Comprobante.signo()`).
- **Al cargar o importar un comprobante, la contraparte se agrega sola al padrón del cliente**
  (venta → clientes del cliente, compra → proveedores). Si ya estaba, no se pisa su razón social.

**Padrón de clientes y proveedores** (`ContraparteServiceImpl`)
- **Cada cliente de la contadora tiene su propio padrón.** Si dos clientes comparten un proveedor, se
  carga en cada uno. Editarlo o borrarlo en uno no afecta al otro.
- Clientes del cliente y proveedores son listas separadas: un mismo CUIT puede estar en las dos.
- No se repite un CUIT dentro de la misma lista. Al editar se puede corregir el CUIT.
- Quitar a alguien del padrón no toca los comprobantes ya cargados (guardan su propio CUIT y razón social).

**Percepciones y retenciones** (`PercepcionServiceImpl`)
- Obligatorios: clase, tributo, fecha, CUIT del agente (11 dígitos) e importe mayor a cero.
- Las de IVA se descuentan en la posición de IVA del mes; las de Ganancias van como ingresos a cuenta.
- Importación CSV o Excel (`LectorPercepciones`), igual que la de comprobantes (ver "Importación de archivos"):
  ```
  fecha;clase;tributo;regimen;jurisdiccion;comprobante;cuitAgente;razonSocialAgente;importe
  05/10/2026;Percepcion;IVA;RG 2408;Nacional;FA 0003-1;30-50001122-7;Papelera del Centro S.A.;1.500,00
  2026-10-07;R;Ganancias;RG 830;Nacional;OP 55;30719988774;Combustibles Cuyanos;200
  ```
  `clase`: Percepcion / Retencion (o P / R, con o sin acento). `tributo`: IVA, Ganancias, SUSS, IIBB o "Ingresos Brutos".
  Con encabezado, obligatorias: fecha, clase, tributo, cuitAgente (también "cuit agente" o "cuit") e importe
  (también "monto"). Opcionales: régimen, jurisdicción, comprobante y razón social del agente (también "agente").
  Al reimportar, los movimientos idénticos (misma fecha, clase, tributo, agente, comprobante e importe) se saltean.

**Impuestos y reportes** (`ImpuestoServiceImpl`, `ReporteServiceImpl`)
- **IVA:** débito fiscal − crédito fiscal (notas de crédito restan) − percepciones − retenciones de IVA.
  El saldo a favor del mes anterior **todavía no se arrastra** (queda en 0).
- **Ganancias (provisorio):** 35% sobre (neto ventas − neto compras) del mes, menos las percepciones/retenciones
  de Ganancias. Si el mes da pérdida, el impuesto es 0 (el mock calculaba un impuesto negativo).
- **SUSS (provisorio, solo si el cliente tiene empleados):** nómina estimada en el 12% de las ventas;
  contribuciones 24% y aportes 17%.
- Las constantes provisorias están juntas al principio de `ImpuestoServiceImpl` para cambiarlas fácil.
- Reportes: una fila por mes del rango, aunque el mes no tenga movimientos, con una sola consulta a la base.

### Importación de archivos (CSV y Excel)

Paquete `service.importacion`. Formatos aceptados: **`.csv`, `.txt`, `.xlsx` y `.xls`**.
PDF todavía no: muestra un mensaje claro (ver §8).

**Cómo funciona** (igual para comprobantes y percepciones):
1. `LectorFilas` convierte el archivo en filas de celdas, sin interpretarlas todavía.
   - **CSV:** separador `;` (o `,` si la línea no tiene `;`). UTF-8, o Windows-1252 si el archivo no es UTF-8
     válido (lo que guarda Excel). No interpreta comillas.
   - **Excel:** lee **la primera hoja**. Calcula las fórmulas. Las fechas y los números se toman con su tipo
     de Excel. El texto se toma como se ve en la celda, así que un punto de venta con formato `0000`
     queda `0001` y un CUIT guardado como número queda bien.
   - Las filas vacías se ignoran.
2. `Tabla` decide qué columna es cada dato:
   - **Con encabezado:** se busca, entre las primeras 10 filas, una que tenga una celda que empiece con "fecha".
     Cada columna se reconoce **por su nombre**, sin importar mayúsculas, acentos, espacios ni puntos
     ("Razón Social" = "razonSocial" = "RAZON SOCIAL"). **El orden da igual**, las columnas de más se ignoran
     y las filas de título que haya arriba del encabezado se saltean.
   - **Sin encabezado:** las columnas se toman en el orden estándar (abajo).
   - Si falta una columna obligatoria, avisa cuál falta.
3. `Registro` convierte cada celda en fecha, importe, CUIT o texto. Si algo está mal, el error dice
   **dónde**: "Linea 3: ..." en CSV, "Fila 4: ..." (la fila de Excel) en Excel.
4. `LectorComprobantes` / `LectorPercepciones` arman los objetos. Después, el service muestra la
   previsualización e importa **todo o nada** (transacción), **salteando lo que ya estaba cargado**.

**Formato de comprobantes** (orden estándar):
```
fecha;tipo;ptoVenta;numero;cuit;razonSocial;neto;iva[;percepciones]
2026-10-01;Factura A;0001;00000010;30-65478912-3;Supermercados Andinos S.A.;1.234,56;259,26;10,00
05/10/2026;Factura B;0001;00000011;30711223345;Almacen Don Pedro S.R.L.;$ 1000;210
```
| Campo | Obligatorio | Nombres de columna aceptados |
|---|---|---|
| fecha | sí | fecha, fecha emisión, fecha de emisión, fecha comprobante |
| tipo | sí | tipo, tipo comprobante, tipo de comprobante |
| ptoVenta | no (vacío → `0001`) | ptoVenta, pto venta, pto vta, punto venta, punto de venta, pv |
| numero | sí | número, nro, nro comprobante, número comprobante, número desde |
| cuit | sí | cuit, cuit contraparte, nro doc, nro documento, nro doc emisor/receptor, cuit emisor/receptor |
| razonSocial | sí | razón social, razón, denominación, denominación emisor/receptor, nombre |
| neto | sí | neto, neto gravado, imp neto gravado, importe neto gravado |
| iva | sí | iva, importe iva, imp iva, monto iva |
| percepciones | no | percepciones, percepción, imp percepciones |

- Fechas `aaaa-mm-dd` o `dd/mm/aaaa` (o fecha de Excel). Importes `1234.56`, `1.234,56` o `$ 1000` (o número
  de Excel, que se redondea a centavos).
- Limitación: el neto y el IVA van a la alícuota 21%.
- Los alias sirven para Excel armados a mano. **El formato real de "Mis Comprobantes" de ARCA hay que
  confirmarlo con archivos de ejemplo** (ver §8): por ejemplo, ARCA escribe el tipo como "1 - Factura A".
- Para sumar un nombre de columna nuevo alcanza con agregarlo a la lista `CAMPOS` del lector.

---

## 4. Base de datos

Esquema versión **2** (`PRAGMA user_version`). Ver `Conexion.crearEsquemaV1` y `crearEsquemaV2`.

```
-- v1
cliente      (cuit PK, razonSocial, nombreFantasia, direccion, impuestosActivos, tieneEmpleados 0/1)
actividad    (id PK, clienteCuit FK→cliente ON DELETE CASCADE, orden, codigo, nombre)
comprobante  (id PK, clienteCuit FK→cliente ON DELETE RESTRICT, operacion VENTA|COMPRA,
              fecha 'aaaa-mm-dd', tipo, puntoVenta, numero, cuitContraparte, razonSocialContraparte,
              neto21, iva21, neto105, iva105, neto27, iva27, noGravado, exento,
              percepciones, otrosTributos, origen,
              UNIQUE(clienteCuit, operacion, tipo, puntoVenta, numero, cuitContraparte))
-- v2
percepcion_retencion (id PK, clienteCuit FK→cliente ON DELETE RESTRICT, clase PERCEPCION|RETENCION,
              tributo IVA|GANANCIAS|SUSS|IIBB, fecha, regimen, jurisdiccion, comprobante,
              cuitAgente, razonSocialAgente, importe, origen)
contraparte  (clienteCuit FK→cliente ON DELETE CASCADE, tipo CLIENTE|PROVEEDOR, cuit, razonSocial,
              PK(clienteCuit, tipo, cuit))      -- un padrón por cliente
```

- **Importes como TEXT** con el valor exacto del `BigDecimal` (`"1234.56"`). SQLite no tiene tipo decimal
  y `REAL` acumula errores de redondeo. Las sumas se hacen en Java.
- **Claves foráneas activas** (`PRAGMA foreign_keys = ON` en cada conexión).
- **Migraciones:** al abrir, si `user_version` es menor que `Conexion.VERSION_ESQUEMA`, se aplican los pasos
  que faltan dentro de una transacción. Para cambiar el esquema: subir `VERSION_ESQUEMA` y agregar un
  `if (desde < 2) { ... }` en `Conexion.migrar`. **Nunca editar un paso que ya se usó.**
- Bases del prototipo (sin versión): las tablas viejas vacías se borran y las que tienen datos se renombran
  a `legacy_*`, así no se pierde nada.

---

## 5. Tests

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.3"
& "C:\netbeans-29\netbeans\java\maven\bin\mvn.cmd" test
```
En NetBeans: clic derecho en el proyecto → **Test**.

Cada test usa una base SQLite temporal propia y **nunca toca la base real**
(clase base `src/test/java/soporte/ConBaseTemporal`).

| Clase | Qué cubre |
|---|---|
| `config.ConexionTest` | Regresión del cierre de conexión, reapertura, esquema y FKs, migraciones (prototipo → v2 y v1 → v2), rollback de transacciones |
| `service.ClienteServiceImplTest` | ABM completo, actividades e impuestos, CUIT, duplicados, filtros, baja con comprobantes |
| `service.ComprobanteServiceImplTest` | Alta/edición/baja, exactitud de importes, filtros por período/operación/texto, duplicados, CSV (formatos, codificación, errores por línea, reimportación), alta automática en el padrón |
| `service.PercepcionServiceImplTest` | Alta/edición/baja, persistencia, filtros, validaciones, CSV y reimportación |
| `service.ImportacionExcelTest` | Excel `.xlsx` y `.xls` generados en el test: título arriba, columnas desordenadas y con acentos, fechas y fórmulas, formato `0000`, CUIT numérico, errores con número de fila, columnas faltantes, archivo dañado |
| `service.ContraparteServiceImplTest` | Un padrón por cliente, listas separadas, CUIT repetido, cambio de CUIT, filtros |
| `service.ImpuestoYReporteServiceTest` | Posición de IVA, Ganancias y SUSS provisorios, totales por mes |
| `view.DialogoClienteTest` | Regresión del formulario de cliente (los 4 campos están en pantalla) |

### Problema conocido al compilar desde la terminal en esta PC
- Si `JAVA_HOME` apunta al Java 21 de Android Studio, falla con *"release version 24 not supported"*.
  Apuntarlo al JDK 25 como en los ejemplos.
- El antivirus **AVG** inspecciona HTTPS y Java no confía en su certificado, así que Maven no puede bajar
  dependencias (*"PKIX path building failed"*). Solución: decirle a Java que use los certificados de Windows:
  ```powershell
  $env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"
  ```
  Solo hace falta cuando hay que bajar algo nuevo. Con todo ya descargado se puede usar `mvn -o` (offline).

---

## 6. Historial de cambios

### Importación desde Excel
- Comprobantes y percepciones se pueden importar desde `.xlsx` y `.xls` (Apache POI 5.2.5).
- Lectores unificados en `service.importacion`: CSV y Excel comparten la misma lógica. Con encabezado,
  las columnas se reconocen por nombre (con alias) y en cualquier orden. Esto también vale para CSV.
- Se quitaron `LectorCsvComprobantes` y `LectorCsvPercepciones`, reemplazados por los nuevos.
- Se silenció el aviso de log4j que genera POI en la consola.

### Backend completo: percepciones, impuestos, reportes y padrones
- `PercepcionServiceImpl` con importación CSV (`LectorCsvPercepciones`): las percepciones ya no se pierden al cerrar.
- `ImpuestoServiceImpl` y `ReporteServiceImpl`: los cálculos salieron del mock y pasaron al backend.
  Ganancias ya no da impuesto negativo cuando el mes tiene pérdida.
- `ContraparteServiceImpl`: un padrón de clientes y proveedores por cada cliente. Los comprobantes cargan
  sus contrapartes al padrón automáticamente.
- Esquema v2 (tablas `percepcion_retencion` y `contraparte`) con migración automática desde la v1.
- La baja de un cliente también se bloquea si tiene percepciones/retenciones.
- `App` usa solo services reales. `ServiciosMock` volvió a su versión original y se usa solo en modo demo.

### Backend real de clientes y comprobantes
- **Bug:** los DAOs cerraban la conexión compartida y la segunda operación fallaba con
  *"database connection closed"*. Ahora no la cierran, y si se cerrara `getConexion()` la reabre.
- **Bug:** en `DialogoCliente` los cuatro campos del formulario usaban `txtCuit`, así que Razón social,
  Nombre de fantasía y Dirección no aparecían y no se podía crear ningún cliente.
- Modelos unificados en `view.Modelos`. Se eliminaron `model.*`, `ContactoDAO` y la clase de prueba
  `SoftwareContableCB` (reemplazada por los tests).
- Nuevo esquema versionado con migración automática desde la base del prototipo.
- `ClienteServiceImpl` y `ComprobanteServiceImpl` sobre SQLite, más el importador CSV.
- `App` usa el backend real por defecto, y el mock completo con `-Dcontabilidad.demo=true`.
- `ServiciosMock` tenía un modo híbrido para que impuestos y reportes usaran los datos reales
  (se quitó en el cambio siguiente, cuando todo pasó al backend).
- `pom.xml`: clase principal `view.App`, JUnit 5 y surefire.

---

## 7. Próximos pasos sugeridos
1. Reemplazar las fórmulas provisorias de impuestos con lo que defina la contadora (ver §8).
2. Ajustar la importación al formato real de ARCA ("Mis Comprobantes" y percepciones) con archivos de ejemplo:
   tipo "1 - Factura A", alícuotas de IVA separadas, etc.
3. PDF: solo si la contadora lo necesita y según qué PDFs use (ver §8). Haría falta Apache PDFBox.
4. Validar el dígito verificador del CUIT.
5. Mover `view.Modelos` a un paquete `model`.
6. Borrar `FrPrincipal` y `FrClientes`: son restos del editor visual y no se usan.

## 8. Pendientes para definir con la contadora
- **Ganancias:** ¿qué necesita ver? ¿Anticipos, DDJJ anual, un estimado? ¿Sus clientes son personas humanas
  (escala progresiva) o sociedades?
- **SUSS:** ¿de dónde sale la nómina real (F.931)? ¿Las retenciones de SUSS sufridas se toman a cuenta?
- **IVA:** ¿se arrastra automáticamente el saldo a favor del mes anterior?
- **Ingresos Brutos:** ¿lo quiere calcular? Hoy se pueden cargar percepciones de IIBB, pero no hay cálculo.
- **Importación (Excel/CSV):** pedirle **archivos reales de ejemplo** de lo que baja de ARCA: "Mis Comprobantes"
  emitidos y recibidos, y los listados de percepciones y retenciones (Mis Retenciones / SIRE). Con eso se
  ajustan los nombres de columnas y el formato del tipo de comprobante.
- **PDF:** antes de programar nada, saber:
  - **¿Qué PDFs usa?** ¿Facturas emitidas desde ARCA ("Comprobantes en línea"), facturas que les mandan los
    proveedores (cada uno con su diseño) o escaneos/fotos de papel?
  - **¿Para qué los usa?** ¿Cargar compras una por una, o tiene listados en PDF?
  - **¿Cuántos por mes?** Si son pocos, cargarlos a mano puede ser más seguro que leerlos automáticamente.
  - **¿Puede bajar lo mismo en Excel?** Si "Mis Comprobantes" ya trae todo, el PDF quizás no haga falta.
  - **Pedirle 3 o 4 PDFs de ejemplo** de cada tipo que use (con datos reales o de prueba).

  Contexto para decidir: un PDF de **un solo diseño conocido** (como los de ARCA) se puede leer bastante bien,
  y además las facturas electrónicas traen un código QR con fecha, CUIT, punto de venta, número y total.
  Si los PDFs son de **diseños distintos** o **escaneados**, leerlos automáticamente es poco confiable y en un
  sistema contable un monto mal leído es un problema serio.
