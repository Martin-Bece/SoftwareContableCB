package service;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import soporte.ConBaseTemporal;
import view.Modelos.Clase;
import view.Modelos.Comprobante;
import view.Modelos.Operacion;
import view.Modelos.PercepcionRetencion;
import view.Modelos.Tributo;
import view.Servicios.ServicioException;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Importacion desde Excel (.xlsx y .xls) y columnas por nombre (tambien en CSV). */
class ImportacionExcelTest extends ConBaseTemporal {

    private static final String CUIT = "30-71234567-9";

    private ComprobanteServiceImpl comprobantes;
    private PercepcionServiceImpl percepciones;

    @BeforeEach
    void preparar() {
        new ClienteServiceImpl().alta(ClienteServiceImplTest.clienteCompleto());
        comprobantes = new ComprobanteServiceImpl();
        percepciones = new PercepcionServiceImpl();
    }

    /** Un Excel "realista": titulo arriba, columnas en otro orden y con acentos, fechas, formulas. */
    private Path excelComprobantes() throws Exception {
        Path archivo = carpeta.resolve("ventas.xlsx");
        try (Workbook libro = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(archivo)) {
            Sheet hoja = libro.createSheet("Ventas");
            CellStyle fecha = libro.createCellStyle();
            fecha.setDataFormat(libro.createDataFormat().getFormat("dd/mm/yyyy"));
            CellStyle cuatroDigitos = libro.createCellStyle();
            cuatroDigitos.setDataFormat(libro.createDataFormat().getFormat("0000"));

            hoja.createRow(0).createCell(0).setCellValue("Mis Comprobantes Emitidos - Octubre 2026"); // titulo
            // fila 2 vacia
            Row enc = hoja.createRow(2);
            String[] titulos = {"Fecha de Emisión", "Tipo", "Punto de Venta", "Número", "CUIT",
                    "Denominación", "Neto Gravado", "IVA", "Imp. Total"};
            for (int i = 0; i < titulos.length; i++) enc.createCell(i).setCellValue(titulos[i]);

            Row r1 = hoja.createRow(3);                          // fila 4 de Excel
            Cell f1 = r1.createCell(0);
            f1.setCellValue(LocalDate.of(2026, 10, 1));
            f1.setCellStyle(fecha);
            r1.createCell(1).setCellValue("Factura A");
            Cell pv = r1.createCell(2);
            pv.setCellValue(1);                                  // numero 1 con formato 0000 -> "0001"
            pv.setCellStyle(cuatroDigitos);
            r1.createCell(3).setCellValue(123);
            r1.createCell(4).setCellValue(30654789123d);         // CUIT como numero
            r1.createCell(5).setCellValue("Supermercados Andinos S.A.");
            r1.createCell(6).setCellValue(1234.56);
            r1.createCell(7).setCellFormula("G4*0.21");          // 259.2576 -> 259.26
            r1.createCell(8).setCellFormula("G4+H4");

            Row r2 = hoja.createRow(4);                          // fila 5: todo como texto
            r2.createCell(0).setCellValue("05/10/2026");
            r2.createCell(1).setCellValue("Factura B");
            r2.createCell(2).setCellValue("0002");
            r2.createCell(3).setCellValue("00000456");
            r2.createCell(4).setCellValue("30-71122334-5");
            r2.createCell(5).setCellValue("Almacén Don Pedro S.R.L.");
            r2.createCell(6).setCellValue("1.000,50");
            r2.createCell(7).setCellValue(210.11);

            libro.write(out);
        }
        return archivo;
    }

    @Test
    void importaComprobantesDesdeXlsx() throws Exception {
        List<Comprobante> previa = comprobantes.previsualizarComprobantes(excelComprobantes().toFile(), Operacion.VENTA);
        assertEquals(2, previa.size(), "saltea el titulo, la fila vacia y el encabezado");

        Comprobante a = previa.get(0);
        assertEquals(LocalDate.of(2026, 10, 1), a.fecha, "fecha con formato de Excel");
        assertEquals("Factura A", a.tipo);
        assertEquals("0001", a.puntoVenta, "respeta el formato 0000 de la celda");
        assertEquals("123", a.numero);
        assertEquals("30-65478912-3", a.cuitContraparte, "CUIT guardado como numero en Excel");
        assertEquals("Supermercados Andinos S.A.", a.razonSocial);
        assertEquals(0, new BigDecimal("1234.56").compareTo(a.neto21));
        assertEquals(0, new BigDecimal("259.26").compareTo(a.iva21), "formula calculada y redondeada a centavos");
        assertEquals("Excel", a.origen);

        Comprobante b = previa.get(1);
        assertEquals(LocalDate.of(2026, 10, 5), b.fecha, "fecha escrita como texto");
        assertEquals("0002", b.puntoVenta);
        assertEquals("30-71122334-5", b.cuitContraparte);
        assertEquals("Almacén Don Pedro S.R.L.", b.razonSocial);
        assertEquals(0, new BigDecimal("1000.50").compareTo(b.neto21), "importe como texto con coma");
        assertEquals(0, new BigDecimal("210.11").compareTo(b.iva21));

        assertEquals(2, comprobantes.importarComprobantes(CUIT, previa));
        assertEquals(0, comprobantes.importarComprobantes(CUIT,
                comprobantes.previsualizarComprobantes(excelComprobantes().toFile(), Operacion.VENTA)),
                "reimportar el mismo Excel no duplica");
    }

    @Test
    void importaXlsViejoSinEncabezado() throws Exception {
        Path archivo = carpeta.resolve("compras.xls");
        try (Workbook libro = new HSSFWorkbook(); OutputStream out = Files.newOutputStream(archivo)) {
            Row r = libro.createSheet().createRow(0);
            Object[] valores = {"2026-10-01", "Factura A", "0003", "77", "30-50001122-7", "Papelera", 100d, 21d};
            for (int i = 0; i < valores.length; i++) {
                if (valores[i] instanceof Double d) r.createCell(i).setCellValue(d);
                else r.createCell(i).setCellValue((String) valores[i]);
            }
            libro.write(out);
        }
        List<Comprobante> previa = comprobantes.previsualizarComprobantes(archivo.toFile(), Operacion.COMPRA);
        assertEquals(1, previa.size());
        assertEquals(Operacion.COMPRA, previa.get(0).operacion);
        assertEquals("0003", previa.get(0).puntoVenta, "sin encabezado: columnas en el orden de siempre");
        assertEquals(0, new BigDecimal("21").compareTo(previa.get(0).iva21));
    }

    @Test
    void errorEnUnaFilaIndicaLaFilaDeExcel() throws Exception {
        Path archivo = carpeta.resolve("malo.xlsx");
        try (Workbook libro = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(archivo)) {
            Sheet hoja = libro.createSheet();
            String[] enc = {"fecha", "tipo", "numero", "cuit", "razon social", "neto", "iva"};
            Row e = hoja.createRow(0);
            for (int i = 0; i < enc.length; i++) e.createCell(i).setCellValue(enc[i]);
            String[] ok = {"2026-10-01", "Factura A", "1", "30-50001122-7", "X", "100", "21"};
            String[] mal = {"2026-10-02", "Factura A", "2", "123", "Y", "100", "21"};
            Row r1 = hoja.createRow(1);
            Row r2 = hoja.createRow(3); // fila 4 de Excel (la 3 queda vacia)
            for (int i = 0; i < ok.length; i++) {
                r1.createCell(i).setCellValue(ok[i]);
                r2.createCell(i).setCellValue(mal[i]);
            }
            libro.write(out);
        }
        ServicioException ex = assertThrows(ServicioException.class,
                () -> comprobantes.previsualizarComprobantes(archivo.toFile(), Operacion.VENTA));
        assertTrue(ex.getMessage().startsWith("Fila 4: el cuit '123'"), ex.getMessage());
    }

    @Test
    void avisaQueColumnasFaltan() throws Exception {
        Path csv = carpeta.resolve("incompleto.csv");
        Files.writeString(csv, "fecha;tipo;numero;cuit;razon social;neto\n2026-10-01;Factura A;1;30-50001122-7;X;100\n");
        ServicioException ex = assertThrows(ServicioException.class,
                () -> comprobantes.previsualizarComprobantes(csv.toFile(), Operacion.VENTA));
        assertTrue(ex.getMessage().contains("no tiene las columnas: iva"), ex.getMessage());
    }

    @Test
    void csvConEncabezadoEnOtroOrden() throws Exception {
        Path csv = carpeta.resolve("desordenado.csv");
        Files.writeString(csv, String.join("\n",
                "IVA;Neto Gravado;Razón Social;CUIT;Número;Tipo de Comprobante;Fecha;Columna Que Sobra",
                "21;100;Papelera;30-50001122-7;9;Factura C;2026-10-03;lo que sea"), StandardCharsets.UTF_8);
        Comprobante c = comprobantes.previsualizarComprobantes(csv.toFile(), Operacion.COMPRA).get(0);
        assertEquals("Factura C", c.tipo);
        assertEquals("9", c.numero);
        assertEquals("", c.puntoVenta, "punto de venta opcional (al guardar queda 0001)");
        assertEquals(0, new BigDecimal("100").compareTo(c.neto21));
        assertEquals("CSV", c.origen);
    }

    @Test
    void excelDanadoDaUnMensajeClaro() throws Exception {
        Path archivo = Files.writeString(carpeta.resolve("roto.xlsx"), "esto no es un excel");
        ServicioException ex = assertThrows(ServicioException.class,
                () -> comprobantes.previsualizarComprobantes(archivo.toFile(), Operacion.VENTA));
        assertTrue(ex.getMessage().startsWith("No se pudo leer el Excel"), ex.getMessage());
    }

    @Test
    void importaPercepcionesDesdeXlsx() throws Exception {
        Path archivo = carpeta.resolve("percepciones.xlsx");
        try (Workbook libro = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(archivo)) {
            Sheet hoja = libro.createSheet();
            CellStyle fecha = libro.createCellStyle();
            fecha.setDataFormat(libro.createDataFormat().getFormat("dd/mm/yyyy"));
            String[] enc = {"Fecha", "Impuesto", "Clase", "CUIT Agente", "Agente", "Régimen", "Importe"};
            Row e = hoja.createRow(0);
            for (int i = 0; i < enc.length; i++) e.createCell(i).setCellValue(enc[i]);
            Row r = hoja.createRow(1);
            Cell f = r.createCell(0);
            f.setCellValue(LocalDate.of(2026, 10, 9));
            f.setCellStyle(fecha);
            r.createCell(1).setCellValue("Ingresos Brutos");
            r.createCell(2).setCellValue("Retención");
            r.createCell(3).setCellValue(30500011227d);
            r.createCell(4).setCellValue("Papelera del Centro S.A.");
            r.createCell(5).setCellValue("Conv. Multilateral");
            r.createCell(6).setCellValue(35.5);
            libro.write(out);
        }
        List<PercepcionRetencion> previa = percepciones.previsualizarPercepciones(archivo.toFile());
        assertEquals(1, previa.size());
        PercepcionRetencion p = previa.get(0);
        assertEquals(LocalDate.of(2026, 10, 9), p.fecha);
        assertEquals(Clase.RETENCION, p.clase);
        assertEquals(Tributo.IIBB, p.tributo);
        assertEquals("30-50001122-7", p.cuitAgente);
        assertEquals("Papelera del Centro S.A.", p.razonSocialAgente);
        assertEquals("Conv. Multilateral", p.regimen);
        assertEquals("", p.jurisdiccion, "columna opcional que no esta");
        assertEquals(0, new BigDecimal("35.5").compareTo(p.importe));
        assertEquals("Excel", p.origen);
        assertEquals(1, percepciones.importarPercepciones(CUIT, previa));
    }
}
