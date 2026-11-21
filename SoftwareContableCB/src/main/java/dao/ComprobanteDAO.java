package dao;

import config.Conexion;
import view.Modelos.Comprobante;
import view.Modelos.Operacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a la tabla {@code comprobante} (ventas y compras de cada cliente).
 *
 * <p>Los importes se guardan como TEXT exacto (ver {@link Sql#importe}).
 * La fecha se guarda en ISO {@code yyyy-MM-dd}, que ordena y compara bien como texto.</p>
 */
public class ComprobanteDAO {

    private static final String COLUMNAS_DATOS =
            "operacion, fecha, tipo, puntoVenta, numero, cuitContraparte, razonSocialContraparte, "
            + "neto21, iva21, neto105, iva105, neto27, iva27, noGravado, exento, percepciones, otrosTributos, origen";

    /** Inserta el comprobante y le asigna el id generado. */
    public void insertar(String cuitCliente, Comprobante c) throws SQLException {
        Connection conn = Conexion.getConexion();
        String sql = "INSERT INTO comprobante (clienteCuit, " + COLUMNAS_DATOS + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cuitCliente);
            cargarDatos(ps, 2, c);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) c.id = rs.getLong(1);
            }
        }
    }

    /** @return {@code false} si el comprobante no existe para ese cliente. */
    public boolean actualizar(String cuitCliente, Comprobante c) throws SQLException {
        Connection conn = Conexion.getConexion();
        String sql = "UPDATE comprobante SET operacion = ?, fecha = ?, tipo = ?, puntoVenta = ?, numero = ?, "
                + "cuitContraparte = ?, razonSocialContraparte = ?, neto21 = ?, iva21 = ?, neto105 = ?, "
                + "iva105 = ?, neto27 = ?, iva27 = ?, noGravado = ?, exento = ?, percepciones = ?, "
                + "otrosTributos = ?, origen = ? WHERE id = ? AND clienteCuit = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = cargarDatos(ps, 1, c);
            ps.setLong(i++, c.id);
            ps.setString(i, cuitCliente);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(String cuitCliente, long id) throws SQLException {
        Connection conn = Conexion.getConexion();
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM comprobante WHERE id = ? AND clienteCuit = ?")) {
            ps.setLong(1, id);
            ps.setString(2, cuitCliente);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Lista comprobantes de un cliente ordenados por fecha y numero.
     *
     * @param operacion VENTA, COMPRA o {@code null} para ambas
     * @param desde     fecha minima inclusive, o {@code null}
     * @param hasta     fecha maxima inclusive, o {@code null}
     * @param filtro    texto a buscar en razon social, CUIT o "Tipo PV-Numero"; {@code null} = sin filtro
     */
    public List<Comprobante> listar(String cuitCliente, Operacion operacion,
                                    LocalDate desde, LocalDate hasta, String filtro) throws SQLException {
        Connection conn = Conexion.getConexion();
        StringBuilder sql = new StringBuilder("SELECT id, " + COLUMNAS_DATOS
                + " FROM comprobante WHERE clienteCuit = ?");
        List<Object> params = new ArrayList<>();
        params.add(cuitCliente);
        if (operacion != null) {
            sql.append(" AND operacion = ?");
            params.add(operacion.name());
        }
        if (desde != null) {
            sql.append(" AND fecha >= ?");
            params.add(desde.toString());
        }
        if (hasta != null) {
            sql.append(" AND fecha <= ?");
            params.add(hasta.toString());
        }
        String f = filtro == null ? "" : filtro.trim();
        if (!f.isEmpty()) {
            sql.append(" AND (razonSocialContraparte LIKE ? ESCAPE '\\' OR cuitContraparte LIKE ? ESCAPE '\\'"
                    + " OR (tipo || ' ' || puntoVenta || '-' || numero) LIKE ? ESCAPE '\\')");
            String patron = Sql.patronLike(f);
            params.add(patron);
            params.add(patron);
            params.add(patron);
        }
        sql.append(" ORDER BY fecha, numero, id");

        List<Comprobante> out = new ArrayList<>();
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
                "SELECT COUNT(*) FROM comprobante WHERE clienteCuit = ?")) {
            ps.setString(1, cuitCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Indica si ya hay otro comprobante con la misma identidad
     * (operacion + tipo + punto de venta + numero + CUIT de la contraparte).
     *
     * @param excluirId id a ignorar (el propio comprobante al editar), o {@code null}
     */
    public boolean existeDuplicado(String cuitCliente, Comprobante c, Long excluirId) throws SQLException {
        Connection conn = Conexion.getConexion();
        String sql = "SELECT 1 FROM comprobante WHERE clienteCuit = ? AND operacion = ? AND tipo = ? "
                + "AND puntoVenta = ? AND numero = ? AND cuitContraparte = ?"
                + (excluirId != null ? " AND id <> ?" : "");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cuitCliente);
            ps.setString(2, c.operacion.name());
            ps.setString(3, c.tipo);
            ps.setString(4, c.puntoVenta);
            ps.setString(5, c.numero);
            ps.setString(6, c.cuitContraparte);
            if (excluirId != null) ps.setLong(7, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /* ------------------------------------------------------------------ */

    /** Carga las columnas de COLUMNAS_DATOS a partir de {@code i}; devuelve el siguiente indice libre. */
    private int cargarDatos(PreparedStatement ps, int i, Comprobante c) throws SQLException {
        ps.setString(i++, c.operacion.name());
        ps.setString(i++, c.fecha.toString());
        ps.setString(i++, c.tipo);
        ps.setString(i++, c.puntoVenta);
        ps.setString(i++, c.numero);
        ps.setString(i++, c.cuitContraparte);
        ps.setString(i++, c.razonSocial);
        ps.setString(i++, Sql.importe(c.neto21));
        ps.setString(i++, Sql.importe(c.iva21));
        ps.setString(i++, Sql.importe(c.neto105));
        ps.setString(i++, Sql.importe(c.iva105));
        ps.setString(i++, Sql.importe(c.neto27));
        ps.setString(i++, Sql.importe(c.iva27));
        ps.setString(i++, Sql.importe(c.noGravado));
        ps.setString(i++, Sql.importe(c.exento));
        ps.setString(i++, Sql.importe(c.percepciones));
        ps.setString(i++, Sql.importe(c.otrosTributos));
        ps.setString(i++, c.origen == null || c.origen.isBlank() ? "Manual" : c.origen);
        return i;
    }

    private Comprobante mapear(ResultSet rs) throws SQLException {
        Comprobante c = new Comprobante();
        c.id = rs.getLong("id");
        c.operacion = Operacion.valueOf(rs.getString("operacion"));
        c.fecha = LocalDate.parse(rs.getString("fecha"));
        c.tipo = rs.getString("tipo");
        c.puntoVenta = rs.getString("puntoVenta");
        c.numero = rs.getString("numero");
        c.cuitContraparte = rs.getString("cuitContraparte");
        c.razonSocial = rs.getString("razonSocialContraparte");
        c.neto21 = Sql.importe(rs.getString("neto21"));
        c.iva21 = Sql.importe(rs.getString("iva21"));
        c.neto105 = Sql.importe(rs.getString("neto105"));
        c.iva105 = Sql.importe(rs.getString("iva105"));
        c.neto27 = Sql.importe(rs.getString("neto27"));
        c.iva27 = Sql.importe(rs.getString("iva27"));
        c.noGravado = Sql.importe(rs.getString("noGravado"));
        c.exento = Sql.importe(rs.getString("exento"));
        c.percepciones = Sql.importe(rs.getString("percepciones"));
        c.otrosTributos = Sql.importe(rs.getString("otrosTributos"));
        c.origen = rs.getString("origen");
        return c;
    }
}
