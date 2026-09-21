package clinica.util;

import java.sql.SQLException;

/**
 * Traduce errores de base de datos a mensajes comprensibles para el
 * usuario final, en lugar de exponer mensajes tecnicos de MySQL/MariaDB.
 */
public final class UtilErrores {

    private UtilErrores() {
    }

    /**
     * Construye la excepcion de negocio a partir de una SQLException.
     * Clasifica los duplicados (SQLSTATE 23xxx) y las violaciones de
     * integridad referencial para dar mensajes claros en espanol.
     */
    public static RuntimeException errorBD(String operacion, SQLException e) {
        String estado = e.getSQLState();
        String mensaje = e.getMessage() == null ? "" : e.getMessage().toLowerCase();

        if (estado != null && estado.startsWith("23")) {
            if (mensaje.contains("duplicate")) {
                return new RuntimeException(
                        "Ya existe un registro con ese valor (por ejemplo, un DNI o un nombre de usuario repetido).",
                        e);
            }
            if (mensaje.contains("foreign key") || mensaje.contains("constraint")) {
                return new RuntimeException(
                        "No se puede completar la operacion porque el registro esta relacionado con otros datos.",
                        e);
            }
            return new RuntimeException(
                    "La operacion viola una regla de integridad de la base de datos.", e);
        }
        return new RuntimeException(
                "Error en la base de datos al " + operacion + ". Detalle: " + e.getMessage(), e);
    }
}