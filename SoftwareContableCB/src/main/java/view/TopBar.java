/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view;

import view.Modelos.Cliente;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;


public class TopBar extends JPanel{
    private static final Locale ES = new Locale("es", "AR");
    
    private final AppContext ctx;
    private final JComboBox<Cliente> comboClientes = new JComboBox<>();
    private final JLabel lblPeriodo = new JLabel();
    private boolean actualizandoCombo;
    
    public TopBar(AppContext ctx){
        super(new BorderLayout());
        this.ctx = ctx;
        setBackground(Color.WHITE);
        setBorder(new javax.swing.border.CompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.BORDE), new EmptyBorder(12, 24, 12, 24)));
        
        add(bloqueCliente(), BorderLayout.WEST);
        add(bloquePeriodo(), BorderLayout.EAST);
        
        recargarClientes();
        actualizarPeriodo();
    }
    
    private JComponent bloqueCliente(){
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.setOpaque(false);
        
        JLabel l = new JLabel("Trabajando sobre");
        l.setFont(Theme.CHICA);
        l.setForeground(Theme.TEXTO_SEC);
        
        Componentes.estilizarCombo(comboClientes);
        comboClientes.setPreferredSize(new Dimension(360, 36));
        comboClientes.setRenderer(new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int idx,
                                                          boolean sel, boolean foco) {
                super.getListCellRendererComponent(lista, valor, idx, sel, foco);
                setBorder(new EmptyBorder(6, 10, 6, 10));
                setFont(Theme.BODY_BOLD);
                if (valor instanceof Cliente) {
                    Cliente c = (Cliente) valor;
                    setText("<html>" + c.getRazonSocial()
                            + " <font color='#6B6C72'>&nbsp;·&nbsp; CUIT " + c.getCuit() + "</font></html>");
                } else {
                    setText("Elegi un cliente para empezar");
                    setFont(Theme.BODY);
                    setForeground(Theme.TEXTO_SEC);
                }
                if (sel) {
                    setBackground(Theme.SELECCION);
                    setForeground(Theme.TEXTO);
                }
                return this;
            }
        });
        comboClientes.addActionListener(e -> {
            if(actualizandoCombo) return;
            Object sel = comboClientes.getSelectedItem();
            ctx.setClienteActivo(sel instanceof Cliente ? (Cliente) sel : null);
        });
        
        p.add(l);
        p.add(comboClientes);
        return p;
    }
    
    private JComponent bloquePeriodo(){
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        p.setOpaque(false);
        
        JLabel l = new JLabel("Periodo");
        l.setFont(Theme.CHICA);
                l.setForeground(Theme.TEXTO_SEC);
 
        Componentes.Boton anterior = Componentes.secundario("‹");
        Componentes.Boton siguiente = Componentes.secundario("›");
        anterior.setBorder(new EmptyBorder(7, 12, 7, 12));
        siguiente.setBorder(new EmptyBorder(7, 12, 7, 12));
        anterior.setToolTipText("Mes anterior");
        siguiente.setToolTipText("Mes siguiente");
 
        lblPeriodo.setFont(Theme.BODY_BOLD);
        lblPeriodo.setForeground(Theme.TEXTO);
        lblPeriodo.setHorizontalAlignment(SwingConstants.CENTER);
        lblPeriodo.setPreferredSize(new Dimension(150, 34));
        lblPeriodo.setBorder(new javax.swing.border.CompoundBorder(
                new Theme.LineaBorder(Theme.BORDE, 1, 4), new EmptyBorder(0, 8, 0, 8)));
        lblPeriodo.setOpaque(false);
 
        anterior.addActionListener(e -> ctx.setPeriodo(ctx.getPeriodo().minusMonths(1)));
        siguiente.addActionListener(e -> ctx.setPeriodo(ctx.getPeriodo().plusMonths(1)));
 
        p.add(l);
        p.add(anterior);
        p.add(lblPeriodo);
        p.add(siguiente);        
        return p;
    }
    
    public void recargarClientes(){
        actualizandoCombo = true;
        Cliente activo = ctx.getClienteActivo();
        comboClientes.removeAllItems();
        List<Cliente> lista = ctx.clientes().listar(null);
        Cliente aSeleccionar = null;
        for (Cliente c : lista) {
            comboClientes.addItem(c);
            if(activo != null && activo.getCuit().equals(c.getCuit())) aSeleccionar = c;
        }
        if (aSeleccionar == null && !lista.isEmpty()) aSeleccionar = lista.get(0);
        comboClientes.setSelectedItem(aSeleccionar);
        actualizandoCombo = false;
        ctx.setClienteActivo(aSeleccionar);
    }
    
    public void actualizarPeriodo(){
        lblPeriodo.setText(formatear(ctx.getPeriodo()));
    }
    
    public static String formatear(YearMonth ym) {
        String mes = ym.getMonth().getDisplayName(TextStyle.FULL, ES);
        return Character.toUpperCase(mes.charAt(0)) + mes.substring(1) + " " + ym.getYear();
    }
    
    public static String formatearCorto(YearMonth ym) {
        String mes = ym.getMonth().getDisplayName(TextStyle.SHORT, ES).replace(".", "");
        return Character.toUpperCase(mes.charAt(0)) + mes.substring(1) + " " + (ym.getYear() % 100);
    }
}
