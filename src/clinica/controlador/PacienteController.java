package clinica.controlador;

import clinica.modelo.EstadoPersona;
import clinica.modelo.Paciente;
import clinica.servicio.PacienteService;
import java.util.List;

/**
 * Controlador del modulo Pacientes.
 * Recibe las peticiones del menu, delega en el servicio y propaga los errores.
 */
public class PacienteController {

    private final PacienteService servicio;

    public PacienteController() {
        this.servicio = new PacienteService();
    }

    public Paciente registrar(String nombre, String dni, String telefono, String correo, String direccion) {
        return servicio.registrar(nombre, dni, telefono, correo, direccion);
    }

    public Paciente buscarPorId(int idPaciente) {
        return servicio.buscarPorId(idPaciente);
    }

    public Paciente buscarPorDni(String dni) {
        return servicio.buscarPorDni(dni);
    }

    public List<Paciente> listar() {
        return servicio.listar();
    }

    public List<Paciente> listarActivos() {
        return servicio.listarActivos();
    }

    public void actualizar(int idPaciente, String nombre, String telefono, String correo, String direccion) {
        servicio.actualizar(idPaciente, nombre, telefono, correo, direccion);
    }

    public void cambiarEstado(int idPaciente, EstadoPersona estado) {
        servicio.cambiarEstado(idPaciente, estado);
    }
}