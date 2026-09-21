package clinica.presentacion;

import java.util.List;
import java.util.Locale;

/**
 * Utilidades de presentacion de la interfaz de consola.
 * Dibuja banners, encabezados, menus en recuadros (compatibles con
 * consolas EBCDIC/codepages OEM) y aplica colores ANSI cuando la
 * terminal lo soporta (con deteccion automatica y degradacion segura).
 */
public final class Interfaz {

    private static final boolean COLOR = soportaColor();

    private static final String RESET = "\u001B[0m";
    private static final String NEGRITA = "\u001B[1m";
    private static final String ROJO = "\u001B[31m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String AZUL = "\u001B[34m";
    private static final String CIAN = "\u001B[36m";
    private static final String BLANCO = "\u001B[37m";

    private Interfaz() {
    }

    // ======================= COLORES =======================

    private static boolean soportaColor() {
        String propiedad = System.getProperty("clinica.color", "auto");
        if ("off".equalsIgnoreCase(propiedad)) {
            return false;
        }
        if ("on".equalsIgnoreCase(propiedad)) {
            return true;
        }
        if (System.console() == null) {
            return false;
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            String wt = System.getenv("WT_SESSION");
            String term = System.getenv("TERM_PROGRAM");
            String conemu = System.getenv("ConEmuANSI");
            return (wt != null && !wt.isEmpty())
                    || "vscode".equalsIgnoreCase(term)
                    || "ON".equalsIgnoreCase(conemu);
        }
        return true;
    }

    private static String color(String texto, String codigo) {
        return COLOR ? codigo + texto + RESET : texto;
    }

    public static boolean usaColores() {
        return COLOR;
    }

    /** Mensaje de exito (verde). */
    public static void exito(String mensaje) {
        System.out.println(color(mensaje, VERDE));
    }

    /** Mensaje informativo (cian). */
    public static void informacion(String mensaje) {
        System.out.println(color(mensaje, CIAN));
    }

    /** Mensaje de aviso (amarillo). */
    public static void aviso(String mensaje) {
        System.out.println(color(mensaje, AMARILLO));
    }

    /** Mensaje de error (rojo). */
    public static void error(String mensaje) {
        System.out.println(color(mensaje, ROJO));
    }

    /** Resalta un texto (negrita). */
    public static String resaltar(String texto) {
        return COLOR ? NEGRITA + texto + RESET : texto;
    }

    // ======================= DIBUJO DE RECUADROS =======================

    /** Banner de bienvenida con recuadro doble. */
    public static void banner(String titulo, String subtitulo) {
        int ancho = Math.max(titulo.length(), subtitulo == null ? 0 : subtitulo.length()) + 8;
        ancho = Math.max(ancho, 46);
        System.out.println();
        linea('╔', '═', '╗', ancho);
        lineaTexto(centrar(titulo, ancho), ancho);
        if (subtitulo != null && !subtitulo.trim().isEmpty()) {
            lineaTexto(centrar(subtitulo, ancho), ancho);
        }
        linea('╚', '═', '╝', ancho);
    }

    /** Encabezado de un modulo (recuadro sencillo). */
    public static void encabezado(String titulo) {
        int ancho = Math.max(titulo.length() + 4, 40);
        linea('┌', '─', '┐', ancho);
        lineaTexto(color(centrar(titulo, ancho), AZUL), ancho);
        linea('└', '─', '┘', ancho);
    }

    /** Menu dentro de un recuadro con titulo. */
    public static void menu(String titulo, List<String> opciones) {
        int ancho = titulo.length() + 4;
        for (String opcion : opciones) {
            ancho = Math.max(ancho, opcion.length() + 4);
        }
        linea('┌', '─', '┐', ancho);
        lineaTexto(color(centrar(titulo, ancho), BLANCO), ancho);
        linea('├', '─', '┤', ancho);
        for (String opcion : opciones) {
            lineaTexto(" " + opcion, ancho);
        }
        linea('└', '─', '┘', ancho);
    }

    /** Linea separadora simple. */
    public static void separador() {
        System.out.println(color(repetir("─", 58), CIAN));
    }

    private static void linea(char izq, char horizontal, char der, int ancho) {
        System.out.println(izq + repetir(String.valueOf(horizontal), ancho - 2) + der);
    }

    private static void lineaTexto(String texto, int ancho) {
        StringBuilder sb = new StringBuilder("│");
        sb.append(texto);
        while (sb.length() < ancho - 1) {
            sb.append(' ');
        }
        sb.append('│');
        System.out.println(sb);
    }

    private static String centrar(String texto, int ancho) {
        int lado = Math.max((ancho - texto.length()) / 2, 0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lado; i++) {
            sb.append(' ');
        }
        sb.append(texto);
        while (sb.length() < ancho) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String repetir(String texto, int veces) {
        if (veces <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(veces * texto.length());
        for (int i = 0; i < veces; i++) {
            sb.append(texto);
        }
        return sb.toString();
    }
}