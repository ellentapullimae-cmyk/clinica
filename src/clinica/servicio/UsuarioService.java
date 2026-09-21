package clinica.servicio;

import clinica.datos.MedicoDAO;
import clinica.datos.UsuarioDAO;
import clinica.modelo.EstadoPersona;
import clinica.modelo.Medico;
import clinica.modelo.Rol;
import clinica.modelo.Usuario;
import clinica.util.UtilCifrado;
import clinica.util.Validaciones;
import java.util.List;

/**
 * Servicio de la entidad Usuario (login y control de acceso).
 * Reglas de negocio:
 * - El nombre de usuario es unico y debe tener formato valido.
 * - La clave minima es de 6 caracteres y se guarda como hash con sal.
 * - Un usuario no puede desactivarse ni cambiarse el rol a si mismo.
 * - Siempre debe existir al menos un administrador activo.
 * - El rol MEDICO debe estar vinculado a un medico registrado.
 */
public class UsuarioService {

    private final UsuarioDAO usuarioDAO;
    private final MedicoDAO medicoDAO;

    public UsuarioService() {
        this.usuarioDAO = new UsuarioDAO();
        this.medicoDAO = new MedicoDAO();
    }

    /** Autentica un usuario contra la base de datos. */
    public Usuario autenticar(String nombreUsuario, String clave) {
        if (!Validaciones.esTextoValido(nombreUsuario)) {
            throw new IllegalArgumentException("Debe ingresar el nombre de usuario.");
        }
        if (!Validaciones.esTextoValido(clave)) {
            throw new IllegalArgumentException("Debe ingresar la clave.");
        }
        Usuario usuario = usuarioDAO.obtenerPorNombreUsuario(nombreUsuario.trim());
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario o clave incorrectos.");
        }
        if (!usuario.getClaveHash().equalsIgnoreCase(UtilCifrado.hash(clave, usuario.getSal()))) {
            throw new IllegalArgumentException("Usuario o clave incorrectos.");
        }
        if (!usuario.isActivo()) {
            throw new IllegalArgumentException("El usuario se encuentra inactivo. Contacte al administrador.");
        }
        return usuario;
    }

    /** Registra un nuevo usuario con la clave protegida (hash + sal). */
    public Usuario registrar(String nombreUsuario, String nombreCompleto, String clave, Rol rol, Integer idMedico) {
        validarNombreUsuario(nombreUsuario, -1);
        validarClave(clave);
        if (!Validaciones.esTextoValido(nombreCompleto)) {
            throw new IllegalArgumentException("El nombre completo es obligatorio.");
        }
        validarVinculoMedico(rol, idMedico, -1);

        String sal = UtilCifrado.generarSal();
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(nombreUsuario.trim());
        usuario.setNombreCompleto(nombreCompleto.trim());
        usuario.setClaveHash(UtilCifrado.hash(clave, sal));
        usuario.setSal(sal);
        usuario.setRol(rol);
        usuario.setIdMedico(rol == Rol.MEDICO ? idMedico : null);
        usuario.setActivo(true);
        int id = usuarioDAO.insertar(usuario);
        usuario.setIdUsuario(id);
        return usuario;
    }

    public List<Usuario> listar() {
        return usuarioDAO.listar();
    }

    public Usuario buscarPorId(int idUsuario) {
        Usuario usuario = usuarioDAO.buscarPorId(idUsuario);
        if (usuario == null) {
            throw new IllegalArgumentException("No existe un usuario con id " + idUsuario + ".");
        }
        return usuario;
    }

    /**
     * Actualiza datos de un usuario (nombre, rol, medico vinculado y estado).
     * Un usuario no puede modificar su propio rol ni estado.
     */
    public void actualizarDatos(int idUsuario, String nombreCompleto, Rol rol, Integer idMedico,
                                boolean activo, int idUsuarioSesion) {
        if (!Validaciones.esTextoValido(nombreCompleto)) {
            throw new IllegalArgumentException("El nombre completo es obligatorio.");
        }
        if (rol == null) {
            throw new IllegalArgumentException("Debe seleccionar un rol.");
        }
        validarVinculoMedico(rol, idMedico, idUsuario);
        Usuario usuario = buscarPorId(idUsuario);

        if (idUsuario == idUsuarioSesion) {
            if (rol != usuario.getRol()) {
                throw new IllegalArgumentException("No puede cambiar su propio rol.");
            }
            if (activo != usuario.isActivo()) {
throw new IllegalArgumentException("No puede modificar el estado de su propia cuenta.");
            }
        }
        if ((rol != Rol.ADMINISTRADOR || !activo) && usuario.getRol() == Rol.ADMINISTRADOR
                && usuario.isActivo()) {
            protegerUltimoAdministrador();
        }
        usuario.setNombreCompleto(nombreCompleto.trim());
        usuario.setRol(rol);
        usuario.setIdMedico(rol == Rol.MEDICO ? idMedico : null);
        usuario.setActivo(activo);
        usuarioDAO.actualizarDatos(usuario);
    }

    /** Cambia la clave de un usuario (se regenera la sal). */
    public void cambiarClave(int idUsuario, String claveNueva) {
        validarClave(claveNueva);
        buscarPorId(idUsuario);
        String sal = UtilCifrado.generarSal();
        usuarioDAO.actualizarClave(idUsuario, UtilCifrado.hash(claveNueva, sal), sal);
    }

    /**
     * Cambio de contrasena propio: exige la clave actual del usuario
     * en sesion para permitir la actualizacion (se regenera la sal).
     */
    public void cambiarMiClave(String nombreUsuario, String claveActual, String claveNueva) {
        if (!Validaciones.esTextoValido(nombreUsuario)) {
            throw new IllegalArgumentException("Debe ingresar su nombre de usuario.");
        }
        if (!Validaciones.esTextoValido(claveActual)) {
            throw new IllegalArgumentException("Debe ingresar la clave actual.");
        }
        Usuario usuario = usuarioDAO.obtenerPorNombreUsuario(nombreUsuario.trim());
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado.");
        }
        if (!usuario.isActivo()) {
            throw new IllegalStateException("El usuario se encuentra inactivo.");
        }
        String hashActual = UtilCifrado.hash(claveActual, usuario.getSal());
        if (!hashActual.equalsIgnoreCase(usuario.getClaveHash())) {
            throw new IllegalArgumentException("La clave actual no es correcta.");
        }
        validarClave(claveNueva);
        String sal = UtilCifrado.generarSal();
        usuarioDAO.actualizarClave(usuario.getIdUsuario(), UtilCifrado.hash(claveNueva, sal), sal);
    }

    /** Activa o desactiva una cuenta de usuario. */
    public void cambiarEstado(int idUsuario, boolean activo, int idUsuarioSesion) {
        Usuario usuario = buscarPorId(idUsuario);
        if (idUsuario == idUsuarioSesion) {
            throw new IllegalArgumentException("No puede desactivar su propia cuenta.");
        }
        if (usuario.getRol() == Rol.ADMINISTRADOR && usuario.isActivo() && !activo) {
            protegerUltimoAdministrador();
        }
        usuario.setActivo(activo);
        usuarioDAO.actualizarDatos(usuario);
    }

    private void protegerUltimoAdministrador() {
        if (usuarioDAO.contarAdministradoresActivos() <= 1) {
            throw new IllegalArgumentException(
                    "No se puede modificar o desactivar al ultimo administrador activo del sistema.");
        }
    }

    private void validarNombreUsuario(String nombreUsuario, int excluirId) {
        if (!Validaciones.esTextoValido(nombreUsuario)) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        String nombre = nombreUsuario.trim();
        if (!nombre.matches("[a-zA-Z0-9_.]{3,30}")) {
            throw new IllegalArgumentException(
                    "El nombre de usuario debe tener entre 3 y 30 caracteres (letras, numeros, _ o .).");
        }
        if (usuarioDAO.existeNombreUsuario(nombre, excluirId)) {
            throw new IllegalArgumentException("El nombre de usuario '" + nombre + "' ya esta en uso.");
        }
    }

    private void validarClave(String clave) {
        if (!Validaciones.esTextoValido(clave) || clave.length() < 6) {
            throw new IllegalArgumentException("La clave debe tener al menos 6 caracteres.");
        }
    }

    private void validarVinculoMedico(Rol rol, Integer idMedico, int excluirId) {
        if (rol != Rol.MEDICO) {
            return;
        }
        if (idMedico == null || idMedico <= 0) {
            throw new IllegalArgumentException("Para el rol MEDICO debe vincular un medico registrado.");
        }
        Medico medico = medicoDAO.buscarPorId(idMedico);
        if (medico == null) {
            throw new IllegalArgumentException("No existe un medico registrado con id " + idMedico + ".");
        }
        if (medico.getEstado() != EstadoPersona.ACTIVO) {
            throw new IllegalArgumentException("No puede vincular un medico inactivo.");
        }
        if (usuarioDAO.existeMedicoVinculado(idMedico, excluirId)) {
            throw new IllegalArgumentException(
                    "El medico ya se encuentra vinculado a otro usuario con rol MEDICO.");
        }
    }
}