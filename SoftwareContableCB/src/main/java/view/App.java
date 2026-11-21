package view;

import config.Conexion;
import service.ClienteServiceImpl;
import service.ComprobanteServiceImpl;

import javax.swing.*;
import java.sql.SQLException;

/**
 * Punto de entrada de la aplicacion.
 *
 * <p>Por defecto usa el backend real (SQLite) para clientes y comprobantes, y el mock
 * en modo hibrido para lo que todavia no tiene backend (percepciones, contrapartes,
 * calculo de impuestos y reportes).</p>
 *
 * <p>Para abrir la version de demostracion con datos de ejemplo (todo en memoria):
 * {@code -Dcontabilidad.demo=true}.</p>
 */
public class App {

    public static final String PROPIEDAD_DEMO = "contabilidad.demo";

    public static void main(String[] args) {
        Theme.aplicarDefaults();
        SwingUtilities.invokeLater(() -> {
            AppContext ctx = Boolean.getBoolean(PROPIEDAD_DEMO) ? contextoDemo() : contextoReal();
            if (ctx != null) new MainFrame(ctx).setVisible(true);
        });
    }

    /** Todo en memoria con datos de ejemplo. */
    static AppContext contextoDemo() {
        ServiciosMock mock = new ServiciosMock();
        return new AppContext(mock, mock, mock, mock, mock, mock);
    }

    /** Clientes y comprobantes reales; el resto sigue en el mock (ver ServiciosMock). */
    static AppContext contextoReal() {
        try {
            Conexion.getConexion(); // abre la base y crea/migra el esquema antes de mostrar nada
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "No se pudo abrir la base de datos:\n" + Conexion.rutaBase() + "\n\n" + e.getMessage(),
                    "Sistema Contable", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return null;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(Conexion::cerrar));

        ClienteServiceImpl clientes = new ClienteServiceImpl();
        ComprobanteServiceImpl comprobantes = new ComprobanteServiceImpl();
        ServiciosMock resto = new ServiciosMock(clientes, comprobantes);
        return new AppContext(clientes, comprobantes, resto, resto, resto, resto);
    }
}
