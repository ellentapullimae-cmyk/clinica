package clinica.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utilidades de rutas para los archivos que genera el sistema
 * (carpeta de reportes y exportaciones).
 */
public final class UtilRutas {

    private UtilRutas() {
    }

    /**
     * Devuelve la carpeta destino para los reportes exportados.
     * Prefiere ./reportes junto al directorio de trabajo; si no se puede
     * crear (por ejemplo, sin permisos de escritura), usa la carpeta
     * reportes dentro del directorio personal del usuario.
     */
    public static Path directorioReportes() {
        Path principal = Paths.get(System.getProperty("user.dir"), "reportes");
        if (intentarCrear(principal)) {
            return principal;
        }
        Path respaldo = Paths.get(System.getProperty("user.home"), "reportes");
        if (intentarCrear(respaldo)) {
            return respaldo;
        }
        throw new RuntimeException("No se pudo crear la carpeta de reportes.");
    }

    private static boolean intentarCrear(Path carpeta) {
        try {
            Files.createDirectories(carpeta);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}