package dao;

import config.Conexion;
import view.Modelos.Contraparte;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a la tabla {@code contraparte}: el padron de clientes y proveedores de CADA cliente.
 * La clave es (cliente, tipo, cuit): el mismo proveedor puede estar en el padron de varios clientes,
 * y cada uno lo tiene por separado.
 */
public class ContraparteDAO {

    /** Tipo de contraparte, tal como se guarda en la columna {@code tipo}. */
    public enum Tipo { CLIENTE, PROVEEDOR }

    public List<Contraparte> listar(String cuitCliente, Tipo tipo, String filtro) throws SQLException {
        Connection conn = Conexion.getConexion();
        String f = filtro == null ? "" : filtro.trim();
        String sql = "SELECT cuit, razonSocial FROM contraparte WHERE clienteCuit = ? AND tipo = ?"
                + (f.isEmpty() ? "" : " AND (razonSocial LIKE ? ESCAPE '\\' OR cuit LIKE ? ESCAPE '\\')")
                + " ORDER BY razonSocial COLLATE NOCASE";
        List<Contraparte> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cuitCliente);
            ps.setString(2, tipo.name());
            if (!f.isEmpty()) {
                String patron = Sql.patronLike(f);
                ps.setString(3, patron);
                ps.setString(4, patron);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Contraparte(rs.getString("cuit"), rs.getString("razonSocial")));
            }
        }
        return out;
    }

    public boolean existe(String cuitCliente, Tipo tipo, String cuit) throws SQLException {
        Connection conn = Conexion.getConexion();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM contraparte WHERE clienteCuit = ? AND tipo = ? AND cuit = ?")) {
            ps.setString(1, cuitCliente);
            ps.setString(2, tipo.name());
            ps.setString(3, cuit);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insertar(String cuitCliente, Tipo tipo, Contraparte c) throws SQLException {
        Connection conn = Conexion.getConexion();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO contraparte (clienteCuit, tipo, cuit, razonSocial) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, cuitCliente);
            ps.setString(2, tipo.name());
            ps.setString(3, c.cuit);
            ps.setString(4, c.razonSocial);
            ps.executeUpdate();
        }
    }

    /** Agrega la contraparte solo si no estaba en el padron (no pisa la razon social existente). */
    public void insertarSiNoExiste(String cuitCliente, Tipo tipo, Contraparte c) throws SQLException {
        Connection conn = Conexion.getConexion();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO contraparte (clienteCuit, tipo, cuit, razonSocial) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, cuitCliente);
            ps.setString(2, tipo.name());
            ps.setString(3, c.cuit);
            ps.setString(4, c.razonSocial);
            ps.executeUpdate();
        }
    }

    /** Reemplaza la contraparte {@code cuitOriginal} por {@code c} (puede cambiar el CUIT). */
    public boolean actualizar(String cuitCliente, Tipo tipo, String cuitOriginal, Contraparte c) throws SQLException {
        Connection conn = Conexion.getConexion();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE contraparte SET cuit = ?, razonSocial = ? WHERE clienteCuit = ? AND tipo = ? AND cuit = ?")) {
            ps.setString(1, c.cuit);
            ps.setString(2, c.razonSocial);
            ps.setString(3, cuitCliente);
            ps.setString(4, tipo.name());
            ps.setString(5, cuitOriginal);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(String cuitCliente, Tipo tipo, String cuit) throws SQLException {
        Connection conn = Conexion.getConexion();
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM contraparte WHERE clienteCuit = ? AND tipo = ? AND cuit = ?")) {
            ps.setString(1, cuitCliente);
            ps.setString(2, tipo.name());
            ps.setString(3, cuit);
            return ps.executeUpdate() > 0;
        }
    }
}
