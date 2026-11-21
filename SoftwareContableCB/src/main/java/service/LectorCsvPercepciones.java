package service;

import util.Cuit;
import view.Modelos.Clase;
import view.Modelos.PercepcionRetencion;
import view.Modelos.Tributo;
import view.Servicios.ServicioException;

import java.io.File;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee percepciones y retenciones desde un CSV.
 *
 * <p>Formato, una fila por movimiento:</p>
 * <pre>fecha;clase;tributo;regimen;jurisdiccion;comprobante;cuitAgente;razonSocialAgente;importe</pre>
 * <ul>
 *   <li>{@code clase}: Percepcion / Retencion (o P / R).</li>
 *   <li>{@code tributo}: IVA, Ganancias, SUSS, IIBB (tambien "Ingresos Brutos").</li>
 *   <li>Mismas reglas que {@link LectorCsvComprobantes} para separador, fecha, importes,
 *       encabezado opcional y codificacion.</li>
 * </ul>
 */
public class LectorCsvPercepciones {

    public List<PercepcionRetencion> leer(File archivo) {
        List<PercepcionRetencion> out = new ArrayList<>();
        boolean primera = true;
        int nro = 0;
        for (String linea : LectorCsvComprobantes.leerLineas(archivo)) {
            nro++;
            if (primera) linea = linea.replace("﻿", "");
            if (linea.trim().isEmpty()) continue;
            if (primera) {
                primera = false;
                if (linea.toLowerCase().contains("fecha")) continue;
            }
            out.add(parsearLinea(linea, nro));
        }
        return out;
    }

    private PercepcionRetencion parsearLinea(String linea, int nro) {
        String[] p = linea.split(linea.contains(";") ? ";" : ",", -1);
        if (p.length < 9) {
            throw new ServicioException("Linea " + nro + ": se esperaban 9 columnas (fecha;clase;tributo;regimen;"
                    + "jurisdiccion;comprobante;cuitAgente;razonSocialAgente;importe) y hay " + p.length + ".");
        }
        PercepcionRetencion r = new PercepcionRetencion();
        r.id = null;
        r.fecha = LectorCsvComprobantes.fecha(p[0], nro);
        r.clase = clase(p[1], nro);
        r.tributo = tributo(p[2], nro);
        r.regimen = p[3].trim();
        r.jurisdiccion = p[4].trim();
        r.comprobante = p[5].trim();
        r.cuitAgente = Cuit.normalizar(p[6]);
        if (r.cuitAgente == null) {
            throw new ServicioException("Linea " + nro + ": el CUIT del agente '" + p[6].trim()
                    + "' no tiene 11 digitos.");
        }
        r.razonSocialAgente = p[7].trim();
        r.importe = LectorCsvComprobantes.importe(p[8], nro, "importe");
        r.origen = "CSV";
        return r;
    }

    static Clase clase(String texto, int nro) {
        String t = simplificar(texto);
        if (t.equals("p") || t.startsWith("percep")) return Clase.PERCEPCION;
        if (t.equals("r") || t.startsWith("retenc")) return Clase.RETENCION;
        throw new ServicioException("Linea " + nro + ": la clase '" + texto.trim()
                + "' tiene que ser Percepcion o Retencion.");
    }

    static Tributo tributo(String texto, int nro) {
        String t = simplificar(texto);
        switch (t) {
            case "iva": return Tributo.IVA;
            case "ganancias": return Tributo.GANANCIAS;
            case "suss": return Tributo.SUSS;
            case "iibb": case "ingresos brutos": return Tributo.IIBB;
            default:
                throw new ServicioException("Linea " + nro + ": el tributo '" + texto.trim()
                        + "' no es valido (IVA, Ganancias, SUSS o IIBB).");
        }
    }

    /** Minusculas, sin espacios de mas y sin acentos ("Percepción" -> "percepcion"). */
    private static String simplificar(String s) {
        String sinAcentos = Normalizer.normalize(s.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.replaceAll("\\s+", " ");
    }
}
