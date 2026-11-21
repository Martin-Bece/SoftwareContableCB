package view;

import view.Modelos.Comprobante;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Modelo de tabla de comprobantes. Se reutiliza en la grilla y en la previsualizacion de importacion. */
public class ModeloComprobantes extends AbstractTableModel {

    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String[] columnas = {"Fecha", "Tipo", "Comprobante", "CUIT",
            "Razon social", "Neto gravado", "IVA", "Percepciones", "Total", "Origen"};

    private List<Comprobante> datos = new ArrayList<>();

    public void setDatos(List<Comprobante> datos) {
        this.datos = datos == null ? new ArrayList<>() : datos;
        fireTableDataChanged();
    }

    public List<Comprobante> getDatos() { return datos; }

    public Comprobante getEn(int fila) { return datos.get(fila); }

    public void quitar(int fila) {
        datos.remove(fila);
        fireTableDataChanged();
    }

    @Override public int getRowCount() { return datos.size(); }

    @Override public int getColumnCount() { return columnas.length; }

    @Override public String getColumnName(int c) { return columnas[c]; }

    @Override public boolean isCellEditable(int f, int c) { return false; }

    @Override
    public Class<?> getColumnClass(int c) {
        return (c >= 5 && c <= 8) ? BigDecimal.class : String.class;
    }

    @Override
    public Object getValueAt(int fila, int col) {
        Comprobante c = datos.get(fila);
        int signo = c.signo();
        switch (col) {
            case 0: return c.fecha == null ? "" : c.fecha.format(FECHA);
            case 1: return c.tipo;
            case 2: return c.puntoVenta + "-" + c.numero;
            case 3: return c.cuitContraparte;
            case 4: return c.razonSocial;
            case 5: return c.netoGravado().multiply(BigDecimal.valueOf(signo));
            case 6: return c.ivaTotal().multiply(BigDecimal.valueOf(signo));
            case 7: return Modelos.nz(c.percepciones).multiply(BigDecimal.valueOf(signo));
            case 8: return c.total().multiply(BigDecimal.valueOf(signo));
            case 9: return c.origen;
            default: return "";
        }
    }

    /** Aplica los renderers propios de cada columna. */
    public void configurar(JTable t) {
        t.getColumnModel().getColumn(0).setCellRenderer(new Componentes.RenderTexto(SwingConstants.LEFT));
        t.getColumnModel().getColumn(1).setCellRenderer(new Componentes.RenderPill());
        t.getColumnModel().getColumn(2).setCellRenderer(new Componentes.RenderTexto(SwingConstants.LEFT));
        t.getColumnModel().getColumn(3).setCellRenderer(new Componentes.RenderTexto(SwingConstants.LEFT));
        t.getColumnModel().getColumn(4).setCellRenderer(new Componentes.RenderTexto(SwingConstants.LEFT));
        for (int i = 5; i <= 8; i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(new Componentes.RenderMoneda());
        }
        t.getColumnModel().getColumn(9).setCellRenderer(new Componentes.RenderPill());
        Componentes.anchos(t, 1.0, 1.3, 1.4, 1.4, 2.6, 1.4, 1.2, 1.2, 1.4, 1.0);
    }

    /* Totales de la coleccion actual */
    public BigDecimal totalNeto() { return sumar(5); }

    public BigDecimal totalIva() { return sumar(6); }

    public BigDecimal totalPercepciones() { return sumar(7); }

    public BigDecimal totalGeneral() { return sumar(8); }

    private BigDecimal sumar(int col) {
        BigDecimal t = BigDecimal.ZERO;
        for (int f = 0; f < getRowCount(); f++) {
            Object v = getValueAt(f, col);
            if (v instanceof BigDecimal) t = t.add((BigDecimal) v);
        }
        return Modelos.dosDecimales(t);
    }
}