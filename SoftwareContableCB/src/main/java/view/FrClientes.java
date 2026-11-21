/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Usuario
 */
public class FrClientes extends javax.swing.JFrame {
    
    private final Color AZUL = new Color(66, 91, 140);
    private final Color ROJO = new Color(191, 27, 27);
    private final Color ROJO_OSCURO = new Color(140, 15, 15);
    private final Color FONDO = new Color(246, 250, 249);
    private final Color BLANCO = Color.WHITE;
    
    private JTextField txtCuit;
    private JTextField txtRazonSocial;
    private JTextField txtNombreFantasia;
    private JTextField txtDireccion;
    private JTextField txtActividades;
    private JTextField txtImpuestos;

    private JCheckBox chkTieneEmpleados;

    private JTable tablaClientes;

    private DefaultTableModel modeloTabla;
    
    

    /**
     * Creates new form FrClientes
     */
    public FrClientes() {
        configurarVentana();
        crearInterfaz();
    }
    
    private void configurarVentana() {

        setTitle("Gestión de Clientes");

        setSize(1000, 700);

        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
    
    private void crearInterfaz() {

        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));

        panelPrincipal.setBackground(FONDO);

        panelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(
                        15, 15, 15, 15
                )
        );


        // =================================================
        // TÍTULO
        // =================================================

        JLabel titulo = new JLabel(
                "GESTIÓN DE CLIENTES"
        );

        titulo.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        25
                )
        );

        titulo.setForeground(AZUL);


        JPanel panelTitulo = new JPanel(
                new FlowLayout(
                        FlowLayout.LEFT
                )
        );

        panelTitulo.setBackground(FONDO);

        panelTitulo.add(titulo);


        // =================================================
        // FORMULARIO
        // =================================================

        JPanel panelFormulario = new JPanel(
                new GridBagLayout()
        );

        panelFormulario.setBackground(BLANCO);

        panelFormulario.setBorder(
                BorderFactory.createTitledBorder(
                        "Datos del cliente"
                )
        );


        txtCuit = new JTextField(20);
        txtRazonSocial = new JTextField(20);
        txtNombreFantasia = new JTextField(20);
        txtDireccion = new JTextField(20);
        txtActividades = new JTextField(20);
        txtImpuestos = new JTextField(20);

        chkTieneEmpleados = new JCheckBox(
                "Sí"
        );

        chkTieneEmpleados.setBackground(BLANCO);


        agregarCampo(
                panelFormulario,
                0,
                "CUIT:",
                txtCuit
        );

        agregarCampo(
                panelFormulario,
                1,
                "Razón Social:",
                txtRazonSocial
        );

        agregarCampo(
                panelFormulario,
                2,
                "Nombre de Fantasía:",
                txtNombreFantasia
        );

        agregarCampo(
                panelFormulario,
                3,
                "Dirección:",
                txtDireccion
        );

        agregarCampo(
                panelFormulario,
                4,
                "Actividades:",
                txtActividades
        );

        agregarCampo(
                panelFormulario,
                5,
                "Impuestos:",
                txtImpuestos
        );

        agregarCampo(
                panelFormulario,
                6,
                "Tiene empleados:",
                chkTieneEmpleados
        );


        // =================================================
        // BOTONES
        // =================================================

        JButton btnNuevo = crearBoton("Nuevo");

        JButton btnGuardar = crearBoton("Guardar");

        JButton btnModificar = crearBoton("Modificar");

        JButton btnEliminar = crearBoton("Eliminar");

        JButton btnLimpiar = crearBoton("Limpiar");


        JPanel panelBotones = new JPanel(
                new FlowLayout(
                        FlowLayout.CENTER,
                        10,
                        10
                )
        );

        panelBotones.setBackground(BLANCO);

        panelBotones.add(btnNuevo);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnModificar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnLimpiar);


        // =================================================
        // TABLA
        // =================================================

        String[] columnas = {
            "CUIT",
            "Razón Social",
            "Nombre Fantasía",
            "Dirección",
            "Actividades",
            "Impuestos",
            "Empleados"
        };


        modeloTabla = new DefaultTableModel(
                columnas,
                0
        ) {

            @Override
            public boolean isCellEditable(
                    int fila,
                    int columna
            ) {
                return false;
            }
        };


        tablaClientes = new JTable(
                modeloTabla
        );

        tablaClientes.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tablaClientes.setRowHeight(25);

        tablaClientes.getTableHeader()
                .setFont(
                        new Font(
                                "Segoe UI",
                                Font.BOLD,
                                13
                        )
                );


        JScrollPane scrollTabla =
                new JScrollPane(tablaClientes);

        scrollTabla.setBorder(
                BorderFactory.createTitledBorder(
                        "Clientes registrados"
                )
        );


        // =================================================
        // PANEL SUPERIOR
        // =================================================

        JPanel panelSuperior = new JPanel(
                new BorderLayout()
        );

        panelSuperior.setBackground(FONDO);

        panelSuperior.add(
                panelTitulo,
                BorderLayout.NORTH
        );

        JPanel formularioCompleto =
                new JPanel(new BorderLayout());

        formularioCompleto.setBackground(FONDO);

        formularioCompleto.add(
                panelFormulario,
                BorderLayout.CENTER
        );

        formularioCompleto.add(
                panelBotones,
                BorderLayout.SOUTH
        );

        panelSuperior.add(
                formularioCompleto,
                BorderLayout.CENTER
        );


        // =================================================
        // AGREGAR TODO
        // =================================================

        panelPrincipal.add(
                panelSuperior,
                BorderLayout.NORTH
        );

        panelPrincipal.add(
                scrollTabla,
                BorderLayout.CENTER
        );


        add(panelPrincipal);


        // =================================================
        // EVENTOS
        // =================================================

        btnLimpiar.addActionListener(
                e -> limpiarFormulario()
        );

        btnNuevo.addActionListener(
                e -> nuevoCliente()
        );

        btnGuardar.addActionListener(
                e -> guardarCliente()
        );

        btnModificar.addActionListener(
                e -> modificarCliente()
        );

        btnEliminar.addActionListener(
                e -> eliminarCliente()
        );


        // Al seleccionar una fila se cargan sus datos
        tablaClientes.getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!e.getValueIsAdjusting()) {

                        cargarClienteSeleccionado();
                    }
                });
    }
    
    private void agregarCampo(
            JPanel panel,
            int fila,
            String texto,
            java.awt.Component componente
    ) {

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets =
                new Insets(5, 10, 5, 10);

        gbc.fill =
                GridBagConstraints.HORIZONTAL;


        gbc.gridx = 0;
        gbc.gridy = fila;

        gbc.weightx = 0;

        JLabel label =
                new JLabel(texto);

        label.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        13
                )
        );

        panel.add(
                label,
                gbc
        );


        gbc.gridx = 1;

        gbc.weightx = 1;

        panel.add(
                componente,
                gbc
        );
    }
    
    private JButton crearBoton(
            String texto
    ) {

        JButton boton =
                new JButton(texto);

        boton.setBackground(ROJO);

        boton.setForeground(BLANCO);

        boton.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        13
                )
        );

        boton.setFocusPainted(false);

        return boton;
    }
    
    private void limpiarFormulario() {

        txtCuit.setText("");

        txtRazonSocial.setText("");

        txtNombreFantasia.setText("");

        txtDireccion.setText("");

        txtActividades.setText("");

        txtImpuestos.setText("");

        chkTieneEmpleados.setSelected(false);

        tablaClientes.clearSelection();
    }
    
    private void nuevoCliente() {

        limpiarFormulario();

        txtCuit.requestFocus();
    }
    
    private void guardarCliente() {

        String cuit =
                txtCuit.getText();

        String razonSocial =
                txtRazonSocial.getText();

        String fantasia =
                txtNombreFantasia.getText();

        String direccion =
                txtDireccion.getText();

        String actividades =
                txtActividades.getText();

        String impuestos =
                txtImpuestos.getText();

        String empleados =
                chkTieneEmpleados.isSelected()
                ? "Sí"
                : "No";


        modeloTabla.addRow(
                new Object[]{
                    cuit,
                    razonSocial,
                    fantasia,
                    direccion,
                    actividades,
                    impuestos,
                    empleados
                }
        );


        limpiarFormulario();
    }
    
    private void modificarCliente() {

        int fila =
                tablaClientes.getSelectedRow();

        if (fila == -1) {

            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Seleccione un cliente de la tabla."
            );

            return;
        }


        modeloTabla.setValueAt(
                txtCuit.getText(),
                fila,
                0
        );

        modeloTabla.setValueAt(
                txtRazonSocial.getText(),
                fila,
                1
        );

        modeloTabla.setValueAt(
                txtNombreFantasia.getText(),
                fila,
                2
        );

        modeloTabla.setValueAt(
                txtDireccion.getText(),
                fila,
                3
        );

        modeloTabla.setValueAt(
                txtActividades.getText(),
                fila,
                4
        );

        modeloTabla.setValueAt(
                txtImpuestos.getText(),
                fila,
                5
        );

        modeloTabla.setValueAt(
                chkTieneEmpleados.isSelected()
                ? "Sí"
                : "No",
                fila,
                6
        );


        limpiarFormulario();
    }
    
    private void eliminarCliente() {

        int fila =
                tablaClientes.getSelectedRow();

        if (fila == -1) {

            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Seleccione un cliente de la tabla."
            );

            return;
        }


        int respuesta =
                javax.swing.JOptionPane.showConfirmDialog(
                        this,
                        "¿Está seguro de eliminar el cliente?",
                        "Confirmar eliminación",
                        javax.swing.JOptionPane.YES_NO_OPTION
                );


        if (respuesta ==
                javax.swing.JOptionPane.YES_OPTION) {

            modeloTabla.removeRow(fila);
        }
    }
    
    private void cargarClienteSeleccionado() {

        int fila =
                tablaClientes.getSelectedRow();

        if (fila == -1) {
            return;
        }


        txtCuit.setText(
                modeloTabla.getValueAt(fila, 0)
                        .toString()
        );

        txtRazonSocial.setText(
                modeloTabla.getValueAt(fila, 1)
                        .toString()
        );

        txtNombreFantasia.setText(
                modeloTabla.getValueAt(fila, 2)
                        .toString()
        );

        txtDireccion.setText(
                modeloTabla.getValueAt(fila, 3)
                        .toString()
        );

        txtActividades.setText(
                modeloTabla.getValueAt(fila, 4)
                        .toString()
        );

        txtImpuestos.setText(
                modeloTabla.getValueAt(fila, 5)
                        .toString()
        );

        String empleados =
                modeloTabla.getValueAt(fila, 6)
                        .toString();

        chkTieneEmpleados.setSelected(
                empleados.equals("Sí")
        );
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        java.awt.EventQueue.invokeLater(() -> {

            FrClientes ventana = new FrClientes();

            ventana.setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
