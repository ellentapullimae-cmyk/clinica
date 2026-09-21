package clinica.servicio;

import clinica.datos.MedicoDAO;
import clinica.modelo.EstadoPersona;
import clinica.modelo.Medico;
import clinica.util.Validaciones;
import java.util.List;

/**
 * Servicio de la entidad Medico (RF-02).
 */
public class MedicoService {

    private final MedicoDAO medicoDAO;

    public MedicoService() {
        this.medicoDAO = new MedicoDAO();
    }

    public Medico registrar(String nombre, String dni, String especialidad, String telefono) {
        if (!Validaciones.esTextoValido(nombre)) {
            throw new IllegalArgumentException("El nombre del medico es obligatorio.");
        }
        if (!Validaciones.esDniValido(dni)) {
            throw new IllegalArgumentException("El DNI debe contener 8 digitos.");
        }
        if (!Validaciones.esTextoValido(especialidad)) {
            throw new IllegalArgumentException("La especialidad es obligatoria.");
        }
        if (!Validaciones.esTelefonoValido(telefono)) {
            throw new IllegalArgumentException("El telefono debe contener de 7 a 15 digitos.");
        }
        if (medicoDAO.existeDni(dni.trim(), -1)) {
            throw new IllegalArgumentException("Ya existe un medico con el DNI " + dni + ".");
        }
        Medico medico = new Medico(
                nombre.trim(),
                dni.trim(),
                especialidad.trim(),
                Validaciones.normalizar(telefono));
        int id = medicoDAO.insertar(medico);
        medico.setIdMedico(id);
        return medico;
    }

    public Medico buscarPorId(int idMedico) {
        Medico medico = medicoDAO.buscarPorId(idMedico);
        if (medico == null) {
            throw new IllegalArgumentException("No existe un medico con id " + idMedico + ".");
        }
        return medico;
    }

    public Medico buscarPorDni(String dni) {
        if (!Validaciones.esDniValido(dni)) {
            throw new IllegalArgumentException("El DNI debe contener 8 digitos.");
        }
        return medicoDAO.buscarPorDni(dni.trim());
    }

    public List<Medico> listar() {
        return medicoDAO.listar();
    }

    public List<Medico> listarActivos() {
        return medicoDAO.listarActivos();
    }

    public void actualizar(int idMedico, String nombre, String especialidad, String telefono) {
        if (!Validaciones.esTextoValido(nombre)) {
            throw new IllegalArgumentException("El nombre del medico es obligatorio.");
        }
        if (!Validaciones.esTextoValido(especialidad)) {
            throw new IllegalArgumentException("La especialidad es obligatoria.");
        }
        if (!Validaciones.esTelefonoValido(telefono)) {
            throw new IllegalArgumentException("El telefono debe contener de 7 a 15 digitos.");
        }
        Medico medico = buscarPorId(idMedico);
        medico.setNombre(nombre.trim());
        medico.setEspecialidad(especialidad.trim());
        medico.setTelefono(Validaciones.normalizar(telefono));
        medicoDAO.actualizar(medico);
    }

    public void cambiarEstado(int idMedico, EstadoPersona estado) {
        if (estado == null) {
            throw new IllegalArgumentException("Debe indicar el nuevo estado.");
        }
        buscarPorId(idMedico);
        medicoDAO.cambiarEstado(idMedico, estado);
    }
}