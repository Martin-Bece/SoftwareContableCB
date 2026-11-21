package service.importacion;

import view.Servicios.ServicioException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decide que columna del archivo es cada campo.
 *
 * <ul>
 *   <li><b>Con encabezado</b> (una fila, entre las primeras {@value #FILAS_BUSQUEDA}, que tiene una
 *       celda que empieza con "fecha"): cada columna se ubica <b>por su nombre</b>, comparando contra
 *       los alias de cada {@link Campo} sin importar mayusculas, acentos, espacios ni signos. El orden
 *       de las columnas da igual, las columnas que no se usan se ignoran y las filas de titulo que
 *       esten arriba del encabezado se saltean.</li>
 *   <li><b>Sin encabezado</b>: las columnas se toman en el orden en que estan declarados los campos.</li>
 * </ul>
 */
final class Tabla {

    static final int FILAS_BUSQUEDA = 10;

    /**
     * Un campo que se espera en el archivo.
     * @param nombre      nombre para mostrar en mensajes
     * @param obligatorio si falta la columna (con encabezado) el archivo se rechaza
     * @param alias       nombres de columna aceptados (se normalizan solos)
     */
    record Campo(String nombre, boolean obligatorio, String... alias) { }

    private Tabla() { }

    /** Devuelve un {@link Registro} por cada fila de datos. */
    static List<Registro> registros(List<Fila> filas, List<Campo> campos) {
        int encabezado = buscarEncabezado(filas);
        Map<String, Integer> columnas;
        int primeraDeDatos;
        boolean posicional = encabezado < 0;

        if (posicional) {
            columnas = new HashMap<>();
            for (int i = 0; i < campos.size(); i++) columnas.put(campos.get(i).nombre(), i);
            primeraDeDatos = 0;
        } else {
            columnas = ubicarPorNombre(filas.get(encabezado), campos);
            primeraDeDatos = encabezado + 1;
        }

        int minimoPosicional = 0;
        for (int i = 0; i < campos.size(); i++) if (campos.get(i).obligatorio()) minimoPosicional = i + 1;

        List<Registro> out = new ArrayList<>();
        for (int i = primeraDeDatos; i < filas.size(); i++) {
            Fila f = filas.get(i);
            if (posicional && f.celdas().size() < minimoPosicional) {
                throw new ServicioException(f.ubicacion() + ": se esperaban al menos " + minimoPosicional
                        + " columnas (" + nombres(campos.subList(0, minimoPosicional)) + ") y hay "
                        + f.celdas().size() + ".");
            }
            out.add(new Registro(f, columnas));
        }
        return out;
    }

    private static int buscarEncabezado(List<Fila> filas) {
        for (int i = 0; i < Math.min(FILAS_BUSQUEDA, filas.size()); i++) {
            for (Celda c : filas.get(i).celdas()) {
                if (c.valor() == null && normalizar(c.texto()).startsWith("fecha")) return i;
            }
        }
        return -1;
    }

    private static Map<String, Integer> ubicarPorNombre(Fila encabezado, List<Campo> campos) {
        Map<String, Integer> columnas = new HashMap<>();
        List<String> faltan = new ArrayList<>();
        for (Campo campo : campos) {
            int col = -1;
            for (int i = 0; i < encabezado.celdas().size() && col < 0; i++) {
                String titulo = normalizar(encabezado.celda(i).texto());
                for (String alias : campo.alias()) {
                    if (titulo.equals(normalizar(alias))) {
                        col = i;
                        break;
                    }
                }
            }
            if (col >= 0) columnas.put(campo.nombre(), col);
            else if (campo.obligatorio()) faltan.add(campo.nombre());
        }
        if (!faltan.isEmpty()) {
            throw new ServicioException("El archivo no tiene las columnas: " + String.join(", ", faltan)
                    + ". Columnas que se aceptan: " + nombres(campos) + ".");
        }
        return columnas;
    }

    /** "Razón Social" -> "razonsocial"; "Pto. Vta." -> "ptovta". */
    static String normalizar(String s) {
        if (s == null) return "";
        String sinAcentos = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.replaceAll("[^a-z0-9]", "");
    }

    private static String nombres(List<Campo> campos) {
        List<String> n = new ArrayList<>();
        for (Campo c : campos) n.add(c.nombre());
        return String.join(";", n);
    }
}
