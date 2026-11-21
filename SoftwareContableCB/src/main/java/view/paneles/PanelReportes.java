/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view.paneles;

import view.Modelos.*;
import view.Componentes.*;
import view.TopBar;
import view.AppContext;
import view.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import view.Componentes;


public class PanelReportes extends PanelBase{
    private static final int MESES_DISPONIBLES = 24;
    
    private final Componentes.Segmentado segTipo = new Componentes.Segmentado("Ventas", "Compras", "Ventas y Compras");
    private final JComboBox<YearMonth> cboDesde = new JComboBox<>();
    private final JComboBox<YearMonth> cboHasta = new JComboBox<>();
    private final JComboBox<String> cboVista = Componentes.combo(new String[]{"Tabla", "Grafico", "Tabla y grafico"});
    private final JComboBox<String> cboGrafico = Componentes.combo(new String[]{"Barras", "Lineas"});
    
    private final Componentes.Grafico grafico = new Componentes.Grafico();
    private final JPanel tarjetaGrafico = Theme.tarjeta();
    private final JPanel contenedorTabla = new JPanel(new BorderLayout());
    
    private Componentes.TablaConTotales tablaContenedor;
    private boolean inicializando = true;
    
    public PanelReportes(AppContext ctx){
        super(ctx, "Reportes", true);
        
        YearMonth hoy = YearMonth.now();
        for(int i = MESES_DISPONIBLES - 1; i >= 0; i--){
            YearMonth ym = hoy.minusMonths(i);
            cboDesde.addItem(ym);
            cboHasta.addItem(ym);
        }
        DefaultListCellRenderer rend = new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof YearMonth) setText(TopBar.formatear((YearMonth) v));
                setBorder(new EmptyBorder(5, 10, 5, 10));
                return this;
            }
        };
        cboDesde.setRenderer(rend);
        cboHasta.setRenderer(rend);
        Componentes.estilizarCombo(cboDesde);
        Componentes.estilizarCombo(cboHasta);
        cboDesde.setSelectedItem(hoy.minusMonths(5));
        cboDesde.setSelectedItem(hoy);
        
        segTipo.setAlCambiar(i -> {reconstruirTabla(); cargarDatos(); });
        cboDesde.addActionListener(e -> cargarDatos());
        cboHasta.addActionListener(e -> cargarDatos());
        cboVista.addActionListener(e -> aplicarVista());
        cboGrafico.addActionListener(e -> grafico.setTipo(cboGrafico.getSelectedIndex() == 0
                ? Componentes.Grafico.Tipo.BARRAS : Componentes.Grafico.Tipo.LINEAS));
 
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        filtros.add(etiqueta("Desde"));
        filtros.add(cboDesde);
        filtros.add(etiqueta("Hasta"));
        filtros.add(cboHasta);
        filtros.add(Box.createHorizontalStrut(10));
        filtros.add(etiqueta("Ver"));
        filtros.add(cboVista);
        filtros.add(cboGrafico);
 
        JPanel controles = new JPanel(new BorderLayout(0, 12));
        controles.setOpaque(false);
        controles.add(segTipo, BorderLayout.NORTH);
        controles.add(filtros, BorderLayout.SOUTH);
 
        tarjetaGrafico.setBorder(new javax.swing.border.CompoundBorder(
                new Theme.LineaBorder(Theme.BORDE, 1, 4), new EmptyBorder(6, 6, 6, 6)));
        tarjetaGrafico.add(grafico, BorderLayout.CENTER);
        tarjetaGrafico.setPreferredSize(new Dimension(0, 320));
 
        contenedorTabla.setOpaque(false);
        reconstruirTabla();
 
        JPanel centro = new JPanel(new BorderLayout(0, 14));
        centro.setOpaque(false);
        centro.add(tarjetaGrafico, BorderLayout.NORTH);
        centro.add(contenedorTabla, BorderLayout.CENTER);
 
        cuerpo.add(controles, BorderLayout.NORTH);
        cuerpo.add(centro, BorderLayout.CENTER);
 
        inicializando = false;
        aplicarVista();
    }
    
    private JLabel etiqueta(String texto){
        JLabel l = new JLabel(texto);
        l.setFont(Theme.CHICA_BOLD);
        l.setForeground(Theme.TEXTO_SEC);
        return l;
    }
    
    private void aplicarVista(){
        int v = cboVista.getSelectedIndex();
        tarjetaGrafico.setVisible(v != 0);
        contenedorTabla.setVisible(v != 1);
        cboGrafico.setVisible(v != 0);
        revalidate();
        repaint();
    }
    
    private void reconstruirTabla() {
        String[] columnas = columnasDe(segTipo.getSeleccion());
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int f, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 0 || c == 1) ? String.class : BigDecimal.class;
            }
        };
        JTable tabla = new JTable(modelo);
        tablaContenedor = new Componentes.TablaConTotales(tabla);
        tablaContenedor.setMensajeVacio("No hay movimientos en el rango elegido.");
        tabla.getColumnModel().getColumn(0).setCellRenderer(new Componentes.RenderTexto(SwingConstants.LEFT));
        tabla.getColumnModel().getColumn(1).setCellRenderer(new Componentes.RenderTexto(SwingConstants.CENTER));
        for (int i = 2; i < columnas.length; i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(new Componentes.RenderMoneda());
        }
 
        JPanel tarjeta = Theme.tarjeta();
        tarjeta.setBorder(new Theme.LineaBorder(Theme.BORDE, 1, 4));
        tarjeta.add(tablaContenedor, BorderLayout.CENTER);
 
        contenedorTabla.removeAll();
        contenedorTabla.add(tarjeta, BorderLayout.CENTER);
        contenedorTabla.revalidate();
        contenedorTabla.repaint();
    }
 
    private String[] columnasDe(int tipo) {
        if (tipo == 0) return new String[]{"Periodo", "Comprobantes", "Neto gravado", "IVA debito", "Total"};
        if (tipo == 1) return new String[]{"Periodo", "Comprobantes", "Neto gravado", "IVA credito", "Total"};
        return new String[]{"Periodo", "Comprobantes", "Neto ventas", "Neto compras",
                "IVA debito", "IVA credito", "Resultado"};
    }
    
    @Override
    protected void cargarDatos() {
        if (inicializando || tablaContenedor == null) return;
        setSubtitulo(ctx.getClienteActivo().getRazonSocial());
 
        YearMonth desde = (YearMonth) cboDesde.getSelectedItem();
        YearMonth hasta = (YearMonth) cboHasta.getSelectedItem();
        if (desde == null || hasta == null) return;
        if (desde.isAfter(hasta)) {
            YearMonth tmp = desde;
            desde = hasta;
            hasta = tmp;
        }
 
        List<TotalPeriodo> totales = ctx.reportes().totalesPorPeriodo(ctx.cuitActivo(), desde, hasta);
        int tipo = segTipo.getSeleccion();
 
        DefaultTableModel modelo = (DefaultTableModel) tablaContenedor.getTabla().getModel();
        modelo.setRowCount(0);
 
        BigDecimal netoVentas = BigDecimal.ZERO, ivaVentas = BigDecimal.ZERO;
        BigDecimal netoCompras = BigDecimal.ZERO, ivaCompras = BigDecimal.ZERO;
        int cantVentas = 0, cantCompras = 0;
 
        List<String> etiquetas = new ArrayList<>();
        List<Double> serieVentas = new ArrayList<>();
        List<Double> serieCompras = new ArrayList<>();
 
        for (TotalPeriodo t : totales) {
            etiquetas.add(TopBar.formatearCorto(t.periodo));
            serieVentas.add(t.netoVentas.doubleValue());
            serieCompras.add(t.netoCompras.doubleValue());
 
            netoVentas = netoVentas.add(t.netoVentas);
            ivaVentas = ivaVentas.add(t.ivaVentas);
            netoCompras = netoCompras.add(t.netoCompras);
            ivaCompras = ivaCompras.add(t.ivaCompras);
            cantVentas += t.cantidadVentas;
            cantCompras += t.cantidadCompras;
 
            String periodo = TopBar.formatear(t.periodo);
            if (tipo == 0) {
                modelo.addRow(new Object[]{periodo, String.valueOf(t.cantidadVentas),
                        t.netoVentas, t.ivaVentas, t.totalVentas()});
            } else if (tipo == 1) {
                modelo.addRow(new Object[]{periodo, String.valueOf(t.cantidadCompras),
                        t.netoCompras, t.ivaCompras, t.totalCompras()});
            } else {
                modelo.addRow(new Object[]{periodo,
                        (t.cantidadVentas + t.cantidadCompras) + "",
                        t.netoVentas, t.netoCompras, t.ivaVentas, t.ivaCompras, t.resultado()});
            }
        }
 
        if (tipo == 0) {
            tablaContenedor.setTotales("Totales", String.valueOf(cantVentas), netoVentas, ivaVentas,
                    view.Modelos.dosDecimales(netoVentas.add(ivaVentas)));
        } else if (tipo == 1) {
            tablaContenedor.setTotales("Totales", String.valueOf(cantCompras), netoCompras, ivaCompras,
                    view.Modelos.dosDecimales(netoCompras.add(ivaCompras)));
        } else {
            tablaContenedor.setTotales("Totales", String.valueOf(cantVentas + cantCompras),
                    netoVentas, netoCompras, ivaVentas, ivaCompras,
                    view.Modelos.dosDecimales(netoVentas.subtract(netoCompras)));
        }
        tablaContenedor.actualizarEstadoVacio();
 
        List<Componentes.Grafico.Serie> series = new ArrayList<>();
        if (tipo == 0 || tipo == 2) {
            Componentes.Grafico.Serie s = new Componentes.Grafico.Serie("Ventas", Theme.VERDE);
            s.valores.addAll(serieVentas);
            series.add(s);
        }
        if (tipo == 1 || tipo == 2) {
            Componentes.Grafico.Serie s = new Componentes.Grafico.Serie("Compras", Theme.AZUL);
            s.valores.addAll(serieCompras);
            series.add(s);
        }
        grafico.setDatos(etiquetas, series);
    }
 
    /** Util para el panel de inicio: ultimos N meses del cliente activo. */
    static List<String> etiquetasDe(List<TotalPeriodo> totales) {
        List<String> out = new ArrayList<>();
        for (TotalPeriodo t : totales) out.add(TopBar.formatearCorto(t.periodo));
        return out;
    }
 
    static List<Double> valores(List<TotalPeriodo> totales, boolean ventas) {
        List<Double> out = new ArrayList<>();
        for (TotalPeriodo t : totales) {
            out.add((ventas ? t.netoVentas : t.netoCompras).doubleValue());
        }
        return out;
    }
 
    static List<String> tiposDisponibles() {
        return Arrays.asList("Ventas", "Compras", "Ventas y compras");
    }


    
}
