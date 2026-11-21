package dao;

import config.Conexion;
import view.Modelos.Clase;
import view.Modelos.PercepcionRetencion;
import view.Modelos.Tributo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla {@code percepcion_retencion} (percepciones y retenciones sufridas por cada cliente). */
public class PercepcionDAO {

    private static final String COLUMNAS_DATOS =
            "clase, tributo, fecha, regimen, jurisdiccion, comprobante, cuitAgente, razonSocialAgente, importe, origen";

    /** Inserta y asigna el id generado. */
    public void insertar(String cuitCliente, PercepcionRetencion p) throws SQLException {
        Connection conn = Conexion.getConexion();
        String sql = "INSERT INTO percepcion_retencion (clienteCuit, " + COLUMNAS_DATOS + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cuitCliente);
            cargarDatos(ps, 2, p);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) p.id = rs.getLong(1);
            }
        }
    }

    /** @return {@code false} si no existe para ese cliente. */
    public boolean actualizar(String cuitCliente, PercepcionRetencion p) throws SQLException {
        Connection conn = Conexion.getConexion();
        String sql = "UPDATE percepcion_retencion SET clase = ?, tributo = ?, fecha = ?, regimen = ?, "
                + "jurisdiccion = ?, comprobante = ?, cuitAgente = ?, razonSocialAgente = ?, importe = ?, "
                + "origen = ? WHERE id = ? AND clienteCuit = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = cargarDatos(ps, 1, p);
            ps.setLong(i++, p.id);
            ps.setString(i, cuitCliente);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(String cuitCliente, long id) throws SQLException {
        Connection conn = Conexion.getConexion();
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM percepcion_retencion WHERE id = ? AND clienteCuit = ?")) {
            ps.setLong(1, id);
            ps.setString(2, cuitCliente);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Lista ordenada por fecha. Cualquier filtro en {@code null} = sin filtrar por ese campo.
     */
    public List<PercepcionRetencion> listar(String cuitCliente, LocalDate desde, LocalDate hasta,
                                           Clase clase, Tributo tributo) throws SQLException {
        Connection conn = Conexion.getConexion();
        StringBuilder sql = new StringBuilder("SELECT id, " + COLUMNAS_DATOS
                + " FROM percepcion_retencion WHERE clienteCuit = ?");
        List<Object> params = new ArrayList<>();
        params.add(cuitCliente);
        if (desde != null) { sql.append(" AND fecha >= ?"); params.add(desde.toString()); }
        if (hasta != null) { sql.append(" AND fecha <= ?"); params.add(hasta.toString()); }
        if (clase != null) { sql.append(" AND clase = ?"); params.add(clase.name()); }
        if (tributo != null) { sql.append(" AND tributo = ?"); params.add(tributo.name()); }
        sql.append(" ORDER BY fecha, id");

        List<PercepcionRetencion> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapear(rs));
            }
        }
        return out;
    }

    public int contarPorCliente(String cuitCliente) throws SQLException {
        Connection conn = Conexion.getConexion();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM percepcion_retencion WHERE clienteCuit = ?")) {
            ps.setString(1, cuitCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Indica si ya hay un movimiento identico (mismos clase, tributo, fecha, agente, comprobante e importe).
     * Se usa al importar para no cargar dos veces el mismo archivo.
     */
    public boolean existeIgual(String cuitCliente, PercepcionRetencion p) throws SQLException {
        for (PercepcionRetencion otra : listar(cuitCliente, p.fecha, p.fecha, p.clase, p.tributo)) {
            if (otra.cuitAgente.equals(p.cuitAgente)
                    && otra.comprobante.equals(p.comprobante)
                    && otra.importe.compareTo(p.importe) == 0) {
                return true;
            }
        }
        return false;
    }

    /* ------------------------------------------------------------------ */

    private int cargarDatos(PreparedStatement ps, int i, PercepcionRetencion p) throws SQLException {
        ps.setString(i++, p.clase.name());
        ps.setString(i++, p.tributo.name());
        ps.setString(i++, p.fecha.toString());
        ps.setString(i++, p.regimen);
        ps.setString(i++, p.jurisdiccion);
        ps.setString(i++, p.comprobante);
        ps.setString(i++, p.cuitAgente);
        ps.setString(i++, p.razonSocialAgente);
        ps.setString(i++, Sql.importe(p.importe));
        ps.setString(i++, p.origen == null || p.origen.isBlank() ? "Manual" : p.origen);
        return i;
    }

    private PercepcionRetencion mapear(ResultSet rs) throws SQLException {
        PercepcionRetencion p = new PercepcionRetencion();
        p.id = rs.getLong("id");
        p.clase = Clase.valueOf(rs.getString("clase"));
        p.tributo = Tributo.valueOf(rs.getString("tributo"));
        p.fecha = LocalDate.parse(rs.getString("fecha"));
        p.regimen = rs.getString("regimen");
        p.jurisdiccion = rs.getString("jurisdiccion");
        p.comprobante = rs.getString("comprobante");
        p.cuitAgente = rs.getString("cuitAgente");
        p.razonSocialAgente = rs.getString("razonSocialAgente");
        p.importe = Sql.importe(rs.getString("importe"));
        p.origen = rs.getString("origen");
        return p;
    }
}
