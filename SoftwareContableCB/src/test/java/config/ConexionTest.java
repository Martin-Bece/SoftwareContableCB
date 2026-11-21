package config;

import dao.ClienteDAO;
import org.junit.jupiter.api.Test;
import soporte.ConBaseTemporal;
import view.Modelos.Cliente;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConexionTest extends ConBaseTemporal {

    private static final Set<String> TABLAS =
            Set.of("cliente", "actividad", "comprobante", "percepcion_retencion", "contraparte");

    /** Regresion del bug original: el DAO cerraba la conexion compartida y la segunda operacion fallaba. */
    @Test
    void laConexionSigueAbiertaDespuesDeUsarLosDaos() throws Exception {
        ClienteDAO dao = new ClienteDAO();
        dao.insertar(new Cliente("20-11111111-1", "Uno", "", "", false));
        assertEquals(1, dao.listar(null).size());
        dao.insertar(new Cliente("20-22222222-2", "Dos", "", "", false));
        assertEquals(2, dao.listar(null).size());
        assertFalse(Conexion.getConexion().isClosed());
    }

    @Test
    void siLaConexionSeCierraSeReabre() throws Exception {
        Connection primera = Conexion.getConexion();
        primera.close();
        Connection segunda = Conexion.getConexion();
        assertFalse(segunda.isClosed());
        assertNotSame(primera, segunda);
    }

    @Test
    void creaElEsquemaYLaVersion() throws Exception {
        Connection c = Conexion.getConexion();
        assertEquals(TABLAS, tablas(c));
        assertEquals(Conexion.VERSION_ESQUEMA, version(c));
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("PRAGMA foreign_keys")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "las claves foraneas tienen que estar activas");
        }
    }

    @Test
    void reabrirUnaBaseExistenteNoPierdeDatos() throws Exception {
        new ClienteDAO().insertar(new Cliente("20-11111111-1", "Uno", "", "", false));
        Conexion.cerrar();
        assertEquals(1, new ClienteDAO().listar(null).size());
    }

    @Test
    void borraLasTablasVaciasDelPrototipo() throws Exception {
        crearBasePrototipo(false);
        Connection c = Conexion.getConexion();
        assertEquals(TABLAS, tablas(c));
        assertEquals(Conexion.VERSION_ESQUEMA, version(c));
    }

    /** Una base creada con la version 1 (ya publicada) pasa a la 2 sin perder clientes ni comprobantes. */
    @Test
    void migraDeLaVersion1ALa2ConservandoDatos() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + archivoBase);
             Statement st = c.createStatement()) {
            st.execute("CREATE TABLE cliente (cuit TEXT PRIMARY KEY, razonSocial TEXT NOT NULL,"
                    + " nombreFantasia TEXT NOT NULL DEFAULT '', direccion TEXT NOT NULL DEFAULT '',"
                    + " impuestosActivos TEXT NOT NULL DEFAULT '', tieneEmpleados INTEGER NOT NULL DEFAULT 0)");
            st.execute("CREATE TABLE actividad (id INTEGER PRIMARY KEY AUTOINCREMENT, clienteCuit TEXT NOT NULL,"
                    + " orden INTEGER NOT NULL, codigo TEXT NOT NULL DEFAULT '', nombre TEXT NOT NULL)");
            st.execute("CREATE TABLE comprobante (id INTEGER PRIMARY KEY AUTOINCREMENT, clienteCuit TEXT NOT NULL)");
            st.execute("INSERT INTO cliente (cuit, razonSocial) VALUES ('30-11111111-1', 'De la v1')");
            st.execute("PRAGMA user_version = 1");
        }
        Connection c = Conexion.getConexion();
        assertEquals(TABLAS, tablas(c));
        assertEquals(Conexion.VERSION_ESQUEMA, version(c));
        assertEquals("De la v1", new ClienteDAO().buscarPorCuit("30-11111111-1").getRazonSocial());
    }

    @Test
    void renombraLasTablasDelPrototipoQueTienenDatos() throws Exception {
        crearBasePrototipo(true);
        Connection c = Conexion.getConexion();
        Set<String> t = tablas(c);
        assertTrue(t.contains("legacy_cliente"), "los datos viejos se conservan: " + t);
        assertFalse(t.contains("contacto"), "contacto estaba vacia y se borra: " + t);
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT razonSocial FROM legacy_cliente")) {
            assertTrue(rs.next());
            assertEquals("Viejo SA", rs.getString(1));
        }
        // y el esquema nuevo funciona
        new ClienteDAO().insertar(new Cliente("20-11111111-1", "Nuevo", "", "", false));
        assertEquals(1, new ClienteDAO().listar(null).size());
    }

    @Test
    void lasOperacionesEnTransaccionSeDeshacenSiFallan() throws Exception {
        assertThrows(SQLException.class, () -> Conexion.enTransaccion(conn -> {
            try (Statement st = conn.createStatement()) {
                st.execute("INSERT INTO cliente (cuit, razonSocial) VALUES ('20-11111111-1', 'X')");
                st.execute("INSERT INTO cliente (cuit, razonSocial) VALUES ('20-11111111-1', 'Duplicado')");
            }
            return null;
        }));
        assertTrue(new ClienteDAO().listar(null).isEmpty());
        assertTrue(Conexion.getConexion().getAutoCommit());
    }

    /* ------------------------------------------------------------------ */

    /** Simula una base creada por la version vieja de Conexion (sin user_version). */
    private void crearBasePrototipo(boolean conDatos) throws SQLException {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + archivoBase);
             Statement st = c.createStatement()) {
            st.execute("CREATE TABLE cliente (cuit TEXT PRIMARY KEY, razonSocial TEXT NOT NULL, nombreFantasia TEXT,"
                    + " direccion TEXT, actividades TEXT, impuestosActivos TEXT, tieneEmpleados NUMERIC NOT NULL)");
            st.execute("CREATE TABLE contacto (cuit TEXT PRIMARY KEY, razonSocial TEXT NOT NULL, tipo TEXT NOT NULL)");
            st.execute("CREATE TABLE comprobante (id INTEGER PRIMARY KEY AUTOINCREMENT, clienteCuit TEXT NOT NULL,"
                    + " contactoCuit TEXT NOT NULL, tipoOperacion TEXT NOT NULL, fecha TEXT NOT NULL,"
                    + " tipoComprobante TEXT NOT NULL, montoNeto REAL NOT NULL, montoIva REAL NOT NULL,"
                    + " montoTotal REAL NOT NULL,"
                    + " FOREIGN KEY (clienteCuit) REFERENCES cliente(cuit) ON DELETE CASCADE,"
                    + " FOREIGN KEY (contactoCuit) REFERENCES contacto(cuit) ON DELETE RESTRICT)");
            if (conDatos) {
                st.execute("INSERT INTO cliente VALUES ('30-11111111-1', 'Viejo SA', '', '', '', 'IVA', 0)");
            }
        }
    }

    private static Set<String> tablas(Connection c) throws SQLException {
        Set<String> out = new HashSet<>();
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'")) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }

    private static int version(Connection c) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }
}
