package view;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Regresion: el formulario agregaba txtCuit en los cuatro lugares, asi que Razon social,
 * Nombre de fantasia y Direccion no aparecian y no se podia crear ningun cliente.
 */
class DialogoClienteTest {

    @Test
    void losCuatroCamposDelFormularioEstanEnPantalla() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "necesita entorno grafico");

        AtomicReference<Throwable> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                AppContext ctx = App.contextoDemo();
                DialogoCliente dlg = new DialogoCliente(null, ctx, null);
                try {
                    for (String campo : new String[]{"txtCuit", "txtRazon", "txtFantasia", "txtDireccion"}) {
                        Component c = (Component) campo(dlg, campo);
                        assertTrue(SwingUtilities.isDescendingFrom(c, dlg),
                                campo + " tiene que estar dentro del formulario");
                    }
                } finally {
                    dlg.dispose();
                }
            } catch (Throwable t) {
                error.set(t);
            }
        });
        if (error.get() != null) {
            if (error.get() instanceof AssertionError ae) throw ae;
            throw new RuntimeException(error.get());
        }
    }

    private static Object campo(Object objeto, String nombre) throws ReflectiveOperationException {
        Field f = objeto.getClass().getDeclaredField(nombre);
        f.setAccessible(true);
        return f.get(objeto);
    }
}
