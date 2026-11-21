# Software Contable – Carolina Becerra

Aplicación de escritorio (Java 24 + Swing/FlatLaf + SQLite) para que la contadora gestione a sus clientes:
ABM de clientes, carga de ventas y compras (manual o desde archivo), percepciones y retenciones,
determinación de impuestos (IVA, Ganancias, SUSS) y reportes.

- **Frontend** (paquetes `view` y `view.paneles`): todas las pantallas.
- **Backend** (paquetes `config`, `dao`, `service`, `util`): base de datos y reglas de negocio.

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
| **Real** (por defecto) | Run normal | Clientes y comprobantes se guardan en SQLite. Lo demás sigue en el mock (ver §3). |
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
| `dao` | `ClienteDAO`, `ComprobanteDAO` y el ayudante interno `Sql`. |
| `service` | `ClienteServiceImpl`, `ComprobanteServiceImpl`, `LectorCsvComprobantes`. |
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
| `ClienteService` | `service.ClienteServiceImpl` | ✅ Real (SQLite) |
| `ComprobanteService` | `service.ComprobanteServiceImpl` | ✅ Real (SQLite). Importación: solo CSV |
| `PercepcionService` | `ServiciosMock` (híbrido) | ⏳ En memoria, **se pierde al cerrar** |
| `ContraparteService` (clientes del cliente / proveedores) | `ServiciosMock` (híbrido) | ⏳ En memoria, **se pierde al cerrar** |
| `ImpuestoService` | `ServiciosMock` (híbrido) | ⚠️ Calcula sobre los comprobantes **reales**. IVA razonable; Ganancias y SUSS son fórmulas de ejemplo |
| `ReporteService` | `ServiciosMock` (híbrido) | ✅ Calcula sobre los comprobantes **reales** |

**Modo híbrido del mock:** `new ServiciosMock(clientesReales, comprobantesReales)` no carga datos de ejemplo
y toma clientes y comprobantes de los services reales. Así impuestos y reportes muestran los números de la base.

### Reglas de negocio implementadas

**Clientes**
- El CUIT es la clave. Se guarda siempre como `XX-XXXXXXXX-X`. Se acepta con o sin guiones y tiene que tener 11 dígitos.
  *No se valida todavía el dígito verificador.*
- La razón social es obligatoria.
- No se puede dar de alta un CUIT repetido.
- **No se puede dar de baja un cliente que tiene comprobantes** (para no borrar historia contable por error).
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

**Importación CSV** (`LectorCsvComprobantes`)
```
fecha;tipo;ptoVenta;numero;cuit;razonSocial;neto;iva[;percepciones]
2026-10-01;Factura A;0001;00000010;30-65478912-3;Supermercados Andinos S.A.;1.234,56;259,26;10,00
05/10/2026;Factura B;0001;00000011;30711223345;Almacen Don Pedro S.R.L.;$ 1000;210
```
- Separador `;` (o `,` si los importes no usan coma decimal). Fechas `aaaa-mm-dd` o `dd/mm/aaaa`.
  Importes `1234.56` o `1.234,56`. El encabezado es opcional.
- Archivos en UTF-8 o Windows-1252 (lo que guarda Excel).
- Una fila inválida muestra un error que dice **qué línea** falla.
- Primero se previsualiza y después se importa. La importación es todo o nada (transacción).
  **Los comprobantes ya cargados se saltean**, así que reimportar el mismo archivo no duplica nada.
- Limitación: el neto y el IVA del CSV van a la alícuota 21%.
- Excel y PDF todavía no: muestran un mensaje claro (antes el mock inventaba filas de ejemplo).

---

## 4. Base de datos

Esquema versión **1** (`PRAGMA user_version`). Ver `Conexion.crearEsquemaV1`.

```
cliente      (cuit PK, razonSocial, nombreFantasia, direccion, impuestosActivos, tieneEmpleados 0/1)
actividad    (id PK, clienteCuit FK→cliente ON DELETE CASCADE, orden, codigo, nombre)
comprobante  (id PK, clienteCuit FK→cliente ON DELETE RESTRICT, operacion VENTA|COMPRA,
              fecha 'aaaa-mm-dd', tipo, puntoVenta, numero, cuitContraparte, razonSocialContraparte,
              neto21, iva21, neto105, iva105, neto27, iva27, noGravado, exento,
              percepciones, otrosTributos, origen,
              UNIQUE(clienteCuit, operacion, tipo, puntoVenta, numero, cuitContraparte))
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
| `config.ConexionTest` | Regresión del cierre de conexión, reapertura, esquema y FKs, migración desde el prototipo, rollback de transacciones |
| `service.ClienteServiceImplTest` | ABM completo, actividades e impuestos, CUIT, duplicados, filtros, baja con comprobantes |
| `service.ComprobanteServiceImplTest` | Alta/edición/baja, exactitud de importes, filtros por período/operación/texto, duplicados, CSV (formatos, codificación, errores por línea, reimportación) |
| `view.ModoHibridoTest` | IVA y reportes calculados sobre los comprobantes reales |
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
- `ServiciosMock` tiene un modo híbrido para que impuestos y reportes usen los datos reales.
- `pom.xml`: clase principal `view.App`, JUnit 5 y surefire.

---

## 7. Próximos pasos sugeridos
1. `PercepcionService` real (tabla `percepcion_retencion`). Es lo más urgente: hoy se pierde al cerrar.
2. `ContraparteService` real. Antes hay que decidir con la clienta si un proveedor se comparte entre clientes
   o cada cliente tiene su lista.
3. Pasar los cálculos de impuestos y reportes al backend (`service.ImpuestoServiceImpl`), confirmando con la
   contadora las fórmulas de Ganancias y SUSS y el arrastre del saldo a favor de IVA.
4. Importación desde el formato real de ARCA ("Mis Comprobantes"): pedir archivos de ejemplo. Para Excel y
   PDF hacen falta Apache POI y PDFBox.
5. Validar el dígito verificador del CUIT.
6. Mover `view.Modelos` a un paquete `model`.
7. Borrar `FrPrincipal` y `FrClientes`: son restos del editor visual y no se usan.
