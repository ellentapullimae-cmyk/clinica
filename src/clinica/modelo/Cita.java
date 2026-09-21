package clinica.modelo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entidad Cita.
 * Atributos del documento: id_cita, fecha, hora, estado.
 * Relaciones: una cita pertenece a un solo paciente y tiene un medico.
 * Regla: no se deben asignar dos citas al mismo medico en el mismo horario.
 */
public class Cita {

    private int idCita;
    private Paciente paciente;
    private Medico medico;
    private LocalDate fecha;
    private LocalTime hora;
    private EstadoCita estado;
    private String observacion;

    public Cita() {
        this.estado = EstadoCita.PROGRAMADA;
    }

    public Cita(Paciente paciente, Medico medico, LocalDate fecha, LocalTime hora, String observacion) {
        this.paciente = paciente;
        this.medico = medico;
        this.fecha = fecha;
        this.hora = hora;
        this.observacion = observacion;
        this.estado = EstadoCita.PROGRAMADA;
    }

    public Cita(int idCita, Paciente paciente, Medico medico, LocalDate fecha, LocalTime hora,
                EstadoCita estado, String observacion) {
        this.idCita = idCita;
        this.paciente = paciente;
        this.medico = medico;
        this.fecha = fecha;
        this.hora = hora;
        this.estado = estado;
        this.observacion = observacion;
    }

    public int getIdCita() {
        return idCita;
    }

    public void setIdCita(int idCita) {
        this.idCita = idCita;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public Medico getMedico() {
        return medico;
    }

    public void setMedico(Medico medico) {
        this.medico = medico;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public EstadoCita getEstado() {
        return estado;
    }

    public void setEstado(EstadoCita estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getNombrePaciente() {
        return paciente != null ? paciente.getNombre() : "";
    }

    public String getNombreMedico() {
        return medico != null ? medico.getNombre() : "";
    }

    @Override
    public String toString() {
        return "Cita{id=" + idCita + ", paciente=" + getNombrePaciente()
                + ", medico=" + getNombreMedico() + ", fecha=" + fecha
                + ", hora=" + hora + ", estado=" + estado + '}';
    }
}