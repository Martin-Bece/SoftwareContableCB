package view;

import view.Modelos.Clase;
import view.Modelos.PercepcionRetencion;
import view.Modelos.Tributo;
import view.Servicios.ServicioException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/** Carga manual de una percepcion o retencion sufrida. */
public class DialogoPercepcion extends JDialog {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AppContext ctx;
    private final PercepcionRetencion original;
    private boolean guardado;

    private final JComboBox<Clase> cboClase = new JComboBox<>(Clase.values());
    private final JComboBox<Tributo> cboTributo = new JComboBox<>(Tributo.values());
    private final Componentes.CampoTexto txtFecha = new Componentes.CampoTexto("dd/mm/aaaa");
    private final Componentes.CampoTexto txtRegimen = new Componentes.CampoTexto("RG 2408");
    private final Componentes.CampoTexto txtJurisdiccion = new Componentes.CampoTexto("Nacional");
    private final Componentes.CampoTexto txtComprobante = new Componentes.CampoTexto("FA 0001-00001234");
    private final Componentes.CampoTexto txtCuit = new Componentes.CampoTexto("30-71234567-9");
    private final Componentes.CampoTexto txtRazon = new Componentes.CampoTexto("");
    private final Componentes.CampoTexto txtImporte = new Componentes.CampoTexto("0,00");
    private final JLabel lblError = new JLabel(" ");

    public DialogoPercepcion(Window padre, AppContext ctx, PercepcionRetencion item) {
        super(padre, item == null ? "Nueva percepcion o retencion" : "Editar movimiento",
                ModalityType.APPLICATION_MODAL);
        this.ctx = ctx;
        this.original = item;

        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.FONDO);
        add(cuerpo(), BorderLayout.CENTER);
        add(pie(), BorderLayout.SOUTH);

        if (item == null) {
            YearMonth p = ctx.getPeriodo();
            txtFecha.setText((p.equals(YearMonth.now()) ? LocalDate.now() : p.atDay(1)).format(FECHA));
        } else {
            cargar(item);
        }

        setSize(640, 560);
        setLocationRelativeTo(padre);
    }

    public boolean fueGuardado() { return guardado; }

    private JComponent cuerpo() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel t = new JLabel(getTitle());
        t.setFont(Theme.H2);
        JLabel s = new JLabel("Se imputa al cliente " + ctx.getClienteActivo().getRazonSocial()
                + " como ingreso a cuenta del tributo elegido.");
        s.setFont(Theme.CHICA);
        s.setForeground(Theme.TEXTO_SEC);

        JPanel norte = new JPanel();
        norte.setOpaque(false);
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        norte.add(t);
        norte.add(Box.createVerticalStrut(4));
        norte.add(s);

        Componentes.estilizarCombo(cboClase);
        Componentes.estilizarCombo(cboTributo);
        txtImporte.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.add(campo("Clase", cboClase), Componentes.gbc(0, 0, 1, 0.34));
        grid.add(campo("Tributo", cboTributo), Componentes.gbc(1, 0, 1, 0.33));
        grid.add(campo("Fecha", txtFecha), Componentes.gbc(2, 0, 1, 0.33));
        grid.add(campo("Regimen", txtRegimen), Componentes.gbc(0, 1, 1, 0.34));
        grid.add(campo("Jurisdiccion", txtJurisdiccion), Componentes.gbc(1, 1, 1, 0.33));
        grid.add(campo("Importe", txtImporte), Componentes.gbc(2, 1, 1, 0.33));
        grid.add(campo("Comprobante", txtComprobante), Componentes.gbc(0, 2, 1, 0.34));
        grid.add(campo("CUIT del agente", txtCuit), Componentes.gbc(1, 2, 2, 0.66));
        grid.add(campo("Agente de recaudacion", txtRazon), Componentes.gbc(0, 3, 3, 1.0));

        p.add(norte, BorderLayout.NORTH);
        p.add(grid, BorderLayout.CENTER);
        return p;
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
        Componentes.Boton guardar = Componentes.primario("Guardar");
        guardar.addActionListener(e -> guardar());

        p.add(lblError, BorderLayout.WEST);
        p.add(Componentes.filaBotones(cancelar, guardar), BorderLayout.EAST);
        return p;
    }

    private void cargar(PercepcionRetencion p) {
        cboClase.setSelectedItem(p.clase);
        cboTributo.setSelectedItem(p.tributo);
        txtFecha.setText(p.fecha.format(FECHA));
        txtRegimen.setText(p.regimen);
        txtJurisdiccion.setText(p.jurisdiccion);
        txtComprobante.setText(p.comprobante);
        txtCuit.setText(p.cuitAgente);
        txtRazon.setText(p.razonSocialAgente);
        txtImporte.setText(Theme.numero(p.importe));
    }

    private void guardar() {
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(txtFecha.getText().trim(), FECHA);
        } catch (Exception e) {
            lblError.setText("Revisa la fecha: tiene que ser dd/mm/aaaa.");
            return;
        }
        BigDecimal importe = Theme.parsear(txtImporte.getText());
        if (importe.signum() <= 0) {
            lblError.setText("El importe tiene que ser mayor a cero.");
            return;
        }
        String cuit = DialogoCliente.normalizarCuit(txtCuit.getText());
        if (cuit == null) {
            lblError.setText("El CUIT del agente debe tener 11 digitos.");
            return;
        }

        PercepcionRetencion p = original == null ? new PercepcionRetencion() : original;
        p.clase = (Clase) cboClase.getSelectedItem();
        p.tributo = (Tributo) cboTributo.getSelectedItem();
        p.fecha = fecha;
        p.regimen = txtRegimen.getText().trim();
        p.jurisdiccion = txtJurisdiccion.getText().trim();
        p.comprobante = txtComprobante.getText().trim();
        p.cuitAgente = cuit;
        p.razonSocialAgente = txtRazon.getText().trim();
        p.importe = importe;
        if (original == null) p.origen = "Manual";

        try {
            ctx.percepciones().guardarPercepcion(ctx.cuitActivo(), p);
            guardado = true;
            dispose();
        } catch (ServicioException ex) {
            lblError.setText(ex.getMessage());
        }
    }
}