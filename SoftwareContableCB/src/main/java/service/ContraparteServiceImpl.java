package service;

import config.Conexion;
import dao.ClienteDAO;
import dao.ContraparteDAO;
import dao.ContraparteDAO.Tipo;
import util.Cuit;
import view.Modelos.Contraparte;
import view.Servicios.ContraparteService;
import view.Servicios.ServicioException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Padron de "clientes del cliente" y proveedores. Cada cliente de la contadora tiene el suyo:
 * si dos clientes comparten un proveedor, se carga en cada uno por separado y editarlo en uno
 * no cambia el otro.
 *
 * <p>Ademas, {@link ComprobanteServiceImpl} agrega automaticamente al padron las contrapartes
 * de los comprobantes que se cargan o importan.</p>
 */
public class ContraparteServiceImpl implements ContraparteService {

    private final ContraparteDAO contraparteDAO;
    private final ClienteDAO clienteDAO;

    public ContraparteServiceImpl() {
        this(new ContraparteDAO(), new ClienteDAO());
    }

    public ContraparteServiceImpl(ContraparteDAO contraparteDAO, ClienteDAO clienteDAO) {
        this.contraparteDAO = contraparteDAO;
        this.clienteDAO = clienteDAO;
    }

    @Override
    public List<Contraparte> listarClientesDeCliente(String cuitCliente, String filtro) {
        return listar(cuitCliente, Tipo.CLIENTE, filtro);
    }

    @Override
    public List<Contraparte> listarProveedores(String cuitCliente, String filtro) {
        return listar(cuitCliente, Tipo.PROVEEDOR, filtro);
    }

    /** @param cuitOriginal {@code null} para dar de alta; el CUIT anterior para modificar. */
    @Override
    public void guardarClienteDeCliente(String cuitCliente, Contraparte c, String cuitOriginal) {
        guardar(cuitCliente, Tipo.CLIENTE, c, cuitOriginal);
    }

    /** @param cuitOriginal {@code null} para dar de alta; el CUIT anterior para modificar. */
    @Override
    public void guardarProveedor(String cuitCliente, Contraparte c, String cuitOriginal) {
        guardar(cuitCliente, Tipo.PROVEEDOR, c, cuitOriginal);
    }

    @Override
    public void eliminarClienteDeCliente(String cuitCliente, String cuit) {
        eliminar(cuitCliente, Tipo.CLIENTE, cuit);
    }

    @Override
    public void eliminarProveedor(String cuitCliente, String cuit) {
        eliminar(cuitCliente, Tipo.PROVEEDOR, cuit);
    }

    /* ------------------------------------------------------------------ */

    private List<Contraparte> listar(String cuitCliente, Tipo tipo, String filtro) {
        String cuit = Cuit.normalizar(cuitCliente);
        if (cuit == null) return new ArrayList<>();
        try {
            return contraparteDAO.listar(cuit, tipo, filtro);
        } catch (SQLException e) {
            throw Comun.error("No se pudo listar el padron", e);
        }
    }

    private void guardar(String cuitCliente, Tipo tipo, Contraparte c, String cuitOriginal) {
        String cliente = Comun.clienteExistente(clienteDAO, cuitCliente);
        if (c == null) throw new ServicioException("Faltan los datos.");
        String cuit = Cuit.normalizar(c.cuit);
        if (cuit == null) throw new ServicioException("El CUIT debe tener 11 digitos.");
        if (c.razonSocial == null || c.razonSocial.isBlank()) {
            throw new ServicioException("La razon social es obligatoria.");
        }
        c.cuit = cuit;
        c.razonSocial = c.razonSocial.trim();
        String original = Cuit.normalizar(cuitOriginal);

        try {
            Conexion.enTransaccion(conn -> {
                boolean cambiaCuit = original == null || !original.equals(cuit);
                if (cambiaCuit && contraparteDAO.existe(cliente, tipo, cuit)) {
                    throw new ServicioException("Ese CUIT ya esta cargado.");
                }
                if (original == null || !contraparteDAO.actualizar(cliente, tipo, original, c)) {
                    contraparteDAO.insertar(cliente, tipo, c);
                }
                return null;
            });
        } catch (SQLException e) {
            throw Comun.error("No se pudo guardar", e);
        }
    }

    /** Quitarlo del padron no toca los comprobantes ya cargados (guardan su propio CUIT y razon social). */
    private void eliminar(String cuitCliente, Tipo tipo, String cuit) {
        String cliente = Cuit.normalizar(cuitCliente);
        String c = Cuit.normalizar(cuit);
        if (cliente == null || c == null) return;
        try {
            contraparteDAO.eliminar(cliente, tipo, c);
        } catch (SQLException e) {
            throw Comun.error("No se pudo eliminar", e);
        }
    }
}
