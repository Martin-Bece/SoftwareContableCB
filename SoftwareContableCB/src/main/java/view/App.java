package view;

import config.Conexion;
import service.ClienteServiceImpl;
import service.ComprobanteServiceImpl;
import service.ContraparteServiceImpl;
import service.ImpuestoServiceImpl;
import service.PercepcionServiceImpl;
import service.ReporteServiceImpl;

import javax.swing.*;
import java.sql.SQLException;

/**
 * Punto de entrada de la aplicacion.
 *
 * <p>Por defecto usa el backend real (SQLite) para todos los modulos.</p>
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

    /** Abre la base (o avisa y sale si no puede) y arma el contexto con los services reales. */
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

        return contextoBase();
    }

    /** Todos los services reales sobre la base configurada (sin dialogos; lo usan tambien los tests). */
    static AppContext contextoBase() {
        ClienteServiceImpl clientes = new ClienteServiceImpl();
        ComprobanteServiceImpl comprobantes = new ComprobanteServiceImpl();
        PercepcionServiceImpl percepciones = new PercepcionServiceImpl();
        ImpuestoServiceImpl impuestos = new ImpuestoServiceImpl(clientes, comprobantes, percepciones);
        ReporteServiceImpl reportes = new ReporteServiceImpl();
        ContraparteServiceImpl contrapartes = new ContraparteServiceImpl();
        return new AppContext(clientes, comprobantes, percepciones, impuestos, reportes, contrapartes);
    }
}
