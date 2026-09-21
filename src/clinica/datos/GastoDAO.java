package clinica.datos;

import clinica.modelo.CategoriaGasto;
import clinica.modelo.EstadoGasto;
import clinica.modelo.Gasto;
import clinica.util.UtilErrores;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de la entidad Gasto (RF-06).
 * Regla: solo los gastos CONTABILIZADO suman en los totales activos.
 */
public class GastoDAO {

    private static final String INSERTAR =
            "INSERT INTO gasto (descripcion, monto, fecha, categoria, estado) VALUES (?, ?, ?, ?, ?)";
    private static final String LISTAR =
            "SELECT id_gasto, descripcion, monto, fecha, categoria, estado "
            + "FROM gasto ORDER BY fecha DESC, id_gasto DESC";
    private static final String LISTAR_RANGO =
            "SELECT id_gasto, descripcion, monto, fecha, categoria, estado "
            + "FROM gasto WHERE fecha BETWEEN ? AND ? ORDER BY fecha ASC";
    private static final String BUSCAR_POR_ID =
            "SELECT id_gasto, descripcion, monto, fecha, categoria, estado "
            + "FROM gasto WHERE id_gasto = ?";
    private static final String LISTAR_RANGO_ACTIVOS =
            "SELECT id_gasto, descripcion, monto, fecha, categoria, estado "
            + "FROM gasto WHERE estado = 'CONTABILIZADO' AND fecha BETWEEN ? AND ? ORDER BY fecha ASC";
    private static final String ANULAR =
            "UPDATE gasto SET estado = 'ANULADO' WHERE id_gasto = ?";
    private static final String TOTAL_GASTOS_ACTIVOS =
            "SELECT COALESCE(SUM(monto), 0) FROM gasto WHERE estado = 'CONTABILIZADO'";
    private static final String TOTAL_GASTOS_ACTIVOS_RANGO =
            "SELECT COALESCE(SUM(monto), 0) FROM gasto "
            + "WHERE estado = 'CONTABILIZADO' AND fecha BETWEEN ? AND ?";
    private static final String TOTAL_POR_CATEGORIA =
            "SELECT categoria, COALESCE(SUM(monto), 0) FROM gasto "
            + "WHERE estado = 'CONTABILIZADO' AND fecha BETWEEN ? AND ? GROUP BY categoria";

    public int insertar(Gasto gasto) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia()
                .getConexion().prepareStatement(INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, gasto.getDescripcion());
            ps.setDouble(2, gasto.getMonto());
            ps.setDate(3, Date.valueOf(gasto.getFecha()));
            ps.setString(4, gasto.getCategoria().name());
            ps.setString(5, EstadoGasto.CONTABILIZADO.name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw UtilErrores.errorBD("registrar el gasto", e);
        }
    }

    public Gasto buscarPorId(int idGasto) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(BUSCAR_POR_ID)) {
            ps.setInt(1, idGasto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("consultar el gasto", e);
        }
        return null;
    }

    public List<Gasto> listarActivosEntreFechas(LocalDate inicio, LocalDate fin) {
        List<Gasto> gastos = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(LISTAR_RANGO_ACTIVOS)) {
            ps.setDate(1, Date.valueOf(inicio));
            ps.setDate(2, Date.valueOf(fin));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    gastos.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("consultar los gastos del periodo", e);
        }
        return gastos;
    }

    public void anular(int idGasto) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion().prepareStatement(ANULAR)) {
            ps.setInt(1, idGasto);
            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new RuntimeException("El gasto con id " + idGasto + " no existe.");
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("anular el gasto", e);
        }
    }

    public List<Gasto> listar() {
        List<Gasto> gastos = new ArrayList<>();
        try (Statement st = ConexionBD.obtenerInstancia().getConexion().createStatement();
             ResultSet rs = st.executeQuery(LISTAR)) {
            while (rs.next()) {
                gastos.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("listar los gastos", e);
        }
        return gastos;
    }

    public List<Gasto> listarEntreFechas(LocalDate inicio, LocalDate fin) {
        List<Gasto> gastos = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(LISTAR_RANGO)) {
            ps.setDate(1, Date.valueOf(inicio));
            ps.setDate(2, Date.valueOf(fin));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    gastos.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("consultar los gastos", e);
        }
        return gastos;
    }

    public double totalGastosActivos() {
        try (Statement st = ConexionBD.obtenerInstancia().getConexion().createStatement();
             ResultSet rs = st.executeQuery(TOTAL_GASTOS_ACTIVOS)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("calcular los gastos", e);
        }
        return 0;
    }

    public double totalGastosActivosEnPeriodo(LocalDate inicio, LocalDate fin) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(TOTAL_GASTOS_ACTIVOS_RANGO)) {
            ps.setDate(1, Date.valueOf(inicio));
            ps.setDate(2, Date.valueOf(fin));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("calcular los gastos del periodo", e);
        }
        return 0;
    }

    public Map<CategoriaGasto, Double> totalGastosActivosPorCategoria(LocalDate inicio, LocalDate fin) {
        Map<CategoriaGasto, Double> resultado = new EnumMap<>(CategoriaGasto.class);
        for (CategoriaGasto categoria : CategoriaGasto.values()) {
            resultado.put(categoria, 0.0);
        }
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(TOTAL_POR_CATEGORIA)) {
            ps.setDate(1, Date.valueOf(inicio));
            ps.setDate(2, Date.valueOf(fin));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CategoriaGasto categoria = CategoriaGasto.valueOf(rs.getString("categoria"));
                    resultado.put(categoria, rs.getDouble(2));
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("calcular los gastos por categoria", e);
        }
        return resultado;
    }

    private Gasto mapear(ResultSet rs) throws SQLException {
        return new Gasto(
                rs.getInt("id_gasto"),
                rs.getString("descripcion"),
                rs.getDouble("monto"),
                rs.getDate("fecha").toLocalDate(),
                CategoriaGasto.valueOf(rs.getString("categoria")),
                EstadoGasto.valueOf(rs.getString("estado")));
    }
}