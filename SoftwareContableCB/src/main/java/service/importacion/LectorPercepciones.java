package service.importacion;

import service.importacion.Tabla.Campo;
import view.Modelos.Clase;
import view.Modelos.PercepcionRetencion;
import view.Modelos.Tributo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee percepciones y retenciones desde CSV o Excel (.xlsx / .xls).
 *
 * <p>Columnas (en este orden si el archivo no tiene encabezado):</p>
 * <pre>fecha ; clase ; tributo ; regimen ; jurisdiccion ; comprobante ; cuitAgente ; razonSocialAgente ; importe</pre>
 * <ul>
 *   <li>{@code clase}: Percepcion / Retencion (o P / R, con o sin acento).</li>
 *   <li>{@code tributo}: IVA, Ganancias, SUSS, IIBB o "Ingresos Brutos".</li>
 *   <li>Con encabezado: columnas por nombre, en cualquier orden. Regimen, jurisdiccion, comprobante
 *       y razon social del agente son opcionales.</li>
 * </ul>
 */
public class LectorPercepciones {

    static final List<Campo> CAMPOS = List.of(
            new Campo("fecha", true, "fecha", "fecha percepcion", "fecha retencion", "fecha comprobante"),
            new Campo("clase", true, "clase", "tipo movimiento", "percepcion o retencion"),
            new Campo("tributo", true, "tributo", "impuesto"),
            new Campo("regimen", false, "regimen", "regimen percepcion", "regimen retencion"),
            new Campo("jurisdiccion", false, "jurisdiccion"),
            new Campo("comprobante", false, "comprobante", "nro comprobante", "numero comprobante", "certificado"),
            new Campo("cuitAgente", true, "cuitAgente", "cuit agente", "cuit del agente", "cuit"),
            new Campo("razonSocialAgente", false, "razonSocialAgente", "razon social agente", "agente",
                    "denominacion agente", "razon social", "denominacion"),
            new Campo("importe", true, "importe", "monto", "importe percibido", "importe retenido"));

    public static boolean soporta(File archivo) {
        return LectorFilas.soporta(archivo);
    }

    public List<PercepcionRetencion> leer(File archivo) {
        String origen = archivo.getName().toLowerCase().matches(".*\\.xlsx?$") ? "Excel" : "CSV";
        List<PercepcionRetencion> out = new ArrayList<>();
        for (Registro r : Tabla.registros(LectorFilas.leer(archivo), CAMPOS)) {
            PercepcionRetencion p = new PercepcionRetencion();
            p.id = null;
            p.fecha = r.fecha("fecha");
            p.clase = clase(r);
            p.tributo = tributo(r);
            p.regimen = r.texto("regimen");
            p.jurisdiccion = r.texto("jurisdiccion");
            p.comprobante = r.texto("comprobante");
            p.cuitAgente = r.cuit("cuitAgente");
            p.razonSocialAgente = r.texto("razonSocialAgente");
            p.importe = r.importe("importe");
            p.origen = origen;
            out.add(p);
        }
        return out;
    }

    private static Clase clase(Registro r) {
        String t = Tabla.normalizar(r.texto("clase"));
        if (t.equals("p") || t.startsWith("percep")) return Clase.PERCEPCION;
        if (t.equals("r") || t.startsWith("retenc")) return Clase.RETENCION;
        throw r.errorEn("la clase '" + r.texto("clase") + "' tiene que ser Percepcion o Retencion.");
    }

    private static Tributo tributo(Registro r) {
        switch (Tabla.normalizar(r.texto("tributo"))) {
            case "iva": return Tributo.IVA;
            case "ganancias": return Tributo.GANANCIAS;
            case "suss": return Tributo.SUSS;
            case "iibb": case "ingresosbrutos": return Tributo.IIBB;
            default:
                throw r.errorEn("el tributo '" + r.texto("tributo") + "' no es valido (IVA, Ganancias, SUSS o IIBB).");
        }
    }
}
