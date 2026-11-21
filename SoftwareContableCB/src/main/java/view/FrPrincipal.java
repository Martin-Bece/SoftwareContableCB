/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
/**
 *
 * @author Usuario
 */
public class FrPrincipal extends javax.swing.JFrame {
    
    private final Color AZUL = new Color(66, 91, 140);
    private final Color ROJO = new Color(191, 27, 27);
    private final Color FONDO = new Color(246, 250, 249);
    private final Color BLANCO = Color.WHITE;
    
    

    /**
     * Creates new form FrPrincipal
     */
    public FrPrincipal() {
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

        // =====================================================
        // PANEL PRINCIPAL
        // =====================================================

        JPanel panelPrincipal = new JPanel(new BorderLayout());
        panelPrincipal.setBackground(FONDO);


        // =====================================================
        // ENCABEZADO
        // =====================================================

        JPanel panelEncabezado = new JPanel();
        panelEncabezado.setLayout(new GridLayout(2, 1));
        panelEncabezado.setBackground(AZUL);
        panelEncabezado.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        JLabel titulo = new JLabel(
                "SOFTWARE CONTABLE",
                SwingConstants.CENTER
        );

        titulo.setForeground(BLANCO);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 28));

        JLabel subtitulo = new JLabel(
                "Contadora Carolina Becerra",
                SwingConstants.CENTER
        );

        subtitulo.setForeground(BLANCO);
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 18));

        panelEncabezado.add(titulo);
        panelEncabezado.add(subtitulo);


        // =====================================================
        // PANEL CENTRAL
        // =====================================================

        JPanel panelCentral = new JPanel(new GridLayout(2, 1, 15, 15));
        panelCentral.setBackground(FONDO);
        panelCentral.setBorder(
                BorderFactory.createEmptyBorder(25, 40, 25, 40)
        );


        // =====================================================
        // GESTIÓN
        // =====================================================

        JPanel panelGestion = crearPanelSeccion("GESTIÓN");

        JButton btnClientes = crearBoton("Clientes");
        JButton btnProveedores = crearBoton("Proveedores");
        JButton btnClientesClientes = crearBoton("Clientes de Clientes");

        panelGestion.add(btnClientes);
        panelGestion.add(btnProveedores);
        panelGestion.add(btnClientesClientes);


        // =====================================================
        // COMPROBANTES
        // =====================================================

        JPanel panelComprobantes = crearPanelSeccion("COMPROBANTES");

        JButton btnVentas = crearBoton("Ventas");
        JButton btnCompras = crearBoton("Compras");
        JButton btnRetenciones = crearBoton(
                "Retenciones / Percepciones"
        );

        panelComprobantes.add(btnVentas);
        panelComprobantes.add(btnCompras);
        panelComprobantes.add(btnRetenciones);


        // =====================================================
        // REPORTES
        // =====================================================

        JPanel panelReportes = crearPanelSeccion("REPORTES");

        JButton btnReporteVentas = crearBoton("Ventas");
        JButton btnReporteCompras = crearBoton("Compras");
        JButton btnReporteAmbos = crearBoton("Ventas y Compras");
        JButton btnReporteImpuestos = crearBoton("Impuestos");

        panelReportes.add(btnReporteVentas);
        panelReportes.add(btnReporteCompras);
        panelReportes.add(btnReporteAmbos);
        panelReportes.add(btnReporteImpuestos);


        // =====================================================
        // AGREGAR TODO
        // =====================================================

        JPanel panelContenido = new JPanel(new GridLayout(3, 1, 10, 10));
        panelContenido.setBackground(FONDO);

        panelContenido.add(panelGestion);
        panelContenido.add(panelComprobantes);
        panelContenido.add(panelReportes);

        panelCentral.add(panelContenido);

        panelPrincipal.add(panelEncabezado, BorderLayout.NORTH);
        panelPrincipal.add(panelCentral, BorderLayout.CENTER);

        add(panelPrincipal);


        // =====================================================
        // EVENTOS
        // =====================================================

        btnClientes.addActionListener(e -> abrirClientes());

        // Los demás los conectaremos cuando creemos sus JFrame.
        btnProveedores.addActionListener(e ->
                System.out.println("Abrir Proveedores")
        );

        btnClientesClientes.addActionListener(e ->
                System.out.println("Abrir Clientes de Clientes")
        );

        btnVentas.addActionListener(e ->
                System.out.println("Abrir Ventas")
        );

        btnCompras.addActionListener(e ->
                System.out.println("Abrir Compras")
        );

        btnRetenciones.addActionListener(e ->
                System.out.println("Abrir Retenciones / Percepciones")
        );

        btnReporteVentas.addActionListener(e ->
                System.out.println("Abrir Reporte de Ventas")
        );

        btnReporteCompras.addActionListener(e ->
                System.out.println("Abrir Reporte de Compras")
        );

        btnReporteAmbos.addActionListener(e ->
                System.out.println("Abrir Reporte de Ventas y Compras")
        );

        btnReporteImpuestos.addActionListener(e ->
                System.out.println("Abrir Reporte de Impuestos")
        );
    }
    
    private JPanel crearPanelSeccion(String titulo) {

        JPanel panel = new JPanel(new GridLayout(1, 4, 15, 15));
        panel.setBackground(FONDO);

        panel.setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(AZUL),
                        titulo,
                        0,
                        0,
                        new Font("Segoe UI", Font.BOLD, 15),
                        AZUL
                )
        );

        return panel;
    }
    
    private JButton crearBoton(String texto) {

        JButton boton = new JButton(texto);

        boton.setBackground(ROJO);
        boton.setForeground(BLANCO);
        boton.setFont(
                new Font("Segoe UI", Font.BOLD, 14)
        );

        boton.setFocusPainted(false);

        boton.setBorder(
                BorderFactory.createEmptyBorder(
                        12, 15, 12, 15
                )
        );

        boton.setMargin(
                new Insets(10, 10, 10, 10)
        );

        boton.setCursor(
                new java.awt.Cursor(
                        java.awt.Cursor.HAND_CURSOR
                )
        );

        return boton;
    }
    
    private void abrirClientes() {

        FrClientes ventana = new FrClientes();

        ventana.setVisible(true);
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

            FrPrincipal ventana = new FrPrincipal();

            ventana.setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
