package view.paneles;

import view.Modelos.Cliente;
import view.Modelos.PosicionIva;
import view.Modelos.ResumenTributo;
import view.Modelos.Tributo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import view.AppContext;
import view.Componentes;
import view.Modelos;
import view.Theme;
import view.TopBar;

/** Determinacion de IVA, Ganancias y SUSS para el cliente y periodo activos. */
public class PanelImpuestos extends PanelBase {

    private final CardLayout cards = new CardLayout();
    private final JPanel contenido = new JPanel(cards);
    private final VistaIva vistaIva = new VistaIva();
    private final VistaTributo vistaGanancias = new VistaTributo(Tributo.GANANCIAS);
    private final VistaTributo vistaSuss = new VistaTributo(Tributo.SUSS);
    private PosicionIva ultimaPosicion;

    public PanelImpuestos(AppContext ctx) {
        super(ctx, "Impuestos", true);

        Componentes.Boton copiar = Componentes.secundario("Copiar resumen");
        copiar.addActionListener(e -> copiarResumen());
        agregarAccion(copiar);

        Componentes.Segmentado segmentado = new Componentes.Segmentado("IVA", "Ganancias", "SUSS");
        segmentado.setAlCambiar(i -> cards.show(contenido, String.valueOf(i)));

        contenido.setOpaque(false);
        contenido.add(vistaIva, "0");
        contenido.add(vistaGanancias, "1");
        contenido.add(vistaSuss, "2");

        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(segmentado, BorderLayout.WEST);
        norte.setBorder(new EmptyBorder(0, 0, 0, 0));

        cuerpo.add(norte, BorderLayout.NORTH);
        cuerpo.add(contenido, BorderLayout.CENTER);
    }

    @Override
    protected void cargarDatos() {
        setSubtitulo(contextoTexto());
        ultimaPosicion = ctx.impuestos().determinarIva(ctx.cuitActivo(), ctx.getPeriodo());
        vistaIva.cargar(ultimaPosicion);
        vistaGanancias.cargar();
        vistaSuss.cargar();
    }

    private void copiarResumen() {
        if (ultimaPosicion == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("Determinacion de IVA - ").append(ctx.getClienteActivo().getRazonSocial()).append("\n");
        sb.append("CUIT ").append(ctx.cuitActivo()).append(" - periodo ")
                .append(TopBar.formatear(ctx.getPeriodo())).append("\n\n");
        sb.append("Debito fiscal: ").append(Theme.pesos(ultimaPosicion.debitoFiscal)).append("\n");
        sb.append("Credito fiscal: ").append(Theme.pesos(ultimaPosicion.creditoFiscal)).append("\n");
        sb.append("Saldo tecnico: ").append(Theme.pesos(ultimaPosicion.saldoTecnico())).append("\n");
        sb.append("Ingresos a cuenta: ").append(Theme.pesos(ultimaPosicion.ingresosDirectos())).append("\n");
        sb.append(ultimaPosicion.aPagar() ? "Saldo a pagar: " : "Saldo a favor: ")
                .append(Theme.pesos(ultimaPosicion.saldoFinal().abs())).append("\n");
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(sb.toString()), null);
        Componentes.aviso(this, "Resumen copiado al portapapeles.");
    }

    /* ================================================================
     *  Vista de IVA
     * ================================================================ */
    private class VistaIva extends JPanel {
        private final Componentes.TarjetaMetrica mDebito =
                new Componentes.TarjetaMetrica("Debito fiscal (ventas)", Theme.ROJO);
        private final Componentes.TarjetaMetrica mCredito =
                new Componentes.TarjetaMetrica("Credito fiscal (compras)", Theme.VERDE);
        private final Componentes.TarjetaMetrica mIngresos =
                new Componentes.TarjetaMetrica("Percepciones y retenciones", Theme.AZUL);

        private final JLabel lblRotuloSaldo = new JLabel("Saldo del periodo");
        private final JLabel lblSaldo = new JLabel("$ 0,00");
        private final JLabel lblDetalleSaldo = new JLabel(" ");

        private final DefaultTableModel modelo = new DefaultTableModel(
                new String[]{"Concepto", "Como se calcula", "Importe"}, 0) {
            @Override public boolean isCellEditable(int f, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return c == 2 ? BigDecimal.class : String.class; }
        };
        private final Set<Integer> destacadas = new HashSet<>();
        private final JTable tabla = new JTable(modelo) {
            @Override
            public Component prepareRenderer(TableCellRenderer r, int fila, int col) {
                Component c = super.prepareRenderer(r, fila, col);
                if (destacadas.contains(fila)) {
                    c.setFont(Theme.BODY_BOLD);
                    if (!isRowSelected(fila)) c.setBackground(Theme.FONDO);
                }
                return c;
            }
        };

        VistaIva() {
            super(new BorderLayout(0, 16));
            setOpaque(false);

            JPanel metricas = new JPanel(new GridLayout(1, 3, 14, 0));
            metricas.setOpaque(false);
            metricas.add(mDebito);
            metricas.add(mCredito);
            metricas.add(mIngresos);

            Theme.estilizarTabla(tabla);
            tabla.getColumnModel().getColumn(0).setCellRenderer(new Componentes.RenderTexto(SwingConstants.LEFT));
            tabla.getColumnModel().getColumn(1).setCellRenderer(new Componentes.RenderTexto(SwingConstants.LEFT));
            tabla.getColumnModel().getColumn(2).setCellRenderer(new Componentes.RenderMoneda());
            Componentes.anchos(tabla, 2.2, 3.4, 1.6);

            JPanel tarjetaTabla = Theme.tarjeta();
            tarjetaTabla.setBorder(new Theme.LineaBorder(Theme.BORDE, 1, 4));
            tarjetaTabla.add(Theme.scroll(tabla), BorderLayout.CENTER);

            JPanel centro = new JPanel(new BorderLayout(0, 14));
            centro.setOpaque(false);
            centro.add(metricas, BorderLayout.NORTH);
            centro.add(tarjetaTabla, BorderLayout.CENTER);
            centro.add(tarjetaSaldo(), BorderLayout.SOUTH);

            add(centro, BorderLayout.CENTER);
        }

        private JComponent tarjetaSaldo() {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(Color.WHITE);
            p.setBorder(new javax.swing.border.CompoundBorder(
                    new Theme.LineaBorder(Theme.BORDE, 1, 4), new EmptyBorder(18, 22, 18, 22)));

            lblRotuloSaldo.setFont(Theme.H3);
            lblDetalleSaldo.setFont(Theme.CHICA);
            lblDetalleSaldo.setForeground(Theme.TEXTO_SEC);
            lblSaldo.setFont(new Font(Theme.FAMILIA, Font.BOLD, 30));
            lblSaldo.setHorizontalAlignment(SwingConstants.RIGHT);

            JPanel izq = new JPanel();
            izq.setOpaque(false);
            izq.setLayout(new BoxLayout(izq, BoxLayout.Y_AXIS));
            lblRotuloSaldo.setAlignmentX(Component.LEFT_ALIGNMENT);
            lblDetalleSaldo.setAlignmentX(Component.LEFT_ALIGNMENT);
            izq.add(lblRotuloSaldo);
            izq.add(Box.createVerticalStrut(3));
            izq.add(lblDetalleSaldo);

            p.add(izq, BorderLayout.WEST);
            p.add(lblSaldo, BorderLayout.EAST);
            return p;
        }

        void cargar(PosicionIva pos) {
            mDebito.setValor(pos.debitoFiscal);
            mDebito.setPie(pos.cantidadVentas + " ventas · neto " + Theme.pesos(pos.netoVentas));
            mCredito.setValor(pos.creditoFiscal);
            mCredito.setPie(pos.cantidadCompras + " compras · neto " + Theme.pesos(pos.netoCompras));
            mIngresos.setValor(pos.ingresosDirectos());
            mIngresos.setPie("se descuentan del saldo tecnico");

            modelo.setRowCount(0);
            destacadas.clear();
            modelo.addRow(new Object[]{"Debito fiscal", "IVA de las ventas del periodo", pos.debitoFiscal});
            modelo.addRow(new Object[]{"Credito fiscal", "IVA de las compras del periodo",
                    pos.creditoFiscal.negate()});
            modelo.addRow(new Object[]{"Saldo tecnico", "Debito menos credito", pos.saldoTecnico()});
            destacadas.add(2);
            modelo.addRow(new Object[]{"Percepciones sufridas", "Regimenes de percepcion de IVA",
                    pos.percepcionesSufridas.negate()});
            modelo.addRow(new Object[]{"Retenciones sufridas", "Regimenes de retencion de IVA",
                    pos.retencionesSufridas.negate()});
            modelo.addRow(new Object[]{"Saldo a favor anterior", "Arrastre del periodo previo",
                    pos.saldoPeriodoAnterior.negate()});
            modelo.addRow(new Object[]{pos.aPagar() ? "Saldo a pagar" : "Saldo a favor",
                    "Resultado de la determinacion", pos.saldoFinal()});
            destacadas.add(6);

            BigDecimal saldo = pos.saldoFinal();
            boolean aPagar = pos.aPagar();
            lblRotuloSaldo.setText(aPagar ? "Saldo a pagar" : "Saldo a favor del contribuyente");
            lblSaldo.setText(Theme.pesos(saldo.abs()));
            lblSaldo.setForeground(aPagar ? Theme.ROJO : Theme.VERDE);
            lblDetalleSaldo.setText(aPagar
                    ? "Se ingresa con el F.2002 del periodo " + TopBar.formatear(pos.periodo)
                    : "Queda como credito para el periodo siguiente");
        }
    }

    /* ================================================================
     *  Vista de Ganancias y SUSS
     * ================================================================ */
    private class VistaTributo extends JPanel {
        private final Tributo tributo;
        private final DefaultTableModel modelo = new DefaultTableModel(
                new String[]{"Concepto", "Detalle", "Base imponible", "Alicuota",
                        "Determinado", "Ingresos a cuenta", "A pagar"}, 0) {
            @Override public boolean isCellEditable(int f, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return c >= 2 ? BigDecimal.class : String.class; }
        };
        private final JTable tabla = new JTable(modelo);
        private final Componentes.TablaConTotales contenedor;
        private final CardLayout cl = new CardLayout();
        private final JPanel swap = new JPanel(cl);
        private final JLabel lblNoInscripto = new JLabel("", SwingConstants.CENTER);

        VistaTributo(Tributo tributo) {
            super(new BorderLayout());
            this.tributo = tributo;
            setOpaque(false);

            contenedor = new Componentes.TablaConTotales(tabla);
            contenedor.setMensajeVacio("No hay nada determinado para este periodo.");
            tabla.getColumnModel().getColumn(0).setCellRenderer(new Componentes.RenderTexto(SwingConstants.LEFT));
            tabla.getColumnModel().getColumn(1).setCellRenderer(new Componentes.RenderTexto(SwingConstants.LEFT));
            tabla.getColumnModel().getColumn(2).setCellRenderer(new Componentes.RenderMoneda());
            tabla.getColumnModel().getColumn(3).setCellRenderer(new Componentes.RenderTexto(SwingConstants.RIGHT));
            for (int i = 4; i <= 6; i++) {
                tabla.getColumnModel().getColumn(i).setCellRenderer(new Componentes.RenderMoneda());
            }
            Componentes.anchos(tabla, 2.2, 2.4, 1.6, 0.9, 1.5, 1.6, 1.5);

            JPanel tarjeta = Theme.tarjeta();
            tarjeta.setBorder(new Theme.LineaBorder(Theme.BORDE, 1, 4));
            tarjeta.add(contenedor, BorderLayout.CENTER);

            JPanel aviso = Theme.tarjeta();
            aviso.setLayout(new GridBagLayout());
            lblNoInscripto.setFont(Theme.BODY);
            lblNoInscripto.setForeground(Theme.TEXTO_SEC);
            aviso.add(lblNoInscripto);

            swap.setOpaque(false);
            swap.add(tarjeta, "datos");
            swap.add(aviso, "aviso");
            add(swap, BorderLayout.CENTER);
        }

        void cargar() {
            Cliente cliente = ctx.getClienteActivo();
            boolean inscripto = cliente != null && cliente.getImpuestos().contains(tributo.rotulo);
            if (tributo == Tributo.SUSS && cliente != null && !cliente.isTieneEmpleados()) {
                inscripto = false;
                lblNoInscripto.setText("El cliente no esta inscripto como empleador, "
                        + "asi que no corresponde determinar SUSS.");
            } else if (!inscripto) {
                lblNoInscripto.setText("El cliente no tiene " + tributo.rotulo
                        + " entre sus impuestos. Podes agregarlo desde la ficha del cliente.");
            }
            if (!inscripto) {
                cl.show(swap, "aviso");
                return;
            }
            cl.show(swap, "datos");

            List<ResumenTributo> filas = ctx.impuestos()
                    .determinar(ctx.cuitActivo(), ctx.getPeriodo(), tributo);
            modelo.setRowCount(0);
            BigDecimal determinado = BigDecimal.ZERO;
            BigDecimal aCuenta = BigDecimal.ZERO;
            BigDecimal aPagar = BigDecimal.ZERO;
            for (ResumenTributo r : filas) {
                modelo.addRow(new Object[]{r.concepto, r.detalle, r.baseImponible,
                        Theme.numero(r.alicuota) + " %", r.determinado, r.ingresosACuenta, r.aPagar()});
                determinado = determinado.add(Modelos.nz(r.determinado));
                aCuenta = aCuenta.add(Modelos.nz(r.ingresosACuenta));
                aPagar = aPagar.add(r.aPagar());
            }
            contenedor.setTotales("Totales", null, null, null,
                    Modelos.dosDecimales(determinado), Modelos.dosDecimales(aCuenta),
                    Modelos.dosDecimales(aPagar));
            contenedor.actualizarEstadoVacio();
        }
    }
}