package service.importacion;

/**
 * Una celda leida de un archivo.
 *
 * @param texto lo que se ve en la celda (en CSV, el texto tal cual; en Excel, el valor formateado)
 * @param valor el valor ya tipado cuando el archivo lo trae: {@code LocalDate} para fechas de Excel,
 *              {@code BigDecimal} para numeros de Excel; {@code null} en CSV o en celdas de texto
 */
record Celda(String texto, Object valor) {

    static final Celda VACIA = new Celda("", null);

    boolean vacia() {
        return texto.isBlank() && valor == null;
    }
}
