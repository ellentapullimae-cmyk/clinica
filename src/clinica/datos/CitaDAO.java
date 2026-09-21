package clinica.datos;

import clinica.modelo.Cita;
import clinica.modelo.EstadoCita;
import clinica.modelo.Medico;
import clinica.modelo.Paciente;
import clinica.util.UtilErrores;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la entidad Cita (RF-03).
 * Regla: no se deben asignar dos citas al mismo medico en el mismo horario.
 */
public class CitaDAO {

    private static final String SELECCIONAR =
            "SELECT c.id_cita, c.fecha, c.hora, c.estado, c.observacion, "
            + "p.id_paciente, p.nombre AS nombre_paciente, p.dni AS dni_paciente, "
            + "m.id_medico, m.nombre AS nombre_medico, m.especialidad "
            + "FROM cita c "
            + "JOIN paciente p ON p.id_paciente = c.id_paciente "
            + "JOIN medico m ON m.id_medico = c.id_medico ";

    private static final String INSERTAR =
            "INSERT INTO cita (id_paciente, id_medico, fecha, hora, estado, observacion) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String BUSCAR_POR_ID = SELECCIONAR + "WHERE c.id_cita = ?";
    private static final String LISTAR = SELECCIONAR + "ORDER BY c.fecha, c.hora, c.id_cita";
    private static final String LISTAR_RANGO =
            SELECCIONAR + "WHERE c.fecha BETWEEN ? AND ? ORDER BY c.fecha, c.hora";
    private static final String LISTAR_POR_MEDICO =
            SELECCIONAR + "WHERE c.id_medico = ? ORDER BY c.fecha, c.hora, c.id_cita";
    private static final String LISTAR_RANGO_POR_MEDICO =
            SELECCIONAR + "WHERE c.id_medico = ? AND c.fecha BETWEEN ? AND ? ORDER BY c.fecha, c.hora";
    private static final String ACTUALIZAR =
            "UPDATE cita SET fecha = ?, hora = ?, estado = ?, observacion = ? WHERE id_cita = ?";
    private static final String CAMBIAR_ESTADO =
            "UPDATE cita SET estado = ? WHERE id_cita = ?";
    private static final String CONFLICTO_MEDICO_HORARIO =
            "SELECT COUNT(*) FROM cita "
            + "WHERE id_medico = ? AND fecha = ? AND hora = ? "
            + "AND estado <> 'CANCELADA' AND id_cita <> ?";

    public int insertar(Cita cita) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia()
                .getConexion().prepareStatement(INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, cita.getPaciente().getIdPaciente());
            ps.setInt(2, cita.getMedico().getIdMedico());
            ps.setDate(3, Date.valueOf(cita.getFecha()));
            ps.setTime(4, Time.valueOf(cita.getHora()));
            ps.setString(5, cita.getEstado().name());
            ps.setString(6, cita.getObservacion());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw UtilErrores.errorBD("programar la cita", e);
        }
    }

    public Cita buscarPorId(int idCita) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(BUSCAR_POR_ID)) {
            ps.setInt(1, idCita);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("consultar la cita", e);
        }
        return null;
    }

    public List<Cita> listar() {
        List<Cita> citas = new ArrayList<>();
        try (Statement st = ConexionBD.obtenerInstancia().getConexion().createStatement();
             ResultSet rs = st.executeQuery(LISTAR)) {
            while (rs.next()) {
                citas.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("listar las citas", e);
        }
        return citas;
    }

    public List<Cita> listarEntreFechas(LocalDate inicio, LocalDate fin) {
        List<Cita> citas = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(LISTAR_RANGO)) {
            ps.setDate(1, Date.valueOf(inicio));
            ps.setDate(2, Date.valueOf(fin));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    citas.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("listar las citas del periodo", e);
        }
        return citas;
    }

    public List<Cita> listarPorMedico(int idMedico) {
        List<Cita> citas = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(LISTAR_POR_MEDICO)) {
            ps.setInt(1, idMedico);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    citas.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("listar las citas del medico", e);
        }
        return citas;
    }

    public List<Cita> listarPorMedicoEntreFechas(int idMedico, LocalDate inicio, LocalDate fin) {
        List<Cita> citas = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(LISTAR_RANGO_POR_MEDICO)) {
            ps.setInt(1, idMedico);
            ps.setDate(2, Date.valueOf(inicio));
            ps.setDate(3, Date.valueOf(fin));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    citas.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("listar las citas del medico", e);
        }
        return citas;
    }

    public void actualizar(Cita cita) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(ACTUALIZAR)) {
            ps.setDate(1, Date.valueOf(cita.getFecha()));
            ps.setTime(2, Time.valueOf(cita.getHora()));
            ps.setString(3, cita.getEstado().name());
            ps.setString(4, cita.getObservacion());
            ps.setInt(5, cita.getIdCita());
            if (ps.executeUpdate() == 0) {
                throw new RuntimeException("La cita con id " + cita.getIdCita() + " no existe.");
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("modificar la cita", e);
        }
    }

    public void cambiarEstado(int idCita, EstadoCita estado) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(CAMBIAR_ESTADO)) {
            ps.setString(1, estado.name());
            ps.setInt(2, idCita);
            if (ps.executeUpdate() == 0) {
                throw new RuntimeException("La cita con id " + idCita + " no existe.");
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("cambiar el estado de la cita", e);
        }
    }

    public boolean existeConflictoMedicoHora(int idMedico, LocalDate fecha, LocalTime hora, int excluirIdCita) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(CONFLICTO_MEDICO_HORARIO)) {
            ps.setInt(1, idMedico);
            ps.setDate(2, Date.valueOf(fecha));
            ps.setTime(3, Time.valueOf(hora));
            ps.setInt(4, excluirIdCita);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("verificar disponibilidad", e);
        }
        return false;
    }

    private Cita mapear(ResultSet rs) throws SQLException {
        Paciente paciente = new Paciente(
                rs.getInt("id_paciente"),
                rs.getString("nombre_paciente"),
                rs.getString("dni_paciente"));
        Medico medico = new Medico(
                rs.getInt("id_medico"),
                rs.getString("nombre_medico"),
                rs.getString("especialidad"));
        return new Cita(
                rs.getInt("id_cita"),
                paciente,
                medico,
                rs.getDate("fecha").toLocalDate(),
                rs.getTime("hora").toLocalTime(),
                EstadoCita.valueOf(rs.getString("estado")),
                rs.getString("observacion"));
    }
}