package service.importacion;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import view.Servicios.ServicioException;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Convierte un archivo (CSV o Excel) en una lista de {@link Fila}s, sin interpretar que significa
 * cada columna. Eso lo hacen {@link Tabla} y los lectores de comprobantes/percepciones.
 * Las filas totalmente vacias se descartan.
 */
final class LectorFilas {

    private LectorFilas() { }

    static boolean soporta(File archivo) {
        String n = archivo.getName().toLowerCase();
        return n.endsWith(".csv") || n.endsWith(".txt") || n.endsWith(".xlsx") || n.endsWith(".xls");
    }

    static List<Fila> leer(File archivo) {
        String n = archivo.getName().toLowerCase();
        if (n.endsWith(".xlsx") || n.endsWith(".xls")) return excel(archivo);
        if (n.endsWith(".csv") || n.endsWith(".txt")) return csv(archivo);
        throw new ServicioException("Formato de archivo no soportado: " + archivo.getName());
    }

    /* ------------------------------ CSV ------------------------------ */

    /**
     * Separador {@code ;} o {@code ,} (se elige por linea: si tiene {@code ;} se usa ese).
     * Codificacion UTF-8, o Windows-1252 si el archivo no es UTF-8 valido (lo que guarda Excel).
     * No interpreta comillas.
     */
    static List<Fila> csv(File archivo) {
        List<String> lineas = leerLineas(archivo);
        List<Fila> filas = new ArrayList<>();
        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            if (i == 0) linea = linea.replace("﻿", ""); // BOM de Excel
            if (linea.trim().isEmpty()) continue;
            List<Celda> celdas = new ArrayList<>();
            for (String parte : linea.split(linea.contains(";") ? ";" : ",", -1)) {
                celdas.add(new Celda(parte.trim(), null));
            }
            filas.add(new Fila("Linea " + (i + 1), celdas));
        }
        return filas;
    }

    private static List<String> leerLineas(File archivo) {
        try {
            return Files.readAllLines(archivo.toPath(), StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            try {
                return Files.readAllLines(archivo.toPath(), Charset.forName("windows-1252"));
            } catch (IOException e2) {
                throw new ServicioException("No se pudo leer el archivo: " + e2.getMessage(), e2);
            }
        } catch (IOException e) {
            throw new ServicioException("No se pudo leer el archivo: " + e.getMessage(), e);
        }
    }

    /* ----------------------------- Excel ----------------------------- */

    /**
     * Lee la PRIMERA hoja de un .xlsx o .xls. Las formulas se calculan. Para cada celda se guarda
     * el texto que se ve (respeta formatos como "0001") y, si es numero o fecha, el valor tipado.
     */
    static List<Fila> excel(File archivo) {
        try (Workbook libro = WorkbookFactory.create(archivo, null, true)) {
            if (libro.getNumberOfSheets() == 0) return new ArrayList<>();
            Sheet hoja = libro.getSheetAt(0);
            FormulaEvaluator evaluador = libro.getCreationHelper().createFormulaEvaluator();
            DataFormatter formato = new DataFormatter(Locale.US);

            List<Fila> filas = new ArrayList<>();
            for (Row row : hoja) {
                List<Celda> celdas = new ArrayList<>();
                for (int c = 0; c < Math.max(row.getLastCellNum(), 0); c++) {
                    celdas.add(celda(row.getCell(c), evaluador, formato));
                }
                Fila fila = new Fila("Fila " + (row.getRowNum() + 1), celdas);
                if (!fila.vacia()) filas.add(fila);
            }
            return filas;
        } catch (ServicioException e) {
            throw e;
        } catch (Exception e) {
            throw new ServicioException("No se pudo leer el Excel " + archivo.getName()
                    + ". Revisa que no este danado ni protegido con contrasena. (" + e.getMessage() + ")", e);
        }
    }

    private static Celda celda(Cell cell, FormulaEvaluator evaluador, DataFormatter formato) {
        if (cell == null) return Celda.VACIA;
        CellType tipo = cell.getCellType();
        if (tipo == CellType.FORMULA) {
            try {
                tipo = evaluador.evaluateFormulaCell(cell);
            } catch (RuntimeException e) {
                tipo = cell.getCachedFormulaResultType(); // formula que POI no sabe calcular: usa lo guardado
            }
        }
        String texto = formato.formatCellValue(cell, evaluador).trim();
        Object valor = null;
        if (tipo == CellType.NUMERIC) {
            valor = DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate()
                    : BigDecimal.valueOf(cell.getNumericCellValue());
        }
        return new Celda(texto, valor);
    }
}
