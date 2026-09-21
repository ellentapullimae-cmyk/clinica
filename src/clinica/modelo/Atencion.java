package clinica.modelo;

import java.time.LocalDate;

/**
 * Entidad Atencion.
 * Atributos del documento: id_atencion, diagnostico, observaciones.
 * Regla: una atencion debe estar relacionada con una cita (1 cita = 1 atencion).
 */
public class Atencion {

    private int idAtencion;
    private Cita cita;
    private Paciente paciente;
    private String diagnostico;
    private String observaciones;
    private LocalDate fechaAtencion;

    public Atencion() {
    }

    public Atencion(Cita cita, Paciente paciente, String diagnostico,
                    String observaciones, LocalDate fechaAtencion) {
        this.cita = cita;
        this.paciente = paciente;
        this.diagnostico = diagnostico;
        this.observaciones = observaciones;
        this.fechaAtencion = fechaAtencion;
    }

    public Atencion(int idAtencion, Cita cita, Paciente paciente, String diagnostico,
                    String observaciones, LocalDate fechaAtencion) {
        this.idAtencion = idAtencion;
        this.cita = cita;
        this.paciente = paciente;
        this.diagnostico = diagnostico;
        this.observaciones = observaciones;
        this.fechaAtencion = fechaAtencion;
    }

    public int getIdAtencion() {
        return idAtencion;
    }

    public void setIdAtencion(int idAtencion) {
        this.idAtencion = idAtencion;
    }

    public Cita getCita() {
        return cita;
    }

    public void setCita(Cita cita) {
        this.cita = cita;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDate getFechaAtencion() {
        return fechaAtencion;
    }

    public void setFechaAtencion(LocalDate fechaAtencion) {
        this.fechaAtencion = fechaAtencion;
    }

    public String getNombrePaciente() {
        if (paciente != null) {
            return paciente.getNombre();
        }
        if (cita != null && cita.getPaciente() != null) {
            return cita.getPaciente().getNombre();
        }
        return "";
    }

    public String getNombreMedico() {
        return cita != null ? cita.getNombreMedico() : "";
    }

    @Override
    public String toString() {
        return "Atencion{id=" + idAtencion + ", cita=" + (cita != null ? cita.getIdCita() : 0)
                + ", paciente=" + getNombrePaciente() + ", diagnostico='" + diagnostico + '\'' + '}';
    }
}