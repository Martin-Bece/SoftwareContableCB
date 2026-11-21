package service.importacion;

import service.importacion.Tabla.Campo;
import view.Modelos.Comprobante;
import view.Modelos.Operacion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee comprobantes desde CSV o Excel (.xlsx / .xls).
 *
 * <p>Columnas (en este orden si el archivo no tiene encabezado):</p>
 * <pre>fecha ; tipo ; ptoVenta ; numero ; cuit ; razonSocial ; neto ; iva [; percepciones]</pre>
 * <p>Con encabezado, cada columna se reconoce por cualquiera de sus nombres (ver {@link #CAMPOS}),
 * en cualquier orden. {@code ptoVenta} y {@code percepciones} son opcionales.</p>
 *
 * <p>Limitacion conocida: el neto y el IVA se cargan en la alicuota 21%.</p>
 */
public class LectorComprobantes {

    static final List<Campo> CAMPOS = List.of(
            new Campo("fecha", true, "fecha", "fecha emision", "fecha de emision", "fecha comprobante"),
            new Campo("tipo", true, "tipo", "tipo comprobante", "tipo de comprobante"),
            new Campo("ptoVenta", false, "ptoVenta", "pto venta", "pto vta", "punto venta", "punto de venta", "pv"),
            new Campo("numero", true, "numero", "nro", "nro comprobante", "numero comprobante", "numero desde"),
            new Campo("cuit", true, "cuit", "cuit contraparte", "nro doc", "nro documento",
                    "nro doc emisor", "nro doc receptor", "cuit emisor", "cuit receptor"),
            new Campo("razonSocial", true, "razonSocial", "razon", "denominacion",
                    "denominacion emisor", "denominacion receptor", "nombre"),
            new Campo("neto", true, "neto", "neto gravado", "imp neto gravado", "importe neto gravado"),
            new Campo("iva", true, "iva", "importe iva", "imp iva", "monto iva"),
            new Campo("percepciones", false, "percepciones", "percepcion", "imp percepciones"));

    public static boolean soporta(File archivo) {
        return LectorFilas.soporta(archivo);
    }

    public List<Comprobante> leer(File archivo, Operacion operacion) {
        String origen = archivo.getName().toLowerCase().matches(".*\\.xlsx?$") ? "Excel" : "CSV";
        List<Comprobante> out = new ArrayList<>();
        for (Registro r : Tabla.registros(LectorFilas.leer(archivo), CAMPOS)) {
            Comprobante c = new Comprobante();
            c.id = null;
            c.operacion = operacion;
            c.fecha = r.fecha("fecha");
            c.tipo = r.textoObligatorio("tipo");
            c.puntoVenta = r.texto("ptoVenta");          // vacio -> el service pone 0001
            c.numero = r.textoObligatorio("numero");
            c.cuitContraparte = r.cuit("cuit");
            c.razonSocial = r.textoObligatorio("razonSocial");
            c.neto21 = r.importe("neto");
            c.iva21 = r.importe("iva");
            c.percepciones = r.importe("percepciones");
            c.origen = origen;
            out.add(c);
        }
        return out;
    }
}
