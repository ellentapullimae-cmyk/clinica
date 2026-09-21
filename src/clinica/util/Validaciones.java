package clinica.util;

import java.time.LocalDate;

/**
 * Validaciones comunes reutilizadas por las capas de servicio.
 */
public final class Validaciones {

    private Validaciones() {
    }

    /** Valida que un rango de fechas sea valido (inicio <= fin). */
    public static void validarRangoFechas(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("Debe indicar la fecha de inicio y de fin.");
        }
        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin.");
        }
    }

    /** Valida que un monto sea un numero real mayor que cero. */
    public static boolean esMontoValido(double monto) {
        return Double.isFinite(monto) && monto > 0;
    }

    public static boolean esTextoValido(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    public static boolean esDniValido(String dni) {
        return esTextoValido(dni) && dni.trim().matches("\\d{8}");
    }

    public static boolean esTelefonoValido(String telefono) {
        if (!esTextoValido(telefono)) {
            return true;
        }
        return telefono.trim().matches("\\d{7,15}");
    }

    public static boolean esCorreoValido(String correo) {
        if (!esTextoValido(correo)) {
            return true;
        }
        return correo.trim().matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    public static String normalizar(String valor) {
        if (!esTextoValido(valor)) {
            return null;
        }
        return valor.trim();
    }
}