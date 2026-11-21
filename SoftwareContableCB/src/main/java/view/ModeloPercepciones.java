package view;

import view.Modelos.PercepcionRetencion;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Modelo de tabla de percepciones y retenciones sufridas. */
public class ModeloPercepciones extends AbstractTableModel {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String[] columnas = {"Fecha", "Clase", "Tributo", "Regimen", "Jurisdiccion",
            "Comprobante", "CUIT agente", "Agente de recaudacion", "Importe"};

    private List<PercepcionRetencion> datos = new ArrayList<>();

    public void setDatos(List<PercepcionRetencion> datos) {
        this.datos = datos == null ? new ArrayList<>() : datos;
        fireTableDataChanged();
    }

    public List<PercepcionRetencion> getDatos() { return datos; }

    public PercepcionRetencion getEn(int fila) { return datos.get(fila); }

    public void quitar(int fila) {
        datos.remove(fila);
        fireTableDataChanged();
    }

    @Override public int getRowCount() { return datos.size(); }

    @Override public int getColumnCount() { return columnas.length; }

    @Override public String getColumnName(int c) { return columnas[c]; }

    @Override public boolean isCellEditable(int f, int c) { return false; }

    @Override public Class<?> getColumnClass(int c) { return c == 8 ? BigDecimal.class : String.class; }

    @Override
    public Object getValueAt(int fila, int col) {
        PercepcionRetencion p = datos.get(fila);
        switch (col) {
            case 0: return p.fecha == null ? "" : p.fecha.format(FECHA);
            case 1: return p.clase.rotulo;
            case 2: return p.tributo.rotulo;
            case 3: return p.regimen;
            case 4: return p.jurisdiccion;
            case 5: return p.comprobante;
            case 6: return p.cuitAgente;
            case 7: return p.razonSocialAgente;
            case 8: return Modelos.nz(p.importe);
            default: return "";
        }
    }

    public void configurar(JTable t) {
        t.getColumnModel().getColumn(1).setCellRenderer(new Componentes.RenderPill());
        t.getColumnModel().getColumn(2).setCellRenderer(new Componentes.RenderPill());
        for (int i : new int[]{0, 3, 4, 5, 6, 7}) {
            t.getColumnModel().getColumn(i).setCellRenderer(new Componentes.RenderTexto(SwingConstants.LEFT));
        }
        t.getColumnModel().getColumn(8).setCellRenderer(new Componentes.RenderMoneda());
        Componentes.anchos(t, 1.0, 1.1, 1.1, 1.2, 1.2, 1.5, 1.4, 2.4, 1.3);
    }

    public BigDecimal total() {
        BigDecimal t = BigDecimal.ZERO;
        for (PercepcionRetencion p : datos) t = t.add(Modelos.nz(p.importe));
        return Modelos.dosDecimales(t);
    }
}
