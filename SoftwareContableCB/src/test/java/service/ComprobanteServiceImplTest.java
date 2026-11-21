package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import soporte.ConBaseTemporal;
import view.Modelos.Comprobante;
import view.Modelos.Operacion;
import view.Servicios.ServicioException;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComprobanteServiceImplTest extends ConBaseTemporal {

    private static final String CUIT = "30-71234567-9";

    private ComprobanteServiceImpl servicio;

    @BeforeEach
    void preparar() {
        new ClienteServiceImpl().alta(ClienteServiceImplTest.clienteCompleto());
        servicio = new ComprobanteServiceImpl();
    }

    static Comprobante factura(Operacion op, LocalDate fecha, String numero, String neto) {
        Comprobante c = new Comprobante();
        c.operacion = op;
        c.fecha = fecha;
        c.tipo = "Factura A";
        c.puntoVenta = "0001";
        c.numero = numero;
        c.cuitContraparte = "30-65478912-3";
        c.razonSocial = "Supermercados Andinos S.A.";
        c.neto21 = new BigDecimal(neto);
        c.iva21 = new BigDecimal(neto).multiply(new BigDecimal("0.21"));
        return c;
    }

    @Test
    void guardarAsignaIdYConservaTodosLosImportesExactos() {
        Comprobante c = factura(Operacion.VENTA, LocalDate.of(2026, 10, 15), "00000123", "1000.10");
        c.neto105 = new BigDecimal("0.10");
        c.iva105 = new BigDecimal("0.01");
        c.neto27 = new BigDecimal("33.33");
        c.iva27 = new BigDecimal("9.00");
        c.noGravado = new BigDecimal("1.11");
        c.exento = new BigDecimal("2.22");
        c.percepciones = new BigDecimal("3.33");
        c.otrosTributos = new BigDecimal("4.44");
        c.origen = "Manual";
        servicio.guardarComprobante(CUIT, c);
        assertNotNull(c.id);

        List<Comprobante> lista = servicio.listarComprobantes(CUIT, Operacion.VENTA, YearMonth.of(2026, 10), null);
        assertEquals(1, lista.size());
        Comprobante l = lista.get(0);
        assertEquals(c.id, l.id);
        assertEquals(LocalDate.of(2026, 10, 15), l.fecha);
        assertEquals("Factura A 0001-00000123", l.comprobanteCompleto());
        // 0.1 + 0.2 en double daria 0.30000000000000004; con BigDecimal en TEXT queda exacto
        assertEquals(0, new BigDecimal("1000.10").compareTo(l.neto21));
        assertEquals(0, new BigDecimal("210.021").compareTo(l.iva21));
        assertEquals(0, new BigDecimal("0.10").compareTo(l.neto105));
        assertEquals(0, new BigDecimal("9.00").compareTo(l.iva27));
        assertEquals(0, new BigDecimal("4.44").compareTo(l.otrosTributos));
        assertEquals(c.total(), l.total());
    }

    @Test
    void listarFiltraPorOperacionPeriodoYTexto() {
        servicio.guardarComprobante(CUIT, factura(Operacion.VENTA, LocalDate.of(2026, 9, 30), "1", "100"));
        servicio.guardarComprobante(CUIT, factura(Operacion.VENTA, LocalDate.of(2026, 10, 1), "2", "100"));
        servicio.guardarComprobante(CUIT, factura(Operacion.VENTA, LocalDate.of(2026, 10, 31), "3", "100"));
        servicio.guardarComprobante(CUIT, factura(Operacion.VENTA, LocalDate.of(2026, 11, 1), "4", "100"));
        Comprobante compra = factura(Operacion.COMPRA, LocalDate.of(2026, 10, 10), "9", "50");
        compra.cuitContraparte = "30-50001122-7";
        compra.razonSocial = "Papelera del Centro S.A.";
        servicio.guardarComprobante(CUIT, compra);

        YearMonth octubre = YearMonth.of(2026, 10);
        List<Comprobante> ventasOct = servicio.listarComprobantes(CUIT, Operacion.VENTA, octubre, null);
        assertEquals(List.of("2", "3"), ventasOct.stream().map(c -> c.numero).toList(),
                "incluye el primer y el ultimo dia del mes, ordenado por fecha");

        assertEquals(1, servicio.listarComprobantes(CUIT, Operacion.COMPRA, octubre, null).size());
        assertEquals(3, servicio.listarComprobantes(CUIT, null, octubre, null).size());
        assertEquals(5, servicio.listarComprobantes(CUIT, null, null, null).size());

        assertEquals(1, servicio.listarComprobantes(CUIT, null, null, "papelera").size(), "por razon social");
        assertEquals(1, servicio.listarComprobantes(CUIT, null, null, "50001122").size(), "por CUIT");
        assertEquals(1, servicio.listarComprobantes(CUIT, null, null, "0001-4").size(), "por numero completo");
        assertTrue(servicio.listarComprobantes(null, null, null, null).isEmpty(), "sin cliente activo");
    }

    @Test
    void modificarYEliminar() {
        Comprobante c = factura(Operacion.VENTA, LocalDate.of(2026, 10, 15), "1", "100");
        servicio.guardarComprobante(CUIT, c);
        Long id = c.id;

        c.neto21 = new BigDecimal("200");
        c.tipo = "Nota de credito A";
        servicio.guardarComprobante(CUIT, c);
        assertEquals(id, c.id, "modificar no crea uno nuevo");
        Comprobante leido = servicio.listarComprobantes(CUIT, null, null, null).get(0);
        assertEquals(0, new BigDecimal("200").compareTo(leido.neto21));
        assertTrue(leido.esNotaCredito());

        servicio.eliminarComprobante(CUIT, id);
        assertTrue(servicio.listarComprobantes(CUIT, null, null, null).isEmpty());
        assertDoesNotThrow(() -> servicio.eliminarComprobante(CUIT, id), "eliminar dos veces no explota");
    }

    @Test
    void rechazaComprobantesDuplicados() {
        servicio.guardarComprobante(CUIT, factura(Operacion.VENTA, LocalDate.of(2026, 10, 15), "7", "100"));
        ServicioException ex = assertThrows(ServicioException.class, () ->
                servicio.guardarComprobante(CUIT, factura(Operacion.VENTA, LocalDate.of(2026, 10, 20), "7", "999")));
        assertTrue(ex.getMessage().contains("Ya esta cargado"), ex.getMessage());

        // el mismo numero como COMPRA (u otra contraparte) no es duplicado
        servicio.guardarComprobante(CUIT, factura(Operacion.COMPRA, LocalDate.of(2026, 10, 15), "7", "100"));
    }

    @Test
    void validaLosDatos() {
        Comprobante sinNumero = factura(Operacion.VENTA, LocalDate.of(2026, 10, 15), " ", "100");
        assertThrows(ServicioException.class, () -> servicio.guardarComprobante(CUIT, sinNumero));

        Comprobante cuitMalo = factura(Operacion.VENTA, LocalDate.of(2026, 10, 15), "1", "100");
        cuitMalo.cuitContraparte = "123";
        assertThrows(ServicioException.class, () -> servicio.guardarComprobante(CUIT, cuitMalo));

        Comprobante ok = factura(Operacion.VENTA, LocalDate.of(2026, 10, 15), "1", "100");
        assertThrows(ServicioException.class, () -> servicio.guardarComprobante("20-99999999-9", ok),
                "el cliente tiene que existir");
        assertThrows(ServicioException.class, () -> servicio.guardarComprobante(null, ok));
    }

    @Test
    void importaCsvYSalteaDuplicados() throws Exception {
        Path csv = carpeta.resolve("ventas.csv");
        Files.writeString(csv, String.join("\n",
                "﻿fecha;tipo;ptoVenta;numero;cuit;razonSocial;neto;iva;percepciones",
                "2026-10-01;Factura A;0001;00000010;30654789123;Supermercados Andinos S.A.;1.234,56;259,26;10,00",
                "05/10/2026;Factura B;0001;00000011;30-71122334-5;Almacen Don Pedro S.R.L.;$ 1000;210",
                "",
                "2026-10-01;Factura A;0001;00000010;30-65478912-3;Supermercados Andinos S.A.;1.234,56;259,26;10,00"),
                StandardCharsets.UTF_8);

        List<Comprobante> previa = servicio.previsualizarComprobantes(csv.toFile(), Operacion.VENTA);
        assertEquals(3, previa.size(), "la previsualizacion no filtra, solo lee");
        assertEquals(0, new BigDecimal("1234.56").compareTo(previa.get(0).neto21), "coma decimal argentina");
        assertEquals(LocalDate.of(2026, 10, 5), previa.get(1).fecha, "fecha dd/mm/aaaa");
        assertEquals("30-65478912-3", previa.get(0).cuitContraparte, "CUIT normalizado");
        assertTrue(servicio.listarComprobantes(CUIT, null, null, null).isEmpty(), "previsualizar no guarda");

        assertEquals(2, servicio.importarComprobantes(CUIT, previa), "la fila repetida se saltea");
        assertEquals(0, servicio.importarComprobantes(CUIT,
                servicio.previsualizarComprobantes(csv.toFile(), Operacion.VENTA)), "reimportar no duplica");

        List<Comprobante> guardados = servicio.listarComprobantes(CUIT, Operacion.VENTA, YearMonth.of(2026, 10), null);
        assertEquals(2, guardados.size());
        assertEquals("CSV", guardados.get(0).origen);
        assertEquals(0, new BigDecimal("10.00").compareTo(guardados.get(0).percepciones));
    }

    @Test
    void csvGuardadoPorExcelEnWindows1252() throws Exception {
        Path csv = carpeta.resolve("compras.csv");
        Files.writeString(csv, "2026-10-01;Factura A;0002;55;30-50001122-7;Compañía Señor S.A.;100;21\n",
                Charset.forName("windows-1252"));
        List<Comprobante> previa = servicio.previsualizarComprobantes(csv.toFile(), Operacion.COMPRA);
        assertEquals("Compañía Señor S.A.", previa.get(0).razonSocial);
        assertEquals(Operacion.COMPRA, previa.get(0).operacion);
    }

    @Test
    void csvConErroresIndicaLaLinea() throws Exception {
        Path csv = carpeta.resolve("malo.csv");
        Files.writeString(csv, "fecha;tipo;pv;nro;cuit;razon;neto;iva\n"
                + "2026-10-01;Factura A;0001;1;30-65478912-3;Uno;100;21\n"
                + "2026-13-45;Factura A;0001;2;30-65478912-3;Dos;100;21\n");
        ServicioException ex = assertThrows(ServicioException.class,
                () -> servicio.previsualizarComprobantes(csv.toFile(), Operacion.VENTA));
        assertTrue(ex.getMessage().startsWith("Linea 3"), ex.getMessage());

        Files.writeString(csv, "2026-10-01;Factura A;0001;1;30-65478912-3;Uno;cien;21\n");
        ex = assertThrows(ServicioException.class,
                () -> servicio.previsualizarComprobantes(csv.toFile(), Operacion.VENTA));
        assertTrue(ex.getMessage().contains("neto"), ex.getMessage());
    }

    @Test
    void lasContrapartesDeLosComprobantesSeAgreganAlPadron() {
        ContraparteServiceImpl padron = new ContraparteServiceImpl();
        padron.guardarClienteDeCliente(CUIT, new view.Modelos.Contraparte("30-65478912-3", "Nombre cargado a mano"), null);

        servicio.guardarComprobante(CUIT, factura(Operacion.VENTA, LocalDate.of(2026, 10, 1), "1", "100"));
        Comprobante compra = factura(Operacion.COMPRA, LocalDate.of(2026, 10, 1), "2", "100");
        compra.cuitContraparte = "30-50001122-7";
        compra.razonSocial = "Papelera del Centro S.A.";
        servicio.importarComprobantes(CUIT, List.of(compra));

        var clientes = padron.listarClientesDeCliente(CUIT, null);
        assertEquals(1, clientes.size());
        assertEquals("Nombre cargado a mano", clientes.get(0).razonSocial, "no pisa el nombre que ya estaba");
        var proveedores = padron.listarProveedores(CUIT, null);
        assertEquals(1, proveedores.size(), "las compras van al padron de proveedores");
        assertEquals("Papelera del Centro S.A.", proveedores.get(0).razonSocial);
    }

    @Test
    void unDuplicadoNoDejaNadaAMedias() {
        servicio.guardarComprobante(CUIT, factura(Operacion.VENTA, LocalDate.of(2026, 10, 1), "1", "100"));
        Comprobante nuevo = factura(Operacion.VENTA, LocalDate.of(2026, 10, 1), "1", "100");
        assertThrows(ServicioException.class, () -> servicio.guardarComprobante(CUIT, nuevo));
        assertNull(nuevo.id);
        assertEquals(1, servicio.listarComprobantes(CUIT, null, null, null).size());
    }

    @Test
    void importacionInvalidaNoGuardaNada() {
        Comprobante bueno = factura(Operacion.VENTA, LocalDate.of(2026, 10, 1), "1", "100");
        Comprobante malo = factura(Operacion.VENTA, LocalDate.of(2026, 10, 2), "2", "100");
        malo.razonSocial = "";
        assertThrows(ServicioException.class, () -> servicio.importarComprobantes(CUIT, List.of(bueno, malo)));
        assertTrue(servicio.listarComprobantes(CUIT, null, null, null).isEmpty());
    }

    @Test
    void excelYPdfDanUnMensajeClaro() throws Exception {
        for (String nombre : new String[]{"a.xlsx", "a.pdf", "a.doc"}) {
            File f = Files.writeString(carpeta.resolve(nombre), "x").toFile();
            ServicioException ex = assertThrows(ServicioException.class,
                    () -> servicio.previsualizarComprobantes(f, Operacion.VENTA));
            assertFalse(ex.getMessage().isBlank());
        }
    }
}
