package service;

import util.Cuit;
import view.Modelos.Comprobante;
import view.Modelos.Operacion;
import view.Servicios.ServicioException;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee comprobantes desde un CSV.
 *
 * <p>Formato (el mismo que aceptaba el mock), una fila por comprobante:</p>
 * <pre>fecha;tipo;ptoVenta;numero;cuit;razonSocial;neto;iva[;percepciones]</pre>
 * <ul>
 *   <li>Separador {@code ;} o {@code ,} (se detecta por linea). Si los importes usan coma
 *       decimal, el separador tiene que ser {@code ;}.</li>
 *   <li>Fecha {@code aaaa-mm-dd} o {@code dd/mm/aaaa}.</li>
 *   <li>Importes {@code 1234.56} o {@code 1.234,56} (con o sin {@code $}).</li>
 *   <li>La primera linea se saltea si es un encabezado (contiene "fecha").</li>
 *   <li>El neto y el IVA se cargan en la alicuota 21% (limitacion conocida del formato).</li>
 *   <li>Archivo en UTF-8; si no lo es, se reintenta como Windows-1252 (lo que guarda Excel).</li>
 * </ul>
 * Cualquier fila invalida corta la lectura con un mensaje que indica la linea.
 */
public class LectorCsvComprobantes {

    private static final DateTimeFormatter DD_MM_AAAA = DateTimeFormatter.ofPattern("d/M/uuuu");

    public List<Comprobante> leer(File archivo, Operacion operacion) {
        List<String> lineas = leerLineas(archivo);
        List<Comprobante> out = new ArrayList<>();
        boolean primera = true;
        int nro = 0;
        for (String linea : lineas) {
            nro++;
            if (primera) {
                linea = linea.replace("﻿", ""); // BOM de Excel
            }
            if (linea.trim().isEmpty()) continue;
            if (primera) {
                primera = false;
                if (linea.toLowerCase().contains("fecha")) continue;
            }
            out.add(parsearLinea(linea, nro, operacion));
        }
        return out;
    }

    private Comprobante parsearLinea(String linea, int nro, Operacion operacion) {
        String[] p = linea.split(linea.contains(";") ? ";" : ",", -1);
        if (p.length < 8) {
            throw new ServicioException("Linea " + nro + ": se esperaban al menos 8 columnas "
                    + "(fecha;tipo;ptoVenta;numero;cuit;razonSocial;neto;iva) y hay " + p.length + ".");
        }
        Comprobante c = new Comprobante();
        c.id = null;
        c.operacion = operacion;
        c.fecha = fecha(p[0], nro);
        c.tipo = p[1].trim();
        c.puntoVenta = p[2].trim();
        c.numero = p[3].trim();
        c.cuitContraparte = Cuit.normalizar(p[4]);
        if (c.cuitContraparte == null) {
            throw new ServicioException("Linea " + nro + ": el CUIT '" + p[4].trim() + "' no tiene 11 digitos.");
        }
        c.razonSocial = p[5].trim();
        c.neto21 = importe(p[6], nro, "neto");
        c.iva21 = importe(p[7], nro, "IVA");
        if (p.length > 8) c.percepciones = importe(p[8], nro, "percepciones");
        c.origen = "CSV";
        return c;
    }

    static LocalDate fecha(String texto, int nro) {
        String t = texto.trim();
        try {
            return t.contains("/") ? LocalDate.parse(t, DD_MM_AAAA) : LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new ServicioException("Linea " + nro + ": la fecha '" + t
                    + "' no es valida (usa aaaa-mm-dd o dd/mm/aaaa).");
        }
    }

    /** Igual criterio que {@code Theme.parsear}, pero estricto: un valor ilegible es error, no cero. */
    static BigDecimal importe(String texto, int nro, String campo) {
        String limpio = texto.replace("$", "").replace(" ", "").trim();
        if (limpio.isEmpty()) return BigDecimal.ZERO;
        if (limpio.contains(",")) {
            limpio = limpio.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            throw new ServicioException("Linea " + nro + ": el importe de " + campo + " '" + texto.trim()
                    + "' no es un numero valido.");
        }
    }

    static List<String> leerLineas(File archivo) {
        try {
            return leerCon(archivo, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            try {
                return leerCon(archivo, Charset.forName("windows-1252"));
            } catch (IOException e2) {
                throw new ServicioException("No se pudo leer el archivo: " + e2.getMessage(), e2);
            }
        } catch (IOException e) {
            throw new ServicioException("No se pudo leer el archivo: " + e.getMessage(), e);
        }
    }

    private static List<String> leerCon(File archivo, Charset charset) throws IOException {
        return Files.readAllLines(archivo.toPath(), charset);
    }
}
