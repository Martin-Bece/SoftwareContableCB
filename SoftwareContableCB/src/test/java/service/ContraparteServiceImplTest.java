package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import soporte.ConBaseTemporal;
import view.Modelos.Cliente;
import view.Modelos.Contraparte;
import view.Servicios.ServicioException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContraparteServiceImplTest extends ConBaseTemporal {

    private static final String CLIENTE_A = "30-71234567-9";
    private static final String CLIENTE_B = "27-28456123-4";

    private ContraparteServiceImpl servicio;

    @BeforeEach
    void preparar() {
        ClienteServiceImpl clientes = new ClienteServiceImpl();
        clientes.alta(new Cliente(CLIENTE_A, "Cliente A", "", "", false));
        clientes.alta(new Cliente(CLIENTE_B, "Cliente B", "", "", false));
        servicio = new ContraparteServiceImpl();
    }

    @Test
    void cadaClienteTieneSuPropioPadron() {
        // El mismo proveedor, cargado en los dos clientes.
        servicio.guardarProveedor(CLIENTE_A, new Contraparte("30-50001122-7", "Papelera del Centro S.A."), null);
        servicio.guardarProveedor(CLIENTE_B, new Contraparte("30500011227", "Papelera del Centro S.A."), null);
        servicio.guardarProveedor(CLIENTE_A, new Contraparte("30-71998877-4", "Combustibles Cuyanos"), null);

        assertEquals(2, servicio.listarProveedores(CLIENTE_A, null).size());
        assertEquals(1, servicio.listarProveedores(CLIENTE_B, null).size());

        // Editarlo en A no cambia B...
        servicio.guardarProveedor(CLIENTE_A, new Contraparte("30-50001122-7", "Papelera (nombre nuevo)"), "30-50001122-7");
        assertEquals("Papelera del Centro S.A.", servicio.listarProveedores(CLIENTE_B, null).get(0).razonSocial);
        // ...y borrarlo de A tampoco.
        servicio.eliminarProveedor(CLIENTE_A, "30-50001122-7");
        assertEquals(1, servicio.listarProveedores(CLIENTE_A, null).size());
        assertEquals(1, servicio.listarProveedores(CLIENTE_B, null).size());
    }

    @Test
    void clientesDelClienteYProveedoresSonListasSeparadas() {
        servicio.guardarClienteDeCliente(CLIENTE_A, new Contraparte("30-65478912-3", "Supermercados Andinos"), null);
        // El mismo CUIT puede ser cliente y proveedor a la vez.
        servicio.guardarProveedor(CLIENTE_A, new Contraparte("30-65478912-3", "Supermercados Andinos"), null);
        assertEquals(1, servicio.listarClientesDeCliente(CLIENTE_A, null).size());
        assertEquals(1, servicio.listarProveedores(CLIENTE_A, null).size());

        servicio.eliminarClienteDeCliente(CLIENTE_A, "30-65478912-3");
        assertTrue(servicio.listarClientesDeCliente(CLIENTE_A, null).isEmpty());
        assertEquals(1, servicio.listarProveedores(CLIENTE_A, null).size());
    }

    @Test
    void noPermiteCuitRepetidoEnElMismoPadron() {
        servicio.guardarProveedor(CLIENTE_A, new Contraparte("30-50001122-7", "Uno"), null);
        servicio.guardarProveedor(CLIENTE_A, new Contraparte("30-71998877-4", "Dos"), null);
        ServicioException ex = assertThrows(ServicioException.class,
                () -> servicio.guardarProveedor(CLIENTE_A, new Contraparte("30-50001122-7", "Otro"), null));
        assertEquals("Ese CUIT ya esta cargado.", ex.getMessage());
        // Tampoco editando: cambiar el CUIT de "Dos" al de "Uno".
        assertThrows(ServicioException.class, () -> servicio.guardarProveedor(CLIENTE_A,
                new Contraparte("30-50001122-7", "Dos"), "30-71998877-4"));
        assertEquals(2, servicio.listarProveedores(CLIENTE_A, null).size());
    }

    @Test
    void editarPuedeCambiarElCuit() {
        servicio.guardarProveedor(CLIENTE_A, new Contraparte("30-50001122-7", "Mal cargado"), null);
        servicio.guardarProveedor(CLIENTE_A, new Contraparte("30-50001122-8", "Bien cargado"), "30-50001122-7");
        List<Contraparte> lista = servicio.listarProveedores(CLIENTE_A, null);
        assertEquals(1, lista.size());
        assertEquals("30-50001122-8", lista.get(0).cuit);
        assertEquals("Bien cargado", lista.get(0).razonSocial);
    }

    @Test
    void listaOrdenadaYConFiltro() {
        servicio.guardarProveedor(CLIENTE_A, new Contraparte("30-00000000-1", "zeta"), null);
        servicio.guardarProveedor(CLIENTE_A, new Contraparte("30-00000000-2", "Alfa"), null);
        assertEquals(List.of("Alfa", "zeta"),
                servicio.listarProveedores(CLIENTE_A, null).stream().map(c -> c.razonSocial).toList());
        assertEquals(1, servicio.listarProveedores(CLIENTE_A, "ZET").size());
        assertEquals(1, servicio.listarProveedores(CLIENTE_A, "0000-2").size(), "por CUIT");
        assertTrue(servicio.listarProveedores(null, null).isEmpty(), "sin cliente activo");
    }

    @Test
    void validaLosDatos() {
        assertThrows(ServicioException.class,
                () -> servicio.guardarProveedor(CLIENTE_A, new Contraparte("123", "X"), null));
        assertThrows(ServicioException.class,
                () -> servicio.guardarProveedor(CLIENTE_A, new Contraparte("30-50001122-7", " "), null));
        assertThrows(ServicioException.class,
                () -> servicio.guardarProveedor("20-99999999-9", new Contraparte("30-50001122-7", "X"), null),
                "el cliente tiene que existir");
    }
}
