/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view.paneles;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import view.AppContext;
import view.Refrescable;
import view.Theme;
import view.TopBar;

public abstract class PanelBase extends JPanel implements Refrescable {
    protected final AppContext ctx;
    private final JLabel lblTitulo = new JLabel();
    private final JLabel lblSubtitulo = new JLabel();
    private final JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    protected final JPanel cuerpo = new JPanel(new BorderLayout(0, 16));
    private final CardLayout cards = new CardLayout();
    private final JPanel conmutador = new JPanel(cards);
    private final boolean requiereCliente;
    
    protected PanelBase(AppContext ctx, String titulo, boolean requiereCliente) {
        super(new BorderLayout());
        this.ctx = ctx;
        this.requiereCliente = requiereCliente;
        setBackground(Theme.FONDO);
        setBorder(new EmptyBorder(24, 24, 24, 24));
 
        lblTitulo.setText(titulo);
        lblTitulo.setFont(Theme.H1);
        lblTitulo.setForeground(Theme.TEXTO);
        lblSubtitulo.setFont(Theme.BODY);
        lblSubtitulo.setForeground(Theme.TEXTO_SEC);
        lblSubtitulo.setBorder(new EmptyBorder(3, 0, 0, 0));
 
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        lblTitulo.setAlignmentX(LEFT_ALIGNMENT);
        lblSubtitulo.setAlignmentX(LEFT_ALIGNMENT);
        textos.add(lblTitulo);
        textos.add(lblSubtitulo);
 
        acciones.setOpaque(false);
 
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.setBorder(new EmptyBorder(0, 0, 18, 0));
        encabezado.add(textos, BorderLayout.WEST);
        encabezado.add(acciones, BorderLayout.EAST);
 
        cuerpo.setOpaque(false);
        conmutador.setOpaque(false);
        conmutador.add(cuerpo, "contenido");
        conmutador.add(estadoSinCliente(), "sinCliente");
 
        add(encabezado, BorderLayout.NORTH);
        add(conmutador, BorderLayout.CENTER);
    }
    
    private JComponent estadoSinCliente() {
        JPanel p = Theme.tarjeta();
        p.setLayout(new GridBagLayout());
        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
 
        JLabel t = new JLabel("Elegi un cliente en la barra superior");
        t.setFont(Theme.H2);
        t.setForeground(Theme.TEXTO);
        t.setAlignmentX(CENTER_ALIGNMENT);
 
        JLabel s = new JLabel("Todo lo que ves en esta pantalla corresponde al cliente activo.");
        s.setFont(Theme.BODY);
        s.setForeground(Theme.TEXTO_SEC);
        s.setAlignmentX(CENTER_ALIGNMENT);
        s.setBorder(new EmptyBorder(8, 0, 0, 0));
 
        centro.add(t);
        centro.add(s);
        p.add(centro);
        return p;
    }
    
    protected void setSubtitulo(String texto){
        lblSubtitulo.setText(texto);
    }
    
    protected void agregarAccion(Component c){
        acciones.add(c);
    }
    
    protected String contextoTexto(){
        if(!ctx.hayClienteActivo()) return "";
        return ctx.getClienteActivo().getRazonSocial() + " . " + TopBar.formatear(ctx.getPeriodo());
    }
    
    @Override
    public void refrescar(){
        boolean listo = !requiereCliente || ctx.hayClienteActivo();
        cards.show(conmutador, listo ? "contenido" : "sinCliente");
        acciones.setVisible(listo);
        if(listo) {
            cargarDatos();
        }else{
            setSubtitulo("");
        }
    }
    
    protected abstract void cargarDatos();


}
