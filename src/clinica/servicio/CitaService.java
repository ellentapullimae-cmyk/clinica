package clinica.servicio;

import clinica.datos.CitaDAO;
import clinica.modelo.Cita;
import clinica.modelo.EstadoCita;
import clinica.modelo.EstadoPersona;
import clinica.modelo.Medico;
import clinica.modelo.Paciente;
import clinica.util.Validaciones;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Servicio de la entidad Cita (RF-03).
 * Reglas de negocio:
 * - Una cita debe tener un medico, fecha y hora registrados.
 * - No se deben asignar dos citas al mismo medico en el mismo horario.
 * - Una cita cancelada no ocupa el horario del medico.
 */
public class CitaService {

    private final CitaDAO citaDAO;
    private final PacienteService pacienteService;
    private final MedicoService medicoService;

    public CitaService() {
        this.citaDAO = new CitaDAO();
        this.pacienteService = new PacienteService();
        this.medicoService = new MedicoService();
    }

    public Cita programar(int idPaciente, int idMedico, LocalDate fecha, LocalTime hora, String observacion) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de la cita es obligatoria.");
        }
        if (hora == null) {
            throw new IllegalArgumentException("La hora de la cita es obligatoria.");
        }
        exigirHorarioFuturo(fecha, hora);
        Paciente paciente = pacienteService.buscarPorId(idPaciente);
        if (paciente.getEstado() != EstadoPersona.ACTIVO) {
            throw new IllegalArgumentException("El paciente se encuentra inactivo y no puede programar citas.");
        }
        Medico medico = medicoService.buscarPorId(idMedico);
        if (medico.getEstado() != EstadoPersona.ACTIVO) {
            throw new IllegalArgumentException("El medico se encuentra inactivo y no recibe citas.");
        }
        verificarDisponibilidad(idMedico, fecha, hora, -1);
        Cita cita = new Cita(paciente, medico, fecha, hora,
                observacion == null ? null : observacion.trim());
        int id = citaDAO.insertar(cita);
        cita.setIdCita(id);
        return cita;
    }

    public Cita buscarPorId(int idCita) {
        Cita cita = citaDAO.buscarPorId(idCita);
        if (cita == null) {
            throw new IllegalArgumentException("No existe una cita con id " + idCita + ".");
        }
        return cita;
    }

    public List<Cita> listar() {
        return citaDAO.listar();
    }

    public List<Cita> listarPorMedico(int idMedico) {
        return citaDAO.listarPorMedico(idMedico);
    }

    public List<Cita> listarPorMedicoEntreFechas(int idMedico, LocalDate inicio, LocalDate fin) {
        Validaciones.validarRangoFechas(inicio, fin);
        return citaDAO.listarPorMedicoEntreFechas(idMedico, inicio, fin);
    }

    public List<Cita> listarEntreFechas(LocalDate inicio, LocalDate fin) {
        Validaciones.validarRangoFechas(inicio, fin);
        return citaDAO.listarEntreFechas(inicio, fin);
    }

    public void modificar(int idCita, LocalDate fecha, LocalTime hora,
                          EstadoCita nuevoEstado, String observacion) {
        if (fecha == null || hora == null) {
            throw new IllegalArgumentException("La fecha y la hora de la cita son obligatorias.");
        }
        exigirHorarioFuturo(fecha, hora);
        Cita cita = buscarPorId(idCita);
        if (cita.getEstado() == EstadoCita.ATENDIDA) {
            throw new IllegalArgumentException("No se puede modificar una cita ya atendida.");
        }
        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new IllegalArgumentException("No se puede modificar una cita cancelada.");
        }
        if (nuevoEstado == null) {
            nuevoEstado = cita.getEstado();
        }
        if (nuevoEstado != EstadoCita.PROGRAMADA && nuevoEstado != EstadoCita.CONFIRMADA) {
            throw new IllegalArgumentException(
                    "El estado de la cita solo puede ser PROGRAMADA o CONFIRMADA; "
                            + "ATENDIDA se asigna al registrar la atencion.");
        }
        Paciente paciente = pacienteService.buscarPorId(cita.getPaciente().getIdPaciente());
        if (paciente.getEstado() != EstadoPersona.ACTIVO) {
            throw new IllegalArgumentException("El paciente se encuentra inactivo y no puede tener citas.");
        }
        Medico medico = medicoService.buscarPorId(cita.getMedico().getIdMedico());
        if (medico.getEstado() != EstadoPersona.ACTIVO) {
            throw new IllegalArgumentException("El medico se encuentra inactivo y no recibe citas.");
        }
        verificarDisponibilidad(cita.getMedico().getIdMedico(), fecha, hora, idCita);
        cita.setFecha(fecha);
        cita.setHora(hora);
        cita.setEstado(nuevoEstado);
        cita.setObservacion(observacion == null ? null : observacion.trim());
        citaDAO.actualizar(cita);
    }

    public void confirmar(int idCita) {
        Cita cita = buscarPorId(idCita);
        if (cita.getEstado() == EstadoCita.ATENDIDA || cita.getEstado() == EstadoCita.CANCELADA) {
            throw new IllegalArgumentException("La cita no puede confirmarse en su estado actual.");
        }
        citaDAO.cambiarEstado(idCita, EstadoCita.CONFIRMADA);
    }

    public void cancelar(int idCita) {
        Cita cita = buscarPorId(idCita);
        if (cita.getEstado() == EstadoCita.ATENDIDA) {
            throw new IllegalArgumentException("No se puede cancelar una cita ya atendida.");
        }
        citaDAO.cambiarEstado(idCita, EstadoCita.CANCELADA);
    }

    private void verificarDisponibilidad(int idMedico, LocalDate fecha, LocalTime hora, int excluirId) {
        if (citaDAO.existeConflictoMedicoHora(idMedico, fecha, hora, excluirId)) {
            throw new IllegalArgumentException(
                    "El medico ya tiene una cita registrada en esa fecha y horario.");
        }
    }

    /** Exige que la cita no se programe ni en el pasado ni en una hora ya pasada de hoy. */
    private void exigirHorarioFuturo(LocalDate fecha, LocalTime hora) {
        LocalDateTime horarioCita = LocalDateTime.of(fecha, hora);
        if (horarioCita.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("No se puede programar una cita en una fecha u hora pasada.");
        }
    }
}