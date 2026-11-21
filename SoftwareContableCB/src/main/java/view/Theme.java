package view;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Sistema visual unico de la aplicacion: paleta, tipografia, bordes y helpers.
 * Tomado de la referencia visual de Intuit QuickBooks.
 */
public final class Theme {

    private Theme() { }

    /* ---------------- Paleta ---------------- */
    public static final Color VERDE        = new Color(0x2CA01C); // accion primaria
    public static final Color VERDE_HOVER  = new Color(0x108000);
    public static final Color VERDE_SUAVE  = new Color(0xE3F1DF);
    public static final Color SIDEBAR      = new Color(0x393A3D); // nav lateral
    public static final Color SIDEBAR_ALT  = new Color(0x4A4B4F);
    public static final Color FONDO        = new Color(0xF4F5F8); // lienzo
    public static final Color BLANCO       = Color.WHITE;
    public static final Color BORDE        = new Color(0xD4D7DC);
    public static final Color BORDE_SUAVE  = new Color(0xE3E5E8);
    public static final Color TEXTO        = new Color(0x393A3D);
    public static final Color TEXTO_SEC    = new Color(0x6B6C72);
    public static final Color AZUL         = new Color(0x0077C5); // enlaces / dato secundario
    public static final Color ROJO         = new Color(0xD52B1E); // saldo a pagar / errores
    public static final Color AMBAR        = new Color(0xFFBF3F);
    public static final Color FILA_ALT     = new Color(0xFAFAFB);
    public static final Color SELECCION    = new Color(0xE8F3E5);

    /* ---------------- Tipografia ---------------- */
    public static final String FAMILIA = elegirFamilia();

    public static final Font H1        = new Font(FAMILIA, Font.BOLD,  24);
    public static final Font H2        = new Font(FAMILIA, Font.BOLD,  17);
    public static final Font H3        = new Font(FAMILIA, Font.BOLD,  14);
    public static final Font CIFRA     = new Font(FAMILIA, Font.BOLD,  26);
    public static final Font BODY      = new Font(FAMILIA, Font.PLAIN, 13);
    public static final Font BODY_BOLD = new Font(FAMILIA, Font.BOLD,  13);
    public static final Font CHICA     = new Font(FAMILIA, Font.PLAIN, 12);
    public static final Font CHICA_BOLD= new Font(FAMILIA, Font.BOLD,  12);

    private static String elegirFamilia() {
        String[] preferidas = {"Segoe UI", "Open Sans", "Noto Sans", "Roboto", "Helvetica Neue", "DejaVu Sans"};
        try {
            List<String> disponibles = Arrays.asList(GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            for (String f : preferidas) {
                if (disponibles.contains(f)) return f;
            }
        } catch (Exception ignored) { }
        return "SansSerif";
    }

    /* ---------------- Formato de numeros ---------------- */
    private static final Locale AR = new Locale("es", "AR");
    private static final NumberFormat NUM = NumberFormat.getNumberInstance(AR);
    private static final NumberFormat ENT = NumberFormat.getIntegerInstance(AR);
    static {
        NUM.setMinimumFractionDigits(2);
        NUM.setMaximumFractionDigits(2);
    }

    public static String pesos(BigDecimal v) {
        if (v == null) return "$ 0,00";
        return "$ " + NUM.format(v);
    }

    public static String numero(BigDecimal v) {
        return v == null ? "0,00" : NUM.format(v);
    }

    public static String entero(long v) {
        return ENT.format(v);
    }

    public static BigDecimal parsear(String texto) {
        if (texto == null) return BigDecimal.ZERO;
        String limpio = texto.replace("$", "").replace(" ", "").trim();
        if (limpio.isEmpty()) return BigDecimal.ZERO;
        // admite 1.234,56 y 1234.56
        if (limpio.contains(",")) {
            limpio = limpio.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /* ---------------- Bordes y contenedores ---------------- */
    public static Border padding(int arriba, int der, int abajo, int izq) {
        return new EmptyBorder(arriba, der, abajo, izq);
    }

    public static Border bordeTarjeta() {
        return new CompoundBorder(new LineaBorder(BORDE, 1, 4), new EmptyBorder(18, 20, 18, 20));
    }

    /** Panel blanco con borde fino: el contenedor base de QuickBooks. */
    public static JPanel tarjeta() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BLANCO);
        p.setBorder(bordeTarjeta());
        return p;
    }

    public static JPanel panel(LayoutManager layout, Color fondo) {
        JPanel p = new JPanel(layout);
        p.setBackground(fondo);
        return p;
    }

    public static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(H1);
        l.setForeground(TEXTO);
        return l;
    }

    public static JLabel subtitulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(BODY);
        l.setForeground(TEXTO_SEC);
        return l;
    }

    public static JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(CHICA_BOLD);
        l.setForeground(TEXTO_SEC);
        return l;
    }

    public static JSeparator separador() {
        JSeparator s = new JSeparator();
        s.setForeground(BORDE_SUAVE);
        s.setBackground(BORDE_SUAVE);
        return s;
    }

    /* ---------------- Tablas ---------------- */
    public static void estilizarTabla(JTable t) {
        t.setFont(BODY);
        t.setForeground(TEXTO);
        t.setBackground(BLANCO);
        t.setRowHeight(38);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(BORDE_SUAVE);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(SELECCION);
        t.setSelectionForeground(TEXTO);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(false);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader h = t.getTableHeader();
        if (h != null) {
            h.setFont(CHICA_BOLD);
            h.setForeground(TEXTO_SEC);
            h.setBackground(FONDO);
            h.setPreferredSize(new Dimension(0, 36));
            h.setReorderingAllowed(false);
            h.setBorder(new MatteBorder(0, 0, 1, 0, BORDE));
            h.setDefaultRenderer(new Componentes.RenderCabecera());
        }
    }

    public static JScrollPane scroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(BLANCO);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    /* ---------------- Borde con esquinas redondeadas ---------------- */
    public static class LineaBorder implements Border {
        private final Color color;
        private final int grosor;
        private final int radio;

        public LineaBorder(Color color, int grosor, int radio) {
            this.color = color;
            this.grosor = grosor;
            this.radio = radio;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(grosor));
            g2.drawRoundRect(x + grosor / 2, y + grosor / 2, w - grosor, h - grosor, radio, radio);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(grosor, grosor, grosor, grosor);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    /** Ajustes globales de Swing para que los controles nativos no rompan la estetica. */
    public static void aplicarDefaults() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) { }
        UIManager.put("Panel.background", FONDO);
        UIManager.put("OptionPane.background", BLANCO);
        UIManager.put("OptionPane.messageFont", BODY);
        UIManager.put("OptionPane.buttonFont", BODY_BOLD);
        UIManager.put("Label.font", BODY);
        UIManager.put("Label.foreground", TEXTO);
        UIManager.put("TextField.font", BODY);
        UIManager.put("TextField.caretForeground", TEXTO);
        UIManager.put("ComboBox.font", BODY);
        UIManager.put("ComboBox.background", BLANCO);
        UIManager.put("CheckBox.font", BODY);
        UIManager.put("CheckBox.background", BLANCO);
        UIManager.put("TabbedPane.font", BODY_BOLD);
        UIManager.put("TabbedPane.background", FONDO);
        UIManager.put("TabbedPane.contentAreaColor", BLANCO);
        UIManager.put("Table.font", BODY);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ToolTip.font", CHICA);
    }
}