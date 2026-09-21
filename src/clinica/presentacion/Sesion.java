package clinica.presentacion;

import clinica.modelo.Rol;
import clinica.modelo.Usuario;

/**
 * Contexto de sesion activa del usuario autenticado.
 */
public final class Sesion {

    private static Usuario usuarioActual;

    private Sesion() {
    }

    public static void iniciar(Usuario usuario) {
        usuarioActual = usuario;
    }

    public static void cerrar() {
        usuarioActual = null;
    }

    public static Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public static Rol getRolActual() {
        return usuarioActual == null ? Rol.ADMINISTRADOR : usuarioActual.getRol();
    }

    public static String getNombreUsuario() {
        return usuarioActual == null ? "" : usuarioActual.getNombreUsuario();
    }

    public static String getNombreCompleto() {
        return usuarioActual == null ? "" : usuarioActual.getNombreCompleto();
    }

    public static Integer getIdMedicoSesion() {
        return usuarioActual == null ? null : usuarioActual.getIdMedico();
    }

    public static int getIdUsuarioSesion() {
        return usuarioActual == null ? -1 : usuarioActual.getIdUsuario();
    }
}