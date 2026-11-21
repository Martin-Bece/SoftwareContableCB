package service.importacion;

import util.Cuit;
import view.Servicios.ServicioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Una fila de datos ya asociada a sus columnas. Convierte cada campo al tipo que corresponde
 * y, si algo esta mal, lanza un error que dice en que fila y en que campo.
 */
final class Registro {

    private static final DateTimeFormatter DD_MM_AAAA = DateTimeFormatter.ofPattern("d/M/uuuu");

    private final Fila fila;
    private final Map<String, Integer> columnas;

    Registro(Fila fila, Map<String, Integer> columnas) {
        this.fila = fila;
        this.columnas = columnas;
    }

    private Celda celda(String campo) {
        Integer col = columnas.get(campo);
        return col == null ? Celda.VACIA : fila.celda(col);
    }

    private ServicioException error(String mensaje) {
        return new ServicioException(fila.ubicacion() + ": " + mensaje);
    }

    /** Texto recortado; "" si la columna no existe o esta vacia. */
    String texto(String campo) {
        return celda(campo).texto().trim();
    }

    String textoObligatorio(String campo) {
        String t = texto(campo);
        if (t.isEmpty()) throw error("falta " + campo + ".");
        return t;
    }

    /** Fecha de Excel, o texto {@code aaaa-mm-dd} / {@code dd/mm/aaaa}. */
    LocalDate fecha(String campo) {
        Celda c = celda(campo);
        if (c.valor() instanceof LocalDate d) return d;
        String t = c.texto().trim();
        if (t.isEmpty()) throw error("falta " + campo + ".");
        try {
            return t.contains("/") ? LocalDate.parse(t, DD_MM_AAAA) : LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw error("la fecha '" + t + "' no es valida (usa aaaa-mm-dd o dd/mm/aaaa).");
        }
    }

    /**
     * Numero de Excel (redondeado a centavos), o texto {@code 1234.56} / {@code 1.234,56} / {@code $ 1000}.
     * Vacio = 0.
     */
    BigDecimal importe(String campo) {
        Celda c = celda(campo);
        if (c.valor() instanceof BigDecimal n) {
            return n.scale() > 2 ? n.setScale(2, RoundingMode.HALF_UP) : n;
        }
        String limpio = c.texto().replace("$", "").replace(" ", "").trim();
        if (limpio.isEmpty()) return BigDecimal.ZERO;
        if (limpio.contains(",")) limpio = limpio.replace(".", "").replace(",", ".");
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            throw error("el importe de " + campo + " '" + c.texto().trim() + "' no es un numero valido.");
        }
    }

    /** CUIT normalizado a XX-XXXXXXXX-X. */
    String cuit(String campo) {
        String t = texto(campo);
        String cuit = Cuit.normalizar(t);
        if (cuit == null) throw error("el " + campo + " '" + t + "' no tiene 11 digitos.");
        return cuit;
    }

    ServicioException errorEn(String mensaje) {
        return error(mensaje);
    }
}
