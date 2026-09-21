package clinica.controlador;

import clinica.modelo.EstadoPersona;
import clinica.modelo.Medico;
import clinica.servicio.MedicoService;
import java.util.List;

/**
 * Controlador del modulo Medicos.
 */
public class MedicoController {

    private final MedicoService servicio;

    public MedicoController() {
        this.servicio = new MedicoService();
    }

    public Medico registrar(String nombre, String dni, String especialidad, String telefono) {
        return servicio.registrar(nombre, dni, especialidad, telefono);
    }

    public Medico buscarPorId(int idMedico) {
        return servicio.buscarPorId(idMedico);
    }

    public Medico buscarPorDni(String dni) {
        return servicio.buscarPorDni(dni);
    }

    public List<Medico> listar() {
        return servicio.listar();
    }

    public List<Medico> listarActivos() {
        return servicio.listarActivos();
    }

    public void actualizar(int idMedico, String nombre, String especialidad, String telefono) {
        servicio.actualizar(idMedico, nombre, especialidad, telefono);
    }

    public void cambiarEstado(int idMedico, EstadoPersona estado) {
        servicio.cambiarEstado(idMedico, estado);
    }
}