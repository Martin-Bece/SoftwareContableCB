package service;

import dao.ClienteDAO;
import util.Cuit;
import view.Servicios.ServicioException;

import java.sql.SQLException;

/** Ayudantes compartidos por los services. */
final class Comun {

    private Comun() { }

    /** Envuelve un error de base en un mensaje para el usuario. */
    static ServicioException error(String accion, SQLException e) {
        return new ServicioException(accion + ": " + e.getMessage(), e);
    }

    /**
     * Normaliza el CUIT del cliente activo y verifica que exista.
     * @throws ServicioException si no hay cliente elegido o no existe
     */
    static String clienteExistente(ClienteDAO clienteDAO, String cuitCliente) {
        String cuit = Cuit.normalizar(cuitCliente);
        if (cuit == null) throw new ServicioException("Primero elegi un cliente.");
        try {
            if (!clienteDAO.existe(cuit)) throw new ServicioException("No existe el cliente " + cuit + ".");
        } catch (SQLException e) {
            throw error("No se pudo verificar el cliente", e);
        }
        return cuit;
    }

    static String texto(String s) {
        return s == null ? "" : s.trim();
    }
}
