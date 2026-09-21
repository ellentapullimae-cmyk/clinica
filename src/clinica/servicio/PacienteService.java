package clinica.servicio;

import clinica.datos.PacienteDAO;
import clinica.modelo.EstadoPersona;
import clinica.modelo.Paciente;
import clinica.util.Validaciones;
import java.util.List;

/**
 * Servicio de la entidad Paciente (RF-01).
 * Aplica las validaciones y reglas de negocio del paciente.
 */
public class PacienteService {

    private final PacienteDAO pacienteDAO;

    public PacienteService() {
        this.pacienteDAO = new PacienteDAO();
    }

    public Paciente registrar(String nombre, String dni, String telefono, String correo, String direccion) {
        if (!Validaciones.esTextoValido(nombre)) {
            throw new IllegalArgumentException("El nombre del paciente es obligatorio.");
        }
        if (!Validaciones.esDniValido(dni)) {
            throw new IllegalArgumentException("El DNI debe contener 8 digitos.");
        }
        if (!Validaciones.esTelefonoValido(telefono)) {
            throw new IllegalArgumentException("El telefono debe contener de 7 a 15 digitos.");
        }
        if (!Validaciones.esCorreoValido(correo)) {
            throw new IllegalArgumentException("El correo electronico no es valido.");
        }
        if (pacienteDAO.existeDni(dni.trim(), -1)) {
            throw new IllegalArgumentException("Ya existe un paciente con el DNI " + dni + ".");
        }
        Paciente paciente = new Paciente(
                nombre.trim(),
                dni.trim(),
                Validaciones.normalizar(telefono),
                Validaciones.normalizar(correo),
                Validaciones.normalizar(direccion));
        int id = pacienteDAO.insertar(paciente);
        paciente.setIdPaciente(id);
        return paciente;
    }

    public Paciente buscarPorId(int idPaciente) {
        Paciente paciente = pacienteDAO.buscarPorId(idPaciente);
        if (paciente == null) {
            throw new IllegalArgumentException("No existe un paciente con id " + idPaciente + ".");
        }
        return paciente;
    }

    public Paciente buscarPorDni(String dni) {
        if (!Validaciones.esDniValido(dni)) {
            throw new IllegalArgumentException("El DNI debe contener 8 digitos.");
        }
        return pacienteDAO.buscarPorDni(dni.trim());
    }

    public List<Paciente> listar() {
        return pacienteDAO.listar();
    }

    public List<Paciente> listarActivos() {
        return pacienteDAO.listarActivos();
    }

    public void actualizar(int idPaciente, String nombre, String telefono, String correo, String direccion) {
        if (!Validaciones.esTextoValido(nombre)) {
            throw new IllegalArgumentException("El nombre del paciente es obligatorio.");
        }
        if (!Validaciones.esTelefonoValido(telefono)) {
            throw new IllegalArgumentException("El telefono debe contener de 7 a 15 digitos.");
        }
        if (!Validaciones.esCorreoValido(correo)) {
            throw new IllegalArgumentException("El correo electronico no es valido.");
        }
        Paciente paciente = buscarPorId(idPaciente);
        paciente.setNombre(nombre.trim());
        paciente.setTelefono(Validaciones.normalizar(telefono));
        paciente.setCorreo(Validaciones.normalizar(correo));
        paciente.setDireccion(Validaciones.normalizar(direccion));
        pacienteDAO.actualizar(paciente);
    }

    public void cambiarEstado(int idPaciente, EstadoPersona estado) {
        if (estado == null) {
            throw new IllegalArgumentException("Debe indicar el nuevo estado.");
        }
        buscarPorId(idPaciente);
        pacienteDAO.cambiarEstado(idPaciente, estado);
    }
}