package clinica.datos;

import clinica.modelo.EstadoPersona;
import clinica.modelo.Paciente;
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
 * Acceso a datos de la entidad Paciente (RF-01).
 */
public class PacienteDAO {

    private static final String COLUMNAS =
            "id_paciente, nombre, dni, telefono, correo, direccion, fecha_registro, estado";

    private static final String INSERTAR =
            "INSERT INTO paciente (nombre, dni, telefono, correo, direccion, fecha_registro, estado) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String LISTAR =
            "SELECT " + COLUMNAS + " FROM paciente ORDER BY nombre";
    private static final String LISTAR_ACTIVOS =
            "SELECT " + COLUMNAS + " FROM paciente WHERE estado = 'ACTIVO' ORDER BY nombre";
    private static final String BUSCAR_POR_ID =
            "SELECT " + COLUMNAS + " FROM paciente WHERE id_paciente = ?";
    private static final String BUSCAR_POR_DNI =
            "SELECT " + COLUMNAS + " FROM paciente WHERE dni = ?";
    private static final String ACTUALIZAR =
            "UPDATE paciente SET nombre = ?, telefono = ?, correo = ?, direccion = ? "
            + "WHERE id_paciente = ?";
    private static final String CAMBIAR_ESTADO =
            "UPDATE paciente SET estado = ? WHERE id_paciente = ?";
    private static final String EXISTE_DNI =
            "SELECT COUNT(*) FROM paciente WHERE dni = ? AND id_paciente <> ?";

    public int insertar(Paciente paciente) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia()
                .getConexion().prepareStatement(INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, paciente.getNombre());
            ps.setString(2, paciente.getDni());
            ps.setString(3, paciente.getTelefono());
            ps.setString(4, paciente.getCorreo());
            ps.setString(5, paciente.getDireccion());
            ps.setDate(6, Date.valueOf(paciente.getFechaRegistro()));
            ps.setString(7, paciente.getEstado().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw UtilErrores.errorBD("registrar el paciente", e);
        }
    }

    public Paciente buscarPorId(int idPaciente) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(BUSCAR_POR_ID)) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("consultar el paciente", e);
        }
        return null;
    }

    public Paciente buscarPorDni(String dni) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(BUSCAR_POR_DNI)) {
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("consultar el paciente por DNI", e);
        }
        return null;
    }

    public List<Paciente> listar() {
        return ejecutarLista(LISTAR);
    }

    public List<Paciente> listarActivos() {
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

    public void actualizar(Paciente paciente) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(ACTUALIZAR)) {
            ps.setString(1, paciente.getNombre());
            ps.setString(2, paciente.getTelefono());
            ps.setString(3, paciente.getCorreo());
            ps.setString(4, paciente.getDireccion());
            ps.setInt(5, paciente.getIdPaciente());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw UtilErrores.errorBD("actualizar el paciente", e);
        }
    }

    public void cambiarEstado(int idPaciente, EstadoPersona estado) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(CAMBIAR_ESTADO)) {
            ps.setString(1, estado.name());
            ps.setInt(2, idPaciente);
            if (ps.executeUpdate() == 0) {
                throw new RuntimeException("El paciente con id " + idPaciente + " no existe.");
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("cambiar el estado del paciente", e);
        }
    }

    private List<Paciente> ejecutarLista(String sql) {
        List<Paciente> pacientes = new ArrayList<>();
        try (Statement st = ConexionBD.obtenerInstancia().getConexion().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                pacientes.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("listar los pacientes", e);
        }
        return pacientes;
    }

    private Paciente mapear(ResultSet rs) throws SQLException {
        return new Paciente(
                rs.getInt("id_paciente"),
                rs.getString("nombre"),
                rs.getString("dni"),
                rs.getString("telefono"),
                rs.getString("correo"),
                rs.getString("direccion"),
                rs.getDate("fecha_registro").toLocalDate(),
                EstadoPersona.valueOf(rs.getString("estado")));
    }
}