/*
 * Click nbfs://nbhost/SystemFileSystem/Temlates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view.paneles;

import view.paneles.PanelBase;
import view.Modelos.PosicionIva;
import view.Modelos.TotalPeriodo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import view.AppContext;
import view.Componentes;
import view.Theme;
import view.TopBar;


public class PanelInicio extends PanelBase{
    private static final int MESES_GRAFICO = 6;
    
    private final Consumer<String> navegar;
    
    private final Componentes.TarjetaMetrica mVentas = new Componentes.TarjetaMetrica("Ventas del periodo", Theme.VERDE);
    private final Componentes.TarjetaMetrica mCompras = new Componentes.TarjetaMetrica("Compras del periodo", Theme.AZUL);
    private final Componentes.TarjetaMetrica mIva = new Componentes.TarjetaMetrica("IVA del mes", Theme.TEXTO);
    private final Componentes.TarjetaMetrica mComprobantes = new Componentes.TarjetaMetrica("Comprobantes cargados", Theme.TEXTO);
    
    private final Componentes.Grafico grafico = new Componentes.Grafico();
    private final JLabel lblGrafico = new JLabel();
    
    public PanelInicio(AppContext ctx, Consumer <String> navegar){
        super(ctx, "Inicio", true);
        this.navegar = navegar;
        
        Componentes.Boton irReportes = new Componentes.Boton("Ver reportes", Componentes.EstiloBoton.SECUNDARIO);
        irReportes.addActionListener(e -> ir("reportes"));
        Componentes.Boton irVentas = new Componentes.Boton("Cargar comprobante", Componentes.EstiloBoton.PRIMARIO);
        irVentas.addActionListener(e -> ir("ventas"));
        agregarAccion(irReportes);
        agregarAccion(irVentas);
        
        cuerpo.add(panelMetricas(), BorderLayout.NORTH);
        cuerpo.add(panelGrafico(), BorderLayout.CENTER);
        cuerpo.add(panelAccesos(), BorderLayout.SOUTH);
    }
    
    private JComponent panelMetricas() {
        JPanel p = new JPanel(new GridLayout(1, 4, 16, 0));
        p.setOpaque(false);
        p.add(mVentas);
        p.add(mCompras);
        p.add(mIva);
        p.add(mComprobantes);
        return p;
    }
    
    private JComponent panelGrafico(){
        JPanel tarjeta = Theme.tarjeta();
        tarjeta.setLayout(new BorderLayout(0, 4));
        
        JLabel titulo = new JLabel("Evolucion de los ultimos " + MESES_GRAFICO + " meses");
        titulo.setFont(Theme.H3);
        titulo.setForeground(Theme.TEXTO);
        
        lblGrafico.setFont(Theme.CHICA);
        lblGrafico.setForeground(Theme.TEXTO_SEC);
        
        JPanel encabezado = new JPanel();
        encabezado.setOpaque(false);
        encabezado.setLayout(new BoxLayout(encabezado, BoxLayout.Y_AXIS));
        encabezado.setBorder(new EmptyBorder(16, 18, 0, 18));
        titulo.setAlignmentX(LEFT_ALIGNMENT);
        lblGrafico.setAlignmentX(LEFT_ALIGNMENT);
        encabezado.add(titulo);
        encabezado.add(lblGrafico);
        
        grafico.setTipo(Componentes.Grafico.Tipo.BARRAS);
        
        tarjeta.add(encabezado, BorderLayout.NORTH);
        tarjeta.add(grafico, BorderLayout.CENTER);
        return tarjeta;
    }
    
    private JComponent panelAccesos(){
        JPanel p = new JPanel(new GridLayout(1, 4, 16, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(4, 0, 0, 0));
        p.add(acceso("ventas", "Ventas", "Cargar manualmente o importar CSV, Excel o PDF de AFIP"));
        p.add(acceso("compras", "Compras", "Comprobantes recibidos y credito fiscal del periodo"));
        p.add(acceso("percepciones", "Percepciones y retenciones", "Sufridas en IVA, Ganancias y SUSS"));
        p.add(acceso("impuestos", "Impuestos", "Determinacion de IVA, Ganancias y SUSS del mes"));
        return p;
    }
    
    private JComponent acceso(String clave, String titulo, String detalle){
        final JPanel tarjeta = Theme.tarjeta();
        tarjeta.setLayout(new BorderLayout(12, 0));
        tarjeta.setBorder(new javax.swing.border.CompoundBorder(
                new Theme.LineaBorder(Theme.BORDE, 1, 4), new EmptyBorder(14, 16, 14, 16)));
        tarjeta.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
 
        JLabel icono = new JLabel(new Componentes.Icono(clave, 20, Theme.VERDE));
        icono.setVerticalAlignment(SwingConstants.TOP);
 
        JLabel t = new JLabel(titulo);
        t.setFont(Theme.BODY_BOLD);
        t.setForeground(Theme.TEXTO);
 
        JLabel d = new JLabel("<html><body style='width:150px'>" + detalle + "</body></html>");
        d.setFont(Theme.CHICA);
        d.setForeground(Theme.TEXTO_SEC);
        d.setBorder(new EmptyBorder(4, 0, 0, 0));
 
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        t.setAlignmentX(LEFT_ALIGNMENT);
        d.setAlignmentX(LEFT_ALIGNMENT);
        textos.add(t);
        textos.add(d);
 
        tarjeta.add(icono, BorderLayout.WEST);
        tarjeta.add(textos, BorderLayout.CENTER);
 
        tarjeta.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { ir(clave); }
            @Override public void mouseEntered(MouseEvent e) {
                tarjeta.setBorder(new javax.swing.border.CompoundBorder(
                        new Theme.LineaBorder(Theme.VERDE, 1, 4), new EmptyBorder(14, 16, 14, 16)));
            }
            @Override public void mouseExited(MouseEvent e) {
                tarjeta.setBorder(new javax.swing.border.CompoundBorder(
                        new Theme.LineaBorder(Theme.BORDE, 1, 4), new EmptyBorder(14, 16, 14, 16)));
            }
        });
        return tarjeta;
    }
    
    private void ir(String clave){
        if (navegar != null) navegar.accept(clave);
    }
    
    
    @Override
    protected void cargarDatos(){
        setSubtitulo(contextoTexto());
        
        YearMonth periodo = ctx.getPeriodo();
        YearMonth desde = periodo.minusMonths(MESES_GRAFICO - 1);
        
        List <TotalPeriodo> totales = ctx.reportes().totalesPorPeriodo(ctx.cuitActivo(), desde, periodo);
        
        TotalPeriodo actual = null;
        for (TotalPeriodo t : totales){
            if(periodo.equals(t.periodo)) actual = t;
        }
        if (actual == null) actual = new TotalPeriodo(periodo);
        
        mVentas.setValor(actual.totalVentas());
        mVentas.setPie(actual.cantidadVentas + " comprobantes . neto " + Theme.pesos(actual.netoVentas));
        
        mCompras.setValor(actual.totalCompras());
        mCompras.setPie(actual.cantidadCompras + " comprobantes . neto " + Theme.pesos(actual.netoCompras));
        
        PosicionIva iva = ctx.impuestos().determinarIva(ctx.cuitActivo(), periodo);
        if(iva == null){
            mIva.setValor("-");
            mIva.setPie("Sin datos para el periodo");
        }else{
            BigDecimal saldo = iva.saldoFinal();
            mIva.setValor(Theme.pesos(saldo.abs()));
            mIva.setAcento(iva.aPagar() ? Theme.ROJO : Theme.VERDE);
            mIva.setPie(iva.aPagar() ? "Saldo a pagar" : "Saldo a favor");
        }
        
        int cantidad = actual.cantidadVentas + actual.cantidadCompras;
        mComprobantes.setValor(String.valueOf(cantidad));
        mComprobantes.setPie(cantidad == 0 ? "Todavía no cargaste comprobantes este mes" : "Ventas y compras del periodo activo");
        
        List<Componentes.Grafico.Serie> series = new ArrayList<>();
        Componentes.Grafico.Serie sv = new Componentes.Grafico.Serie("Ventas", Theme.VERDE);
        sv.valores.addAll(PanelReportes.valores(totales, true));
        series.add(sv);
        Componentes.Grafico.Serie sc = new Componentes.Grafico.Serie("Compras", Theme.AZUL);
        sc.valores.addAll(PanelReportes.valores(totales, false));
        series.add(sc);
        grafico.setDatos(PanelReportes.etiquetasDe(totales), series);
        
        lblGrafico.setText("Montos netos, sin IVA - " + TopBar.formatear(desde) + " a " + TopBar.formatear(periodo));
    }
}
