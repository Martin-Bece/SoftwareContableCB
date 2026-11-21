/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view;

import view.paneles.PanelInicio;
import view.Modelos.Operacion;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import view.paneles.PanelClientes;
import view.paneles.PanelComprobantes;
import view.paneles.PanelImpuestos;
import view.paneles.PanelMaestros;
import view.paneles.PanelPercepciones;
import view.paneles.PanelReportes;

/**
 *
 * @author Usuario
 */
public class MainFrame extends JFrame implements AppContext.Listener{
    
    private final AppContext ctx;
    private final CardLayout cards = new CardLayout();
    private final JPanel contenido = new JPanel(cards);
    private final Map<String, JComponent> vistas = new HashMap<>();
    private final TopBar topbar;
    private final SideBar sidebar;
    private String vistaActual = "inicio";
    
    public MainFrame(AppContext ctx) {
        super("Sistema Contable - Carolina Becerra");
        this.ctx = ctx;
        
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 720));
        setSize(1420, 880);
        setLocationRelativeTo(null);
        
        contenido.setBackground(Theme.FONDO);
        
        topbar = new TopBar(ctx);
        sidebar = new SideBar(this::navegar);
        
        registrar("inicio", new PanelInicio(ctx, this::navegar));
        registrar("clientes", new PanelClientes(ctx, topbar::recargarClientes));
        registrar("ventas", new PanelComprobantes(ctx, Operacion.VENTA));
        registrar("compras", new PanelComprobantes(ctx, Operacion.COMPRA));
        registrar("percepciones", new PanelPercepciones(ctx));
        registrar("impuestos", new PanelImpuestos(ctx));
        registrar("reportes", new PanelReportes(ctx));
        registrar("maestros", new PanelMaestros(ctx));
        
        JPanel derecha = new JPanel(new BorderLayout());
        derecha.add(topbar, BorderLayout.NORTH);
        derecha.add(contenido, BorderLayout.CENTER);
        
        setLayout(new BorderLayout());
        add(sidebar, BorderLayout.WEST);
        add(derecha, BorderLayout.CENTER);
        
        ctx.addListener(this);
        sidebar.seleccionar("inicio");
    }
    
    private void registrar(String clave, JComponent vista){
        vistas.put(clave, vista);
        contenido.add(vista, clave);
    }
    
    private void navegar(String clave){
        if(!vistas.containsKey(clave)) return;
        vistaActual = clave;
        cards.show(contenido, clave);
        refrescarVistaActual();
    }
    
    private void refrescarVistaActual(){
        JComponent v = vistas.get(vistaActual);
        if (v instanceof Refrescable) {
            ((Refrescable) v).refrescar();
        }
    }
    
    @Override
    public void contextoCambio(AppContext ctx){
        topbar.actualizarPeriodo();
        refrescarVistaActual();
    }
}
