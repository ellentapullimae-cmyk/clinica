package clinica.controlador;

import clinica.modelo.Cita;
import clinica.modelo.EstadoCita;
import clinica.servicio.CitaService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Controlador del modulo Citas.
 */
public class CitaController {

    private final CitaService servicio;

    public CitaController() {
        this.servicio = new CitaService();
    }

    public Cita programar(int idPaciente, int idMedico, LocalDate fecha, LocalTime hora, String observacion) {
        return servicio.programar(idPaciente, idMedico, fecha, hora, observacion);
    }

    public Cita buscarPorId(int idCita) {
        return servicio.buscarPorId(idCita);
    }

    public List<Cita> listar() {
        return servicio.listar();
    }

    public List<Cita> listarPorMedico(int idMedico) {
        return servicio.listarPorMedico(idMedico);
    }

    public List<Cita> listarPorMedicoEntreFechas(int idMedico, LocalDate inicio, LocalDate fin) {
        return servicio.listarPorMedicoEntreFechas(idMedico, inicio, fin);
    }

    public List<Cita> listarEntreFechas(LocalDate inicio, LocalDate fin) {
        return servicio.listarEntreFechas(inicio, fin);
    }

    public void modificar(int idCita, LocalDate fecha, LocalTime hora,
                          EstadoCita estado, String observacion) {
        servicio.modificar(idCita, fecha, hora, estado, observacion);
    }

    public void confirmar(int idCita) {
        servicio.confirmar(idCita);
    }

    public void cancelar(int idCita) {
        servicio.cancelar(idCita);
    }
}