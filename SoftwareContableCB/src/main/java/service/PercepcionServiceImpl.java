package service;

import config.Conexion;
import dao.ClienteDAO;
import dao.PercepcionDAO;
import util.Cuit;
import view.Modelos.Clase;
import view.Modelos.PercepcionRetencion;
import view.Modelos.Tributo;
import view.Servicios.PercepcionService;
import view.Servicios.ServicioException;

import java.io.File;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Implementacion real de {@link PercepcionService}: percepciones y retenciones sufridas por cada cliente. */
public class PercepcionServiceImpl implements PercepcionService {

    private final PercepcionDAO percepcionDAO;
    private final ClienteDAO clienteDAO;
    private final LectorCsvPercepciones lectorCsv;

    public PercepcionServiceImpl() {
        this(new PercepcionDAO(), new ClienteDAO(), new LectorCsvPercepciones());
    }

    public PercepcionServiceImpl(PercepcionDAO percepcionDAO, ClienteDAO clienteDAO, LectorCsvPercepciones lectorCsv) {
        this.percepcionDAO = percepcionDAO;
        this.clienteDAO = clienteDAO;
        this.lectorCsv = lectorCsv;
    }

    /** Cualquier filtro en {@code null} = todos. Sin cliente activo devuelve una lista vacia. */
    @Override
    public List<PercepcionRetencion> listarPercepciones(String cuitCliente, YearMonth periodo,
                                                        Clase clase, Tributo tributo) {
        String cuit = Cuit.normalizar(cuitCliente);
        if (cuit == null) return new ArrayList<>();
        try {
            return percepcionDAO.listar(cuit,
                    periodo == null ? null : periodo.atDay(1),
                    periodo == null ? null : periodo.atEndOfMonth(),
                    clase, tributo);
        } catch (SQLException e) {
            throw Comun.error("No se pudo listar las percepciones y retenciones", e);
        }
    }

    /** Alta (id nulo) o modificacion (id cargado). */
    @Override
    public void guardarPercepcion(String cuitCliente, PercepcionRetencion item) {
        String cuit = Comun.clienteExistente(clienteDAO, cuitCliente);
        validar(item);
        try {
            if (item.id == null) {
                percepcionDAO.insertar(cuit, item);
            } else if (!percepcionDAO.actualizar(cuit, item)) {
                throw new ServicioException("El movimiento que intentas modificar ya no existe.");
            }
        } catch (SQLException e) {
            throw Comun.error("No se pudo guardar el movimiento", e);
        }
    }

    /** Si no existe no hace nada. */
    @Override
    public void eliminarPercepcion(String cuitCliente, Long id) {
        String cuit = Cuit.normalizar(cuitCliente);
        if (cuit == null || id == null) return;
        try {
            percepcionDAO.eliminar(cuit, id);
        } catch (SQLException e) {
            throw Comun.error("No se pudo eliminar el movimiento", e);
        }
    }

    @Override
    public List<PercepcionRetencion> previsualizarPercepciones(File archivo) {
        if (archivo == null || !archivo.isFile()) throw new ServicioException("No se encontro el archivo.");
        String nombre = archivo.getName().toLowerCase();
        if (nombre.endsWith(".csv") || nombre.endsWith(".txt")) return lectorCsv.leer(archivo);
        if (nombre.endsWith(".xls") || nombre.endsWith(".xlsx")) {
            throw new ServicioException("La importacion desde Excel todavia no esta disponible. "
                    + "Guarda el archivo como CSV desde Excel y volve a intentar.");
        }
        if (nombre.endsWith(".pdf")) {
            throw new ServicioException("La importacion desde PDF todavia no esta disponible. "
                    + "Por ahora carga esos movimientos a mano o desde un CSV.");
        }
        throw new ServicioException("Formato de archivo no soportado: " + archivo.getName());
    }

    /**
     * Todo o nada (una transaccion). Los movimientos identicos a uno ya cargado se saltean,
     * asi reimportar el mismo archivo no duplica.
     * @return cantidad efectivamente agregada
     */
    @Override
    public int importarPercepciones(String cuitCliente, List<PercepcionRetencion> items) {
        String cuit = Comun.clienteExistente(clienteDAO, cuitCliente);
        if (items == null || items.isEmpty()) return 0;
        for (PercepcionRetencion p : items) {
            p.id = null;
            validar(p);
        }
        try {
            return Conexion.enTransaccion(conn -> {
                int agregados = 0;
                for (PercepcionRetencion p : items) {
                    if (percepcionDAO.existeIgual(cuit, p)) continue;
                    percepcionDAO.insertar(cuit, p);
                    agregados++;
                }
                return agregados;
            });
        } catch (SQLException e) {
            throw Comun.error("No se pudo importar los movimientos", e);
        }
    }

    /* ------------------------------------------------------------------ */

    private void validar(PercepcionRetencion p) {
        if (p == null) throw new ServicioException("Faltan los datos del movimiento.");
        if (p.clase == null) throw new ServicioException("Falta indicar si es percepcion o retencion.");
        if (p.tributo == null) throw new ServicioException("Falta el tributo.");
        if (p.fecha == null) throw new ServicioException("Falta la fecha.");
        if (p.importe == null || p.importe.signum() <= 0) {
            throw new ServicioException("El importe tiene que ser mayor a cero.");
        }
        String cuit = Cuit.normalizar(p.cuitAgente);
        if (cuit == null) throw new ServicioException("El CUIT del agente debe tener 11 digitos.");
        p.cuitAgente = cuit;
        p.regimen = Comun.texto(p.regimen);
        p.jurisdiccion = Comun.texto(p.jurisdiccion);
        p.comprobante = Comun.texto(p.comprobante);
        p.razonSocialAgente = Comun.texto(p.razonSocialAgente);
        if (p.origen == null || p.origen.isBlank()) p.origen = "Manual";
    }
}
