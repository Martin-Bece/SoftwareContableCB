package service;

import config.Conexion;
import dao.ClienteDAO;
import dao.ComprobanteDAO;
import dao.ContraparteDAO;
import service.importacion.LectorComprobantes;
import util.Cuit;
import view.Modelos;
import view.Modelos.Comprobante;
import view.Modelos.Contraparte;
import view.Modelos.Operacion;
import view.Servicios.ComprobanteService;
import view.Servicios.ServicioException;

import java.io.File;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementacion real de {@link ComprobanteService} sobre SQLite
 * (ventas y compras de cada cliente, carga manual e importacion CSV o Excel).
 */
public class ComprobanteServiceImpl implements ComprobanteService {

    private final ComprobanteDAO comprobanteDAO;
    private final ClienteDAO clienteDAO;
    private final ContraparteDAO contraparteDAO;
    private final LectorComprobantes lector;

    public ComprobanteServiceImpl() {
        this(new ComprobanteDAO(), new ClienteDAO(), new ContraparteDAO(), new LectorComprobantes());
    }

    public ComprobanteServiceImpl(ComprobanteDAO comprobanteDAO, ClienteDAO clienteDAO,
                                  ContraparteDAO contraparteDAO, LectorComprobantes lector) {
        this.comprobanteDAO = comprobanteDAO;
        this.clienteDAO = clienteDAO;
        this.contraparteDAO = contraparteDAO;
        this.lector = lector;
    }

    /**
     * @param periodo mes a listar, o {@code null} para todos
     * @param filtro  texto libre (razon social, CUIT o "Factura A 0001-123"), o {@code null}
     */
    @Override
    public List<Comprobante> listarComprobantes(String cuitCliente, Operacion operacion,
                                                YearMonth periodo, String filtro) {
        String cuit = Cuit.normalizar(cuitCliente);
        if (cuit == null) return new ArrayList<>(); // sin cliente activo
        try {
            return comprobanteDAO.listar(cuit, operacion,
                    periodo == null ? null : periodo.atDay(1),
                    periodo == null ? null : periodo.atEndOfMonth(),
                    filtro);
        } catch (SQLException e) {
            throw Comun.error("No se pudo listar los comprobantes", e);
        }
    }

    /**
     * Alta (id nulo) o modificacion (id cargado). Asigna el id al dar de alta.
     * Si la contraparte no estaba en el padron del cliente, la agrega.
     */
    @Override
    public void guardarComprobante(String cuitCliente, Comprobante comprobante) {
        String cuit = Comun.clienteExistente(clienteDAO, cuitCliente);
        validar(comprobante);
        try {
            Conexion.enTransaccion(conn -> {
                if (comprobanteDAO.existeDuplicado(cuit, comprobante, comprobante.id)) {
                    throw new ServicioException("Ya esta cargado el comprobante "
                            + comprobante.comprobanteCompleto() + " de " + comprobante.razonSocial + ".");
                }
                if (comprobante.id == null) {
                    comprobanteDAO.insertar(cuit, comprobante);
                } else if (!comprobanteDAO.actualizar(cuit, comprobante)) {
                    throw new ServicioException("El comprobante que intentas modificar ya no existe.");
                }
                registrarContraparte(cuit, comprobante);
                return null;
            });
        } catch (SQLException e) {
            throw Comun.error("No se pudo guardar el comprobante", e);
        }
    }

    /** Si el comprobante no existe no hace nada (igual que el mock). */
    @Override
    public void eliminarComprobante(String cuitCliente, Long id) {
        String cuit = Cuit.normalizar(cuitCliente);
        if (cuit == null || id == null) return;
        try {
            comprobanteDAO.eliminar(cuit, id);
        } catch (SQLException e) {
            throw Comun.error("No se pudo eliminar el comprobante", e);
        }
    }

    /** Lee el archivo sin guardar nada, para que el usuario revise antes de importar. */
    @Override
    public List<Comprobante> previsualizarComprobantes(File archivo, Operacion operacion) {
        if (archivo == null || !archivo.isFile()) {
            throw new ServicioException("No se encontro el archivo.");
        }
        if (LectorComprobantes.soporta(archivo)) {
            return lector.leer(archivo, operacion); // CSV, .xlsx o .xls
        }
        if (archivo.getName().toLowerCase().endsWith(".pdf")) {
            throw new ServicioException("La importacion desde PDF todavia no esta disponible. "
                    + "Por ahora carga esos comprobantes a mano o desde un Excel o CSV.");
        }
        throw new ServicioException("Formato de archivo no soportado: " + archivo.getName());
    }

    /**
     * Importa todo en una sola transaccion: si una fila es invalida no se guarda ninguna.
     * Los comprobantes que ya estaban cargados (o repetidos dentro del archivo) se saltean.
     *
     * @return cantidad de comprobantes efectivamente agregados
     */
    @Override
    public int importarComprobantes(String cuitCliente, List<Comprobante> comprobantes) {
        String cuit = Comun.clienteExistente(clienteDAO, cuitCliente);
        if (comprobantes == null || comprobantes.isEmpty()) return 0;
        for (Comprobante c : comprobantes) {
            c.id = null;
            validar(c);
        }
        try {
            return Conexion.enTransaccion(conn -> {
                int agregados = 0;
                for (Comprobante c : comprobantes) {
                    if (comprobanteDAO.existeDuplicado(cuit, c, null)) continue;
                    comprobanteDAO.insertar(cuit, c);
                    registrarContraparte(cuit, c);
                    agregados++;
                }
                return agregados;
            });
        } catch (SQLException e) {
            throw Comun.error("No se pudo importar los comprobantes", e);
        }
    }

    /* ------------------------------------------------------------------ */

    /** Ventas -> padron de clientes del cliente; compras -> padron de proveedores. */
    private void registrarContraparte(String cuitCliente, Comprobante c) throws SQLException {
        ContraparteDAO.Tipo tipo = c.operacion == Operacion.VENTA
                ? ContraparteDAO.Tipo.CLIENTE : ContraparteDAO.Tipo.PROVEEDOR;
        contraparteDAO.insertarSiNoExiste(cuitCliente, tipo, new Contraparte(c.cuitContraparte, c.razonSocial));
    }

    /** Valida y normaliza el comprobante (CUIT con guiones, importes nulos a cero, textos recortados). */
    private void validar(Comprobante c) {
        if (c == null) throw new ServicioException("Faltan los datos del comprobante.");
        if (c.operacion == null) throw new ServicioException("Falta indicar si es venta o compra.");
        if (c.fecha == null) throw new ServicioException("Falta la fecha del comprobante.");
        if (c.tipo == null || c.tipo.isBlank()) throw new ServicioException("Falta el tipo de comprobante.");
        if (c.numero == null || c.numero.isBlank()) throw new ServicioException("Falta el numero del comprobante.");
        c.tipo = c.tipo.trim();
        c.numero = c.numero.trim();
        c.puntoVenta = c.puntoVenta == null || c.puntoVenta.isBlank() ? "0001" : c.puntoVenta.trim();

        String cuit = Cuit.normalizar(c.cuitContraparte);
        if (cuit == null) {
            throw new ServicioException("El CUIT de la contraparte del comprobante " + c.numero
                    + " debe tener 11 digitos.");
        }
        c.cuitContraparte = cuit;
        if (c.razonSocial == null || c.razonSocial.isBlank()) {
            throw new ServicioException("Falta la razon social de la contraparte del comprobante " + c.numero + ".");
        }
        c.razonSocial = c.razonSocial.trim();

        c.neto21 = Modelos.nz(c.neto21);
        c.iva21 = Modelos.nz(c.iva21);
        c.neto105 = Modelos.nz(c.neto105);
        c.iva105 = Modelos.nz(c.iva105);
        c.neto27 = Modelos.nz(c.neto27);
        c.iva27 = Modelos.nz(c.iva27);
        c.noGravado = Modelos.nz(c.noGravado);
        c.exento = Modelos.nz(c.exento);
        c.percepciones = Modelos.nz(c.percepciones);
        c.otrosTributos = Modelos.nz(c.otrosTributos);
        if (c.origen == null || c.origen.isBlank()) c.origen = "Manual";
    }
}
