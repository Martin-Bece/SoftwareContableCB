package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import soporte.ConBaseTemporal;
import view.Modelos.Actividad;
import view.Modelos.Cliente;
import view.Modelos.Comprobante;
import view.Modelos.Operacion;
import view.Servicios.ServicioException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClienteServiceImplTest extends ConBaseTemporal {

    private ClienteServiceImpl servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ClienteServiceImpl();
    }

    static Cliente clienteCompleto() {
        Cliente c = new Cliente("30-71234567-9", "Distribuidora del Cuyo S.R.L.", "DisCuyo",
                "Av. Illia 1240, San Luis", true);
        c.getActividades().add(new Actividad("463199", "Venta al por mayor de productos alimenticios"));
        c.getActividades().add(new Actividad("492280", "Servicio de transporte automotor de cargas"));
        c.setImpuestosDesdeTexto("IVA, Ganancias, SUSS");
        return c;
    }

    @Test
    void altaYBusquedaConservanTodosLosDatos() {
        servicio.alta(clienteCompleto());

        Cliente leido = servicio.buscarPorCuit("30-71234567-9");
        assertNotNull(leido);
        assertEquals("Distribuidora del Cuyo S.R.L.", leido.getRazonSocial());
        assertEquals("DisCuyo", leido.getNombreFantasia());
        assertEquals("Av. Illia 1240, San Luis", leido.getDireccion());
        assertTrue(leido.isTieneEmpleados());
        assertEquals(Set.of("IVA", "Ganancias", "SUSS"), leido.getImpuestos());
        assertEquals("IVA, Ganancias, SUSS", leido.getImpuestosComoTexto());
        assertEquals(2, leido.getActividades().size());
        assertEquals("463199", leido.getActividades().get(0).codigo, "respeta el orden de carga");
        assertEquals("Servicio de transporte automotor de cargas", leido.getActividades().get(1).nombre);
    }

    @Test
    void normalizaElCuitSinGuiones() {
        Cliente c = new Cliente("20123456786", "Persona", "", "", false);
        servicio.alta(c);
        assertNotNull(servicio.buscarPorCuit("20-12345678-6"));
        assertNotNull(servicio.buscarPorCuit("20123456786"), "se puede buscar con o sin guiones");
    }

    @Test
    void noPermiteCuitDuplicado() {
        servicio.alta(clienteCompleto());
        ServicioException ex = assertThrows(ServicioException.class, () -> servicio.alta(clienteCompleto()));
        assertTrue(ex.getMessage().contains("Ya existe"), ex.getMessage());
    }

    @Test
    void validaCuitYRazonSocial() {
        assertThrows(ServicioException.class,
                () -> servicio.alta(new Cliente("123", "Algo", "", "", false)));
        assertThrows(ServicioException.class,
                () -> servicio.alta(new Cliente("20-11111111-1", "   ", "", "", false)));
        assertTrue(servicio.listar(null).isEmpty());
    }

    @Test
    void listarOrdenaYFiltra() {
        servicio.alta(new Cliente("30-00000000-1", "Zeta SA", "", "", false));
        servicio.alta(new Cliente("30-00000000-2", "alfa srl", "Fantasia Rara", "", false));
        servicio.alta(new Cliente("30-99999999-3", "Beta SA", "", "", false));

        List<String> nombres = servicio.listar(null).stream().map(Cliente::getRazonSocial).toList();
        assertEquals(List.of("alfa srl", "Beta SA", "Zeta SA"), nombres, "orden alfabetico sin importar mayusculas");

        assertEquals(1, servicio.listar("ZETA").size(), "filtro por razon social sin importar mayusculas");
        assertEquals(1, servicio.listar("rara").size(), "filtro por nombre de fantasia");
        assertEquals(1, servicio.listar("99999").size(), "filtro por CUIT");
        assertEquals(3, servicio.listar("  ").size(), "filtro en blanco = todos");
        assertEquals(0, servicio.listar("%").size(), "los comodines de SQL se buscan literalmente");
    }

    @Test
    void modificarReemplazaDatosYActividades() {
        servicio.alta(clienteCompleto());
        Cliente c = servicio.buscarPorCuit("30-71234567-9");
        c.setRazonSocial("Nueva Razon");
        c.getActividades().clear();
        c.getActividades().add(new Actividad("111", "Unica"));
        c.setImpuestosDesdeTexto("IVA");
        c.setTieneEmpleados(false);
        servicio.modificar(c);

        Cliente leido = servicio.buscarPorCuit("30-71234567-9");
        assertEquals("Nueva Razon", leido.getRazonSocial());
        assertEquals(1, leido.getActividades().size());
        assertEquals("Unica", leido.getActividades().get(0).nombre);
        assertEquals(Set.of("IVA"), leido.getImpuestos());
        assertFalse(leido.isTieneEmpleados());
    }

    @Test
    void modificarUnClienteInexistenteFalla() {
        assertThrows(ServicioException.class, () -> servicio.modificar(clienteCompleto()));
    }

    @Test
    void bajaBorraElClienteYSusActividades() {
        servicio.alta(clienteCompleto());
        servicio.baja("30-71234567-9");
        assertNull(servicio.buscarPorCuit("30-71234567-9"));
        // si se vuelve a dar de alta no arrastra actividades viejas
        Cliente c = new Cliente("30-71234567-9", "Otra", "", "", false);
        servicio.alta(c);
        assertTrue(servicio.buscarPorCuit("30-71234567-9").getActividades().isEmpty());
    }

    @Test
    void noPermiteBajaSiTieneComprobantes() {
        servicio.alta(clienteCompleto());
        Comprobante cb = new Comprobante();
        cb.operacion = Operacion.VENTA;
        cb.fecha = LocalDate.of(2026, 10, 5);
        cb.numero = "1";
        cb.cuitContraparte = "30-65478912-3";
        cb.razonSocial = "Supermercados Andinos S.A.";
        cb.neto21 = new BigDecimal("100");
        cb.iva21 = new BigDecimal("21");
        new ComprobanteServiceImpl().guardarComprobante("30-71234567-9", cb);

        ServicioException ex = assertThrows(ServicioException.class, () -> servicio.baja("30-71234567-9"));
        assertTrue(ex.getMessage().contains("1 comprobantes"), ex.getMessage());
        assertNotNull(servicio.buscarPorCuit("30-71234567-9"));
    }

    @Test
    void noPermiteBajaSiTienePercepciones() {
        servicio.alta(clienteCompleto());
        new PercepcionServiceImpl().guardarPercepcion("30-71234567-9",
                PercepcionServiceImplTest.percepcion(LocalDate.of(2026, 10, 5), "100"));
        ServicioException ex = assertThrows(ServicioException.class, () -> servicio.baja("30-71234567-9"));
        assertTrue(ex.getMessage().contains("percepciones"), ex.getMessage());
    }

    @Test
    void laBajaBorraSuPadronDeContrapartes() {
        servicio.alta(clienteCompleto());
        ContraparteServiceImpl padron = new ContraparteServiceImpl();
        padron.guardarProveedor("30-71234567-9", new view.Modelos.Contraparte("30-50001122-7", "Papelera"), null);
        servicio.baja("30-71234567-9");
        servicio.alta(clienteCompleto());
        assertTrue(padron.listarProveedores("30-71234567-9", null).isEmpty());
    }
}
