package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Widgets reutilizables con la estetica de la aplicacion. */
public final class Componentes {

    private Componentes() { }

    /* ================================================================
     *  Botones
     * ================================================================ */
    public enum EstiloBoton { PRIMARIO, SECUNDARIO, TEXTO, PELIGRO }

    public static class Boton extends JButton {
        private final EstiloBoton estilo;
        private boolean hover;

        public Boton(String texto, EstiloBoton estilo) {
            super(texto);
            this.estilo = estilo;
            setFont(Theme.BODY_BOLD);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setBorder(new EmptyBorder(9, 18, 9, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setForeground(estilo == EstiloBoton.PRIMARIO ? Color.WHITE
                    : estilo == EstiloBoton.PELIGRO ? Theme.ROJO
                    : estilo == EstiloBoton.TEXTO ? Theme.AZUL : Theme.TEXTO);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            RoundRectangle2D forma = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, 4, 4);

            switch (estilo) {
                case PRIMARIO:
                    g2.setColor(!isEnabled() ? new Color(0xB9D9B3) : hover ? Theme.VERDE_HOVER : Theme.VERDE);
                    g2.fill(forma);
                    break;
                case SECUNDARIO:
                    g2.setColor(hover ? Theme.FONDO : Color.WHITE);
                    g2.fill(forma);
                    g2.setColor(Theme.BORDE);
                    g2.draw(forma);
                    break;
                case PELIGRO:
                    g2.setColor(hover ? new Color(0xFDEDEC) : Color.WHITE);
                    g2.fill(forma);
                    g2.setColor(Theme.ROJO);
                    g2.draw(forma);
                    break;
                case TEXTO:
                default:
                    if (hover) {
                        g2.setColor(new Color(0xEDF5FB));
                        g2.fill(forma);
                    }
                    break;
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static Boton primario(String texto) { return new Boton(texto, EstiloBoton.PRIMARIO); }
    public static Boton secundario(String texto) { return new Boton(texto, EstiloBoton.SECUNDARIO); }
    public static Boton enlace(String texto) { return new Boton(texto, EstiloBoton.TEXTO); }
    public static Boton peligro(String texto) { return new Boton(texto, EstiloBoton.PELIGRO); }

    /* ================================================================
     *  Campos de texto
     * ================================================================ */
    public static class CampoTexto extends JTextField {
        private String marcador = "";

        public CampoTexto() { this(""); }

        public CampoTexto(String marcador) {
            this.marcador = marcador;
            setFont(Theme.BODY);
            setForeground(Theme.TEXTO);
            setBackground(Color.WHITE);
            setBorder(new javax.swing.border.CompoundBorder(
                    new Theme.LineaBorder(Theme.BORDE, 1, 4), new EmptyBorder(7, 10, 7, 10)));
        }

        public void setMarcador(String marcador) {
            this.marcador = marcador;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !marcador.isEmpty() && !hasFocus()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(Theme.TEXTO_SEC);
                g2.setFont(getFont());
                Insets in = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(marcador, in.left, in.top + fm.getAscent());
                g2.dispose();
            }
        }
    }

    /** Campo de busqueda con lupa dibujada. */
    public static class CampoBusqueda extends CampoTexto {
        public CampoBusqueda(String marcador) {
            super(marcador);
            setBorder(new javax.swing.border.CompoundBorder(
                    new Theme.LineaBorder(Theme.BORDE, 1, 4), new EmptyBorder(7, 30, 7, 10)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.TEXTO_SEC);
            g2.setStroke(new BasicStroke(1.6f));
            int cy = getHeight() / 2;
            g2.drawOval(10, cy - 6, 9, 9);
            g2.drawLine(18, cy + 3, 22, cy + 7);
            g2.dispose();
        }
    }

    public static JComboBox<String> combo(String[] items) {
        JComboBox<String> c = new JComboBox<>(items);
        estilizarCombo(c);
        return c;
    }

    public static void estilizarCombo(JComboBox<?> c) {
        c.setFont(Theme.BODY);
        c.setBackground(Color.WHITE);
        c.setForeground(Theme.TEXTO);
        c.setBorder(new Theme.LineaBorder(Theme.BORDE, 1, 4));
        ((JComponent) c.getRenderer()).setBorder(new EmptyBorder(4, 8, 4, 8));
    }

    /* ================================================================
     *  Tarjeta de metrica
     * ================================================================ */
    public static class TarjetaMetrica extends JPanel {
        private final JLabel lblValor = new JLabel("-");
        private final JLabel lblPie = new JLabel(" ");
        private Color acento;

        public TarjetaMetrica(String rotulo, Color acento) {
            super(new BorderLayout());
            this.acento = acento;
            setBackground(Color.WHITE);
            setBorder(new javax.swing.border.CompoundBorder(
                    new Theme.LineaBorder(Theme.BORDE, 1, 4), new EmptyBorder(16, 18, 16, 18)));

            JLabel lblRotulo = new JLabel(rotulo);
            lblRotulo.setFont(Theme.CHICA);
            lblRotulo.setForeground(Theme.TEXTO_SEC);

            lblValor.setFont(Theme.CIFRA);
            lblValor.setForeground(acento);
            lblValor.setBorder(new EmptyBorder(6, 0, 2, 0));

            lblPie.setFont(Theme.CHICA);
            lblPie.setForeground(Theme.TEXTO_SEC);

            JPanel centro = new JPanel();
            centro.setOpaque(false);
            centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
            lblRotulo.setAlignmentX(LEFT_ALIGNMENT);
            lblValor.setAlignmentX(LEFT_ALIGNMENT);
            lblPie.setAlignmentX(LEFT_ALIGNMENT);
            centro.add(lblRotulo);
            centro.add(lblValor);
            centro.add(lblPie);
            add(centro, BorderLayout.CENTER);
        }

        public void setValor(String valor) { lblValor.setText(valor); }

        public void setValor(BigDecimal valor) { lblValor.setText(Theme.pesos(valor)); }

        public void setPie(String pie) { lblPie.setText(pie == null || pie.isEmpty() ? " " : pie); }

        public void setAcento(Color c) {
            this.acento = c;
            lblValor.setForeground(c);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(acento);
            g2.fillRect(1, 1, 3, getHeight() - 2);
            g2.dispose();
        }
    }

    /* ================================================================
     *  Renderers de tabla
     * ================================================================ */
    public static class RenderCabecera extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean foco, int fila, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foco, fila, col);
            l.setFont(Theme.CHICA_BOLD);
            l.setForeground(Theme.TEXTO_SEC);
            l.setBackground(Theme.FONDO);
            l.setOpaque(true);
            l.setBorder(new EmptyBorder(0, 12, 0, 12));
            l.setHorizontalAlignment(alineacionDe(t, col));
            return l;
        }

        private int alineacionDe(JTable t, int col) {
            TableCellRenderer r = t.getColumnModel().getColumn(col).getCellRenderer();
            return (r instanceof RenderMoneda) ? SwingConstants.RIGHT : SwingConstants.LEFT;
        }
    }

    public static class RenderTexto extends DefaultTableCellRenderer {
        private final int alineacion;

        public RenderTexto(int alineacion) { this.alineacion = alineacion; }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean foco, int fila, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foco, fila, col);
            l.setHorizontalAlignment(alineacion);
            l.setBorder(new EmptyBorder(0, 12, 0, 12));
            if (!sel) {
                l.setBackground(fila % 2 == 0 ? Color.WHITE : Theme.FILA_ALT);
                l.setForeground(Theme.TEXTO);
            }
            return l;
        }
    }

    /** Importes alineados a la derecha; negativos en rojo. */
    public static class RenderMoneda extends DefaultTableCellRenderer {
        private final boolean resaltarNegativos;

        public RenderMoneda() { this(true); }

        public RenderMoneda(boolean resaltarNegativos) { this.resaltarNegativos = resaltarNegativos; }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean foco, int fila, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foco, fila, col);
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            l.setBorder(new EmptyBorder(0, 12, 0, 12));
            BigDecimal valor = (v instanceof BigDecimal) ? (BigDecimal) v : null;
            l.setText(valor == null ? (v == null ? "" : v.toString()) : Theme.pesos(valor));
            if (!sel) {
                l.setBackground(fila % 2 == 0 ? Color.WHITE : Theme.FILA_ALT);
                boolean negativo = valor != null && valor.signum() < 0;
                l.setForeground(resaltarNegativos && negativo ? Theme.ROJO : Theme.TEXTO);
            }
            return l;
        }
    }

    /** Etiqueta tipo "pill" para tipos de comprobante y origen del dato. */
    public static class RenderPill extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean foco, int fila, int col) {
            super.getTableCellRendererComponent(t, v, sel, foco, fila, col);
            final String texto = v == null ? "" : v.toString();
            JPanel cont = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
                @Override public void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setFont(Theme.CHICA_BOLD);
                    FontMetrics fm = g2.getFontMetrics();
                    int ancho = fm.stringWidth(texto) + 18;
                    int alto = 21;
                    int y = (getHeight() - alto) / 2;
                    Color fondo = colorDe(texto);
                    g2.setColor(fondo);
                    g2.fillRoundRect(12, y, ancho, alto, 11, 11);
                    g2.setColor(textoDe(texto));
                    g2.drawString(texto, 12 + 9, y + (alto + fm.getAscent()) / 2 - 2);
                    g2.dispose();
                }
            };
            cont.setBackground(sel ? Theme.SELECCION : (fila % 2 == 0 ? Color.WHITE : Theme.FILA_ALT));
            return cont;
        }

        private Color colorDe(String texto) {
            String t = texto.toLowerCase();
            if (t.contains("credito")) return new Color(0xFDECEA);
            if (t.contains("debito")) return new Color(0xFFF4DB);
            if (t.startsWith("pdf") || t.contains("afip")) return new Color(0xE7F1FA);
            if (t.equals("csv") || t.equals("excel")) return new Color(0xEDEEF1);
            return Theme.VERDE_SUAVE;
        }

        private Color textoDe(String texto) {
            String t = texto.toLowerCase();
            if (t.contains("credito")) return new Color(0xA5251B);
            if (t.contains("debito")) return new Color(0x8A6100);
            if (t.startsWith("pdf") || t.contains("afip")) return new Color(0x0F5C9C);
            if (t.equals("csv") || t.equals("excel")) return Theme.TEXTO_SEC;
            return new Color(0x1C6C13);
        }
    }

    /* ================================================================
     *  Tabla con fila de totales fija
     * ================================================================ */
    public static class TablaConTotales extends JPanel {
        private final JTable tabla;
        private final JTable filaTotales;
        private final DefaultTableModel modeloTotales;
        private final JPanel vacio;
        private final CardLayout cards = new CardLayout();
        private final JPanel contenido = new JPanel(cards);
        private final JLabel lblVacio = new JLabel("", SwingConstants.CENTER);

        public TablaConTotales(JTable tabla) {
            super(new BorderLayout());
            this.tabla = tabla;
            setBackground(Color.WHITE);
            Theme.estilizarTabla(tabla);

            int columnas = tabla.getColumnModel().getColumnCount();
            modeloTotales = new DefaultTableModel(1, columnas) {
                @Override public boolean isCellEditable(int f, int c) { return false; }
                @Override public Class<?> getColumnClass(int c) { return Object.class; }
            };

            filaTotales = new JTable(modeloTotales, tabla.getColumnModel()) {
                @Override
                public Component prepareRenderer(TableCellRenderer r, int fila, int col) {
                    Component c = super.prepareRenderer(r, fila, col);
                    c.setFont(Theme.BODY_BOLD);
                    c.setForeground(Theme.TEXTO);
                    c.setBackground(Theme.FONDO);
                    if (c instanceof JComponent) ((JComponent) c).setOpaque(true);
                    return c;
                }
            };
            filaTotales.setTableHeader(null);
            filaTotales.setRowHeight(42);
            filaTotales.setShowGrid(false);
            filaTotales.setBackground(Theme.FONDO);
            filaTotales.setRowSelectionAllowed(false);
            filaTotales.setFocusable(false);
            filaTotales.setBorder(new javax.swing.border.MatteBorder(1, 0, 0, 0, Theme.BORDE));
            filaTotales.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

            vacio = new JPanel(new GridBagLayout());
            vacio.setBackground(Color.WHITE);
            lblVacio.setFont(Theme.BODY);
            lblVacio.setForeground(Theme.TEXTO_SEC);
            vacio.add(lblVacio);

            contenido.add(Theme.scroll(tabla), "tabla");
            contenido.add(vacio, "vacio");

            add(contenido, BorderLayout.CENTER);
            add(filaTotales, BorderLayout.SOUTH);
        }

        public JTable getTabla() { return tabla; }

        /** Texto que se muestra cuando no hay filas: siempre indica el proximo paso. */
        public void setMensajeVacio(String mensaje) { lblVacio.setText(mensaje); }

        public void setTotales(Object... valores) {
            for (int c = 0; c < modeloTotales.getColumnCount(); c++) {
                modeloTotales.setValueAt(c < valores.length ? valores[c] : null, 0, c);
            }
        }

        public void actualizarEstadoVacio() {
            boolean hayFilas = tabla.getRowCount() > 0;
            cards.show(contenido, hayFilas ? "tabla" : "vacio");
            filaTotales.setVisible(hayFilas);
            revalidate();
            repaint();
        }
    }

    /** Ajusta anchos relativos de columnas: pesos[i] proporcional. */
    public static void anchos(JTable t, double... pesos) {
        TableColumnModel cm = t.getColumnModel();
        for (int i = 0; i < cm.getColumnCount() && i < pesos.length; i++) {
            cm.getColumn(i).setPreferredWidth((int) (pesos[i] * 100));
        }
    }

    /* ================================================================
     *  Grafico (barras agrupadas / lineas) dibujado a mano
     * ================================================================ */
    public static class Grafico extends JPanel {
        public enum Tipo { BARRAS, LINEAS }

        public static class Serie {
            public final String nombre;
            public final Color color;
            public final List<Double> valores = new ArrayList<>();

            public Serie(String nombre, Color color) {
                this.nombre = nombre;
                this.color = color;
            }
        }

        private Tipo tipo = Tipo.BARRAS;
        private final List<String> etiquetas = new ArrayList<>();
        private final List<Serie> series = new ArrayList<>();

        public Grafico() {
            setBackground(Color.WHITE);
            setBorder(new EmptyBorder(16, 12, 8, 16));
        }

        public void setTipo(Tipo t) { this.tipo = t; repaint(); }

        public Tipo getTipo() { return tipo; }

        public void setDatos(List<String> etiquetas, List<Serie> series) {
            this.etiquetas.clear();
            this.etiquetas.addAll(etiquetas);
            this.series.clear();
            this.series.addAll(series);
            repaint();
        }

        @Override
        public Dimension getPreferredSize() { return new Dimension(600, 300); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Insets in = getInsets();
            int ancho = getWidth() - in.left - in.right;
            int alto = getHeight() - in.top - in.bottom;

            if (series.isEmpty() || etiquetas.isEmpty() || ancho < 80 || alto < 80) {
                g2.setFont(Theme.BODY);
                g2.setColor(Theme.TEXTO_SEC);
                String m = "Sin datos para el rango elegido";
                g2.drawString(m, in.left + (ancho - g2.getFontMetrics().stringWidth(m)) / 2,
                        in.top + alto / 2);
                g2.dispose();
                return;
            }

            int margenIzq = 78, margenInf = 34, margenSup = 34;
            int x0 = in.left + margenIzq;
            int y0 = in.top + margenSup;
            int w = ancho - margenIzq - 8;
            int h = alto - margenSup - margenInf;

            double max = 0;
            for (Serie s : series) {
                for (Double v : s.valores) {
                    if (v != null) max = Math.max(max, Math.abs(v));
                }
            }
            if (max <= 0) max = 1;
            double paso = escalaBonita(max / 4.0);
            double tope = Math.ceil(max / paso) * paso;

            // Grilla horizontal y eje Y
            g2.setFont(Theme.CHICA);
            for (double v = 0; v <= tope + 0.001; v += paso) {
                int y = y0 + h - (int) (h * (v / tope));
                g2.setColor(Theme.BORDE_SUAVE);
                g2.drawLine(x0, y, x0 + w, y);
                g2.setColor(Theme.TEXTO_SEC);
                String rot = abreviar(v);
                g2.drawString(rot, x0 - 10 - g2.getFontMetrics().stringWidth(rot), y + 4);
            }

            int n = etiquetas.size();
            double anchoGrupo = (double) w / n;

            if (tipo == Tipo.BARRAS) {
                int cantidad = series.size();
                double anchoBarra = Math.max(6, Math.min(34, (anchoGrupo * 0.62) / cantidad));
                for (int i = 0; i < n; i++) {
                    double centro = x0 + anchoGrupo * (i + 0.5);
                    double inicio = centro - (anchoBarra * cantidad) / 2.0;
                    for (int s = 0; s < cantidad; s++) {
                        Serie serie = series.get(s);
                        double v = valor(serie, i);
                        int altoBarra = (int) (h * (Math.abs(v) / tope));
                        int bx = (int) (inicio + s * anchoBarra);
                        int by = y0 + h - altoBarra;
                        g2.setColor(serie.color);
                        g2.fillRoundRect(bx, by, (int) anchoBarra - 2, Math.max(altoBarra, 1), 3, 3);
                    }
                }
            } else {
                for (Serie serie : series) {
                    GeneralPath path = new GeneralPath(Path2D.WIND_NON_ZERO);
                    for (int i = 0; i < n; i++) {
                        double v = valor(serie, i);
                        float px = (float) (x0 + anchoGrupo * (i + 0.5));
                        float py = (float) (y0 + h - h * (Math.abs(v) / tope));
                        if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
                    }
                    g2.setColor(serie.color);
                    g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.draw(path);
                    for (int i = 0; i < n; i++) {
                        double v = valor(serie, i);
                        int px = (int) (x0 + anchoGrupo * (i + 0.5));
                        int py = (int) (y0 + h - h * (Math.abs(v) / tope));
                        g2.setColor(Color.WHITE);
                        g2.fillOval(px - 4, py - 4, 8, 8);
                        g2.setColor(serie.color);
                        g2.setStroke(new BasicStroke(2f));
                        g2.drawOval(px - 4, py - 4, 8, 8);
                    }
                }
            }

            // Eje X
            g2.setColor(Theme.BORDE);
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(x0, y0 + h, x0 + w, y0 + h);
            g2.setColor(Theme.TEXTO_SEC);
            g2.setFont(Theme.CHICA);
            int saltar = (int) Math.max(1, Math.ceil(n / (w / 62.0)));
            for (int i = 0; i < n; i++) {
                if (i % saltar != 0) continue;
                String rot = etiquetas.get(i);
                int px = (int) (x0 + anchoGrupo * (i + 0.5));
                g2.drawString(rot, px - g2.getFontMetrics().stringWidth(rot) / 2, y0 + h + 20);
            }

            // Leyenda
            int lx = x0 + w;
            g2.setFont(Theme.CHICA);
            for (int i = series.size() - 1; i >= 0; i--) {
                Serie s = series.get(i);
                int anchoTexto = g2.getFontMetrics().stringWidth(s.nombre);
                lx -= anchoTexto + 26;
                g2.setColor(s.color);
                g2.fillRoundRect(lx, in.top + 8, 10, 10, 2, 2);
                g2.setColor(Theme.TEXTO_SEC);
                g2.drawString(s.nombre, lx + 16, in.top + 17);
            }

            g2.dispose();
        }

        private double valor(Serie s, int i) {
            if (i >= s.valores.size() || s.valores.get(i) == null) return 0;
            return s.valores.get(i);
        }

        private double escalaBonita(double crudo) {
            double exp = Math.floor(Math.log10(crudo <= 0 ? 1 : crudo));
            double base = Math.pow(10, exp);
            double norm = crudo / base;
            double mult = norm <= 1 ? 1 : norm <= 2 ? 2 : norm <= 5 ? 5 : 10;
            return mult * base;
        }

        private String abreviar(double v) {
            if (Math.abs(v) >= 1_000_000) return String.format("%.1f M", v / 1_000_000).replace('.', ',');
            if (Math.abs(v) >= 1_000) return String.format("%.0f k", v / 1_000);
            return String.format("%.0f", v);
        }
    }

    /* ================================================================
     *  Iconos vectoriales para la navegacion
     * ================================================================ */
    public static class Icono implements Icon {
        private final String tipo;
        private final int tam;
        private Color color;

        public Icono(String tipo, int tam, Color color) {
            this.tipo = tipo;
            this.tam = tam;
            this.color = color;
        }

        public void setColor(Color c) { this.color = c; }

        @Override public int getIconWidth() { return tam; }

        @Override public int getIconHeight() { return tam; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int s = tam;
            switch (tipo) {
                case "inicio": {
                    GeneralPath p = new GeneralPath();
                    p.moveTo(s * 0.5, s * 0.12);
                    p.lineTo(s * 0.92, s * 0.46);
                    p.lineTo(s * 0.08, s * 0.46);
                    p.closePath();
                    g2.fill(p);
                    g2.drawRect((int) (s * 0.2), (int) (s * 0.46), (int) (s * 0.6), (int) (s * 0.42));
                    break;
                }
                case "clientes": {
                    g2.drawOval((int) (s * 0.3), (int) (s * 0.12), (int) (s * 0.4), (int) (s * 0.4));
                    g2.drawArc((int) (s * 0.12), (int) (s * 0.56), (int) (s * 0.76), (int) (s * 0.6), 0, 180);
                    break;
                }
                case "ventas":
                case "compras": {
                    boolean arriba = "ventas".equals(tipo);
                    int cx = s / 2;
                    g2.drawLine(cx, (int) (s * 0.15), cx, (int) (s * 0.85));
                    if (arriba) {
                        g2.drawLine(cx, (int) (s * 0.15), (int) (s * 0.25), (int) (s * 0.42));
                        g2.drawLine(cx, (int) (s * 0.15), (int) (s * 0.75), (int) (s * 0.42));
                    } else {
                        g2.drawLine(cx, (int) (s * 0.85), (int) (s * 0.25), (int) (s * 0.58));
                        g2.drawLine(cx, (int) (s * 0.85), (int) (s * 0.75), (int) (s * 0.58));
                    }
                    break;
                }
                case "percepciones": {
                    g2.drawOval((int) (s * 0.14), (int) (s * 0.14), (int) (s * 0.26), (int) (s * 0.26));
                    g2.drawOval((int) (s * 0.6), (int) (s * 0.6), (int) (s * 0.26), (int) (s * 0.26));
                    g2.drawLine((int) (s * 0.82), (int) (s * 0.16), (int) (s * 0.18), (int) (s * 0.84));
                    break;
                }
                case "impuestos": {
                    g2.drawRect((int) (s * 0.18), (int) (s * 0.1), (int) (s * 0.64), (int) (s * 0.8));
                    g2.drawLine((int) (s * 0.32), (int) (s * 0.32), (int) (s * 0.68), (int) (s * 0.32));
                    g2.drawLine((int) (s * 0.32), (int) (s * 0.5), (int) (s * 0.68), (int) (s * 0.5));
                    g2.drawLine((int) (s * 0.32), (int) (s * 0.68), (int) (s * 0.55), (int) (s * 0.68));
                    break;
                }
                case "reportes": {
                    g2.fillRect((int) (s * 0.16), (int) (s * 0.52), (int) (s * 0.16), (int) (s * 0.34));
                    g2.fillRect((int) (s * 0.42), (int) (s * 0.28), (int) (s * 0.16), (int) (s * 0.58));
                    g2.fillRect((int) (s * 0.68), (int) (s * 0.14), (int) (s * 0.16), (int) (s * 0.72));
                    break;
                }
                case "maestros":
                default: {
                    GeneralPath p = new GeneralPath();
                    p.moveTo(s * 0.1, s * 0.26);
                    p.lineTo(s * 0.42, s * 0.26);
                    p.lineTo(s * 0.5, s * 0.38);
                    p.lineTo(s * 0.9, s * 0.38);
                    p.lineTo(s * 0.9, s * 0.82);
                    p.lineTo(s * 0.1, s * 0.82);
                    p.closePath();
                    g2.draw(p);
                    break;
                }
            }
            g2.dispose();
        }
    }

    /* ================================================================
     *  Control segmentado (alternativa a las solapas de Swing)
     * ================================================================ */
    public static class Segmentado extends JPanel {
        private final List<JLabel> opciones = new ArrayList<>();
        private int seleccion = 0;
        private java.util.function.IntConsumer alCambiar;

        public Segmentado(String... etiquetas) {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setOpaque(false);
            for (int i = 0; i < etiquetas.length; i++) {
                final int indice = i;
                JLabel l = new JLabel(etiquetas[i], SwingConstants.CENTER) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                        boolean activo = indice == seleccion;
                        g2.setColor(activo ? Color.WHITE : Theme.FONDO);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        g2.setColor(Theme.BORDE);
                        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                        if (activo) {
                            g2.setColor(Theme.VERDE);
                            g2.fillRect(0, getHeight() - 3, getWidth(), 3);
                        }
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                l.setFont(Theme.BODY_BOLD);
                l.setForeground(Theme.TEXTO_SEC);
                l.setBorder(new EmptyBorder(9, 20, 9, 20));
                l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                l.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { seleccionar(indice); }
                });
                opciones.add(l);
                add(l);
            }
            refrescarColores();
        }

        public void setAlCambiar(java.util.function.IntConsumer alCambiar) {
            this.alCambiar = alCambiar;
        }

        public int getSeleccion() { return seleccion; }

        public void seleccionar(int indice) {
            if (indice < 0 || indice >= opciones.size()) return;
            seleccion = indice;
            refrescarColores();
            if (alCambiar != null) alCambiar.accept(indice);
        }

        private void refrescarColores() {
            for (int i = 0; i < opciones.size(); i++) {
                JLabel l = opciones.get(i);
                l.setForeground(i == seleccion ? Theme.TEXTO : Theme.TEXTO_SEC);
                l.repaint();
            }
        }
    }

    /* ================================================================
     *  Utilidades de formulario
     * ================================================================ */
    public static GridBagConstraints gbc(int x, int y, int ancho, double pesoX) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = ancho;
        c.weightx = pesoX;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 0, 6, 12);
        return c;
    }

    public static JPanel filaBotones(Component... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        p.setOpaque(false);
        for (Component c : comps) p.add(c);
        return p;
    }

    public static void aviso(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Aviso", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "No se pudo completar", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirmar(Component padre, String mensaje) {
        return JOptionPane.showConfirmDialog(padre, mensaje, "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
}
