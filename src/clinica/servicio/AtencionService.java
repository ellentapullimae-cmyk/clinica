package clinica.servicio;

import clinica.datos.AtencionDAO;
import clinica.datos.CitaDAO;
import clinica.datos.ConexionBD;
import clinica.modelo.Atencion;
import clinica.modelo.Cita;
import clinica.modelo.EstadoCita;
import clinica.util.Validaciones;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio de la entidad Atencion (RF-04 y RF-09).
 * Reglas de negocio:
 * - Una atencion debe estar relacionada con una cita existente.
 * - Una cita genera una sola atencion (una cita atendida no se vuelve a atender).
 * - Al registrar la atencion, la cita cambia a estado ATENDIDA (transaccional).
 */
public class AtencionService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AtencionDAO atencionDAO;
    private final CitaDAO citaDAO;

    public AtencionService() {
        this.atencionDAO = new AtencionDAO();
        this.citaDAO = new CitaDAO();
    }

    public Atencion registrar(int idCita, String diagnostico, String observaciones, LocalDate fechaAtencion) {
        if (fechaAtencion == null) {
            throw new IllegalArgumentException("La fecha de la atencion es obligatoria.");
        }
        if (!Validaciones.esTextoValido(diagnostico)) {
            throw new IllegalArgumentException("El diagnostico es obligatorio.");
        }
        Cita cita = citaDAO.buscarPorId(idCita);
        if (cita == null) {
            throw new IllegalArgumentException("La cita con id " + idCita + " no existe. "
                    + "Toda atencion debe estar relacionada con una cita.");
        }
        if (fechaAtencion.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de la atencion no puede ser futura.");
        }
        if (fechaAtencion.isBefore(cita.getFecha())) {
            throw new IllegalArgumentException(
                    "La atencion no puede registrarse antes de la fecha de la cita ("
                            + cita.getFecha().format(FORMATO_FECHA) + ").");
        }
        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new IllegalArgumentException("No se puede registrar una atencion para una cita cancelada.");
        }
        if (cita.getEstado() == EstadoCita.ATENDIDA) {
            throw new IllegalArgumentException("La cita ya fue atendida: una cita genera una sola atencion.");
        }

        Connection conexion = ConexionBD.obtenerInstancia().getConexion();
        boolean autocommitOriginal;
        try {
            autocommitOriginal = conexion.getAutoCommit();
        } catch (SQLException e) {
            throw new RuntimeException("Error de conexion al registrar la atencion.", e);
        }

        try {
            conexion.setAutoCommit(false);
            Atencion atencion = new Atencion(
                    cita,
                    cita.getPaciente(),
                    diagnostico.trim(),
                    Validaciones.normalizar(observaciones),
                    fechaAtencion);
            int id = atencionDAO.insertar(atencion);
            atencion.setIdAtencion(id);
            citaDAO.cambiarEstado(idCita, EstadoCita.ATENDIDA);
            conexion.commit();
            return atencion;
        } catch (Exception e) {
            try {
                conexion.rollback();
            } catch (SQLException ex) {
                e.addSuppressed(ex);
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Error al registrar la atencion: " + e.getMessage(), e);
        } finally {
            try {
                conexion.setAutoCommit(autocommitOriginal);
            } catch (SQLException e) {
                // No se puede restablecer el modo de autocommit.
            }
        }
    }

    public List<Atencion> listar() {
        return atencionDAO.listar();
    }

    public List<Atencion> listarEntreFechas(LocalDate inicio, LocalDate fin) {
        Validaciones.validarRangoFechas(inicio, fin);
        return atencionDAO.listarEntreFechas(inicio, fin);
    }

    /** RF-09: Consultar el historial de atenciones de un paciente. */
    public List<Atencion> historialPorPaciente(int idPaciente) {
        return atencionDAO.listarPorPaciente(idPaciente);
    }
}