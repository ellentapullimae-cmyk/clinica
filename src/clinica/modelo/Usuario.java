package clinica.modelo;

import java.time.LocalDateTime;

/**
 * Entidad Usuario: cuentas de acceso al sistema.
 * La clave nunca se guarda en texto plano: se almacena el hash SHA-256
 * de sal + clave, junto con la sal utilizada.
 */
public class Usuario {

    private int idUsuario;
    private String nombreUsuario;
    private String nombreCompleto;
    private String claveHash;
    private String sal;
    private Rol rol;
    private Integer idMedico;
    private boolean activo;
    private LocalDateTime fechaRegistro;
    private String nombreMedicoVinculado;

    public Usuario() {
        this.activo = true;
    }

    public Usuario(int idUsuario, String nombreUsuario, String nombreCompleto,
                   Rol rol, Integer idMedico, boolean activo, LocalDateTime fechaRegistro) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.idMedico = idMedico;
        this.activo = activo;
        this.fechaRegistro = fechaRegistro;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getClaveHash() {
        return claveHash;
    }

    public void setClaveHash(String claveHash) {
        this.claveHash = claveHash;
    }

    public String getSal() {
        return sal;
    }

    public void setSal(String sal) {
        this.sal = sal;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Integer getIdMedico() {
        return idMedico;
    }

    public void setIdMedico(Integer idMedico) {
        this.idMedico = idMedico;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getNombreMedicoVinculado() {
        return nombreMedicoVinculado;
    }

    public void setNombreMedicoVinculado(String nombreMedicoVinculado) {
        this.nombreMedicoVinculado = nombreMedicoVinculado;
    }

    @Override
    public String toString() {
        return "Usuario{id=" + idUsuario + ", usuario=" + nombreUsuario
                + ", rol=" + rol + ", activo=" + activo + '}';
    }
}