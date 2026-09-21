package clinica.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Cifrado de claves de usuario.
 * Las claves se protegen con SHA-256 sobre (sal + clave); la sal se
 * genera aleatoriamente y se guarda junto al hash en la base de datos.
 */
public final class UtilCifrado {

    private UtilCifrado() {
    }

    /** Genera una sal aleatoria. */
    public static String generarSal() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** Calcula el hash hexadecimal SHA-256 de (sal + clave). */
    public static String hash(String clave, String sal) {
        if (clave == null || sal == null) {
            throw new IllegalArgumentException("La clave y la sal son obligatorias.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((sal + clave).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("El algoritmo SHA-256 no esta disponible.", e);
        }
    }
}