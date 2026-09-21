package clinica.modelo;

import java.time.LocalDate;

/**
 * Entidad Paciente.
 * Atributos del documento: id_paciente, nombre, DNI.
 * Regla: un paciente puede tener varias citas.
 */
public class Paciente {

    private int idPaciente;
    private String nombre;
    private String dni;
    private String telefono;
    private String correo;
    private String direccion;
    private LocalDate fechaRegistro;
    private EstadoPersona estado;

    public Paciente() {
    }

    public Paciente(String nombre, String dni, String telefono, String correo, String direccion) {
        this.nombre = nombre;
        this.dni = dni;
        this.telefono = telefono;
        this.correo = correo;
        this.direccion = direccion;
        this.fechaRegistro = LocalDate.now();
        this.estado = EstadoPersona.ACTIVO;
    }

    public Paciente(int idPaciente, String nombre, String dni) {
        this.idPaciente = idPaciente;
        this.nombre = nombre;
        this.dni = dni;
        this.estado = EstadoPersona.ACTIVO;
    }

    public Paciente(int idPaciente, String nombre, String dni, String telefono, String correo,
                    String direccion, LocalDate fechaRegistro, EstadoPersona estado) {
        this.idPaciente = idPaciente;
        this.nombre = nombre;
        this.dni = dni;
        this.telefono = telefono;
        this.correo = correo;
        this.direccion = direccion;
        this.fechaRegistro = fechaRegistro;
        this.estado = estado;
    }

    public int getIdPaciente() {
        return idPaciente;
    }

    public void setIdPaciente(int idPaciente) {
        this.idPaciente = idPaciente;
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

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
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
        return "Paciente{id=" + idPaciente + ", nombre='" + nombre + '\''
                + ", dni='" + dni + '\'' + ", estado=" + estado + '}';
    }
}