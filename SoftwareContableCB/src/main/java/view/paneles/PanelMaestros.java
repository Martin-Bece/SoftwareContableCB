package view.paneles;

import view.Modelos.Contraparte;
import view.Servicios.ServicioException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import view.AppContext;
import view.Componentes;
import view.DialogoCliente;
import view.Theme;

/** Clientes del cliente y proveedores: dos padrones con la misma forma. */
public class PanelMaestros extends PanelBase {

    private final CardLayout cards = new CardLayout();
    private final JPanel contenido = new JPanel(cards);
    private final Padron padronClientes;
    private final Padron padronProveedores;

    public PanelMaestros(AppContext ctx) {
        super(ctx, "Clientes y proveedores", true);

        padronClientes = new Padron(false);
        padronProveedores = new Padron(true);

        Componentes.Segmentado seg = new Componentes.Segmentado("Clientes del cliente", "Proveedores");
        seg.setAlCambiar(i -> cards.show(contenido, String.valueOf(i)));

        contenido.setOpaque(false);
        contenido.add(padronClientes, "0");
        contenido.add(padronProveedores, "1");

        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(seg, BorderLayout.WEST);

        cuerpo.add(norte, BorderLayout.NORTH);
        cuerpo.add(contenido, BorderLayout.CENTER);
    }

    @Override
    protected void cargarDatos() {
        setSubtitulo("Padrones de " + ctx.getClienteActivo().getRazonSocial());
        padronClientes.cargar();
        padronProveedores.cargar();
    }

    /* ================================================================ */
    private class Padron extends JPanel {
        private final boolean esProveedor;
        private final ModeloContrapartes modelo = new ModeloContrapartes();
        private final JTable tabla = new JTable(modelo);
        private final Componentes.TablaConTotales contenedor;
        private final Componentes.CampoBusqueda busqueda =
                new Componentes.CampoBusqueda("Buscar por razon social o CUIT");
        private final Componentes.Boton btnEditar = Componentes.secundario("Editar");
        private final Componentes.Boton btnEliminar = Componentes.peligro("Eliminar");

        Padron(boolean esProveedor) {
            super(new BorderLayout(0, 12));
            this.esProveedor = esProveedor;
            setOpaque(false);

            busqueda.setPreferredSize(new Dimension(300, 36));
            busqueda.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { cargar(); }
                @Override public void removeUpdate(DocumentEvent e) { cargar(); }
                @Override public void changedUpdate(DocumentEvent e) { cargar(); }
            });

            Componentes.Boton nuevo = Componentes.primario(esProveedor ? "Nuevo proveedor" : "Nuevo cliente");
            nuevo.addActionListener(e -> abrir(null));

            JPanel barra = new JPanel(new BorderLayout());
            barra.setOpaque(false);
            barra.add(busqueda, BorderLayout.WEST);
            barra.add(Componentes.filaBotones(nuevo), BorderLayout.EAST);

            contenedor = new Componentes.TablaConTotales(tabla);
            contenedor.setMensajeVacio(esProveedor
                    ? "Todavia no hay proveedores cargados para este cliente."
                    : "Todavia no hay clientes cargados para este cliente.");
            tabla.getColumnModel().getColumn(0).setCellRenderer(new Componentes.RenderTexto(SwingConstants.LEFT));
            tabla.getColumnModel().getColumn(1).setCellRenderer(new Componentes.RenderTexto(SwingConstants.LEFT));
            Componentes.anchos(tabla, 1.2, 3.2);

            tabla.getSelectionModel().addListSelectionListener(e -> actualizarBotones());
            tabla.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && seleccionado() != null) abrir(seleccionado());
                }
            });
            btnEditar.addActionListener(e -> abrir(seleccionado()));
            btnEliminar.addActionListener(e -> eliminar());

            JPanel tarjeta = Theme.tarjeta();
            tarjeta.setBorder(new Theme.LineaBorder(Theme.BORDE, 1, 4));
            tarjeta.add(contenedor, BorderLayout.CENTER);

            JPanel inferior = new JPanel(new BorderLayout());
            inferior.setOpaque(false);
            inferior.add(Componentes.filaBotones(btnEditar, btnEliminar), BorderLayout.EAST);

            add(barra, BorderLayout.NORTH);
            add(tarjeta, BorderLayout.CENTER);
            add(inferior, BorderLayout.SOUTH);

            actualizarBotones();
        }

        private Contraparte seleccionado() {
            int fila = tabla.getSelectedRow();
            return fila < 0 ? null : modelo.getEn(tabla.convertRowIndexToModel(fila));
        }

        private void actualizarBotones() {
            boolean hay = seleccionado() != null;
            btnEditar.setEnabled(hay);
            btnEliminar.setEnabled(hay);
        }

        private void abrir(Contraparte c) {
            Window padre = SwingUtilities.getWindowAncestor(PanelMaestros.this);
            DialogoContraparte dlg = new DialogoContraparte(padre, ctx, esProveedor, c);
            dlg.setVisible(true);
            if (dlg.fueGuardado()) cargar();
        }

        private void eliminar() {
            Contraparte c = seleccionado();
            if (c == null) return;
            if (!Componentes.confirmar(PanelMaestros.this,
                    "Se quita a " + c.razonSocial + " del padron. Continuar?")) return;
            if (esProveedor) {
                ctx.contrapartes().eliminarProveedor(ctx.cuitActivo(), c.cuit);
            } else {
                ctx.contrapartes().eliminarClienteDeCliente(ctx.cuitActivo(), c.cuit);
            }
            cargar();
        }

        void cargar() {
            if (!ctx.hayClienteActivo()) return;
            List<Contraparte> lista = esProveedor
                    ? ctx.contrapartes().listarProveedores(ctx.cuitActivo(), busqueda.getText())
                    : ctx.contrapartes().listarClientesDeCliente(ctx.cuitActivo(), busqueda.getText());
            modelo.setDatos(lista);
            contenedor.setTotales(lista.size() + (lista.size() == 1 ? " registro" : " registros"), null);
            contenedor.actualizarEstadoVacio();
            actualizarBotones();
        }
    }

    /* ================================================================ */
    private static class ModeloContrapartes extends AbstractTableModel {
        private final String[] columnas = {"CUIT", "Razon social"};
        private List<Contraparte> datos = new ArrayList<>();

        void setDatos(List<Contraparte> datos) {
            this.datos = datos;
            fireTableDataChanged();
        }

        Contraparte getEn(int fila) { return datos.get(fila); }

        @Override public int getRowCount() { return datos.size(); }

        @Override public int getColumnCount() { return columnas.length; }

        @Override public String getColumnName(int c) { return columnas[c]; }

        @Override public boolean isCellEditable(int f, int c) { return false; }

        @Override
        public Object getValueAt(int fila, int col) {
            Contraparte c = datos.get(fila);
            return col == 0 ? c.cuit : c.razonSocial;
        }
    }

    /* ================================================================ */
    private static class DialogoContraparte extends JDialog {
        private final AppContext ctx;
        private final boolean esProveedor;
        private final Contraparte original;
        private boolean guardado;

        private final Componentes.CampoTexto txtCuit = new Componentes.CampoTexto("30-71234567-9");
        private final Componentes.CampoTexto txtRazon = new Componentes.CampoTexto("");
        private final JLabel lblError = new JLabel(" ");

        DialogoContraparte(Window padre, AppContext ctx, boolean esProveedor, Contraparte c) {
            super(padre, ModalityType.APPLICATION_MODAL);
            this.ctx = ctx;
            this.esProveedor = esProveedor;
            this.original = c;
            String que = esProveedor ? "proveedor" : "cliente";
            setTitle(c == null ? "Nuevo " + que : "Editar " + que);

            JPanel centro = new JPanel(new BorderLayout(0, 14));
            centro.setBackground(Color.WHITE);
            centro.setBorder(new EmptyBorder(20, 24, 20, 24));

            JLabel t = new JLabel(getTitle());
            t.setFont(Theme.H2);

            JPanel grid = new JPanel(new GridBagLayout());
            grid.setOpaque(false);
            grid.add(campo("CUIT", txtCuit), Componentes.gbc(0, 0, 1, 0.4));
            grid.add(campo("Razon social", txtRazon), Componentes.gbc(1, 0, 1, 0.6));

            centro.add(t, BorderLayout.NORTH);
            centro.add(grid, BorderLayout.CENTER);

            JPanel pie = new JPanel(new BorderLayout());
            pie.setBackground(Color.WHITE);
            pie.setBorder(new javax.swing.border.CompoundBorder(
                    new MatteBorder(1, 0, 0, 0, Theme.BORDE), new EmptyBorder(14, 24, 14, 24)));
            lblError.setFont(Theme.CHICA);
            lblError.setForeground(Theme.ROJO);
            Componentes.Boton cancelar = Componentes.secundario("Cancelar");
            cancelar.addActionListener(e -> dispose());
            Componentes.Boton guardar = Componentes.primario("Guardar");
            guardar.addActionListener(e -> guardar());
            pie.add(lblError, BorderLayout.WEST);
            pie.add(Componentes.filaBotones(cancelar, guardar), BorderLayout.EAST);

            setLayout(new BorderLayout());
            add(centro, BorderLayout.CENTER);
            add(pie, BorderLayout.SOUTH);

            if (c != null) {
                txtCuit.setText(c.cuit);
                txtRazon.setText(c.razonSocial);
            }

            setSize(560, 260);
            setLocationRelativeTo(padre);
        }

        private JComponent campo(String rotulo, JComponent control) {
            JPanel p = new JPanel(new BorderLayout(0, 5));
            p.setOpaque(false);
            JLabel l = new JLabel(rotulo);
            l.setFont(Theme.CHICA_BOLD);
            l.setForeground(Theme.TEXTO_SEC);
            control.setPreferredSize(new Dimension(control.getPreferredSize().width, 34));
            p.add(l, BorderLayout.NORTH);
            p.add(control, BorderLayout.CENTER);
            return p;
        }

        boolean fueGuardado() { return guardado; }

        private void guardar() {
            String cuit = DialogoCliente.normalizarCuit(txtCuit.getText());
            if (cuit == null) {
                lblError.setText("El CUIT debe tener 11 digitos.");
                return;
            }
            if (txtRazon.getText().trim().isEmpty()) {
                lblError.setText("La razon social es obligatoria.");
                return;
            }
            Contraparte c = new Contraparte(cuit, txtRazon.getText().trim());
            try {
                if (esProveedor) {
                    ctx.contrapartes().guardarProveedor(ctx.cuitActivo(), c,
                            original == null ? null : original.cuit);
                } else {
                    ctx.contrapartes().guardarClienteDeCliente(ctx.cuitActivo(), c,
                            original == null ? null : original.cuit);
                }
                guardado = true;
                dispose();
            } catch (ServicioException ex) {
                lblError.setText(ex.getMessage());
            }
        }
    }
}