package clinica.datos;

import clinica.modelo.EstadoPersona;
import clinica.modelo.Medico;
import clinica.util.UtilErrores;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la entidad Medico (RF-02).
 */
public class MedicoDAO {

    private static final String COLUMNAS =
            "id_medico, nombre, dni, especialidad, telefono, fecha_registro, estado";

    private static final String INSERTAR =
            "INSERT INTO medico (nombre, dni, especialidad, telefono, fecha_registro, estado) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String LISTAR =
            "SELECT " + COLUMNAS + " FROM medico ORDER BY nombre";
    private static final String LISTAR_ACTIVOS =
            "SELECT " + COLUMNAS + " FROM medico WHERE estado = 'ACTIVO' ORDER BY nombre";
    private static final String BUSCAR_POR_ID =
            "SELECT " + COLUMNAS + " FROM medico WHERE id_medico = ?";
    private static final String BUSCAR_POR_DNI =
            "SELECT " + COLUMNAS + " FROM medico WHERE dni = ?";
    private static final String ACTUALIZAR =
            "UPDATE medico SET nombre = ?, especialidad = ?, telefono = ? WHERE id_medico = ?";
    private static final String CAMBIAR_ESTADO =
            "UPDATE medico SET estado = ? WHERE id_medico = ?";
    private static final String EXISTE_DNI =
            "SELECT COUNT(*) FROM medico WHERE dni = ? AND id_medico <> ?";

    public int insertar(Medico medico) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia()
                .getConexion().prepareStatement(INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, medico.getNombre());
            ps.setString(2, medico.getDni());
            ps.setString(3, medico.getEspecialidad());
            ps.setString(4, medico.getTelefono());
            ps.setDate(5, Date.valueOf(medico.getFechaRegistro()));
            ps.setString(6, medico.getEstado().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw UtilErrores.errorBD("registrar el medico", e);
        }
    }

    public Medico buscarPorId(int idMedico) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(BUSCAR_POR_ID)) {
            ps.setInt(1, idMedico);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("consultar el medico", e);
        }
        return null;
    }

    public Medico buscarPorDni(String dni) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(BUSCAR_POR_DNI)) {
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("consultar el medico por DNI", e);
        }
        return null;
    }

    public List<Medico> listar() {
        return ejecutarLista(LISTAR);
    }

    public List<Medico> listarActivos() {
        return ejecutarLista(LISTAR_ACTIVOS);
    }

    public boolean existeDni(String dni, int excluirId) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(EXISTE_DNI)) {
            ps.setString(1, dni);
            ps.setInt(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("validar el DNI", e);
        }
        return false;
    }

    public void actualizar(Medico medico) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(ACTUALIZAR)) {
            ps.setString(1, medico.getNombre());
            ps.setString(2, medico.getEspecialidad());
            ps.setString(3, medico.getTelefono());
            ps.setInt(4, medico.getIdMedico());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw UtilErrores.errorBD("actualizar el medico", e);
        }
    }

    public void cambiarEstado(int idMedico, EstadoPersona estado) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(CAMBIAR_ESTADO)) {
            ps.setString(1, estado.name());
            ps.setInt(2, idMedico);
            if (ps.executeUpdate() == 0) {
                throw new RuntimeException("El medico con id " + idMedico + " no existe.");
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("cambiar el estado del medico", e);
        }
    }

    private List<Medico> ejecutarLista(String sql) {
        List<Medico> medicos = new ArrayList<>();
        try (Statement st = ConexionBD.obtenerInstancia().getConexion().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                medicos.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("listar los medicos", e);
        }
        return medicos;
    }

    private Medico mapear(ResultSet rs) throws SQLException {
        return new Medico(
                rs.getInt("id_medico"),
                rs.getString("nombre"),
                rs.getString("dni"),
                rs.getString("especialidad"),
                rs.getString("telefono"),
                rs.getDate("fecha_registro").toLocalDate(),
                EstadoPersona.valueOf(rs.getString("estado")));
    }
}