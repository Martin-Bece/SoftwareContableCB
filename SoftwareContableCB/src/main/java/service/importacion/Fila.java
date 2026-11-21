package service.importacion;

import java.util.List;

/**
 * Una fila de un archivo importado.
 *
 * @param ubicacion como se le nombra al usuario en los errores: "Linea 3" (CSV) o "Fila 3" (Excel)
 */
record Fila(String ubicacion, List<Celda> celdas) {

    /** La celda en la posicion {@code i}, o una vacia si la fila es mas corta. */
    Celda celda(int i) {
        return i >= 0 && i < celdas.size() ? celdas.get(i) : Celda.VACIA;
    }

    boolean vacia() {
        return celdas.stream().allMatch(Celda::vacia);
    }
}
