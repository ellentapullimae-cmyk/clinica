package clinica.modelo;

/**
 * Roles del sistema (control de acceso).
 * Cada rol define los permisos que puede ejercer.
 */
public enum Rol {

    ADMINISTRADOR,
    RECEPCIONISTA,
    MEDICO;

    public boolean gestionarUsuarios() {
        return this == ADMINISTRADOR;
    }

    public boolean gestionarMedicos() {
        return this == ADMINISTRADOR;
    }

    public boolean registrarPaciente() {
        return this == ADMINISTRADOR || this == RECEPCIONISTA;
    }

    public boolean actualizarPaciente() {
        return this == ADMINISTRADOR || this == RECEPCIONISTA;
    }

    public boolean verPacientes() {
        return true;
    }

    public boolean programarCita() {
        return this == ADMINISTRADOR || this == RECEPCIONISTA;
    }

    public boolean modificarCita() {
        return this == ADMINISTRADOR || this == RECEPCIONISTA;
    }

    public boolean verCitas() {
        return true;
    }

    public boolean registrarAtencion() {
        return this == ADMINISTRADOR || this == MEDICO;
    }

    public boolean verAtenciones() {
        return true;
    }

    public boolean gestionarPagos() {
        return this == ADMINISTRADOR || this == RECEPCIONISTA;
    }

    public boolean gestionarGastos() {
        return this == ADMINISTRADOR;
    }

    public boolean gestionarPresupuesto() {
        return this == ADMINISTRADOR;
    }

    public boolean verReportes() {
        return true;
    }

    public boolean exportarReportes() {
        return true;
    }

    public boolean verDashboard() {
        return true;
    }

    public boolean esMedico() {
        return this == MEDICO;
    }
}