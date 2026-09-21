package clinica.modelo;

import java.time.LocalDate;

/**
 * Entidad Medico.
 * Atributos del documento: id_medico, nombre, especialidad.
 * Responsabilidad: registrar informacion profesional y realizar atenciones.
 */
public class Medico {

    private int idMedico;
    private String nombre;
    private String dni;
    private String especialidad;
    private String telefono;
    private LocalDate fechaRegistro;
    private EstadoPersona estado;

    public Medico() {
    }

    public Medico(String nombre, String dni, String especialidad, String telefono) {
        this.nombre = nombre;
        this.dni = dni;
        this.especialidad = especialidad;
        this.telefono = telefono;
        this.fechaRegistro = LocalDate.now();
        this.estado = EstadoPersona.ACTIVO;
    }

    public Medico(int idMedico, String nombre, String especialidad) {
        this.idMedico = idMedico;
        this.nombre = nombre;
        this.especialidad = especialidad;
        this.estado = EstadoPersona.ACTIVO;
    }

    public Medico(int idMedico, String nombre, String dni, String especialidad, String telefono,
                  LocalDate fechaRegistro, EstadoPersona estado) {
        this.idMedico = idMedico;
        this.nombre = nombre;
        this.dni = dni;
        this.especialidad = especialidad;
        this.telefono = telefono;
        this.fechaRegistro = fechaRegistro;
        this.estado = estado;
    }

    public int getIdMedico() {
        return idMedico;
    }

    public void setIdMedico(int idMedico) {
        this.idMedico = idMedico;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public EstadoPersona getEstado() {
        return estado;
    }

    public void setEstado(EstadoPersona estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "Medico{id=" + idMedico + ", nombre='" + nombre + '\''
                + ", especialidad='" + especialidad + '\'' + ", estado=" + estado + '}';
    }
}