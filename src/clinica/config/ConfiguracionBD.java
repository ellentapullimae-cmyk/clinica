package clinica.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuracion centralizada de la conexion a MySQL.
 * Los valores se leen del archivo db.properties (raiz del proyecto).
 * Si el archivo no existe se usan los valores por defecto.
 */
public class ConfiguracionBD {

    private static final String ARCHIVO_PROPIEDADES = "db.properties";
    private static final String URL_DEFECTO =
            "jdbc:mysql://localhost:3306/clinica?useSSL=false&serverTimezone=UTC"
                    + "&characterEncoding=utf8&connectTimeout=5000&socketTimeout=60000";
    private static final String USUARIO_DEFECTO = "root";
    private static final String CLAVE_DEFECTO = "";

    private static final Properties PROPIEDADES = new Properties();

    static {
        try (InputStream entrada = ConfiguracionBD.class
                .getClassLoader().getResourceAsStream(ARCHIVO_PROPIEDADES)) {
            if (entrada != null) {
                PROPIEDADES.load(entrada);
            }
        } catch (IOException e) {
            // Se usan los valores por defecto.
        }
    }

    private ConfiguracionBD() {
    }

    public static String getUrl() {
        return PROPIEDADES.getProperty("db.url", URL_DEFECTO);
    }

    public static String getUsuario() {
        String usuario = PROPIEDADES.getProperty("db.usuario", USUARIO_DEFECTO);
        return usuario == null ? USUARIO_DEFECTO : usuario.trim();
    }

    public static String getClave() {
        return PROPIEDADES.getProperty("db.clave", CLAVE_DEFECTO);
    }
}