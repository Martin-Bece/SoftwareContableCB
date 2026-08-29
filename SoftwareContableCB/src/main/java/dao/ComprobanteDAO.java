package dao;

import config.Conexion;
import model.comprobante;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ComprobanteDAO {

    public boolean insertar(comprobante cb) {
        String sql = "INSERT INTO comprobante (clienteCuit, contactoCuit, tipoOperacion, fecha, tipoComprobante, montoNeto, montoIva, montoTotal) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = Conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cb.getClienteCuit());
            stmt.setString(2, cb.getContactoCuit());
            stmt.setString(3, cb.getTipoOperacion());
            stmt.setString(4, cb.getFecha());
            stmt.setString(5, cb.getTipoComprobante());
            stmt.setDouble(6, cb.getMontoNeto());
            stmt.setDouble(7, cb.getMontoIva());
            stmt.setDouble(8, cb.getMontoTotal());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al insertar comprobante: " + e.getMessage());
            return false;
        }
    }

    public comprobante buscarPorId(int id) {
        String sql = "SELECT * FROM comprobante WHERE id = ?";
        
        try (Connection conn = Conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearComprobante(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar comprobante por ID: " + e.getMessage());
        }
        return null;
    }

    public List<comprobante> listarTodos() {
        List<comprobante> lista = new ArrayList<>();
        String sql = "SELECT * FROM comprobante";

        try (Connection conn = Conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearComprobante(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar comprobantes: " + e.getMessage());
        }
        return lista;
    }

    public List<comprobante> listarPorCliente(String clienteCuit) {
        List<comprobante> lista = new ArrayList<>();
        String sql = "SELECT * FROM comprobante WHERE clienteCuit = ?";

        try (Connection conn = Conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, clienteCuit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearComprobante(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar comprobantes por cliente: " + e.getMessage());
        }
        return lista;
    }

    public boolean actualizar(comprobante cb) {
        String sql = "UPDATE comprobante SET clienteCuit = ?, contactoCuit = ?, tipoOperacion = ?, fecha = ?, tipoComprobante = ?, montoNeto = ?, montoIva = ?, montoTotal = ? WHERE id = ?";
        
        try (Connection conn = Conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cb.getClienteCuit());
            stmt.setString(2, cb.getContactoCuit());
            stmt.setString(3, cb.getTipoOperacion());
            stmt.setString(4, cb.getFecha());
            stmt.setString(5, cb.getTipoComprobante());
            stmt.setDouble(6, cb.getMontoNeto());
            stmt.setDouble(7, cb.getMontoIva());
            stmt.setDouble(8, cb.getMontoTotal());
            stmt.setInt(9, cb.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar comprobante: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(int id) {
        String sql = "DELETE FROM comprobante WHERE id = ?";
        
        try (Connection conn = Conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar comprobante: " + e.getMessage());
            return false;
        }
    }

    private comprobante mapearComprobante(ResultSet rs) throws SQLException {
        return new comprobante(
            rs.getInt("id"),
            rs.getString("clienteCuit"),
            rs.getString("contactoCuit"),
            rs.getString("tipoOperacion"),
            rs.getString("fecha"),
            rs.getString("tipoComprobante"),
            rs.getDouble("montoNeto"),
            rs.getDouble("montoIva"),
            rs.getDouble("montoTotal")
        );
    }
}