/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view;

import view.Modelos.Actividad;
import view.Modelos.Cliente;
import view.Servicios.ServicioException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class DialogoCliente extends JDialog{
    private final AppContext ctx;
    private final Cliente original;
    private boolean guardado;
    
    private final Componentes.CampoTexto txtCuit = new Componentes.CampoTexto("30-71234567-9");
    private final Componentes.CampoTexto txtRazon = new Componentes.CampoTexto("");
    private final Componentes.CampoTexto txtFantasia = new Componentes.CampoTexto("");
    private final Componentes.CampoTexto txtDireccion = new Componentes.CampoTexto("");
    private final Componentes.CampoTexto txtCodActividad = new Componentes.CampoTexto("Codigo");
    private final Componentes.CampoTexto txtNomActividad = new Componentes.CampoTexto("Descripcion de la actividad");
    private final DefaultListModel<Actividad> modeloActividades = new DefaultListModel<>();
    private final JList<Actividad> listaActividades = new JList<>(modeloActividades);
    private final Map<String, JCheckBox> checksImpuestos = new LinkedHashMap<>();
    private final JCheckBox chkEmpleados = new JCheckBox("Esta inscripto como empleador");
    private final JLabel lblError = new JLabel(" ");
    
    public DialogoCliente(Window padre, AppContext ctx, Cliente cliente){
        super(padre, cliente == null ? "Nuevo cliente" : "Editar cliente", ModalityType.APPLICATION_MODAL);
        this.ctx = ctx;
        this.original = cliente;
        
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.FONDO);
        
        add(encabezado(cliente), BorderLayout.NORTH);
        add(formulario(), BorderLayout.CENTER);
        add(pie(), BorderLayout.SOUTH);
        
        if(cliente != null) cargar(cliente);
        
        setSize(720, 700);
        setLocationRelativeTo(padre);
        getRootPane().setDefaultButton(null);
    }
    
    public boolean fueGuardado() { return guardado;}
    
    private JComponent encabezado(Cliente cliente) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new javax.swing.border.CompoundBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDE), new EmptyBorder(18, 24, 18, 24)));
        JLabel t = new JLabel(cliente == null ? "Nuevo cliente" : cliente.getRazonSocial());
        t.setFont(Theme.H2);
        t.setForeground(Theme.TEXTO);
        JLabel s = new JLabel(cliente == null ? "El CUIT identifica al cliente y no se puede cambiar despues." : "CUIT " + cliente.getCuit());
        s.setFont(Theme.CHICA);
        s.setForeground(Theme.TEXTO_SEC);
        
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        t.setAlignmentX(LEFT_ALIGNMENT);
        s.setAlignmentX(LEFT_ALIGNMENT);
        textos.add(t);
        textos.add(Box.createVerticalStrut(2));
        textos.add(s);
        p.add(textos, BorderLayout.WEST);
        return p;
    }
    
    private JComponent formulario(){
        JPanel contenido = new JPanel();
        contenido.setBackground(Theme.FONDO);
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBorder(new EmptyBorder(18, 24, 18, 24));
        
        contenido.add(seccionDatos());
        contenido.add(Box.createVerticalStrut(16));
        contenido.add(seccionActividades());
        contenido.add(Box.createVerticalStrut(16));
        contenido.add(seccionImpuestos());
        
        JScrollPane sp = new JScrollPane(contenido);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.FONDO);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }
    
    private JPanel tarjetaSeccion(String titulo){
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(Color.WHITE);
        p.setBorder(new javax.swing.border.CompoundBorder(new Theme.LineaBorder(Theme.BORDE, 1, 4), new EmptyBorder(16, 18, 18, 18)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel t = new JLabel(titulo);
        t.setFont(Theme.H3);
        t.setForeground(Theme.TEXTO);
        p.add(t, BorderLayout.NORTH);
        return p;
    }
    
    private JComponent seccionDatos(){
        JPanel p = tarjetaSeccion("Datos de identificacion");
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        
        grid.add(campo("CUIT", txtCuit), Componentes.gbc(0, 0, 1, 0.4));
        grid.add(campo("Razon social", txtCuit), Componentes.gbc(1, 0, 1, 0.6));
        grid.add(campo("Nombre de fantasia", txtCuit), Componentes.gbc(0, 1, 1, 0.4));
        grid.add(campo("Direccion", txtCuit), Componentes.gbc(1, 1, 1, 0.6));
        
        p.add(grid, BorderLayout.CENTER);
        return p;
    }
    
    private JComponent seccionActividades(){
        JPanel p = tarjetaSeccion("Actividades declaradas");
        
        JPanel alta = new JPanel(new BorderLayout(8, 0));
        alta.setOpaque(false);
        txtCodActividad.setPreferredSize(new Dimension(110, 34));
        Componentes.Boton agregar = Componentes.secundario("Agregar");
        agregar.addActionListener(e -> agregarActividad());
        alta.add(txtCodActividad, BorderLayout.WEST);
        alta.add(txtNomActividad, BorderLayout.CENTER);
        alta.add(agregar, BorderLayout.EAST);
        
        listaActividades.setFont(Theme.BODY);
        listaActividades.setFixedCellHeight(30);
        listaActividades.setBorder(new EmptyBorder(4, 8, 4, 8));
        listaActividades.setSelectionBackground(Theme.SELECCION);
        listaActividades.setSelectionForeground(Theme.TEXTO);
        JScrollPane sp = new JScrollPane(listaActividades);
        sp.setBorder(new Theme.LineaBorder(Theme.BORDE, 1, 4));
        sp.setPreferredSize(new Dimension(0, 108));
        
        Componentes.Boton quitar = Componentes.enlace("Quitar seleccionada");
        quitar.addActionListener(e -> {
            int i = listaActividades.getSelectedIndex();
            if (i >= 0) modeloActividades.remove(i);
        });
        
        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setOpaque(false);
        centro.add(alta, BorderLayout.NORTH);
        centro.add(sp, BorderLayout.CENTER);
        centro.add(Componentes.filaBotones(quitar), BorderLayout.SOUTH);
        
        p.add(centro, BorderLayout.CENTER);
        return p;
    }
    
    private JComponent seccionImpuestos(){
        JPanel p = tarjetaSeccion("Impuestos en los que esta inscripto");
        
        JPanel checks = new JPanel(new GridLayout(0, 3, 8, 6));
        checks.setOpaque(false);
        for(String imp : Modelos.IMPUESTOS_DISPONIBLES){
            JCheckBox ch = new JCheckBox(imp);
            ch.setFont(Theme.BODY);
            ch.setBackground(Color.WHITE);
            ch.setForeground(Theme.TEXTO);
            ch.setFocusPainted(false);
            if("SUSS".equals(imp)){
                ch.addActionListener(e -> {
                    if (ch.isSelected()) chkEmpleados.setSelected(true);
                });
            }
            checksImpuestos.put(imp, ch);
            checks.add(ch);
        }
        
        chkEmpleados.setFont(Theme.BODY);
        chkEmpleados.setBackground(Color.WHITE);
        chkEmpleados.setForeground(Theme.TEXTO);
        chkEmpleados.setFocusPainted(false);
        chkEmpleados.addActionListener(e -> {
           JCheckBox suss = checksImpuestos.get("SUSS");
           if (chkEmpleados.isSelected() && suss != null) suss.setSelected(true);
        });
        
        JLabel ayuda = new JLabel("Si el cliente tiene empleados se habilita la determinacion de SUSS.");
        ayuda.setFont(Theme.CHICA);
        ayuda.setForeground(Theme.TEXTO_SEC);
        
        JPanel abajo = new JPanel();
        abajo.setOpaque(false);
        abajo.setLayout(new BoxLayout(abajo, BoxLayout.Y_AXIS));
        chkEmpleados.setAlignmentX(Component.LEFT_ALIGNMENT);
        ayuda.setAlignmentX(Component.LEFT_ALIGNMENT);
        abajo.add(Box.createVerticalStrut(10));
        abajo.add(chkEmpleados);
        abajo.add(Box.createVerticalStrut(4));
        abajo.add(ayuda);
        
        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(checks, BorderLayout.CENTER);
        centro.add(abajo, BorderLayout.SOUTH);
        
        p.add(centro, BorderLayout.CENTER);
        return p;
    }
    
    private JComponent campo(String rotulo, JComponent control) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        JLabel l = new JLabel(rotulo);
        l.setFont(Theme.CHICA_BOLD);
        l.setForeground(Theme.TEXTO_SEC);
        control.setPreferredSize(new Dimension(control.getPreferredSize().width, 34));
        p.add(l, BorderLayout.NORTH);
        p.add(control, BorderLayout.CENTER);
        return p;
    }
    
    private JComponent pie(){
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new javax.swing.border.CompoundBorder(new MatteBorder(1, 0, 0, 0, Theme.BORDE), new EmptyBorder(14, 24, 14, 24)));
        
        lblError.setFont(Theme.CHICA);
        lblError.setForeground(Theme.ROJO);
        
        Componentes.Boton cancelar = Componentes.secundario("Cancelar");
        cancelar.addActionListener(e -> dispose());
        Componentes.Boton guardar = Componentes.primario(original == null ? "Crear cliente" : "Guardar cambios");
        guardar.addActionListener(e -> guardar());
        
        p.add(lblError, BorderLayout.WEST);
        p.add(Componentes.filaBotones(cancelar, guardar), BorderLayout.EAST);
        return p;
    }
    
    private void agregarActividad(){
        String codigo = txtCodActividad.getText().trim();
        String nombre = txtNomActividad.getText().trim();
        if(nombre.isEmpty()){
            lblError.setText("Escribi la descripcion de la actividad antes de agregarla.");
            return;
        }
        modeloActividades.addElement(new Actividad(codigo, nombre));
        txtCodActividad.setText("");
        txtNomActividad.setText("");
        lblError.setText(" ");
    }
    
    private void cargar(Cliente c) {
        txtCuit.setText(c.getCuit());
        txtCuit.setEditable(false);
        txtCuit.setBackground(Theme.FONDO);
        txtCuit.setToolTipText("El CUIT es la clave del cliente y no se modifica.");
        txtRazon.setText(c.getRazonSocial());
        txtFantasia.setText(c.getNombreFantasia());
        txtDireccion.setText(c.getDireccion());
        for (Actividad a : c.getActividades()) modeloActividades.addElement(a);
        for (String imp : c.getImpuestos()) {
            JCheckBox ch = checksImpuestos.get(imp);
            if (ch != null) ch.setSelected(true);
        }
        chkEmpleados.setSelected(c.isTieneEmpleados());
    }
    
    private void guardar() {
        String cuit = normalizarCuit(txtCuit.getText());
        if (cuit == null) {
            lblError.setText("El CUIT debe tener 11 digitos (formato 30-71234567-9).");
            txtCuit.requestFocus();
            return;
        }
        if (txtRazon.getText().trim().isEmpty()) {
            lblError.setText("La razon social es obligatoria.");
            txtRazon.requestFocus();
            return;
        }
 
        Cliente c = original == null ? new Cliente() : original;
        c.setCuit(cuit);
        c.setRazonSocial(txtRazon.getText().trim());
        c.setNombreFantasia(txtFantasia.getText().trim());
        c.setDireccion(txtDireccion.getText().trim());
 
        c.getActividades().clear();
        for (int i = 0; i < modeloActividades.size(); i++) {
            c.getActividades().add(modeloActividades.get(i));
        }
 
        List<String> seleccionados = new ArrayList<>();
        for (Map.Entry<String, JCheckBox> e : checksImpuestos.entrySet()) {
            if (e.getValue().isSelected()) seleccionados.add(e.getKey());
        }
        c.setImpuestosDesdeTexto(String.join(",", seleccionados));
        c.setTieneEmpleados(chkEmpleados.isSelected());
 
        try {
            if (original == null) {
                ctx.clientes().alta(c);
            } else {
                ctx.clientes().modificar(c);
            }
            guardado = true;
            dispose();
        } catch (ServicioException ex) {
            lblError.setText(ex.getMessage());
        }
    }
    
    public static String normalizarCuit(String entrada) {
        if (entrada == null) return null;
        String soloDigitos = entrada.replaceAll("[^0-9]", "");
        if (soloDigitos.length() != 11) return null;
        return soloDigitos.substring(0, 2) + "-" + soloDigitos.substring(2, 10) + "-" + soloDigitos.substring(10);
    }
}
