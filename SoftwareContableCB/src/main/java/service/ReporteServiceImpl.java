package service;

import dao.ComprobanteDAO;
import util.Cuit;
import view.Modelos.Comprobante;
import view.Modelos.Operacion;
import view.Modelos.TotalPeriodo;
import view.Servicios.ReporteService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Totales mensuales de ventas y compras para los reportes y la pantalla de inicio.
 * Las notas de credito restan.
 */
public class ReporteServiceImpl implements ReporteService {

    private final ComprobanteDAO comprobanteDAO;

    public ReporteServiceImpl() {
        this(new ComprobanteDAO());
    }

    public ReporteServiceImpl(ComprobanteDAO comprobanteDAO) {
        this.comprobanteDAO = comprobanteDAO;
    }

    /** Una fila por mes entre {@code desde} y {@code hasta} inclusive, aunque el mes no tenga movimientos. */
    @Override
    public List<TotalPeriodo> totalesPorPeriodo(String cuitCliente, YearMonth desde, YearMonth hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) return new ArrayList<>();
        Map<YearMonth, TotalPeriodo> porMes = new LinkedHashMap<>();
        for (YearMonth m = desde; !m.isAfter(hasta); m = m.plusMonths(1)) {
            porMes.put(m, new TotalPeriodo(m));
        }
        String cuit = Cuit.normalizar(cuitCliente);
        if (cuit == null) return new ArrayList<>(porMes.values()); // sin cliente: meses en cero

        List<Comprobante> lista;
        try {
            // Una sola consulta para todo el rango (en vez de una por mes).
            lista = comprobanteDAO.listar(cuit, null, desde.atDay(1), hasta.atEndOfMonth(), null);
        } catch (SQLException e) {
            throw Comun.error("No se pudo calcular el reporte", e);
        }
        for (Comprobante c : lista) {
            TotalPeriodo t = porMes.get(YearMonth.from(c.fecha));
            BigDecimal signo = BigDecimal.valueOf(c.signo());
            if (c.operacion == Operacion.VENTA) {
                t.cantidadVentas++;
                t.netoVentas = t.netoVentas.add(c.netoGravado().multiply(signo));
                t.ivaVentas = t.ivaVentas.add(c.ivaTotal().multiply(signo));
            } else {
                t.cantidadCompras++;
                t.netoCompras = t.netoCompras.add(c.netoGravado().multiply(signo));
                t.ivaCompras = t.ivaCompras.add(c.ivaTotal().multiply(signo));
            }
        }
        return new ArrayList<>(porMes.values());
    }
}
