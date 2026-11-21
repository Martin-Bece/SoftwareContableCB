/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public class SideBar extends JPanel{
    private final List<Item> items = new ArrayList<>();
    private final Consumer<String> alNavegar;
    private String seleccionado;
    
    public SideBar(Consumer<String> alNavegar){
        this.alNavegar = alNavegar;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.SIDEBAR);
        setPreferredSize(new Dimension(236, 0));
        setBorder(new EmptyBorder(0, 0, 16, 0));
        
        add(encabezado());
        add(Box.createVerticalStrut(10));
        
        agregarItem("inicio", "inicio", "Inicio");
        agregarItem("clientes", "clientes", "Clientes");
        agregarItem("ventas", "ventas", "Ventas");
        agregarItem("compras", "compras", "Compras");
        agregarItem("percepciones", "percepciones", "Percepciones y retenciones");
        agregarItem("impuestos", "impuestos", "Impuestos");
        agregarItem("reportes", "reportes", "Reportes");
        
        add(Box.createVerticalStrut(14));
        add(rotuloSeccion("Datos del cliente"));
        agregarItem("maestros", "maestros", "Clientes y proveedores");
        
        add(Box.createVerticalGlue());
        add(pie());
    }
    
    private JComponent encabezado() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(22, 18, 18, 18));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        
        JPanel marca = new JPanel(null){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(Theme.VERDE);
                g2.fillRoundRect(0, 0, 34, 34, 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font(Theme.FAMILIA, Font.BOLD, 17));
                g2.drawString("CB", 6, 23);
                g2.dispose();
            }
        };
        marca.setOpaque(false);
        marca.setPreferredSize(new Dimension(34, 34));
        
        JLabel nombre = new JLabel("Carolina Becerra");
        nombre.setFont(new Font(Theme.FAMILIA, Font.BOLD, 14));
        nombre.setForeground(Color.WHITE);
        JLabel rol = new JLabel("Estudio contable");
        rol.setFont(Theme.CHICA);
        rol.setForeground(new Color(0xB8B9BC));
        
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        nombre.setAlignmentX(LEFT_ALIGNMENT);
        rol.setAlignmentX(LEFT_ALIGNMENT);
        textos.add(nombre);
        textos.add(rol);
        
        p.add(marca, BorderLayout.WEST);
        p.add(textos, BorderLayout.CENTER);
        
        return p;
    }
    
    private JComponent rotuloSeccion(String texto){
        JLabel l = new JLabel(texto);
        l.setFont(Theme.CHICA_BOLD);
        l.setForeground(new Color(0x8D8E92));
        l.setBorder(new EmptyBorder(6, 20, 6, 20));
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        
        return l;
    }
    
    private JComponent pie(){
        JLabel l = new JLabel("Periodo fiscal en curso");
        l.setFont(Theme.CHICA);
        l.setForeground(new Color(0x8D8E92));
        l.setBorder(new EmptyBorder(10, 20, 0, 20));
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        
        return l;
    }
    
    private void agregarItem(String clave, String icono, String texto){
        Item item = new Item(clave, icono, texto);
        items.add(item);
        add(item);
    }
    
    public void seleccionar(String clave){
        seleccionado = clave;
        for (Item i : items) i.repaint();
        if (alNavegar != null) alNavegar.accept(clave);
    }

    private class Item extends JPanel {
        private final String clave;
        private final Componentes.Icono icono;
        private final JLabel etiqueta;
        private boolean hover;

        Item(String clave, String tipoIcono, String texto){
            super(new BorderLayout(12, 0));
            this.clave = clave;
            this.icono = new Componentes.Icono(tipoIcono, 18, new Color(0xD4D5D7));
            setOpaque(false);
            setBorder(new EmptyBorder(11, 20, 11, 16));
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel iconoLbl = new JLabel(icono);
            etiqueta = new JLabel(texto);
            etiqueta.setFont(Theme.BODY);
            etiqueta.setForeground(new Color(0xD4D5D7));

            add(iconoLbl, BorderLayout.WEST);
            add(etiqueta, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter(){
               @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); } 
               @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); } 
               @Override public void mouseClicked(MouseEvent e) { seleccionar(Item.this.clave); } 
            });
        }

        @Override
        protected void paintComponent(Graphics g){
            boolean activo = clave.equals(seleccionado);
                Graphics2D g2 = (Graphics2D) g.create();
                if (activo) {
                    g2.setColor(new Color(0x4C4D51));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(Theme.VERDE);
                    g2.fillRect(0, 0, 4, getHeight());
                } else if (hover) {
                    g2.setColor(Theme.SIDEBAR_ALT);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.dispose();

                Color c = activo ? Color.WHITE : new Color(0xD4D5D7);
                etiqueta.setForeground(c);
                etiqueta.setFont(activo ? Theme.BODY_BOLD : Theme.BODY);
                icono.setColor(c);
                super.paintComponent(g);

        }
    }
}