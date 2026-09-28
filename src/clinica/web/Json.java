package clinica.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilidades minimas de JSON (escritor y lector) sin dependencias externas.
 * Suficiente para la API propia del sistema: objetos, arreglos, textos,
 * numeros, booleanos y null.
 */
public final class Json {

    private Json() {
    }

    // ======================== ESCRITOR ========================

    /** Construye un mapa ordenado a partir de pares clave/valor (ayuda visual). */
    public static Map<String, Object> m(Object... claveValor) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i + 1 < claveValor.length; i += 2) {
            mapa.put(String.valueOf(claveValor[i]), claveValor[i + 1]);
        }
        return mapa;
    }

    /** Construye una lista (ayuda visual). */
    public static List<Object> a(Object... valores) {
        List<Object> lista = new ArrayList<>();
        for (Object v : valores) {
            lista.add(v);
        }
        return lista;
    }

    /** Serializa cualquier valor soportado a JSON. */
    public static String json(Object valor) {
        if (valor == null) {
            return "null";
        }
        if (valor instanceof String s) {
            return cita(s);
        }
        if (valor instanceof Number || valor instanceof Boolean) {
            return String.valueOf(valor);
        }
        if (valor instanceof LocalDate || valor instanceof LocalTime
                || valor instanceof LocalDateTime) {
            return cita(String.valueOf(valor));
        }
        if (valor instanceof Enum<?> e) {
            return cita(e.name());
        }
        if (valor instanceof Map<?, ?> mapa) {
            StringBuilder sb = new StringBuilder("{");
            int i = 0;
            for (Map.Entry<?, ?> en : mapa.entrySet()) {
                if (i++ > 0) {
                    sb.append(',');
                }
                sb.append(cita(String.valueOf(en.getKey()))).append(':')
                        .append(json(en.getValue()));
            }
            return sb.append('}').toString();
        }
        if (valor instanceof Iterable<?> it) {
            StringBuilder sb = new StringBuilder("[");
            int i = 0;
            for (Object v : it) {
                if (i++ > 0) {
                    sb.append(',');
                }
                sb.append(json(v));
            }
            return sb.append(']').toString();
        }
        return cita(String.valueOf(valor));
    }

    private static String cita(String texto) {
        if (texto == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    // ======================== LECTOR ========================

    private static final class Lector {
        private final String texto;
        private int pos;

        Lector(String texto) {
            this.texto = texto == null ? "" : texto;
        }

        Object valorCompleto() {
            espacios();
            Object v = valor();
            espacios();
            return v;
        }

        private Object valor() {
            if (pos >= texto.length()) {
                throw error("Fin de entrada inesperado.");
            }
            char c = texto.charAt(pos);
            return switch (c) {
                case '{' -> objeto();
                case '[' -> arreglo();
                case '"' -> cadena();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> numero();
            };
        }

        private Map<String, Object> objeto() {
            Map<String, Object> mapa = new LinkedHashMap<>();
            pos++;
            espacios();
            if (coincide('}')) {
                pos++;
                return mapa;
            }
            while (true) {
                espacios();
                if (pos >= texto.length() || texto.charAt(pos) != '"') {
                    throw error("Se esperaba una clave de objeto.");
                }
                String clave = cadena();
                espacios();
                if (!coincide(':')) {
                    throw error("Se esperaba ':'.");
                }
                pos++;
                mapa.put(clave, valor());
                espacios();
                if (coincide(',')) {
                    pos++;
                    continue;
                }
                if (coincide('}')) {
                    pos++;
                    return mapa;
                }
                throw error("Se esperaba ',' o '}'.");
            }
        }

        private List<Object> arreglo() {
            List<Object> lista = new ArrayList<>();
            pos++;
            espacios();
            if (coincide(']')) {
                pos++;
                return lista;
            }
            while (true) {
                lista.add(valor());
                espacios();
                if (coincide(',')) {
                    pos++;
                    continue;
                }
                if (coincide(']')) {
                    pos++;
                    return lista;
                }
                throw error("Se esperaba ',' o ']'.");
            }
        }

        private String cadena() {
            pos++; // comilla inicial
            StringBuilder sb = new StringBuilder();
            while (pos < texto.length()) {
                char c = texto.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (pos >= texto.length()) {
                        break;
                    }
                    char e = texto.charAt(pos++);
                    switch (e) {
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case 'r' -> sb.append('\r');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            if (pos + 4 <= texto.length()) {
                                sb.append((char) Integer.parseInt(
                                        texto.substring(pos, pos + 4), 16));
                                pos += 4;
                            }
                        }
                        default -> sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw error("Cadena sin terminar.");
        }

        private Object literal(String palabra, Object resultado) {
            if (!texto.regionMatches(pos, palabra, 0, palabra.length())) {
                throw error("Literal invalido.");
            }
            pos += palabra.length();
            return resultado;
        }

        private Object numero() {
            int inicio = pos;
            while (pos < texto.length()) {
                char c = texto.charAt(pos);
                if (c == '-' || c == '+' || c == '.' || (c >= '0' && c <= '9')
                        || c == 'e' || c == 'E') {
                    pos++;
                } else {
                    break;
                }
            }
            String token = texto.substring(inicio, pos);
            if (token.isEmpty()) {
                throw error("Se esperaba un valor.");
            }
            try {
                if (token.indexOf('.') < 0 && token.indexOf('e') < 0 && token.indexOf('E') < 0) {
                    return Long.parseLong(token);
                }
                return Double.parseDouble(token);
            } catch (NumberFormatException ex) {
                throw error("Numero invalido: " + token);
            }
        }

        private void espacios() {
            while (pos < texto.length() && Character.isWhitespace(texto.charAt(pos))) {
                pos++;
            }
        }

        private boolean coincide(char c) {
            return pos < texto.length() && texto.charAt(pos) == c;
        }

        private RuntimeException error(String mensaje) {
            return new IllegalArgumentException("JSON invalido en la posicion "
                    + pos + ": " + mensaje);
        }
    }

    /** Parsea un documento JSON y devuelve objetos Java (Map, List, String, Number, Boolean, null). */
    public static Object leer(String textoJSON) {
        return new Lector(textoJSON).valorCompleto();
    }

    /** Parsea un documento JSON a un mapa de texto (lanza si no es objeto). */
    public static Map<String, Object> leerObjeto(String textoJSON) {
        Object valor = leer(textoJSON);
        if (!(valor instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Se esperaba un objeto JSON.");
        }
        Map<String, Object> salida = new LinkedHashMap<>();
        for (Map.Entry<?, ?> en : ((Map<?, ?>) valor).entrySet()) {
            salida.put(String.valueOf(en.getKey()), en.getValue());
        }
        return salida;
    }

    // ======================== EXTRACTORES ========================

    /** Texto (trim) o null si falta. */
    public static String txt(Map<String, Object> m, String clave) {
        Object v = m.get(clave);
        return v == null ? null : String.valueOf(v).trim();
    }

    /** Numero entero; lanza si no es numerico o falta. */
    public static int entero(Map<String, Object> m, String clave) {
        Object v = m.get(clave);
        if (v instanceof Number n) {
            return n.intValue();
        }
        throw new IllegalArgumentException("Falta el campo: " + clave);
    }

    /** Numero entero o fallback si falta. */
    public static int entero(Map<String, Object> m, String clave, int porDefecto) {
        Object v = m.get(clave);
        if (v == null || "".equals(String.valueOf(v).trim())) {
            return porDefecto;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(v).trim());
    }

    /** Numero con coma decimal (acepta "1234.5" y "1234,5") o fallback. */
    public static double monto(Map<String, Object> m, String clave, double porDefecto) {
        Object v = m.get(clave);
        if (v == null || "".equals(String.valueOf(v).trim())) {
            return porDefecto;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(String.valueOf(v).trim().replace(',', '.'));
    }

    /** Booleano o fallback. */
    public static boolean booleano(Map<String, Object> m, String clave, boolean porDefecto) {
        Object v = m.get(clave);
        if (v == null) {
            return porDefecto;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(v).trim();
        return "true".equalsIgnoreCase(s) || "1".equals(s) || "si".equalsIgnoreCase(s)
                || "activo".equalsIgnoreCase(s);
    }

    /** Entero que puede ser null (para campos opcionales). */
    public static Integer enteroOpcional(Map<String, Object> m, String clave) {
        Object v = m.get(clave);
        if (v == null || "".equals(String.valueOf(v).trim())) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(v).trim());
    }
}