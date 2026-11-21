package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import soporte.ConBaseTemporal;
import view.Modelos.Clase;
import view.Modelos.PercepcionRetencion;
import view.Modelos.Tributo;
import view.Servicios.ServicioException;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PercepcionServiceImplTest extends ConBaseTemporal {

    private static final String CUIT = "30-71234567-9";
    private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

    private PercepcionServiceImpl servicio;

    @BeforeEach
    void preparar() {
        new ClienteServiceImpl().alta(ClienteServiceImplTest.clienteCompleto());
        servicio = new PercepcionServiceImpl();
    }

    static PercepcionRetencion percepcion(LocalDate fecha, String importe) {
        PercepcionRetencion p = new PercepcionRetencion();
        p.clase = Clase.PERCEPCION;
        p.tributo = Tributo.IVA;
        p.fecha = fecha;
        p.regimen = "RG 2408";
        p.jurisdiccion = "Nacional";
        p.comprobante = "FA 0003-00012345";
        p.cuitAgente = "30500011227";
        p.razonSocialAgente = "Papelera del Centro S.A.";
        p.importe = new BigDecimal(importe);
        return p;
    }

    @Test
    void guardarYLeerConservaTodo() {
        PercepcionRetencion p = percepcion(LocalDate.of(2026, 10, 5), "1234.56");
        servicio.guardarPercepcion(CUIT, p);
        assertNotNull(p.id);

        List<PercepcionRetencion> lista = servicio.listarPercepciones(CUIT, OCTUBRE, null, null);
        assertEquals(1, lista.size());
        PercepcionRetencion l = lista.get(0);
        assertEquals(Clase.PERCEPCION, l.clase);
        assertEquals(Tributo.IVA, l.tributo);
        assertEquals(LocalDate.of(2026, 10, 5), l.fecha);
        assertEquals("RG 2408", l.regimen);
        assertEquals("Nacional", l.jurisdiccion);
        assertEquals("FA 0003-00012345", l.comprobante);
        assertEquals("30-50001122-7", l.cuitAgente, "CUIT normalizado");
        assertEquals("Papelera del Centro S.A.", l.razonSocialAgente);
        assertEquals(0, new BigDecimal("1234.56").compareTo(l.importe));
        assertEquals("Manual", l.origen);
    }

    @Test
    void sobreviveAlCerrarLaApp() {
        servicio.guardarPercepcion(CUIT, percepcion(LocalDate.of(2026, 10, 5), "10"));
        config.Conexion.cerrar();
        assertEquals(1, new PercepcionServiceImpl().listarPercepciones(CUIT, null, null, null).size());
    }

    @Test
    void filtraPorPeriodoClaseYTributo() {
        PercepcionRetencion ret = percepcion(LocalDate.of(2026, 10, 31), "20");
        ret.clase = Clase.RETENCION;
        ret.tributo = Tributo.GANANCIAS;
        servicio.guardarPercepcion(CUIT, percepcion(LocalDate.of(2026, 10, 1), "10"));
        servicio.guardarPercepcion(CUIT, ret);
        servicio.guardarPercepcion(CUIT, percepcion(LocalDate.of(2026, 11, 1), "30"));

        assertEquals(2, servicio.listarPercepciones(CUIT, OCTUBRE, null, null).size());
        assertEquals(1, servicio.listarPercepciones(CUIT, OCTUBRE, Clase.RETENCION, null).size());
        assertEquals(1, servicio.listarPercepciones(CUIT, OCTUBRE, null, Tributo.IVA).size());
        assertEquals(0, servicio.listarPercepciones(CUIT, OCTUBRE, Clase.RETENCION, Tributo.IVA).size());
        assertEquals(3, servicio.listarPercepciones(CUIT, null, null, null).size());
        assertTrue(servicio.listarPercepciones(null, OCTUBRE, null, null).isEmpty(), "sin cliente activo");
    }

    @Test
    void modificarYEliminar() {
        PercepcionRetencion p = percepcion(LocalDate.of(2026, 10, 5), "10");
        servicio.guardarPercepcion(CUIT, p);
        p.importe = new BigDecimal("99");
        servicio.guardarPercepcion(CUIT, p);
        List<PercepcionRetencion> lista = servicio.listarPercepciones(CUIT, null, null, null);
        assertEquals(1, lista.size());
        assertEquals(0, new BigDecimal("99").compareTo(lista.get(0).importe));

        servicio.eliminarPercepcion(CUIT, p.id);
        assertTrue(servicio.listarPercepciones(CUIT, null, null, null).isEmpty());
        assertDoesNotThrow(() -> servicio.eliminarPercepcion(CUIT, p.id));
    }

    @Test
    void validaLosDatos() {
        PercepcionRetencion cero = percepcion(LocalDate.of(2026, 10, 5), "0");
        assertThrows(ServicioException.class, () -> servicio.guardarPercepcion(CUIT, cero));
        PercepcionRetencion cuitMalo = percepcion(LocalDate.of(2026, 10, 5), "1");
        cuitMalo.cuitAgente = "123";
        assertThrows(ServicioException.class, () -> servicio.guardarPercepcion(CUIT, cuitMalo));
        assertThrows(ServicioException.class,
                () -> servicio.guardarPercepcion("20-99999999-9", percepcion(LocalDate.of(2026, 10, 5), "1")));
    }

    @Test
    void importaCsvYNoDuplicaAlReimportar() throws Exception {
        Path csv = carpeta.resolve("percepciones.csv");
        Files.writeString(csv, String.join("\n",
                "fecha;clase;tributo;regimen;jurisdiccion;comprobante;cuitAgente;razonSocialAgente;importe",
                "05/10/2026;Percepción;IVA;RG 2408;Nacional;FA 0003-1;30-50001122-7;Papelera;1.500,00",
                "2026-10-07;R;Ganancias;RG 830;Nacional;OP 55;30719988774;Combustibles;200",
                "2026-10-08;Retencion;Ingresos Brutos;Conv. Multilateral;San Luis;;30-50001122-7;Papelera;35,5"),
                StandardCharsets.UTF_8);

        List<PercepcionRetencion> previa = servicio.previsualizarPercepciones(csv.toFile());
        assertEquals(3, previa.size());
        assertEquals(Clase.PERCEPCION, previa.get(0).clase, "acepta acentos");
        assertEquals(Clase.RETENCION, previa.get(1).clase, "acepta la abreviatura R");
        assertEquals(Tributo.IIBB, previa.get(2).tributo, "acepta 'Ingresos Brutos'");
        assertEquals(0, new BigDecimal("1500.00").compareTo(previa.get(0).importe));
        assertTrue(servicio.listarPercepciones(CUIT, null, null, null).isEmpty(), "previsualizar no guarda");

        assertEquals(3, servicio.importarPercepciones(CUIT, previa));
        assertEquals(0, servicio.importarPercepciones(CUIT, servicio.previsualizarPercepciones(csv.toFile())),
                "reimportar el mismo archivo no duplica");
        assertEquals("CSV", servicio.listarPercepciones(CUIT, null, null, null).get(0).origen);
    }

    @Test
    void csvConErroresIndicaLaLinea() throws Exception {
        Path csv = carpeta.resolve("malo.csv");
        Files.writeString(csv, "2026-10-05;Percepcion;Monotributo;;;;30-50001122-7;X;10\n");
        ServicioException ex = assertThrows(ServicioException.class,
                () -> servicio.previsualizarPercepciones(csv.toFile()));
        assertTrue(ex.getMessage().startsWith("Linea 1") && ex.getMessage().contains("tributo"), ex.getMessage());
    }

    @Test
    void excelYPdfDanUnMensajeClaro() throws Exception {
        for (String nombre : new String[]{"a.xlsx", "a.pdf"}) {
            File f = Files.writeString(carpeta.resolve(nombre), "x").toFile();
            assertThrows(ServicioException.class, () -> servicio.previsualizarPercepciones(f));
        }
    }
}
