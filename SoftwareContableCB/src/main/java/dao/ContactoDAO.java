package dao;

import config.Conexion;
import model.contacto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ContactoDAO {

    public boolean insertar(contacto ct) {
        String sql = "INSERT INTO contacto (cuit, razonSocial, tipo) VALUES (?, ?, ?)";
        
        try (Connection conn = Conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, ct.getCuit());
            stmt.setString(2, ct.getRazonSocial());
            stmt.setString(3, ct.getTipo());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al insertar contacto: " + e.getMessage());
            return false;
        }
    }

    public contacto buscarPorCuit(String cuit) {
        String sql = "SELECT * FROM contacto WHERE cuit = ?";
        
        try (Connection conn = Conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cuit);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearContacto(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar contacto por CUIT: " + e.getMessage());
        }
        return null;
    }

    public List<contacto> listarTodos() {
        List<contacto> lista = new ArrayList<>();
        String sql = "SELECT * FROM contacto";

        try (Connection conn = Conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearContacto(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar contactos: " + e.getMessage());
        }
        return lista;
    }

    public boolean actualizar(contacto ct) {
        String sql = "UPDATE contacto SET razonSocial = ?, tipo = ? WHERE cuit = ?";
        
        try (Connection conn = Conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, ct.getRazonSocial());
            stmt.setString(2, ct.getTipo());
            stmt.setString(3, ct.getCuit());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar contacto: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(String cuit) {
        String sql = "DELETE FROM contacto WHERE cuit = ?";
        
        try (Connection conn = Conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cuit);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar contacto: " + e.getMessage());
            return false;
        }
    }

    private contacto mapearContacto(ResultSet rs) throws SQLException {
        return new contacto(
            rs.getString("cuit"),
            rs.getString("razonSocial"),
            rs.getString("tipo")
        );
    }
}