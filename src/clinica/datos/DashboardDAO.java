package clinica.datos;

import clinica.modelo.EstadoCita;
import clinica.util.UtilErrores;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumMap;
import java.util.Map;

/**
 * Consultas agregadas para el dashboard de estadisticas (RF-10).
 */
public class DashboardDAO {

    public int contarPacientes() {
        return contar("SELECT COUNT(*) FROM paciente");
    }

    public int contarMedicos() {
        return contar("SELECT COUNT(*) FROM medico WHERE estado = 'ACTIVO'");
    }

    public int contarCitas() {
        return contar("SELECT COUNT(*) FROM cita");
    }

    public int contarAtenciones() {
        return contar("SELECT COUNT(*) FROM atencion");
    }

    public double totalIngresos() {
        return monto("SELECT COALESCE(SUM(monto), 0) FROM pago");
    }

    public double totalGastosActivos() {
        return monto("SELECT COALESCE(SUM(monto), 0) FROM gasto WHERE estado = 'CONTABILIZADO'");
    }

    public Map<EstadoCita, Integer> citasPorEstado() {
        Map<EstadoCita, Integer> porEstado = new EnumMap<>(EstadoCita.class);
        for (EstadoCita estado : EstadoCita.values()) {
            porEstado.put(estado, 0);
        }
        String sql = "SELECT estado, COUNT(*) FROM cita GROUP BY estado";
        try (Statement st = ConexionBD.obtenerInstancia().getConexion().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String estado = rs.getString(1);
                int cantidad = rs.getInt(2);
                EstadoCita enumEstado = EstadoCita.valueOf(estado);
                porEstado.put(enumEstado, cantidad);
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("contar las citas por estado", e);
        }
        return porEstado;
    }

    private int contar(String sql) {
        try (Statement st = ConexionBD.obtenerInstancia().getConexion().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("consultar las estadisticas", e);
        }
        return 0;
    }

    private double monto(String sql) {
        try (Statement st = ConexionBD.obtenerInstancia().getConexion().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("consultar los montos", e);
        }
        return 0;
    }
}