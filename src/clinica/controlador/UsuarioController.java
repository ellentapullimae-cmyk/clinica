package clinica.controlador;

import clinica.modelo.Rol;
import clinica.modelo.Usuario;
import clinica.servicio.UsuarioService;
import java.util.List;

/**
 * Controlador del modulo de Usuarios y login.
 */
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController() {
        this.servicio = new UsuarioService();
    }

    public Usuario autenticar(String nombreUsuario, String clave) {
        return servicio.autenticar(nombreUsuario, clave);
    }

    public Usuario registrar(String nombreUsuario, String nombreCompleto, String clave,
                             Rol rol, Integer idMedico) {
        return servicio.registrar(nombreUsuario, nombreCompleto, clave, rol, idMedico);
    }

    public List<Usuario> listar() {
        return servicio.listar();
    }

    public Usuario buscarPorId(int idUsuario) {
        return servicio.buscarPorId(idUsuario);
    }

    public void actualizarDatos(int idUsuario, String nombreCompleto, Rol rol, Integer idMedico,
                                boolean activo, int idUsuarioSesion) {
        servicio.actualizarDatos(idUsuario, nombreCompleto, rol, idMedico, activo, idUsuarioSesion);
    }

    public void cambiarClave(int idUsuario, String claveNueva) {
        servicio.cambiarClave(idUsuario, claveNueva);
    }

    public void cambiarMiClave(String nombreUsuario, String claveActual, String claveNueva) {
        servicio.cambiarMiClave(nombreUsuario, claveActual, claveNueva);
    }

    public void cambiarEstado(int idUsuario, boolean activo, int idUsuarioSesion) {
        servicio.cambiarEstado(idUsuario, activo, idUsuarioSesion);
    }
}