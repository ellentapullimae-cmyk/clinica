package clinica.modelo;

import java.util.Locale;

/**
 * Celda de un reporte exportable (PDF / Excel).
 * Distingue el tipo del valor para que Excel exporte numeros reales
 * (montos con formato de moneda e identificadores/conteos numericos)
 * en lugar de texto; en PDF se usa siempre la representacion de texto.
 */
public final class Celda {

    public enum Tipo {
        TEXTO, NUMERO, MONEDA
    }

    private final String texto;
    private final double numero;
    private final Tipo tipo;

    private Celda(String texto, double numero, Tipo tipo) {
        this.texto = texto;
        this.numero = numero;
        this.tipo = tipo;
    }

    public static Celda texto(String valor) {
        return new Celda(valor == null ? "" : valor, 0, Tipo.TEXTO);
    }

    /** Numero entero o generico (identificadores, conteos). */
    public static Celda numero(double valor) {
        return new Celda(redondear(valor), valor, Tipo.NUMERO);
    }

    /** Monto con formato de moneda (S/ x.xx) y valor numerico real. */
    public static Celda moneda(double valor) {
        return new Celda("S/ " + redondear(valor), valor, Tipo.MONEDA);
    }

    public String getTexto() {
        return texto;
    }

    public double getNumero() {
        return numero;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public boolean esNumero() {
        return tipo != Tipo.TEXTO;
    }

    private static String redondear(double valor) {
        return String.format(Locale.ROOT, "%.2f", valor);
    }
}