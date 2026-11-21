package dao;

import config.Conexion;
import view.Modelos.Actividad;
import view.Modelos.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a las tablas {@code cliente} y {@code actividad}.
 *
 * <p>Regla de la casa: los DAOs solo hablan SQL y propagan {@link SQLException}.
 * Las validaciones y los mensajes para el usuario van en la capa {@code service}.
 * Nunca cierran la conexion compartida (ver {@link Conexion}).</p>
 */
public class ClienteDAO {

    private static final String COLUMNAS =
            "cuit, razonSocial, nombreFantasia, direccion, impuestosActivos, tieneEmpleados";

    /** Inserta el cliente y sus actividades en una sola transaccion. */
    public void insertar(Cliente c) throws SQLException {
        Conexion.enTransaccion(conn -> {
            String sql = "INSERT INTO cliente (" + COLUMNAS + ") VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, c.getCuit());
                ps.setString(2, c.getRazonSocial());
                ps.setString(3, texto(c.getNombreFantasia()));
                ps.setString(4, texto(c.getDireccion()));
                ps.setString(5, c.getImpuestosComoTexto());
                ps.setInt(6, c.isTieneEmpleados() ? 1 : 0);
                ps.executeUpdate();
            }
            guardarActividades(conn, c);
            return null;
        });
    }

    /**
     * Actualiza los datos del cliente (el CUIT no cambia) y reemplaza sus actividades.
     * @return {@code false} si no existe un cliente con ese CUIT.
     */
    public boolean actualizar(Cliente c) throws SQLException {
        return Conexion.enTransaccion(conn -> {
            String sql = "UPDATE cliente SET razonSocial = ?, nombreFantasia = ?, direccion = ?, "
                    + "impuestosActivos = ?, tieneEmpleados = ? WHERE cuit = ?";
            int filas;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, c.getRazonSocial());
                ps.setString(2, texto(c.getNombreFantasia()));
                ps.setString(3, texto(c.getDireccion()));
                ps.setString(4, c.getImpuestosComoTexto());
                ps.setInt(5, c.isTieneEmpleados() ? 1 : 0);
                ps.setString(6, c.getCuit());
                filas = ps.executeUpdate();
            }
            if (filas == 0) return false;
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM actividad WHERE clienteCuit = ?")) {
                ps.setString(1, c.getCuit());
                ps.executeUpdate();
            }
            guardarActividades(conn, c);
            return true;
        });
    }

    /** Borra el cliente (sus actividades se borran en cascada). */
    public boolean eliminar(String cuit) throws SQLException {
        Connection conn = Conexion.getConexion();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM cliente WHERE cuit = ?")) {
            ps.setString(1, cuit);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean existe(String cuit) throws SQLException {
        Connection conn = Conexion.getConexion();
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM cliente WHERE cuit = ?")) {
            ps.setString(1, cuit);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Cliente buscarPorCuit(String cuit) throws SQLException {
        Connection conn = Conexion.getConexion();
        try (PreparedStatement ps = conn.prepareStatement("SELECT " + COLUMNAS + " FROM cliente WHERE cuit = ?")) {
            ps.setString(1, cuit);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Cliente c = mapear(rs);
                c.getActividades().addAll(actividadesDe(conn, cuit));
                return c;
            }
        }
    }

    /**
     * Lista clientes ordenados por razon social. Si {@code filtro} no esta vacio, busca
     * (sin distinguir mayusculas) en razon social, nombre de fantasia y CUIT.
     */
    public List<Cliente> listar(String filtro) throws SQLException {
        Connection conn = Conexion.getConexion();
        String f = filtro == null ? "" : filtro.trim();
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNAS + " FROM cliente");
        if (!f.isEmpty()) {
            sql.append(" WHERE razonSocial LIKE ? ESCAPE '\\' OR nombreFantasia LIKE ? ESCAPE '\\'"
                    + " OR cuit LIKE ? ESCAPE '\\'");
        }
        sql.append(" ORDER BY razonSocial COLLATE NOCASE");

        List<Cliente> lista = new ArrayList<>();
        Map<String, Cliente> porCuit = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (!f.isEmpty()) {
                String patron = Sql.patronLike(f);
                ps.setString(1, patron);
                ps.setString(2, patron);
                ps.setString(3, patron);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Cliente c = mapear(rs);
                    lista.add(c);
                    porCuit.put(c.getCuit(), c);
                }
            }
        }
        if (lista.isEmpty()) return lista;

        // Una sola consulta para todas las actividades (evita N+1 consultas).
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT clienteCuit, codigo, nombre FROM actividad ORDER BY clienteCuit, orden");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Cliente c = porCuit.get(rs.getString("clienteCuit"));
                if (c != null) c.getActividades().add(new Actividad(rs.getString("codigo"), rs.getString("nombre")));
            }
        }
        return lista;
    }

    /* ------------------------------------------------------------------ */

    private void guardarActividades(Connection conn, Cliente c) throws SQLException {
        String sql = "INSERT INTO actividad (clienteCuit, orden, codigo, nombre) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int orden = 0;
            for (Actividad a : c.getActividades()) {
                ps.setString(1, c.getCuit());
                ps.setInt(2, orden++);
                ps.setString(3, texto(a.codigo));
                ps.setString(4, texto(a.nombre));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private List<Actividad> actividadesDe(Connection conn, String cuit) throws SQLException {
        List<Actividad> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT codigo, nombre FROM actividad WHERE clienteCuit = ? ORDER BY orden")) {
            ps.setString(1, cuit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Actividad(rs.getString("codigo"), rs.getString("nombre")));
            }
        }
        return out;
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente(
                rs.getString("cuit"),
                rs.getString("razonSocial"),
                rs.getString("nombreFantasia"),
                rs.getString("direccion"),
                rs.getInt("tieneEmpleados") == 1);
        c.setImpuestosDesdeTexto(rs.getString("impuestosActivos"));
        return c;
    }

    private static String texto(String s) {
        return s == null ? "" : s.trim();
    }
}
