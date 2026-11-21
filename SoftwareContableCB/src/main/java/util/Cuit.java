package util;

/**
 * Utilidades de CUIT/CUIL para el backend.
 * Formato canonico que se guarda en la base: {@code XX-XXXXXXXX-X}.
 */
public final class Cuit {

    private Cuit() { }

    /**
     * Normaliza un CUIT escrito de cualquier forma ("30712345679", "30-71234567-9",
     * "30.71234567.9") al formato canonico. Devuelve {@code null} si no tiene 11 digitos.
     * Es la misma regla que usa {@code DialogoCliente.normalizarCuit} en la vista.
     */
    public static String normalizar(String entrada) {
        if (entrada == null) return null;
        String digitos = entrada.replaceAll("[^0-9]", "");
        if (digitos.length() != 11) return null;
        return digitos.substring(0, 2) + "-" + digitos.substring(2, 10) + "-" + digitos.substring(10);
    }
}
