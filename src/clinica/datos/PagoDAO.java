package clinica.datos;

import clinica.modelo.MetodoPago;
import clinica.modelo.Pago;
import clinica.util.UtilErrores;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la entidad Pago (RF-05).
 */
public class PagoDAO {

    private static final String INSERTAR =
            "INSERT INTO pago (id_atencion, monto, fecha, metodo) VALUES (?, ?, ?, ?)";
    private static final String LISTAR =
            "SELECT id_pago, id_atencion, monto, fecha, metodo FROM pago ORDER BY fecha DESC, id_pago DESC";
    private static final String LISTAR_RANGO =
            "SELECT id_pago, id_atencion, monto, fecha, metodo FROM pago "
            + "WHERE fecha BETWEEN ? AND ? ORDER BY fecha ASC";
    private static final String TOTAL_INGRESOS =
            "SELECT COALESCE(SUM(monto), 0) FROM pago WHERE fecha BETWEEN ? AND ?";
    private static final String TOTAL_INGRESOS_TOTAL =
            "SELECT COALESCE(SUM(monto), 0) FROM pago";

    public int insertar(Pago pago) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia()
                .getConexion().prepareStatement(INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            if (pago.getIdAtencion() == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, pago.getIdAtencion());
            }
            ps.setDouble(2, pago.getMonto());
            ps.setDate(3, Date.valueOf(pago.getFecha()));
            ps.setString(4, pago.getMetodo().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw UtilErrores.errorBD("registrar el pago", e);
        }
    }

    public List<Pago> listar() {
        List<Pago> pagos = new ArrayList<>();
        try (Statement st = ConexionBD.obtenerInstancia().getConexion().createStatement();
             ResultSet rs = st.executeQuery(LISTAR)) {
            while (rs.next()) {
                pagos.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("listar los pagos", e);
        }
        return pagos;
    }

    public List<Pago> listarEntreFechas(LocalDate inicio, LocalDate fin) {
        List<Pago> pagos = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(LISTAR_RANGO)) {
            ps.setDate(1, Date.valueOf(inicio));
            ps.setDate(2, Date.valueOf(fin));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pagos.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("consultar los pagos", e);
        }
        return pagos;
    }

    public double totalIngresosEnPeriodo(LocalDate inicio, LocalDate fin) {
        return consultarMonto(TOTAL_INGRESOS, inicio, fin);
    }

    public double totalIngresos() {
        return consultarMonto(TOTAL_INGRESOS_TOTAL, null, null);
    }

    private double consultarMonto(String sql, LocalDate inicio, LocalDate fin) {
        try {
            if (inicio == null) {
                try (Statement st = ConexionBD.obtenerInstancia().getConexion().createStatement();
                     ResultSet rs = st.executeQuery(sql)) {
                    if (rs.next()) {
                        return rs.getDouble(1);
                    }
                }
            } else {
                try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion().prepareStatement(sql)) {
                    ps.setDate(1, Date.valueOf(inicio));
                    ps.setDate(2, Date.valueOf(fin));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getDouble(1);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("calcular los ingresos", e);
        }
        return 0;
    }

    private Pago mapear(ResultSet rs) throws SQLException {
        int id = rs.getInt("id_pago");
        Integer idAtencion = rs.getObject("id_atencion") == null ? null : rs.getInt("id_atencion");
        double monto = rs.getDouble("monto");
        LocalDate fecha = rs.getDate("fecha").toLocalDate();
        MetodoPago metodo = MetodoPago.valueOf(rs.getString("metodo"));
        return new Pago(id, idAtencion, monto, fecha, metodo);
    }
}