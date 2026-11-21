package view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.ClienteServiceImpl;
import service.ComprobanteServiceImpl;
import soporte.ConBaseTemporal;
import view.Modelos.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que la app (App.contextoReal) calcule IVA y reportes sobre los
 * comprobantes reales de la base, y no sobre los datos de ejemplo del mock.
 */
class ModoHibridoTest extends ConBaseTemporal {

    private static final String CUIT = "30-71234567-9";
    private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

    private AppContext ctx;

    @BeforeEach
    void preparar() {
        ClienteServiceImpl clientes = new ClienteServiceImpl();
        ComprobanteServiceImpl comprobantes = new ComprobanteServiceImpl();
        ServiciosMock resto = new ServiciosMock(clientes, comprobantes);
        ctx = new AppContext(clientes, comprobantes, resto, resto, resto, resto);

        clientes.alta(new Cliente(CUIT, "Cliente Real", "", "", true));
        guardar(Operacion.VENTA, "1", "Factura A", "1000", "210");
        guardar(Operacion.VENTA, "2", "Nota de credito A", "100", "21");
        guardar(Operacion.COMPRA, "3", "Factura A", "400", "84");
    }

    private void guardar(Operacion op, String numero, String tipo, String neto, String iva) {
        Comprobante c = new Comprobante();
        c.operacion = op;
        c.fecha = LocalDate.of(2026, 10, 10);
        c.tipo = tipo;
        c.numero = numero;
        c.cuitContraparte = "30-65478912-3";
        c.razonSocial = "Contraparte";
        c.neto21 = new BigDecimal(neto);
        c.iva21 = new BigDecimal(iva);
        ctx.comprobantes().guardarComprobante(CUIT, c);
    }

    @Test
    void noHayDatosDeEjemploDelMock() {
        List<Cliente> lista = ctx.clientes().listar(null);
        assertEquals(1, lista.size());
        assertEquals("Cliente Real", lista.get(0).getRazonSocial());
        assertTrue(ctx.contrapartes().listarProveedores(CUIT, null).isEmpty());
        assertTrue(ctx.percepciones().listarPercepciones(CUIT, OCTUBRE, null, null).isEmpty());
    }

    @Test
    void elIvaSeCalculaConLosComprobantesReales() {
        PosicionIva iva = ctx.impuestos().determinarIva(CUIT, OCTUBRE);
        assertEquals(2, iva.cantidadVentas);
        assertEquals(1, iva.cantidadCompras);
        assertEquals(0, new BigDecimal("189.00").compareTo(iva.debitoFiscal), "210 - 21 de la nota de credito");
        assertEquals(0, new BigDecimal("84.00").compareTo(iva.creditoFiscal));
        assertEquals(0, new BigDecimal("105.00").compareTo(iva.saldoTecnico()));

        // con percepciones (todavia en memoria) se descuentan
        PercepcionRetencion p = new PercepcionRetencion();
        p.fecha = LocalDate.of(2026, 10, 5);
        p.importe = new BigDecimal("5");
        ctx.percepciones().guardarPercepcion(CUIT, p);
        assertEquals(0, new BigDecimal("100.00").compareTo(ctx.impuestos().determinarIva(CUIT, OCTUBRE).saldoFinal()));
    }

    @Test
    void losReportesUsanLosComprobantesReales() {
        List<TotalPeriodo> totales = ctx.reportes().totalesPorPeriodo(CUIT, OCTUBRE.minusMonths(1), OCTUBRE);
        assertEquals(2, totales.size());
        assertEquals(0, totales.get(0).cantidadVentas, "septiembre vacio");
        assertEquals(0, new BigDecimal("900.00").compareTo(totales.get(1).netoVentas));
        assertEquals(0, new BigDecimal("400.00").compareTo(totales.get(1).netoCompras));
    }

    @Test
    void sussUsaElClienteReal() {
        assertFalse(ctx.impuestos().determinar(CUIT, OCTUBRE, Tributo.SUSS).isEmpty(),
                "el cliente real tiene empleados, asi que hay filas de SUSS");
    }

    @Test
    void sinClienteActivoNoExplota() {
        assertEquals(0, ctx.impuestos().determinarIva(null, OCTUBRE).cantidadVentas);
        assertEquals(1, ctx.reportes().totalesPorPeriodo(null, OCTUBRE, OCTUBRE).size());
    }
}
