package clinica.web;

import clinica.modelo.Usuario;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sesiones web en memoria: devuelve un token aleatorio por usuario
 * autenticado y permite resolverlo a {@link Usuario} en cada peticion.
 * Reutiliza la capa de autenticacion existente (UsuarioController); solo
 * gestiona el transporte de la identidad en la API.
 */
public final class SesionWeb {

    private static final Map<String, Usuario> SESIONES = new ConcurrentHashMap<>();
    private static final SecureRandom AZAR = new SecureRandom();

    private SesionWeb() {
    }

    /** Crea una sesion para el usuario y devuelve su token. */
    public static String crear(Usuario usuario) {
        byte[] extra = new byte[8];
        AZAR.nextBytes(extra);
        String token = UUID.randomUUID().toString().replace("-", "")
                + Base64.getUrlEncoder().withoutPadding().encodeToString(extra);
        SESIONES.put(token, usuario);
        return token;
    }

    /** Devuelve el usuario de la sesion o null si el token no existe. */
    public static Usuario obtener(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return SESIONES.get(token);
    }

    /** Cierra la sesion. */
    public static void cerrar(String token) {
        if (token != null) {
            SESIONES.remove(token);
        }
    }
}