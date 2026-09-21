package clinica.datos;

import clinica.modelo.Atencion;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la entidad Atencion (RF-04 y RF-09).
 * Regla: una atencion debe relacionarse con una cita (1 cita = 1 atencion).
 */
public class AtencionDAO {

    private static final String SELECCIONAR =
            "SELECT a.id_atencion, a.diagnostico, a.observaciones, a.fecha_atencion, "
            + "c.id_cita, c.fecha, c.hora, c.estado, c.observacion, "
            + "p.id_paciente, p.nombre AS nombre_paciente, p.dni AS dni_paciente, "
            + "m.id_medico, m.nombre AS nombre_medico, m.especialidad "
            + "FROM atencion a "
            + "JOIN cita c ON c.id_cita = a.id_cita "
            + "JOIN paciente p ON p.id_paciente = a.id_paciente "
            + "JOIN medico m ON m.id_medico = c.id_medico ";

    private static final String INSERTAR =
            "INSERT INTO atencion (id_cita, id_paciente, diagnostico, observaciones, fecha_atencion) "
            + "VALUES (?, ?, ?, ?, ?)";
    private static final String BUSCAR_POR_ID = SELECCIONAR + "WHERE a.id_atencion = ?";
    private static final String LISTAR = SELECCIONAR + "ORDER BY a.fecha_atencion DESC, a.id_atencion";
    private static final String LISTAR_RANGO =
            SELECCIONAR + "WHERE a.fecha_atencion BETWEEN ? AND ? ORDER BY a.fecha_atencion, a.id_atencion";
    private static final String LISTAR_POR_PACIENTE =
            SELECCIONAR + "WHERE a.id_paciente = ? ORDER BY a.fecha_atencion DESC, a.id_atencion";

    public int insertar(Atencion atencion) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia()
                .getConexion().prepareStatement(INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, atencion.getCita().getIdCita());
            ps.setInt(2, atencion.getPaciente().getIdPaciente());
            ps.setString(3, atencion.getDiagnostico());
            ps.setString(4, atencion.getObservaciones());
            ps.setDate(5, Date.valueOf(atencion.getFechaAtencion()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw UtilErrores.errorBD("registrar la atencion", e);
        }
    }

    public Atencion buscarPorId(int idAtencion) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(BUSCAR_POR_ID)) {
            ps.setInt(1, idAtencion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("consultar la atencion", e);
        }
        return null;
    }

    public List<Atencion> listar() {
        List<Atencion> atenciones = new ArrayList<>();
        try (Statement st = ConexionBD.obtenerInstancia().getConexion().createStatement();
             ResultSet rs = st.executeQuery(LISTAR)) {
            while (rs.next()) {
                atenciones.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("listar las atenciones", e);
        }
        return atenciones;
    }

    public List<Atencion> listarEntreFechas(LocalDate inicio, LocalDate fin) {
        List<Atencion> atenciones = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(LISTAR_RANGO)) {
            ps.setDate(1, Date.valueOf(inicio));
            ps.setDate(2, Date.valueOf(fin));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    atenciones.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("listar las atenciones del periodo", e);
        }
        return atenciones;
    }

    public List<Atencion> listarPorPaciente(int idPaciente) {
        List<Atencion> atenciones = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(LISTAR_POR_PACIENTE)) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    atenciones.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("consultar el historial", e);
        }
        return atenciones;
    }

    private Atencion mapear(ResultSet rs) throws SQLException {
        Paciente paciente = new Paciente(
                rs.getInt("id_paciente"),
                rs.getString("nombre_paciente"),
                rs.getString("dni_paciente"));
        Medico medico = new Medico(
                rs.getInt("id_medico"),
                rs.getString("nombre_medico"),
                rs.getString("especialidad"));
        Cita cita = new Cita(
                rs.getInt("id_cita"),
                paciente,
                medico,
                rs.getDate("fecha").toLocalDate(),
                rs.getTime("hora").toLocalTime(),
                EstadoCita.valueOf(rs.getString("estado")),
                rs.getString("observacion"));
        return new Atencion(
                rs.getInt("id_atencion"),
                cita,
                paciente,
                rs.getString("diagnostico"),
                rs.getString("observaciones"),
                rs.getDate("fecha_atencion").toLocalDate());
    }
}