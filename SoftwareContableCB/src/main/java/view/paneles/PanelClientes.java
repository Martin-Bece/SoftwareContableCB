/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view.paneles;

import view.Modelos.*;
import view.Servicios.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import view.AppContext;
import view.Componentes;
import view.DialogoCliente;
import view.Theme;


/**
 *
 * @author Usuario
 */
public class PanelClientes extends PanelBase{
    private final ModeloClientes modelo = new ModeloClientes();
    private final JTable tabla = new JTable(modelo);
    private final Componentes.TablaConTotales contenedor;
    private final Componentes.CampoBusqueda busqueda = new Componentes.CampoBusqueda("Buscar por razon social o CUIT");
    private final Componentes.Boton btnEditar = Componentes.secundario("Editar");
    private final Componentes.Boton btnEliminar = Componentes.secundario("Eliminar");
    private final Componentes.Boton btnActivar = Componentes.secundario("Trabajar con este cliente");
    private final Runnable alCambiarClientes;
    
    public PanelClientes(AppContext ctx, Runnable alCambiarClientes) {
        super(ctx, "Clientes", false);
        this.alCambiarClientes = alCambiarClientes;
        
        busqueda.setPreferredSize(new Dimension(280, 36));
        busqueda.getDocument().addDocumentListener(new DocumentListener(){
            @Override public void insertUpdate(DocumentEvent e) {cargarDatos();}
            @Override public void removeUpdate(DocumentEvent e) {cargarDatos();}
            @Override public void changedUpdate(DocumentEvent e) {cargarDatos();}
        });
        
        Componentes.Boton nuevo = Componentes.primario("Nuevo cliente");
        nuevo.addActionListener(e -> abrirFormulario(null));
        
        agregarAccion(busqueda);
        agregarAccion(nuevo);
        
        contenedor = new Componentes.TablaConTotales(tabla);
        contenedor.setMensajeVacio("Todavia no hay clientes cargados. Crea el primero con \"Nuevo cliente\".");
        Componentes.anchos(tabla, 1.4, 2.6, 1.8, 2.4, 2.0, 1.0);
        tabla.getColumnModel().getColumn(4).setCellRenderer(new Componentes.RenderTexto(SwingConstants.LEFT));
        tabla.getColumnModel().getColumn(5).setCellRenderer(new Componentes.RenderTexto(SwingConstants.CENTER));
        for(int i = 0; i < 4; i++){
            tabla.getColumnModel().getColumn(i).setCellRenderer(new Componentes.RenderTexto(SwingConstants.LEFT));
        }
        tabla.getSelectionModel().addListSelectionListener(e -> actualizarBotones());
        tabla.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){
                if(e.getClickCount() == 2 && clienteSeleccionado() != null){
                    abrirFormulario(clienteSeleccionado());
                }
            }
        });
        
        btnEditar.addActionListener(e -> abrirFormulario(clienteSeleccionado()));
        btnEliminar.addActionListener(e -> eliminar());
        btnActivar.addActionListener(e -> {
            Cliente c = clienteSeleccionado();
            if (c != null) ctx.setClienteActivo(c);
        });
        
        JPanel tarjeta = Theme.tarjeta();
        tarjeta.setBorder(new javax.swing.border.CompoundBorder(new Theme.LineaBorder(Theme.BORDE, 1, 4), new EmptyBorder(0, 0, 0, 0)));
        tarjeta.add(contenedor, BorderLayout.CENTER);
        
        JPanel barraInferior = new JPanel(new BorderLayout());
        barraInferior.setOpaque(false);
        barraInferior.setBorder(new EmptyBorder(12, 0, 0, 0));
        barraInferior.add(Componentes.filaBotones(btnActivar, btnEditar, btnEliminar), BorderLayout.EAST);
        
        cuerpo.add(tarjeta, BorderLayout.CENTER);
        cuerpo.add(barraInferior, BorderLayout.SOUTH);
        
        actualizarBotones();
    }
    
    private Cliente clienteSeleccionado(){
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : modelo.getEn(tabla.convertRowIndexToModel(fila));
    }
    
    private void actualizarBotones(){
        boolean hay = clienteSeleccionado() != null;
        btnEditar.setEnabled(hay);
        btnEliminar.setEnabled(hay);
        btnActivar.setEnabled(hay);
    }
    
    private void abrirFormulario(Cliente cliente) {
        Window padre = SwingUtilities.getWindowAncestor(this);
        DialogoCliente dlg = new DialogoCliente(padre, ctx, cliente);
        dlg.setVisible(true);
        if(dlg.fueGuardado()){
            cargarDatos();
            if(alCambiarClientes != null) alCambiarClientes.run();
        }
    }
    
    private void eliminar() {
        Cliente c = clienteSeleccionado();
        if(c == null) return;
        if(!Componentes.confirmar(this, "Se va a dar de baja a " + c.getRazonSocial() + ". Esta accion no se puede deshacer. ¿Desea continuar?")) return;
        
        try {
            ctx.clientes().baja(c.getCuit());
            if(ctx.hayClienteActivo() && ctx.getClienteActivo().getCuit().equals(c.getCuit())){
                ctx.setClienteActivo(null);
            }
            cargarDatos();
            if(alCambiarClientes != null) alCambiarClientes.run();
        } catch (ServicioException ex) {
            Componentes.error(this, ex.getMessage());
        }
    }
    
    @Override
    protected void cargarDatos(){
        List<Cliente> lista = ctx.clientes().listar(busqueda.getText());
        modelo.setDatos(lista);
        setSubtitulo(lista.size() == 1 ? "1 cliente en cartera" : lista.size() + " clientes en cartera");
        long conEmpleados = lista.stream().filter(Cliente::isTieneEmpleados).count();
        contenedor.setTotales(lista.size() + (lista.size() == 1 ? " cliente" : " clientes"), null, null, null, null, conEmpleados + " con empleados");
        contenedor.actualizarEstadoVacio();
        actualizarBotones();
    }
    
    
    private static class ModeloClientes extends AbstractTableModel{
        private final String[] columnas = {"CUIT", "Razon social", "Nombre de fantasia", "Direccion", "Impuestos", "Empleados"};
        private List<Cliente> datos = new ArrayList<>();
        
        void setDatos(List<Cliente> datos){
            this.datos = datos;
            fireTableDataChanged();
        }
        
        Cliente getEn(int fila) {return datos.get(fila);}
        
        @Override public int getRowCount() { return datos.size(); }
        
        @Override public int getColumnCount() { return columnas.length; }

        @Override public String getColumnName(int c) { return columnas[c]; }
        
        @Override public boolean isCellEditable(int f, int c) { return false; }
        
        @Override
        public Object getValueAt(int fila, int col){
            Cliente c = datos.get(fila);
            switch(col){
                case 0: return c.getCuit();
                case 1: return c.getRazonSocial();
                case 2: return c.getNombreFantasia();
                case 3: return c.getDireccion();
                case 4: return c.getImpuestosComoTexto();
                case 5: return c.isTieneEmpleados() ? "Si" : "No";
                default: return "";
            }
        }      
    }
}
