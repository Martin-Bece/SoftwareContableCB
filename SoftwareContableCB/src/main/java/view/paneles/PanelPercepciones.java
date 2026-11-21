package view.paneles;

import view.Modelos.Clase;
import view.Modelos.PercepcionRetencion;
import view.Modelos.Tributo;
import view.Servicios.ServicioException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import view.AppContext;
import view.Componentes;
import view.DialogoImportacion;
import view.DialogoPercepcion;
import view.ModeloPercepciones;
import view.Modelos;
import view.Theme;

/** Percepciones y retenciones sufridas por el cliente en el periodo. */
public class PanelPercepciones extends PanelBase {

    private final ModeloPercepciones modelo = new ModeloPercepciones();
    private final JTable tabla = new JTable(modelo);
    private final Componentes.TablaConTotales contenedor;

    private final JComboBox<Object> cboClase = new JComboBox<>();
    private final JComboBox<Object> cboTributo = new JComboBox<>();

    private final Componentes.TarjetaMetrica mIva =
            new Componentes.TarjetaMetrica("Ingresos a cuenta de IVA", Theme.VERDE);
    private final Componentes.TarjetaMetrica mGanancias =
            new Componentes.TarjetaMetrica("Ingresos a cuenta de Ganancias", Theme.AZUL);
    private final Componentes.TarjetaMetrica mOtros =
            new Componentes.TarjetaMetrica("Ingresos brutos y SUSS", Theme.TEXTO);
    private final Componentes.TarjetaMetrica mTotal =
            new Componentes.TarjetaMetrica("Total del periodo", Theme.TEXTO);

    private final Componentes.Boton btnEditar = Componentes.secundario("Editar");
    private final Componentes.Boton btnEliminar = Componentes.peligro("Eliminar");

    public PanelPercepciones(AppContext ctx) {
        super(ctx, "Percepciones y retenciones", true);

        cboClase.addItem("Todas las clases");
        for (Clase c : Clase.values()) cboClase.addItem(c);
        cboTributo.addItem("Todos los tributos");
        for (Tributo t : Tributo.values()) cboTributo.addItem(t);
        Componentes.estilizarCombo(cboClase);
        Componentes.estilizarCombo(cboTributo);
        cboClase.setPreferredSize(new Dimension(170, 36));
        cboTributo.setPreferredSize(new Dimension(180, 36));
        cboClase.addActionListener(e -> cargarDatos());
        cboTributo.addActionListener(e -> cargarDatos());

        Componentes.Boton importar = Componentes.secundario("Importar archivo");
        importar.addActionListener(e -> importar());
        Componentes.Boton nuevo = Componentes.primario("Nuevo movimiento");
        nuevo.addActionListener(e -> abrirFormulario(null));

        agregarAccion(cboClase);
        agregarAccion(cboTributo);
        agregarAccion(importar);
        agregarAccion(nuevo);

        contenedor = new Componentes.TablaConTotales(tabla);
        contenedor.setMensajeVacio("No hay percepciones ni retenciones en este periodo.");
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
        metricas.add(mIva);
        metricas.add(mGanancias);
        metricas.add(mOtros);
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

    private PercepcionRetencion seleccionado() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : modelo.getEn(tabla.convertRowIndexToModel(fila));
    }

    private void actualizarBotones() {
        boolean hay = seleccionado() != null;
        btnEditar.setEnabled(hay);
        btnEliminar.setEnabled(hay);
    }

    private void abrirFormulario(PercepcionRetencion p) {
        Window padre = SwingUtilities.getWindowAncestor(this);
        DialogoPercepcion dlg = new DialogoPercepcion(padre, ctx, p);
        dlg.setVisible(true);
        if (dlg.fueGuardado()) ctx.notificar();
    }

    private void eliminar() {
        PercepcionRetencion p = seleccionado();
        if (p == null) return;
        if (!Componentes.confirmar(this, "Se elimina la " + p.clase.rotulo.toLowerCase()
                + " de " + Theme.pesos(p.importe) + ". Continuar?")) return;
        ctx.percepciones().eliminarPercepcion(ctx.cuitActivo(), p.id);
        ctx.notificar();
    }

    private void importar() {
        File archivo = DialogoImportacion.elegirArchivo(this);
        if (archivo == null) return;
        try {
            List<PercepcionRetencion> detectadas = ctx.percepciones().previsualizarPercepciones(archivo);
            if (detectadas.isEmpty()) {
                Componentes.error(this, "El archivo no tiene movimientos que el sistema pueda leer.");
                return;
            }
            ModeloPercepciones previo = new ModeloPercepciones();
            previo.setDatos(detectadas);

            Window padre = SwingUtilities.getWindowAncestor(this);
            DialogoImportacion dlg = new DialogoImportacion(padre, "Revisar importacion",
                    archivo.getName() + "  ·  se imputan a "
                            + ctx.getClienteActivo().getRazonSocial(),
                    previo, "Importar movimientos");
            previo.configurar(dlg.getTabla());
            dlg.setVisible(true);

            if (dlg.fueConfirmado()) {
                int n = ctx.percepciones().importarPercepciones(ctx.cuitActivo(), previo.getDatos());
                ctx.notificar();
                Componentes.aviso(this, n + (n == 1 ? " movimiento importado." : " movimientos importados."));
            }
        } catch (ServicioException ex) {
            Componentes.error(this, ex.getMessage());
        }
    }

    @Override
    protected void cargarDatos() {
        setSubtitulo(contextoTexto());
        Object selClase = cboClase.getSelectedItem();
        Object selTributo = cboTributo.getSelectedItem();
        Clase clase = selClase instanceof Clase ? (Clase) selClase : null;
        Tributo tributo = selTributo instanceof Tributo ? (Tributo) selTributo : null;

        List<PercepcionRetencion> lista = ctx.percepciones()
                .listarPercepciones(ctx.cuitActivo(), ctx.getPeriodo(), clase, tributo);
        modelo.setDatos(lista);

        BigDecimal iva = BigDecimal.ZERO, ganancias = BigDecimal.ZERO, otros = BigDecimal.ZERO;
        for (PercepcionRetencion p : lista) {
            BigDecimal i = Modelos.nz(p.importe);
            if (p.tributo == Tributo.IVA) iva = iva.add(i);
            else if (p.tributo == Tributo.GANANCIAS) ganancias = ganancias.add(i);
            else otros = otros.add(i);
        }

        mIva.setValor(iva);
        mIva.setPie("se descuentan del IVA del mes");
        mGanancias.setValor(ganancias);
        mGanancias.setPie("se computan en la DDJJ anual");
        mOtros.setValor(otros);
        mOtros.setPie("otros regimenes");
        mTotal.setValor(modelo.total());
        mTotal.setPie(lista.size() + (lista.size() == 1 ? " movimiento" : " movimientos"));

        contenedor.setTotales("Totales", null, null, null, null, null, null, null, modelo.total());
        contenedor.actualizarEstadoVacio();
        actualizarBotones();
    }
}