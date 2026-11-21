package config;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Punto unico de acceso a la base SQLite.
 *
 * <p>La aplicacion comparte UNA conexion abierta durante toda la ejecucion.
 * Por eso los DAOs <b>nunca</b> deben cerrarla (no la pongan dentro de un
 * {@code try (Connection c = ...)}): solo cierran sus {@code PreparedStatement}
 * y {@code ResultSet}. Si por algun motivo la conexion quedo cerrada,
 * {@link #getConexion()} la vuelve a abrir.</p>
 *
 * <p>Ubicacion de la base: {@code ~/.contabilidad_app/contabilidad.db}, salvo que
 * se indique otra con la propiedad de sistema {@value #PROPIEDAD_RUTA}
 * (la usan los tests para trabajar sobre una base temporal).</p>
 *
 * <p>El esquema esta versionado con {@code PRAGMA user_version}. Para cambiar
 * tablas en el futuro: subir {@link #VERSION_ESQUEMA} y agregar un paso en
 * {@link #migrar(Connection, int)}; nunca editar un paso ya publicado.</p>
 */
public final class Conexion {

    /** Propiedad de sistema para usar otra ruta de base (ej. tests). */
    public static final String PROPIEDAD_RUTA = "contabilidad.db";

    /** Version actual del esquema. */
    public static final int VERSION_ESQUEMA = 1;

    private static Connection conexion;

    private Conexion() { }

    /** Trabajo a ejecutar dentro de una transaccion. */
    @FunctionalInterface
    public interface Trabajo<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }

    /**
     * Devuelve la conexion compartida, abriendola (y creando/migrando el esquema)
     * si hace falta. No la cierres.
     */
    public static synchronized Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            conexion = abrir();
        }
        return conexion;
    }

    /** Cierra la conexion compartida. Util en tests o al salir de la app. */
    public static synchronized void cerrar() {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException ignorada) {
                // nada que hacer: igual la descartamos
            }
            conexion = null;
        }
    }

    /** Ruta del archivo .db que se esta usando. */
    public static String rutaBase() {
        String ruta = System.getProperty(PROPIEDAD_RUTA);
        if (ruta != null && !ruta.isBlank()) return ruta;
        return System.getProperty("user.home") + File.separator + ".contabilidad_app"
                + File.separator + "contabilidad.db";
    }

    /**
     * Ejecuta {@code trabajo} en una transaccion: si algo falla se deshace todo.
     * Si ya hay una transaccion en curso, se suma a ella (no abre otra).
     */
    public static <T> T enTransaccion(Trabajo<T> trabajo) throws SQLException {
        Connection c = getConexion();
        if (!c.getAutoCommit()) {
            return trabajo.ejecutar(c); // transaccion anidada: la maneja el de afuera
        }
        c.setAutoCommit(false);
        try {
            T resultado = trabajo.ejecutar(c);
            c.commit();
            return resultado;
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }

    /* ------------------------------------------------------------------ */

    private static Connection abrir() throws SQLException {
        File archivo = new File(rutaBase());
        File carpeta = archivo.getAbsoluteFile().getParentFile();
        if (carpeta != null && !carpeta.exists() && !carpeta.mkdirs()) {
            throw new SQLException("No se pudo crear la carpeta " + carpeta);
        }

        Connection c = DriverManager.getConnection("jdbc:sqlite:" + archivo.getAbsolutePath());
        try {
            try (Statement st = c.createStatement()) {
                // SQLite trae las claves foraneas apagadas por defecto: hay que activarlas por conexion.
                st.execute("PRAGMA foreign_keys = ON");
            }
            int version = leerVersion(c);
            if (version < VERSION_ESQUEMA) {
                migrar(c, version);
            } else if (version > VERSION_ESQUEMA) {
                throw new SQLException("La base fue creada por una version mas nueva del sistema (esquema "
                        + version + "). Actualiza la aplicacion.");
            }
            return c;
        } catch (SQLException e) {
            c.close();
            throw e;
        }
    }

    private static int leerVersion(Connection c) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Lleva el esquema desde {@code desde} hasta {@link #VERSION_ESQUEMA}, todo en una transaccion. */
    private static void migrar(Connection c, int desde) throws SQLException {
        c.setAutoCommit(false);
        try (Statement st = c.createStatement()) {
            if (desde < 1) {
                apartarTablasPrototipo(c);
                crearEsquemaV1(st);
            }
            st.execute("PRAGMA user_version = " + VERSION_ESQUEMA);
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }

    /**
     * Las bases creadas por el prototipo (antes de versionar el esquema) tienen tablas
     * cliente/contacto/comprobante con otra estructura. Si estan vacias se borran;
     * si tienen datos se renombran a legacy_* para no perder nada.
     */
    private static void apartarTablasPrototipo(Connection c) throws SQLException {
        // Orden: primero las que tienen claves foraneas hacia las otras.
        String[] viejas = {"comprobante", "contacto", "cliente"};
        List<String> existentes = new ArrayList<>();
        for (String t : viejas) {
            if (existeTabla(c, t)) existentes.add(t);
        }
        if (existentes.isEmpty()) return;

        try (Statement st = c.createStatement()) {
            // Durante el reacomodo apagamos las FK para poder borrar/renombrar en cualquier orden.
            st.execute("PRAGMA defer_foreign_keys = ON");
            for (String t : existentes) {
                if (contarFilas(c, t) == 0) {
                    st.execute("DROP TABLE " + t);
                } else {
                    String destino = "legacy_" + t;
                    if (existeTabla(c, destino)) destino = destino + "_" + System.currentTimeMillis();
                    st.execute("ALTER TABLE " + t + " RENAME TO " + destino);
                }
            }
        }
    }

    private static void crearEsquemaV1(Statement st) throws SQLException {
        // Importes: se guardan como TEXT con el valor exacto de BigDecimal (ej. "1234.56").
        // Asi no hay errores de redondeo de punto flotante; las sumas se hacen en Java.
        st.execute("""
            CREATE TABLE cliente (
                cuit             TEXT PRIMARY KEY,          -- formato 30-12345678-9
                razonSocial      TEXT NOT NULL,
                nombreFantasia   TEXT NOT NULL DEFAULT '',
                direccion        TEXT NOT NULL DEFAULT '',
                impuestosActivos TEXT NOT NULL DEFAULT '',  -- "IVA, Ganancias, SUSS"
                tieneEmpleados   INTEGER NOT NULL DEFAULT 0 CHECK (tieneEmpleados IN (0, 1))
            )
            """);
        st.execute("""
            CREATE TABLE actividad (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                clienteCuit TEXT NOT NULL REFERENCES cliente(cuit) ON DELETE CASCADE,
                orden       INTEGER NOT NULL,               -- para respetar el orden en que se cargaron
                codigo      TEXT NOT NULL DEFAULT '',
                nombre      TEXT NOT NULL
            )
            """);
        st.execute("CREATE INDEX idx_actividad_cliente ON actividad(clienteCuit, orden)");
        st.execute("""
            CREATE TABLE comprobante (
                id                     INTEGER PRIMARY KEY AUTOINCREMENT,
                clienteCuit            TEXT NOT NULL REFERENCES cliente(cuit) ON DELETE RESTRICT,
                operacion              TEXT NOT NULL CHECK (operacion IN ('VENTA', 'COMPRA')),
                fecha                  TEXT NOT NULL,       -- ISO yyyy-MM-dd (ordena bien como texto)
                tipo                   TEXT NOT NULL,       -- "Factura A", "Nota de credito B", ...
                puntoVenta             TEXT NOT NULL,
                numero                 TEXT NOT NULL,
                cuitContraparte        TEXT NOT NULL,
                razonSocialContraparte TEXT NOT NULL,
                neto21        TEXT NOT NULL DEFAULT '0',
                iva21         TEXT NOT NULL DEFAULT '0',
                neto105       TEXT NOT NULL DEFAULT '0',
                iva105        TEXT NOT NULL DEFAULT '0',
                neto27        TEXT NOT NULL DEFAULT '0',
                iva27         TEXT NOT NULL DEFAULT '0',
                noGravado     TEXT NOT NULL DEFAULT '0',
                exento        TEXT NOT NULL DEFAULT '0',
                percepciones  TEXT NOT NULL DEFAULT '0',
                otrosTributos TEXT NOT NULL DEFAULT '0',
                origen        TEXT NOT NULL DEFAULT 'Manual', -- Manual | CSV | Excel | PDF AFIP
                -- Un mismo comprobante no puede cargarse dos veces para el mismo cliente.
                UNIQUE (clienteCuit, operacion, tipo, puntoVenta, numero, cuitContraparte)
            )
            """);
        st.execute("CREATE INDEX idx_comprobante_periodo ON comprobante(clienteCuit, operacion, fecha)");
    }

    private static boolean existeTabla(Connection c, String tabla) throws SQLException {
        try (var ps = c.prepareStatement("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            ps.setString(1, tabla);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static long contarFilas(Connection c, String tabla) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }
}
