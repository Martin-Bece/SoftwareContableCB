package dao;

import java.math.BigDecimal;

/** Pequenos ayudantes compartidos por los DAOs. */
final class Sql {

    private Sql() { }

    /**
     * Arma un patron {@code %texto%} para LIKE, escapando los comodines que escriba el usuario
     * (usar junto con {@code ESCAPE '\'} en la consulta).
     */
    static String patronLike(String texto) {
        String escapado = texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escapado + "%";
    }

    /** BigDecimal -> TEXT exacto para guardar en la base ("1234.56"). */
    static String importe(BigDecimal v) {
        return v == null ? "0" : v.toPlainString();
    }

    /** TEXT de la base -> BigDecimal. */
    static BigDecimal importe(String v) {
        return v == null || v.isBlank() ? BigDecimal.ZERO : new BigDecimal(v);
    }
}
