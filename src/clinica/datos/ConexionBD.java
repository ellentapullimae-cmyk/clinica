package clinica.datos;

import clinica.config.ConfiguracionBD;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Conexion unica (singleton) a la base de datos MySQL/MariaDB "clinica".
 *
 * La configuracion se obtiene de forma centralizada mediante
 * ConfiguracionBD (archivo db.properties). La conexion es autocorregible:
 * si el servidor la cerro (por ejemplo, por wait_timeout o un corte de red),
 * se restablece automaticamente en la siguiente operacion. La lectura y
 * escritura son seguras para el flujo secuencial de la consola.
 */
public class ConexionBD {

    private static ConexionBD instancia;

    private volatile Connection conexion;

    private ConexionBD() {
    }

    public static ConexionBD obtenerInstancia() {
        if (instancia == null) {
            instancia = new ConexionBD();
        }
        return instancia;
    }

    /** Devuelve la conexion, restableciendola si fue cerrada o quedo invalida. */
    public synchronized Connection getConexion() {
        if (!valida(conexion)) {
            cerrar();
            conexion = crear();
        }
        return conexion;
    }

    private static boolean valida(Connection conexion) {
        if (conexion == null) {
            return false;
        }
        try {
            return !conexion.isClosed() && conexion.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    private static void cerrar() {
        Connection anterior = instancia == null ? null : instancia.conexion;
        if (anterior != null) {
            try {
                anterior.close();
            } catch (SQLException e) {
                // No se puede cerrar la conexion obsoleta; se ignora.
            }
        }
    }

    private Connection crear() {
        try {
            return DriverManager.getConnection(
                    ConfiguracionBD.getUrl(),
                    ConfiguracionBD.getUsuario(),
                    ConfiguracionBD.getClave());
        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo conectar a MySQL. Verifique que el servicio de base de datos este activo "
                            + "y que el archivo db.properties tenga los datos correctos. Detalle: " + e.getMessage(),
                    e);
        }
    }
}