/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view.paneles;

import view.Modelos.Comprobante;
import view.Modelos.Operacion;
import view.Servicios.ServicioException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import view.AppContext;
import view.Componentes;
import view.DialogoComprobante;
import view.DialogoImportacion;
import view.ModeloComprobantes;
import view.Theme;
 
/** Grilla de ventas o de compras del cliente y periodo activos. */
public class PanelComprobantes extends PanelBase {
 
    private final Operacion operacion;
    private final ModeloComprobantes modelo = new ModeloComprobantes();
    private final JTable tabla = new JTable(modelo);
    private final Componentes.TablaConTotales contenedor;
    private final Componentes.CampoBusqueda busqueda =
            new Componentes.CampoBusqueda("Buscar por razon social, CUIT o numero");
 
    private final Componentes.TarjetaMetrica mCantidad =
            new Componentes.TarjetaMetrica("Comprobantes del periodo", Theme.TEXTO);
    private final Componentes.TarjetaMetrica mNeto =
            new Componentes.TarjetaMetrica("Neto gravado", Theme.TEXTO);
    private final Componentes.TarjetaMetrica mIva;
    private final Componentes.TarjetaMetrica mTotal;
 
    private final Componentes.Boton btnEditar = Componentes.secundario("Editar");
    private final Componentes.Boton btnEliminar = Componentes.peligro("Eliminar");
 
    public PanelComprobantes(AppContext ctx, Operacion operacion) {
        super(ctx, operacion == Operacion.VENTA ? "Ventas" : "Compras", true);
        this.operacion = operacion;
 
        boolean esVenta = operacion == Operacion.VENTA;
        mIva = new Componentes.TarjetaMetrica(esVenta ? "IVA debito fiscal" : "IVA credito fiscal",
                esVenta ? Theme.ROJO : Theme.VERDE);
        mTotal = new Componentes.TarjetaMetrica("Total facturado", Theme.AZUL);
 
        busqueda.setPreferredSize(new Dimension(300, 36));
        busqueda.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { cargarDatos(); }
            @Override public void removeUpdate(DocumentEvent e) { cargarDatos(); }
            @Override public void changedUpdate(DocumentEvent e) { cargarDatos(); }
        });
 
        Componentes.Boton importar = Componentes.secundario("Importar archivo");
        importar.addActionListener(e -> importar());
        Componentes.Boton nuevo = Componentes.primario(esVenta ? "Nueva venta" : "Nueva compra");
        nuevo.addActionListener(e -> abrirFormulario(null));
 
        agregarAccion(busqueda);
        agregarAccion(importar);
        agregarAccion(nuevo);
 
        contenedor = new Componentes.TablaConTotales(tabla);
        contenedor.setMensajeVacio(esVenta
                ? "No hay ventas cargadas en este periodo. Carga una o importa el archivo de AFIP."
                : "No hay compras cargadas en este periodo. Carga una o importa el archivo de AFIP.");
        modelo.configurar(tabla);
 
        tabla.getSelectionModel().addListSelectionListener(e -> actualizarBotones());
        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && seleccionado() != null) abrirFormulario(seleccionado());
            }
        });
        btnEditar.addActionListener(e -> abrirFormulario(seleccionado()));
        btnEliminar.addActionListener(e -> eliminar());
 
        JPanel metricas = new JPanel(new GridLayout(1, 4, 14, 0));
        metricas.setOpaque(false);
        metricas.add(mCantidad);
        metricas.add(mNeto);
        metricas.add(mIva);
        metricas.add(mTotal);
 
        JPanel tarjeta = Theme.tarjeta();
        tarjeta.setBorder(new Theme.LineaBorder(Theme.BORDE, 1, 4));
        tarjeta.add(contenedor, BorderLayout.CENTER);
 
        JPanel barraInferior = new JPanel(new BorderLayout());
        barraInferior.setOpaque(false);
        barraInferior.setBorder(new EmptyBorder(12, 0, 0, 0));
        barraInferior.add(Componentes.filaBotones(btnEditar, btnEliminar), BorderLayout.EAST);
 
        cuerpo.add(metricas, BorderLayout.NORTH);
        cuerpo.add(tarjeta, BorderLayout.CENTER);
        cuerpo.add(barraInferior, BorderLayout.SOUTH);
 
        actualizarBotones();
    }
 
    private Comprobante seleccionado() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : modelo.getEn(tabla.convertRowIndexToModel(fila));
    }
 
    private void actualizarBotones() {
        boolean hay = seleccionado() != null;
        btnEditar.setEnabled(hay);
        btnEliminar.setEnabled(hay);
    }
 
    private void abrirFormulario(Comprobante c) {
        Window padre = SwingUtilities.getWindowAncestor(this);
        DialogoComprobante dlg = new DialogoComprobante(padre, ctx, operacion, c);
        dlg.setVisible(true);
        if (dlg.fueGuardado()) {
            ctx.notificar();
        }
    }
 
    private void eliminar() {
        Comprobante c = seleccionado();
        if (c == null) return;
        if (!Componentes.confirmar(this, "Se elimina " + c.comprobanteCompleto()
                + " de " + c.razonSocial + ". Continuar?")) return;
        ctx.comprobantes().eliminarComprobante(ctx.cuitActivo(), c.id);
        ctx.notificar();
    }
 
    private void importar() {
        File archivo = DialogoImportacion.elegirArchivo(this);
        if (archivo == null) return;
        try {
            List<Comprobante> detectados =
                    ctx.comprobantes().previsualizarComprobantes(archivo, operacion);
            if (detectados.isEmpty()) {
                Componentes.error(this, "El archivo no tiene comprobantes que el sistema pueda leer. "
                        + "Revisa el formato o carga los datos a mano.");
                return;
            }
            ModeloComprobantes previo = new ModeloComprobantes();
            previo.setDatos(detectados);
 
            Window padre = SwingUtilities.getWindowAncestor(this);
            DialogoImportacion dlg = new DialogoImportacion(padre,
                    "Revisar importacion",
                    archivo.getName() + "  ·  se van a cargar como "
                            + (operacion == Operacion.VENTA ? "ventas" : "compras")
                            + " de " + ctx.getClienteActivo().getRazonSocial(),
                    previo, "Importar comprobantes");
            previo.configurar(dlg.getTabla());
            dlg.setVisible(true);
 
            if (dlg.fueConfirmado()) {
                int n = ctx.comprobantes().importarComprobantes(ctx.cuitActivo(), previo.getDatos());
                ctx.notificar();
                Componentes.aviso(this, n + (n == 1 ? " comprobante importado." : " comprobantes importados."));
            }
        } catch (ServicioException ex) {
            Componentes.error(this, ex.getMessage());
        }
    }
 
    @Override
    protected void cargarDatos() {
        setSubtitulo(contextoTexto());
        List<Comprobante> lista = ctx.comprobantes().listarComprobantes(
                ctx.cuitActivo(), operacion, ctx.getPeriodo(), busqueda.getText());
        modelo.setDatos(lista);
 
        mCantidad.setValor(String.valueOf(lista.size()));
        mCantidad.setPie(operacion == Operacion.VENTA ? "emitidos en el mes" : "recibidos en el mes");
        mNeto.setValor(modelo.totalNeto());
        mNeto.setPie("base imponible del periodo");
        mIva.setValor(modelo.totalIva());
        mIva.setPie(operacion == Operacion.VENTA ? "se suma al debito" : "se computa como credito");
        mTotal.setValor(modelo.totalGeneral());
        mTotal.setPie("incluye IVA y percepciones");
 
        contenedor.setTotales("Totales", null, null, null, null,
                modelo.totalNeto(), modelo.totalIva(), modelo.totalPercepciones(),
                modelo.totalGeneral(), null);
        contenedor.actualizarEstadoVacio();
        actualizarBotones();
    }
}

