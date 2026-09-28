package clinica.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

/**
 * Servidor HTTP embebido del Sistema de Gestion para una Clinica.
 * Sirve la interfaz web (carpeta web/) y expone la API JSON. La terminal solo
 * se usa para poner en marcha el servidor.
 */
public final class ServidorWeb {

    private static final int PUERTO_POR_DEFECTO = 8080;

    private ServidorWeb() {
    }

    public static int puerto() {
        String env = System.getProperty("clinicaport",
                System.getenv().getOrDefault("CLINICA_PORT", null));
        if (env == null || env.trim().isEmpty()) {
            return PUERTO_POR_DEFECTO;
        }
        try {
            return Integer.parseInt(env.trim());
        } catch (NumberFormatException e) {
            System.out.println("Puerto invalido en clinicaport/CLINICA_PORT; usando "
                    + PUERTO_POR_DEFECTO + ".");
            return PUERTO_POR_DEFECTO;
        }
    }

    /** Localiza la carpeta web (web/) relativa al directorio de trabajo. */
    public static Path carpetaWeb() {
        Path base = Paths.get(System.getProperty("user.dir", "."));
        Path web = base.resolve("web");
        if (Files.isDirectory(web)) {
            return web;
        }
        // Respaldo: junto a las clases compiladas (si se ejecuta desde bin).
        Path junto = Paths.get(System.getProperty("user.dir", "."), "..", "web");
        if (Files.isDirectory(junto)) {
            return junto;
        }
        return web;
    }

    /** Inicia el servidor y lo deja aceptando peticiones. */
    public static HttpServer iniciar() throws IOException {
        int puerto = puerto();
        Path web = carpetaWeb();
        if (!Files.isDirectory(web)) {
            throw new IllegalStateException("No se encontro la carpeta web en " + web);
        }

        HttpServer servidor = HttpServer.create(new InetSocketAddress("0.0.0.0", puerto), 0);
        servidor.createContext("/api", new ApiHandler());
        servidor.createContext("/", new EstaticosHandler(web));
        servidor.setExecutor(null);
        servidor.start();

        String url = "http://localhost:" + puerto + "/";
        System.out.println("┌─────────────────────────────────────────────────────┐");
        System.out.println("│  SISTEMA DE GESTION PARA UNA CLINICA  (version web)  │");
        System.out.println("├─────────────────────────────────────────────────────┤");
        System.out.println("│  Servidor iniciado.                                  │");
        System.out.println("│  Abre esta direccion en tu navegador:                │");
        System.out.println("│       " + url);
        System.out.println("│  Para detener el servidor cierra esta ventana.       │");
        System.out.println("└─────────────────────────────────────────────────────┘");
        abrirNavegador(url);
        return servidor;
    }

    private static void abrirNavegador(String url) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (IOException | RuntimeException exIgnorado) {
            // si no se puede abrir el navegador, la URL ya se mostro en consola.
        }
    }

    /** Sirve los archivos estaticos de la interfaz web. */
    private static final class EstaticosHandler implements HttpHandler {
        private final Path raiz;

        EstaticosHandler(Path raiz) {
            this.raiz = raiz;
        }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            String ruta = ex.getRequestURI().getPath();
            if (ruta == null || "/".equals(ruta) || "/index.html".equals(ruta)) {
                ruta = "index.html";
            } else {
                ruta = ruta.replaceFirst("^/", "");
            }
            Path archivo = raiz.resolve(ruta).normalize();
            if (!archivo.startsWith(raiz.toAbsolutePath().normalize())
                    || !Files.isRegularFile(archivo)) {
                byte[] noEncontrado = "404 - Recurso no encontrado".getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                ex.sendResponseHeaders(404, noEncontrado.length);
                try (OutputStream out = ex.getResponseBody()) {
                    out.write(noEncontrado);
                }
                return;
            }
            byte[] bytes = Files.readAllBytes(archivo);
            ex.getResponseHeaders().set("Content-Type", tipoContenido(archivo.getFileName().toString()));
            if ("app.js".equals(archivo.getFileName().toString())
                    || "style.css".equals(archivo.getFileName().toString())) {
                ex.getResponseHeaders().set("Cache-Control", "no-cache");
            }
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = ex.getResponseBody()) {
                out.write(bytes);
            }
        }

        private String tipoContenido(String nombre) {
            String ext = nombre.contains(".")
                    ? nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
                    : "";
            return switch (ext) {
                case "html" -> "text/html; charset=utf-8";
                case "css" -> "text/css; charset=utf-8";
                case "js" -> "application/javascript; charset=utf-8";
                case "json" -> "application/json; charset=utf-8";
                case "svg" -> "image/svg+xml";
                case "png" -> "image/png";
                case "jpg", "jpeg" -> "image/jpeg";
                case "gif" -> "image/gif";
                case "ico" -> "image/x-icon";
                case "woff" -> "font/woff";
                case "woff2" -> "font/woff2";
                default -> "application/octet-stream";
            };
        }
    }
}