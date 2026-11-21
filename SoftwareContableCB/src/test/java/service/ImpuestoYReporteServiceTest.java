package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import soporte.ConBaseTemporal;
import view.Modelos.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Calculos de IVA, Ganancias/SUSS (provisorios) y reportes sobre datos guardados en la base. */
class ImpuestoYReporteServiceTest extends ConBaseTemporal {

    private static final String CUIT = "30-71234567-9";
    private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

    private ClienteServiceImpl clientes;
    private ComprobanteServiceImpl comprobantes;
    private PercepcionServiceImpl percepciones;
    private ImpuestoServiceImpl impuestos;
    private ReporteServiceImpl reportes;

    @BeforeEach
    void preparar() {
        clientes = new ClienteServiceImpl();
        comprobantes = new ComprobanteServiceImpl();
        percepciones = new PercepcionServiceImpl();
        impuestos = new ImpuestoServiceImpl(clientes, comprobantes, percepciones);
        reportes = new ReporteServiceImpl();

        clientes.alta(new Cliente(CUIT, "Cliente Real", "", "", true));
        guardar(Operacion.VENTA, "1", "Factura A", LocalDate.of(2026, 10, 10), "1000", "210");
        guardar(Operacion.VENTA, "2", "Nota de credito A", LocalDate.of(2026, 10, 20), "100", "21");
        guardar(Operacion.COMPRA, "3", "Factura A", LocalDate.of(2026, 10, 31), "400", "84");
        guardar(Operacion.VENTA, "4", "Factura A", LocalDate.of(2026, 11, 1), "5000", "1050"); // otro mes
    }

    private void guardar(Operacion op, String numero, String tipo, LocalDate fecha, String neto, String iva) {
        Comprobante c = new Comprobante();
        c.operacion = op;
        c.fecha = fecha;
        c.tipo = tipo;
        c.numero = numero;
        c.cuitContraparte = "30-65478912-3";
        c.razonSocial = "Contraparte";
        c.neto21 = new BigDecimal(neto);
        c.iva21 = new BigDecimal(iva);
        comprobantes.guardarComprobante(CUIT, c);
    }

    private void movimiento(Clase clase, Tributo tributo, String importe) {
        PercepcionRetencion p = PercepcionServiceImplTest.percepcion(LocalDate.of(2026, 10, 5), importe);
        p.clase = clase;
        p.tributo = tributo;
        p.comprobante = clase + "-" + tributo + "-" + importe;
        percepciones.guardarPercepcion(CUIT, p);
    }

    private static void igual(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperaba " + esperado + " y dio " + real);
    }

    @Test
    void posicionDeIva() {
        movimiento(Clase.PERCEPCION, Tributo.IVA, "5");
        movimiento(Clase.RETENCION, Tributo.IVA, "3");
        movimiento(Clase.PERCEPCION, Tributo.GANANCIAS, "1000"); // no es de IVA: no cuenta

        PosicionIva iva = impuestos.determinarIva(CUIT, OCTUBRE);
        assertEquals(OCTUBRE, iva.periodo);
        assertEquals(2, iva.cantidadVentas);
        assertEquals(1, iva.cantidadCompras);
        igual("900", iva.netoVentas);
        igual("189", iva.debitoFiscal);   // 210 - 21 de la nota de credito
        igual("84", iva.creditoFiscal);
        igual("105", iva.saldoTecnico());
        igual("5", iva.percepcionesSufridas);
        igual("3", iva.retencionesSufridas);
        igual("97", iva.saldoFinal());
        assertTrue(iva.aPagar());
    }

    @Test
    void ivaSinMovimientosOSinCliente() {
        PosicionIva vacio = impuestos.determinarIva(CUIT, YearMonth.of(2020, 1));
        assertEquals(0, vacio.cantidadVentas);
        igual("0", vacio.saldoFinal());
        assertEquals(0, impuestos.determinarIva(null, OCTUBRE).cantidadVentas);
    }

    @Test
    void gananciasProvisorio() {
        movimiento(Clase.RETENCION, Tributo.GANANCIAS, "50");
        List<ResumenTributo> filas = impuestos.determinar(CUIT, OCTUBRE, Tributo.GANANCIAS);
        assertEquals(1, filas.size());
        ResumenTributo g = filas.get(0);
        igual("500", g.baseImponible);    // 900 - 400
        igual("175", g.determinado);      // 35%
        igual("50", g.ingresosACuenta);
        igual("125", g.aPagar());
    }

    @Test
    void gananciasConPerdidaNoDaImpuestoNegativo() {
        guardar(Operacion.COMPRA, "9", "Factura A", LocalDate.of(2026, 10, 15), "10000", "2100");
        ResumenTributo g = impuestos.determinar(CUIT, OCTUBRE, Tributo.GANANCIAS).get(0);
        igual("0", g.baseImponible);
        igual("0", g.determinado);
    }

    @Test
    void sussSoloSiTieneEmpleados() {
        List<ResumenTributo> filas = impuestos.determinar(CUIT, OCTUBRE, Tributo.SUSS);
        assertEquals(2, filas.size());
        igual("108", filas.get(0).baseImponible);   // 12% de 900
        igual("25.92", filas.get(0).determinado);   // 24%
        igual("18.36", filas.get(1).determinado);   // 17%

        Cliente c = clientes.buscarPorCuit(CUIT);
        c.setTieneEmpleados(false);
        clientes.modificar(c);
        assertTrue(impuestos.determinar(CUIT, OCTUBRE, Tributo.SUSS).isEmpty());
        assertTrue(impuestos.determinar(CUIT, OCTUBRE, Tributo.IIBB).isEmpty());
    }

    @Test
    void reportePorMes() {
        List<TotalPeriodo> t = reportes.totalesPorPeriodo(CUIT, YearMonth.of(2026, 9), YearMonth.of(2026, 11));
        assertEquals(3, t.size(), "una fila por mes, aunque este vacio");
        assertEquals(YearMonth.of(2026, 9), t.get(0).periodo);
        assertEquals(0, t.get(0).cantidadVentas);

        TotalPeriodo oct = t.get(1);
        assertEquals(2, oct.cantidadVentas);
        assertEquals(1, oct.cantidadCompras);
        igual("900", oct.netoVentas);
        igual("189", oct.ivaVentas);
        igual("400", oct.netoCompras);
        igual("84", oct.ivaCompras);
        igual("500", oct.resultado());

        igual("5000", t.get(2).netoVentas);
    }

    @Test
    void reporteCasosBorde() {
        assertTrue(reportes.totalesPorPeriodo(CUIT, OCTUBRE, OCTUBRE.minusMonths(1)).isEmpty(), "rango invertido");
        List<TotalPeriodo> sinCliente = reportes.totalesPorPeriodo(null, OCTUBRE, OCTUBRE);
        assertEquals(1, sinCliente.size());
        assertEquals(0, sinCliente.get(0).cantidadVentas);
    }
}
