package dao;

import config.Conexion;
import model.cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    public boolean insertar(cliente c) {
        String sql = "INSERT INTO cliente (cuit, razonSocial, nombreFantasia, direccion, actividades, impuestosActivos, tieneEmpleados) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = Conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, c.getCuit());
            stmt.setString(2, c.getRazonSocial());
            stmt.setString(3, c.getNombreFantasia());
            stmt.setString(4, c.getDireccion());
            stmt.setString(5, c.getActividades());
            stmt.setString(6, c.getImpuestosActivos());
            stmt.setBoolean(7, c.isTieneEmpleados());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al insertar cliente: " + e.getMessage());
            return false;
        }
    }

    public cliente buscarPorCuit(String cuit) {
        String sql = "SELECT * FROM cliente WHERE cuit = ?";
        
        try (Connection conn = Conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cuit);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearCliente(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar cliente por CUIT: " + e.getMessage());
        }
        return null;
    }

    public List<cliente> listarTodos() {
        List<cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM cliente";

        try (Connection conn = Conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearCliente(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar clientes: " + e.getMessage());
        }
        return lista;
    }

    public boolean actualizar(cliente c) {
        String sql = "UPDATE cliente SET razonSocial = ?, nombreFantasia = ?, direccion = ?, actividades = ?, impuestosActivos = ?, tieneEmpleados = ? WHERE cuit = ?";
        
        try (Connection conn = Conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, c.getRazonSocial());
            stmt.setString(2, c.getNombreFantasia());
            stmt.setString(3, c.getDireccion());
            stmt.setString(4, c.getActividades());
            stmt.setString(5, c.getImpuestosActivos());
            stmt.setBoolean(6, c.isTieneEmpleados());
            stmt.setString(7, c.getCuit());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar cliente: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(String cuit) {
        String sql = "DELETE FROM cliente WHERE cuit = ?";
        
        try (Connection conn = Conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cuit);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar cliente: " + e.getMessage());
            return false;
        }
    }

    private cliente mapearCliente(ResultSet rs) throws SQLException {
        return new cliente(
            rs.getString("cuit"),
            rs.getString("razonSocial"),
            rs.getString("nombreFantasia"),
            rs.getString("direccion"),
            rs.getString("actividades"),
            rs.getString("impuestosActivos"),
            rs.getBoolean("tieneEmpleados")
        );
    }
}