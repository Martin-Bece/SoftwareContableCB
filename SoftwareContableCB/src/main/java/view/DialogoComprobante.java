package view;

import view.Modelos.Comprobante;
import view.Modelos.Contraparte;
import view.Modelos.Operacion;
import view.Servicios.ServicioException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Carga manual de un comprobante. El segmento "Impuestos" vive dentro de la
 * factura: de ahi salen el debito y el credito fiscal del mes.
 */
public class DialogoComprobante extends JDialog {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal A21 = new BigDecimal("0.21");
    private static final BigDecimal A105 = new BigDecimal("0.105");
    private static final BigDecimal A27 = new BigDecimal("0.27");

    private final AppContext ctx;
    private final Operacion operacion;
    private final Comprobante original;
    private boolean guardado;

    private final Componentes.CampoTexto txtFecha = new Componentes.CampoTexto("dd/mm/aaaa");
    private final JComboBox<String> cboTipo = Componentes.combo(Modelos.TIPOS_COMPROBANTE);
    private final Componentes.CampoTexto txtPtoVenta = new Componentes.CampoTexto("0001");
    private final Componentes.CampoTexto txtNumero = new Componentes.CampoTexto("00000001");
    private final JComboBox<Object> cboContraparte = new JComboBox<>();
    private final Componentes.CampoTexto txtCuit = new Componentes.CampoTexto("30-71234567-9");
    private final Componentes.CampoTexto txtRazon = new Componentes.CampoTexto("");

    private final CampoImporte neto21 = new CampoImporte();
    private final CampoImporte iva21 = new CampoImporte();
    private final CampoImporte neto105 = new CampoImporte();
    private final CampoImporte iva105 = new CampoImporte();
    private final CampoImporte neto27 = new CampoImporte();
    private final CampoImporte iva27 = new CampoImporte();
    private final CampoImporte noGravado = new CampoImporte();
    private final CampoImporte exento = new CampoImporte();
    private final CampoImporte percepciones = new CampoImporte();
    private final CampoImporte otrosTributos = new CampoImporte();

    private final JCheckBox chkAuto = new JCheckBox("Calcular el IVA desde el neto");
    private final JLabel lblTotal = new JLabel("$ 0,00");
    private final JLabel lblResumenIva = new JLabel(" ");
    private final JLabel lblError = new JLabel(" ");
    private boolean recalculando;

    public DialogoComprobante(Window padre, AppContext ctx, Operacion operacion, Comprobante comprobante) {
        super(padre, ModalityType.APPLICATION_MODAL);
        this.ctx = ctx;
        this.operacion = operacion;
        this.original = comprobante;

        boolean esVenta = operacion == Operacion.VENTA;
        setTitle(comprobante == null
                ? (esVenta ? "Nueva venta" : "Nueva compra")
                : "Editar " + comprobante.comprobanteCompleto());

        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.FONDO);
        add(encabezado(), BorderLayout.NORTH);
        add(cuerpo(), BorderLayout.CENTER);
        add(pie(), BorderLayout.SOUTH);

        chkAuto.setSelected(true);
        aplicarModoAutomatico();

        if (comprobante == null) {
            YearMonth p = ctx.getPeriodo();
            LocalDate porDefecto = p.equals(YearMonth.now()) ? LocalDate.now() : p.atDay(1);
            txtFecha.setText(porDefecto.format(FECHA));
        } else {
            cargar(comprobante);
        }
        recalcular();

        setSize(760, 760);
        setLocationRelativeTo(padre);
    }

    public boolean fueGuardado() { return guardado; }

    /* ------------------------------------------------------------ */
    private JComponent encabezado() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new javax.swing.border.CompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.BORDE), new EmptyBorder(18, 24, 18, 24)));
        JLabel t = new JLabel(getTitle());
        t.setFont(Theme.H2);
        JLabel s = new JLabel(ctx.getClienteActivo().getRazonSocial()
                + "  ·  " + (operacion == Operacion.VENTA ? "comprobante emitido" : "comprobante recibido"));
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
        p.add(textos, BorderLayout.WEST);
        return p;
    }

    private JComponent cuerpo() {
        JPanel contenido = new JPanel();
        contenido.setBackground(Theme.FONDO);
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBorder(new EmptyBorder(18, 24, 18, 24));
        contenido.add(seccionCabecera());
        contenido.add(Box.createVerticalStrut(16));
        contenido.add(seccionImpuestos());

        JScrollPane sp = new JScrollPane(contenido);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.FONDO);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private JPanel tarjetaSeccion(String titulo, String ayuda) {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(Color.WHITE);
        p.setBorder(new javax.swing.border.CompoundBorder(
                new Theme.LineaBorder(Theme.BORDE, 1, 4), new EmptyBorder(16, 18, 18, 18)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel t = new JLabel(titulo);
        t.setFont(Theme.H3);
        JPanel norte = new JPanel();
        norte.setOpaque(false);
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        norte.add(t);
        if (ayuda != null) {
            JLabel a = new JLabel(ayuda);
            a.setFont(Theme.CHICA);
            a.setForeground(Theme.TEXTO_SEC);
            a.setAlignmentX(Component.LEFT_ALIGNMENT);
            norte.add(Box.createVerticalStrut(3));
            norte.add(a);
        }
        p.add(norte, BorderLayout.NORTH);
        return p;
    }

    private JComponent seccionCabecera() {
        JPanel p = tarjetaSeccion("Datos del comprobante", null);
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);

        txtPtoVenta.setPreferredSize(new Dimension(90, 34));
        grid.add(campo("Fecha", txtFecha), Componentes.gbc(0, 0, 1, 0.3));
        grid.add(campo("Tipo", cboTipo), Componentes.gbc(1, 0, 1, 0.4));
        grid.add(campo("Punto de venta", txtPtoVenta), Componentes.gbc(2, 0, 1, 0.15));
        grid.add(campo("Numero", txtNumero), Componentes.gbc(3, 0, 1, 0.15));

        cargarContrapartes();
        Componentes.estilizarCombo(cboContraparte);
        cboContraparte.addActionListener(e -> {
            Object sel = cboContraparte.getSelectedItem();
            if (sel instanceof Contraparte) {
                Contraparte c = (Contraparte) sel;
                txtCuit.setText(c.cuit);
                txtRazon.setText(c.razonSocial);
                txtCuit.setEditable(false);
                txtRazon.setEditable(false);
                txtCuit.setBackground(Theme.FONDO);
                txtRazon.setBackground(Theme.FONDO);
            } else {
                txtCuit.setEditable(true);
                txtRazon.setEditable(true);
                txtCuit.setBackground(Color.WHITE);
                txtRazon.setBackground(Color.WHITE);
            }
        });

        String rotulo = operacion == Operacion.VENTA ? "Cliente" : "Proveedor";
        grid.add(campo(rotulo + " registrado", cboContraparte), Componentes.gbc(0, 1, 2, 0.6));
        grid.add(campo("CUIT", txtCuit), Componentes.gbc(2, 1, 2, 0.4));
        grid.add(campo("Razon social", txtRazon), Componentes.gbc(0, 2, 4, 1.0));

        p.add(grid, BorderLayout.CENTER);
        return p;
    }

    private JComponent seccionImpuestos() {
        JPanel p = tarjetaSeccion("Impuestos",
                "Lo que cargues aca determina el IVA del mes: el debito sale de las ventas y el credito de las compras.");

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);

        grid.add(encabezadoColumna("Concepto"), Componentes.gbc(0, 0, 1, 0.34));
        grid.add(encabezadoColumna("Neto gravado"), Componentes.gbc(1, 0, 1, 0.33));
        grid.add(encabezadoColumna("IVA"), Componentes.gbc(2, 0, 1, 0.33));

        agregarFilaAlicuota(grid, 1, "Alicuota 21 %", neto21, iva21);
        agregarFilaAlicuota(grid, 2, "Alicuota 10,5 %", neto105, iva105);
        agregarFilaAlicuota(grid, 3, "Alicuota 27 %", neto27, iva27);

        chkAuto.setFont(Theme.CHICA);
        chkAuto.setBackground(Color.WHITE);
        chkAuto.setForeground(Theme.TEXTO_SEC);
        chkAuto.setFocusPainted(false);
        chkAuto.addActionListener(e -> {
            aplicarModoAutomatico();
            recalcular();
        });
        GridBagConstraints gAuto = Componentes.gbc(1, 4, 2, 1.0);
        grid.add(chkAuto, gAuto);

        grid.add(separadorFila(), Componentes.gbc(0, 5, 3, 1.0));

        agregarFilaSimple(grid, 6, "No gravado", noGravado);
        agregarFilaSimple(grid, 7, "Exento", exento);
        agregarFilaSimple(grid, 8, operacion == Operacion.VENTA
                ? "Percepciones practicadas" : "Percepciones sufridas", percepciones);
        agregarFilaSimple(grid, 9, "Otros tributos", otrosTributos);

        JPanel total = new JPanel(new BorderLayout());
        total.setOpaque(false);
        total.setBorder(new EmptyBorder(14, 0, 0, 0));

        JLabel rot = new JLabel("Total del comprobante");
        rot.setFont(Theme.BODY_BOLD);
        lblTotal.setFont(new Font(Theme.FAMILIA, Font.BOLD, 22));
        lblTotal.setForeground(Theme.TEXTO);
        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        lblResumenIva.setFont(Theme.CHICA);
        lblResumenIva.setForeground(Theme.TEXTO_SEC);

        JPanel izq = new JPanel();
        izq.setOpaque(false);
        izq.setLayout(new BoxLayout(izq, BoxLayout.Y_AXIS));
        rot.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblResumenIva.setAlignmentX(Component.LEFT_ALIGNMENT);
        izq.add(rot);
        izq.add(Box.createVerticalStrut(2));
        izq.add(lblResumenIva);

        total.add(izq, BorderLayout.WEST);
        total.add(lblTotal, BorderLayout.EAST);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(grid, BorderLayout.CENTER);
        centro.add(total, BorderLayout.SOUTH);

        p.add(centro, BorderLayout.CENTER);
        return p;
    }

    private JComponent encabezadoColumna(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Theme.CHICA_BOLD);
        l.setForeground(Theme.TEXTO_SEC);
        return l;
    }

    private JComponent separadorFila() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(6, 0, 6, 0));
        p.add(Theme.separador(), BorderLayout.CENTER);
        return p;
    }

    private void agregarFilaAlicuota(JPanel grid, int fila, String rotulo,
                                     CampoImporte neto, CampoImporte iva) {
        JLabel l = new JLabel(rotulo);
        l.setFont(Theme.BODY);
        grid.add(l, Componentes.gbc(0, fila, 1, 0.34));
        grid.add(neto, Componentes.gbc(1, fila, 1, 0.33));
        grid.add(iva, Componentes.gbc(2, fila, 1, 0.33));
    }

    private void agregarFilaSimple(JPanel grid, int fila, String rotulo, CampoImporte campo) {
        JLabel l = new JLabel(rotulo);
        l.setFont(Theme.BODY);
        grid.add(l, Componentes.gbc(0, fila, 1, 0.34));
        grid.add(campo, Componentes.gbc(1, fila, 2, 0.66));
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

    private JComponent pie() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new javax.swing.border.CompoundBorder(
                new MatteBorder(1, 0, 0, 0, Theme.BORDE), new EmptyBorder(14, 24, 14, 24)));
        lblError.setFont(Theme.CHICA);
        lblError.setForeground(Theme.ROJO);

        Componentes.Boton cancelar = Componentes.secundario("Cancelar");
        cancelar.addActionListener(e -> dispose());
        Componentes.Boton guardar = Componentes.primario(original == null ? "Guardar comprobante" : "Guardar cambios");
        guardar.addActionListener(e -> guardar());

        p.add(lblError, BorderLayout.WEST);
        p.add(Componentes.filaBotones(cancelar, guardar), BorderLayout.EAST);
        return p;
    }

    /* ------------------------------------------------------------ */
    private void cargarContrapartes() {
        cboContraparte.removeAllItems();
        cboContraparte.addItem("Cargar otro CUIT");
        List<Contraparte> lista = operacion == Operacion.VENTA
                ? ctx.contrapartes().listarClientesDeCliente(ctx.cuitActivo(), null)
                : ctx.contrapartes().listarProveedores(ctx.cuitActivo(), null);
        for (Contraparte c : lista) cboContraparte.addItem(c);
    }

    private void aplicarModoAutomatico() {
        boolean auto = chkAuto.isSelected();
        for (CampoImporte c : new CampoImporte[]{iva21, iva105, iva27}) {
            c.setEditable(!auto);
            c.setBackground(auto ? Theme.FONDO : Color.WHITE);
        }
    }

    private void recalcular() {
        if (recalculando) return;
        recalculando = true;
        try {
            if (chkAuto.isSelected()) {
                iva21.setImporte(neto21.getImporte().multiply(A21).setScale(2, RoundingMode.HALF_UP));
                iva105.setImporte(neto105.getImporte().multiply(A105).setScale(2, RoundingMode.HALF_UP));
                iva27.setImporte(neto27.getImporte().multiply(A27).setScale(2, RoundingMode.HALF_UP));
            }
            Comprobante temporal = construir(new Comprobante());
            lblTotal.setText(Theme.pesos(temporal.total()));
            lblResumenIva.setText("Neto gravado " + Theme.pesos(temporal.netoGravado())
                    + "  ·  IVA " + Theme.pesos(temporal.ivaTotal()));
        } finally {
            recalculando = false;
        }
    }

    private Comprobante construir(Comprobante destino) {
        destino.neto21 = neto21.getImporte();
        destino.iva21 = iva21.getImporte();
        destino.neto105 = neto105.getImporte();
        destino.iva105 = iva105.getImporte();
        destino.neto27 = neto27.getImporte();
        destino.iva27 = iva27.getImporte();
        destino.noGravado = noGravado.getImporte();
        destino.exento = exento.getImporte();
        destino.percepciones = percepciones.getImporte();
        destino.otrosTributos = otrosTributos.getImporte();
        return destino;
    }

    private void cargar(Comprobante c) {
        txtFecha.setText(c.fecha.format(FECHA));
        cboTipo.setSelectedItem(c.tipo);
        txtPtoVenta.setText(c.puntoVenta);
        txtNumero.setText(c.numero);
        txtCuit.setText(c.cuitContraparte);
        txtRazon.setText(c.razonSocial);
        neto21.setImporte(c.neto21);
        iva21.setImporte(c.iva21);
        neto105.setImporte(c.neto105);
        iva105.setImporte(c.iva105);
        neto27.setImporte(c.neto27);
        iva27.setImporte(c.iva27);
        noGravado.setImporte(c.noGravado);
        exento.setImporte(c.exento);
        percepciones.setImporte(c.percepciones);
        otrosTributos.setImporte(c.otrosTributos);
        chkAuto.setSelected(false);
        aplicarModoAutomatico();
    }

    private void guardar() {
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(txtFecha.getText().trim(), FECHA);
        } catch (Exception e) {
            lblError.setText("Revisa la fecha: tiene que ser dd/mm/aaaa.");
            txtFecha.requestFocus();
            return;
        }
        if (txtNumero.getText().trim().isEmpty()) {
            lblError.setText("Falta el numero del comprobante.");
            txtNumero.requestFocus();
            return;
        }
        String cuit = DialogoCliente.normalizarCuit(txtCuit.getText());
        if (cuit == null) {
            lblError.setText("El CUIT de la contraparte debe tener 11 digitos.");
            txtCuit.requestFocus();
            return;
        }
        if (txtRazon.getText().trim().isEmpty()) {
            lblError.setText("Falta la razon social de la contraparte.");
            txtRazon.requestFocus();
            return;
        }

        Comprobante c = original == null ? new Comprobante() : original;
        c.operacion = operacion;
        c.fecha = fecha;
        c.tipo = (String) cboTipo.getSelectedItem();
        c.puntoVenta = txtPtoVenta.getText().trim().isEmpty() ? "0001" : txtPtoVenta.getText().trim();
        c.numero = txtNumero.getText().trim();
        c.cuitContraparte = cuit;
        c.razonSocial = txtRazon.getText().trim();
        if (original == null) c.origen = "Manual";
        construir(c);

        if (c.total().signum() == 0) {
            lblError.setText("El comprobante quedo en cero. Carga al menos un importe.");
            return;
        }

        try {
            ctx.comprobantes().guardarComprobante(ctx.cuitActivo(), c);
            guardado = true;
            if (!YearMonth.from(fecha).equals(ctx.getPeriodo())) {
                Componentes.aviso(this, "El comprobante se guardo en "
                        + TopBar.formatear(YearMonth.from(fecha))
                        + ", que no es el periodo que estas viendo.");
            }
            dispose();
        } catch (ServicioException ex) {
            lblError.setText(ex.getMessage());
        }
    }

    /* ------------------------------------------------------------ */
    /** Campo de importe alineado a la derecha que dispara el recalculo. */
    private class CampoImporte extends Componentes.CampoTexto {
        CampoImporte() {
            super("0,00");
            setHorizontalAlignment(SwingConstants.RIGHT);
            getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { SwingUtilities.invokeLater(DialogoComprobante.this::recalcular); }
                @Override public void removeUpdate(DocumentEvent e) { SwingUtilities.invokeLater(DialogoComprobante.this::recalcular); }
                @Override public void changedUpdate(DocumentEvent e) { }
            });
        }

        BigDecimal getImporte() { return Theme.parsear(getText()); }

        /**
         * Solo escribe si el texto cambia. Sin esto, cada setText dispara otro
         * recalculo y el ciclo no termina nunca.
         */
        void setImporte(BigDecimal v) {
            String nuevo = (v == null || v.signum() == 0) ? "" : Theme.numero(v);
            if (!nuevo.equals(getText())) setText(nuevo);
        }
    }
}