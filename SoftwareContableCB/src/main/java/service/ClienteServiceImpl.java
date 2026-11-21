package service;

import dao.ClienteDAO;
import dao.ComprobanteDAO;
import util.Cuit;
import view.Modelos.Cliente;
import view.Servicios.ClienteService;
import view.Servicios.ServicioException;

import java.sql.SQLException;
import java.util.List;

/**
 * Implementacion real de {@link ClienteService} sobre SQLite.
 *
 * <p>Valida los datos, traduce los errores de base a {@link ServicioException}
 * (que la vista muestra al usuario) y delega el SQL en los DAOs.</p>
 */
public class ClienteServiceImpl implements ClienteService {

    private final ClienteDAO clienteDAO;
    private final ComprobanteDAO comprobanteDAO;

    public ClienteServiceImpl() {
        this(new ClienteDAO(), new ComprobanteDAO());
    }

    public ClienteServiceImpl(ClienteDAO clienteDAO, ComprobanteDAO comprobanteDAO) {
        this.clienteDAO = clienteDAO;
        this.comprobanteDAO = comprobanteDAO;
    }

    @Override
    public List<Cliente> listar(String filtro) {
        try {
            return clienteDAO.listar(filtro);
        } catch (SQLException e) {
            throw error("No se pudo listar los clientes", e);
        }
    }

    @Override
    public Cliente buscarPorCuit(String cuit) {
        String normalizado = Cuit.normalizar(cuit);
        if (normalizado == null) return null;
        try {
            return clienteDAO.buscarPorCuit(normalizado);
        } catch (SQLException e) {
            throw error("No se pudo buscar el cliente", e);
        }
    }

    @Override
    public void alta(Cliente cliente) {
        validar(cliente);
        try {
            if (clienteDAO.existe(cliente.getCuit())) {
                throw new ServicioException("Ya existe un cliente con el CUIT " + cliente.getCuit() + ".");
            }
            clienteDAO.insertar(cliente);
        } catch (SQLException e) {
            throw error("No se pudo dar de alta el cliente", e);
        }
    }

    @Override
    public void modificar(Cliente cliente) {
        validar(cliente);
        try {
            if (!clienteDAO.actualizar(cliente)) {
                throw new ServicioException("No existe el cliente " + cliente.getCuit() + ".");
            }
        } catch (SQLException e) {
            throw error("No se pudo modificar el cliente", e);
        }
    }

    /**
     * Da de baja al cliente. Igual que el mock, no se permite si tiene comprobantes:
     * en un sistema contable no queremos borrar historia por accidente.
     */
    @Override
    public void baja(String cuit) {
        String normalizado = Cuit.normalizar(cuit);
        if (normalizado == null) throw new ServicioException("CUIT invalido: " + cuit);
        try {
            int cantidad = comprobanteDAO.contarPorCliente(normalizado);
            if (cantidad > 0) {
                throw new ServicioException("El cliente tiene " + cantidad
                        + " comprobantes cargados. Archivalos antes de darlo de baja.");
            }
            if (!clienteDAO.eliminar(normalizado)) {
                throw new ServicioException("No existe el cliente " + normalizado + ".");
            }
        } catch (SQLException e) {
            throw error("No se pudo dar de baja el cliente", e);
        }
    }

    /* ------------------------------------------------------------------ */

    /** Valida y normaliza (CUIT con guiones, textos sin espacios sobrantes). */
    private void validar(Cliente c) {
        if (c == null) throw new ServicioException("Faltan los datos del cliente.");
        String cuit = Cuit.normalizar(c.getCuit());
        if (cuit == null) {
            throw new ServicioException("El CUIT debe tener 11 digitos (formato 30-71234567-9).");
        }
        c.setCuit(cuit);
        if (c.getRazonSocial() == null || c.getRazonSocial().trim().isEmpty()) {
            throw new ServicioException("La razon social es obligatoria.");
        }
        c.setRazonSocial(c.getRazonSocial().trim());
    }

    static ServicioException error(String accion, SQLException e) {
        return new ServicioException(accion + ": " + e.getMessage(), e);
    }
}
