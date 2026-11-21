package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.File;

/**
 * Previsualizacion de una importacion. Nunca se persiste nada sin que la
 * contadora vea primero las filas detectadas.
 */
public class DialogoImportacion extends JDialog {

    private boolean confirmado;
    private final JTable tabla;

    public DialogoImportacion(Window padre, String titulo, String descripcion,
                              TableModel modelo, String textoConfirmar) {
        super(padre, titulo, ModalityType.APPLICATION_MODAL);
        this.tabla = new JTable(modelo);

        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.FONDO);

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBackground(Color.WHITE);
        cabecera.setBorder(new javax.swing.border.CompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.BORDE), new EmptyBorder(18, 24, 18, 24)));
        JLabel t = new JLabel(titulo);
        t.setFont(Theme.H2);
        JLabel s = new JLabel(descripcion);
        s.setFont(Theme.CHICA);
        s.setForeground(Theme.TEXTO_SEC);
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        t.setAlignmentX(LEFT_ALIGNMENT);
        s.setAlignmentX(LEFT_ALIGNMENT);
        textos.add(t);
        textos.add(Box.createVerticalStrut(3));
        textos.add(s);
        cabecera.add(textos, BorderLayout.WEST);

        Theme.estilizarTabla(tabla);
        JPanel marco = Theme.tarjeta();
        marco.setBorder(new javax.swing.border.CompoundBorder(
                new EmptyBorder(18, 24, 12, 24), new Theme.LineaBorder(Theme.BORDE, 1, 4)));
        marco.add(Theme.scroll(tabla), BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setBackground(Color.WHITE);
        pie.setBorder(new javax.swing.border.CompoundBorder(
                new MatteBorder(1, 0, 0, 0, Theme.BORDE), new EmptyBorder(14, 24, 14, 24)));

        JLabel detalle = new JLabel(modelo.getRowCount() + " filas detectadas");
        detalle.setFont(Theme.CHICA);
        detalle.setForeground(Theme.TEXTO_SEC);

        Componentes.Boton cancelar = Componentes.secundario("Cancelar");
        cancelar.addActionListener(e -> dispose());
        Componentes.Boton quitar = Componentes.enlace("Quitar fila");
        quitar.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila >= 0 && modelo instanceof ModeloComprobantes) {
                ((ModeloComprobantes) modelo).quitar(tabla.convertRowIndexToModel(fila));
                detalle.setText(modelo.getRowCount() + " filas detectadas");
            }
        });
        Componentes.Boton confirmar = Componentes.primario(textoConfirmar);
        confirmar.setEnabled(modelo.getRowCount() > 0);
        confirmar.addActionListener(e -> {
            confirmado = true;
            dispose();
        });

        pie.add(detalle, BorderLayout.WEST);
        pie.add(Componentes.filaBotones(quitar, cancelar, confirmar), BorderLayout.EAST);

        add(cabecera, BorderLayout.NORTH);
        add(marco, BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);

        setSize(1080, 620);
        setLocationRelativeTo(padre);
    }

    public JTable getTabla() { return tabla; }

    public boolean fueConfirmado() { return confirmado; }

    /** Selector de archivo con los formatos que acepta el sistema. */
    public static File elegirArchivo(Component padre) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Elegi el archivo a importar");
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(new FileNameExtensionFilter(
                "Todos los formatos aceptados (csv, xls, xlsx, pdf)", "csv", "xls", "xlsx", "pdf"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("CSV (*.csv)", "csv"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("Excel (*.xls, *.xlsx)", "xls", "xlsx"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("PDF de AFIP (*.pdf)", "pdf"));
        return fc.showOpenDialog(padre) == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
    }
}
