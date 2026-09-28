package clinica.web;

import clinica.controlador.AtencionController;
import clinica.controlador.CitaController;
import clinica.controlador.DashboardController;
import clinica.controlador.GastoController;
import clinica.controlador.MedicoController;
import clinica.controlador.PacienteController;
import clinica.controlador.PagoController;
import clinica.controlador.ReporteController;
import clinica.controlador.UsuarioController;
import clinica.modelo.Atencion;
import clinica.modelo.CategoriaGasto;
import clinica.modelo.Cita;
import clinica.modelo.Estadisticas;
import clinica.modelo.EstadoCita;
import clinica.modelo.EstadoGasto;
import clinica.modelo.EstadoPersona;
import clinica.modelo.FormatoExportacion;
import clinica.modelo.Gasto;
import clinica.modelo.Medico;
import clinica.modelo.MetodoPago;
import clinica.modelo.Paciente;
import clinica.modelo.Pago;
import clinica.modelo.Reporte;
import clinica.modelo.ResumenContable;
import clinica.modelo.Rol;
import clinica.modelo.Usuario;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.nio.file.Paths;
import java.util.Base64;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API HTTP JSON del sistema. Reutiliza por completo los controladores
 * (controlador -> servicio -> DAO) y la autenticacion existente por rol;
 * solo expone esa logica como peticiones web y valida los permisos igual que
 * el menu de consola (y con las mismas restricciones por rol).
 */
public final class ApiHandler implements HttpHandler {

    private static final String UTF8 = "utf-8";

    private final PacienteController pacientes;
    private final MedicoController medicos;
    private final CitaController citas;
    private final AtencionController atenciones;
    private final PagoController pagos;
    private final GastoController gastos;
    private final ReporteController reportes;
    private final UsuarioController usuarios;
    private final DashboardController dashboard;

    public ApiHandler() {
        this.pacientes = new PacienteController();
        this.medicos = new MedicoController();
        this.citas = new CitaController();
        this.atenciones = new AtencionController();
        this.pagos = new PagoController();
        this.gastos = new GastoController();
        this.reportes = new ReporteController();
        this.usuarios = new UsuarioController();
        this.dashboard = new DashboardController();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            enrutar(ex);
        } catch (Throwable t) {
            t.printStackTrace();
            try {
                error(ex, 500, "Error interno del servidor: " + t.getMessage());
            } catch (IOException inn) {
                // si ya se envio la respuesta, se ignora.
            }
        } finally {
            ex.close();
        }
    }

    // ======================== ENRUTADOR ========================

    private void enrutar(HttpExchange ex) throws IOException {
        String ruta = ex.getRequestURI().getPath();
        String metodo = ex.getRequestMethod().toUpperCase();

        if (!ruta.startsWith("/api/") && !"/api".equals(ruta)) {
            noEncontrado(ex, "Recurso no encontrado.");
            return;
        }
        if (ruta.length() <= 4) {
            noEncontrado(ex, "Recurso no encontrado.");
            return;
        }
        String[] seg = ruta.substring("/api/".length()).split("/");
        String recurso = seg[0].toLowerCase();

        if ("login".equals(recurso)) {
            if (!"POST".equals(metodo)) {
                metodoNoPermitido(ex);
                return;
            }
            login(ex);
            return;
        }
        if ("logout".equals(recurso)) {
            if (!"POST".equals(metodo)) {
                metodoNoPermitido(ex);
                return;
            }
            logout(ex);
            return;
        }
        if ("exportar".equals(recurso)) {
            exportar(ex, seg);
            return;
        }

        Usuario usuario = autorizar(ex);
        if (usuario == null) {
            return;
        }

        try {
            switch (recurso) {
                case "me" -> me(ex, usuario);
                case "catalogos" -> catalogos(ex, usuario);
                case "dashboard" -> dashboard(ex, usuario);
                case "pacientes" -> pacientes(ex, usuario, seg, metodo);
                case "medicos" -> medicos(ex, usuario, seg, metodo);
                case "citas" -> citas(ex, usuario, seg, metodo);
                case "atenciones" -> atenciones(ex, usuario, seg, metodo);
                case "pagos" -> pagos(ex, usuario, seg, metodo);
                case "gastos" -> gastos(ex, usuario, seg, metodo);
                case "presupuesto" -> presupuesto(ex, usuario, metodo);
                case "reportes" -> reportes(ex, usuario, seg, metodo);
                case "usuarios" -> usuarios(ex, usuario, seg, metodo);
                case "cambiar-clave" -> cambiarMiClave(ex, usuario, metodo);
                case "foto" -> foto(ex, usuario, seg, metodo);
                default -> noEncontrado(ex, "Recurso no encontrado.");
            }
        } catch (IllegalArgumentException exBad) {
            error(ex, 400, exBad.getMessage());
        } catch (IllegalStateException exDen) {
            error(ex, 403, exDen.getMessage());
        }
    }

    // ======================== AUTENTICACION ========================

    private void login(HttpExchange ex) throws IOException {
        Map<String, Object> cuerpo = cuerpoJson(ex);
        String nombreUsuario = Json.txt(cuerpo, "usuario");
        String clave = Json.txt(cuerpo, "clave");
        if (nombreUsuario == null || nombreUsuario.isEmpty()
                || clave == null || clave.isEmpty()) {
            error(ex, 400, "Ingrese el usuario y la clave.");
            return;
        }
        try {
            Usuario u = usuarios.autenticar(nombreUsuario, clave);
            String token = SesionWeb.crear(u);
            enviar(ex, 200, Json.json(Json.m(
                    "token", token,
                    "usuario", usuarioPublico(u))));
        } catch (RuntimeException e) {
            error(ex, 401, e.getMessage());
        }
    }

    private void logout(HttpExchange ex) throws IOException {
        SesionWeb.cerrar(tokenDe(ex));
        enviar(ex, 200, Json.json(Json.m("ok", true)));
    }

    private Usuario autorizar(HttpExchange ex) throws IOException {
        Usuario u = SesionWeb.obtener(tokenDe(ex));
        if (u == null) {
            error(ex, 401, "Sesion no valida o expirada. Inicie sesion nuevamente.");
        }
        return u;
    }

    private void me(HttpExchange ex, Usuario u) throws IOException {
        enviar(ex, 200, Json.json(Json.m("usuario", usuarioPublico(u),
                "permisos", permisos(u))));
    }

    // ======================== FOTO DE PERFIL ========================

    private void foto(HttpExchange ex, Usuario u, String[] seg, String metodo) throws IOException {
        if ("GET".equals(metodo)) {
            servirFoto(ex, u, seg);
            return;
        }
        if (!"PUT".equals(metodo)) {
            metodoNoPermitido(ex);
            return;
        }
        Map<String, Object> cuerpo = cuerpoJson(ex);
        String datos = Json.txt(cuerpo, "imagen");
        if (datos == null || datos.trim().isEmpty()) {
            datos = Json.txt(cuerpo, "foto");
        }
        if (datos == null || datos.trim().isEmpty()) {
            error(ex, 400, "Debe seleccionar una imagen.");
            return;
        }
        byte[] imagen = decodificarImagen(datos);
        String ext = extensionDe(datos);
        if (!estaticoValido(imagen)) {
            error(ex, 400, "La imagen debe ser JPEG, PNG o WEBP de hasta 1 MB.");
            return;
        }
        Path destino = rutaFoto(u.getIdUsuario(), ext);
        Files.createDirectories(destino.getParent());
        Files.write(destino, imagen);
        // Se borran las fotos con otra extension para que el usuario tenga
        // siempre una sola imagen: si no, quedaria sirviendo la anterior.
        for (String otra : new String[] { "jpg", "png", "webp" }) {
            if (!otra.equals(ext)) {
                try {
                    Files.deleteIfExists(rutaFoto(u.getIdUsuario(), otra));
                } catch (IOException e) {
                    // Si no se puede borrar, la nueva imagen sigue siendo la valida.
                }
            }
        }
        Usuario actualizado = new Usuario();
        actualizado.setIdUsuario(u.getIdUsuario());
        actualizado.setNombreUsuario(u.getNombreUsuario());
        actualizado.setNombreCompleto(u.getNombreCompleto());
        actualizado.setRol(u.getRol());
        actualizado.setIdMedico(u.getIdMedico());
        actualizado.setActivo(u.isActivo());
        enviar(ex, 200, Json.json(Json.m("ok", true, "foto", "/api/foto/" + u.getIdUsuario())));
    }

    private void servirFoto(HttpExchange ex, Usuario u, String[] seg) throws IOException {
        if (seg.length < 2) {
            error(ex, 404, "Foto no encontrada.");
            return;
        }
        int idUsuario;
        try {
            idUsuario = Integer.parseInt(seg[1]);
        } catch (NumberFormatException e) {
            error(ex, 400, "Identificador de usuario invalido.");
            return;
        }
        if (!permisoAdministrador(u) && !seg[1].equals(String.valueOf(u.getIdUsuario()))) {
            error(ex, 403, "No tiene permiso para ver esta foto.");
            return;
        }
        byte[] contenido = leerFoto(idUsuario);
        if (contenido == null) {
            error(ex, 404, "Foto no encontrada.");
            return;
        }
        String nombre = nombreFoto(idUsuario);
        if (nombre == null) {
            error(ex, 404, "Foto no encontrada.");
            return;
        }
        enviarBinario(ex, 200, tipoImagen(nombre), contenido, true);
    }

    private static String tipoImagen(String extension) {
        String n = extension.toLowerCase();
        if (n.startsWith(".")) {
            n = n.substring(1);
        }
        if (n.equals("png")) {
            return "image/png";
        }
        if (n.equals("webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private static boolean permisoAdministrador(Usuario u) {
        return Rol.ADMINISTRADOR == u.getRol();
    }

    private static byte[] decodificarImagen(String datos) {
        int coma = datos.indexOf(',');
        String base = coma > 0 ? datos.substring(coma + 1) : datos;
        try {
            return Base64.getMimeDecoder().decode(base);
        } catch (IllegalArgumentException e) {
            return new byte[0];
        }
    }

    private static String extensionDe(String datos) {
        if (datos == null) {
            return "";
        }
        String prefijo = datos.substring(0, Math.min(32, datos.length())).toLowerCase();
        if (prefijo.contains("png")) {
            return "png";
        }
        if (prefijo.contains("webp")) {
            return "webp";
        }
        return "jpg";
    }

    private static boolean estaticoValido(byte[] imagen) {
        if (imagen == null || imagen.length == 0 || imagen.length > 1024 * 1024) {
            return false;
        }
        if (imagen.length >= 4 && (imagen[0] & 0xFF) == 0xFF && (imagen[1] & 0xFF) == 0xD8
                && (imagen[2] & 0xFF) == 0xFF) {
            return true; // JPEG
        }
        if (imagen.length >= 8 && (imagen[0] & 0xFF) == 0x89 && imagen[1] == 'P' && imagen[2] == 'N'
                && imagen[3] == 'G') {
            return true; // PNG
        }
        if (imagen.length >= 12 && imagen[0] == 'R' && imagen[1] == 'I' && imagen[2] == 'F'
                && imagen[3] == 'F' && imagen[8] == 'W' && imagen[9] == 'E' && imagen[10] == 'B'
                && imagen[11] == 'P') {
            return true; // WEBP
        }
        return false;
    }

    private static Path rutaFoto(int idUsuario, String ext) {
        return ServidorWeb.carpetaWeb().resolve("fotos")
                .resolve(idUsuario + "." + ext);
    }

    private static byte[] leerFoto(int idUsuario) {
        String nombre = nombreFoto(idUsuario);
        if (nombre == null) {
            return null;
        }
        try {
            return Files.readAllBytes(rutaFoto(idUsuario, nombre));
        } catch (IOException e) {
            return null;
        }
    }

    private static String nombreFoto(int idUsuario) {
        for (String ext : new String[] { "jpg", "png", "webp" }) {
            if (Files.exists(rutaFoto(idUsuario, ext))) {
                return ext;
            }
        }
        return null;
    }

    private static void enviarBinario(HttpExchange ex, int codigo, String tipo,
            byte[] contenido, boolean publica) throws IOException {
        if (publica) {
            ex.getResponseHeaders().set("Cache-Control", "public, max-age=31536000");
        }
        ex.getResponseHeaders().set("Content-Type", tipo);
        ex.sendResponseHeaders(codigo, contenido.length);
        try (OutputStream salida = ex.getResponseBody()) {
            salida.write(contenido);
        }
    }

    private static String tokenDe(HttpExchange ex) {
        String enc = ex.getRequestHeaders().getFirst("Authorization");
        if (enc != null && enc.startsWith("Bearer ")) {
            return enc.substring(7).trim();
        }
        return null;
    }

    private static Map<String, Object> usuarioPublico(Usuario u) {
        return Json.m(
                "id", u.getIdUsuario(),
                "usuario", u.getNombreUsuario(),
                "nombre", u.getNombreCompleto(),
                "rol", u.getRol().name(),
                "rolNombre", nombreRol(u.getRol()),
                "idMedico", u.getIdMedico(),
                "activo", u.isActivo(),
                "foto", nombreFoto(u.getIdUsuario()) == null
                        ? null : "/api/foto/" + u.getIdUsuario());
    }

    private static String nombreRol(Rol r) {
        return switch (r) {
            case ADMINISTRADOR -> "Administrador";
            case RECEPCIONISTA -> "Recepcionista";
            case MEDICO -> "Medico";
        };
    }

    /** Mapa de permisos que replica las reglas del menu de consola. */
    private static Map<String, Object> permisos(Usuario u) {
        Rol r = u.getRol();
        return Json.m(
                "gestionarUsuarios", r.gestionarUsuarios(),
                "gestionarMedicos", r.gestionarMedicos(),
                "registrarPaciente", r.registrarPaciente(),
                "actualizarPaciente", r.actualizarPaciente(),
                "programarCita", r.programarCita(),
                "modificarCita", r.modificarCita(),
                "registrarAtencion", r.registrarAtencion(),
                "gestionarPagos", r.gestionarPagos(),
                "gestionarGastos", r.gestionarGastos(),
                "gestionarPresupuesto", r.gestionarPresupuesto(),
                "esMedico", r.esMedico());
    }

    private static void exigir(boolean permitido, String funcion) {
        if (!permitido) {
            throw new IllegalStateException(
                    "Acceso denegado: su rol no tiene permiso para " + funcion + ".");
        }
    }

    /**
     * Si la sesion es de un medico, la cita debe pertenecer a ese medico
     * (mismo criterio que el sistema de consola 'exigirCitaDeSesion').
     */
    private Cita exigirCitaDeSesion(Usuario u, int idCita) {
        if (u.getRol().esMedico() && u.getIdMedico() != null) {
            boolean propia = false;
            for (Cita c : citas.listarPorMedico(u.getIdMedico())) {
                if (c.getIdCita() == idCita) {
                    propia = true;
                    break;
                }
            }
            if (!propia) {
                throw new IllegalArgumentException(
                        "La cita debe pertenecer al medico en sesion.");
            }
        }
        return citas.buscarPorId(idCita);
    }

    // ======================== CATALOGOS ========================

    private void catalogos(HttpExchange ex, Usuario u) throws IOException {
        List<Object> estadosCita = new ArrayList<>();
        for (EstadoCita e : EstadoCita.values()) {
            estadosCita.add(Json.m("nombre", e.name(), "etiqueta", e.name()));
        }
        List<Object> metodos = new ArrayList<>();
        for (MetodoPago m : MetodoPago.values()) {
            metodos.add(Json.m("nombre", m.name(), "etiqueta", m.name()));
        }
        List<Object> categorias = new ArrayList<>();
        for (CategoriaGasto c : CategoriaGasto.values()) {
            categorias.add(Json.m("nombre", c.name(), "etiqueta", c.name()));
        }
        List<Object> estadosPersona = new ArrayList<>();
        for (EstadoPersona e : EstadoPersona.values()) {
            estadosPersona.add(Json.m("nombre", e.name(), "etiqueta", e.name()));
        }
        List<Object> roles = new ArrayList<>();
        for (Rol r : Rol.values()) {
            roles.add(Json.m("nombre", r.name(), "etiqueta", nombreRol(r)));
        }
        enviar(ex, 200, Json.json(Json.m(
                "estadosCita", estadosCita,
                "metodos", metodos,
                "categorias", categorias,
                "estadosPersona", estadosPersona,
                "roles", roles)));
    }

    // ======================== DASHBOARD ========================

    private void dashboard(HttpExchange ex, Usuario u) throws IOException {
        Estadisticas e = dashboard.obtenerEstadisticas();

        // Citas del dia y proximas (por rol medico).
        List<Cita> citasLista = u.getRol().esMedico() && u.getIdMedico() != null
                ? citas.listarPorMedico(u.getIdMedico())
                : citas.listar();
        LocalDate hoy = LocalDate.now();
        List<Cita> proximas = new ArrayList<>();
        for (Cita c : citasLista) {
            if ((c.getEstado() == EstadoCita.PROGRAMADA
                    || c.getEstado() == EstadoCita.CONFIRMADA)
                    && !c.getFecha().isBefore(hoy)) {
                proximas.add(c);
            }
        }
        proximas.sort(Comparator.comparing(Cita::getFecha)
                .thenComparing(Cita::getHora, Comparator.nullsLast(LocalTime::compareTo)));
        if (proximas.size() > 8) {
            proximas = new ArrayList<>(proximas.subList(0, 8));
        }

        List<Object> proximasJson = new ArrayList<>();
        for (Cita c : proximas) {
            proximasJson.add(citaResumen(c));
        }

        List<Atencion> atencionesLista = atenciones.listar();
        List<Object> actividad = new ArrayList<>();
        int limite = Math.min(6, atencionesLista.size());
        for (int i = 0; i < limite; i++) {
            actividad.add(atencionResumen(atencionesLista.get(i)));
        }

        Map<Object, Object> citasPorEstado = new LinkedHashMap<>();
        if (e.getCitasPorEstado() != null) {
            for (Map.Entry<EstadoCita, Integer> en : e.getCitasPorEstado().entrySet()) {
                citasPorEstado.put(en.getKey().name(), en.getValue());
            }
        }

        ResumenContable res = reportes.obtenerResumen();
        enviar(ex, 200, Json.json(Json.m(
                "totalPacientes", e.getTotalPacientes(),
                "totalMedicos", e.getTotalMedicos(),
                "totalCitas", e.getTotalCitas(),
                "citasPorEstado", citasPorEstado,
                "totalAtenciones", e.getTotalAtenciones(),
                "pagosRegistrados", pagos.listar().size(),
                "resumen", resumenJson(res),
                "proximas", proximasJson,
                "actividad", actividad)));
    }

    private static Map<String, Object> resumenJson(ResumenContable r) {
        return Json.m(
                "ingresos", r.getIngresos(),
                "gastos", r.getGastos(),
                "presupuesto", r.getPresupuesto(),
                "saldo", r.getSaldo(),
                "saldoPresupuestal", r.getSaldoPresupuestal(),
                "excedePresupuesto", r.isExcedePresupuesto());
    }

    private static Map<String, Object> citaResumen(Cita c) {
        return Json.m(
                "id", c.getIdCita(),
                "fecha", c.getFecha() != null ? c.getFecha().toString() : null,
                "hora", c.getHora() != null ? c.getHora().toString() : null,
                "estado", c.getEstado().name(),
                "paciente", c.getNombrePaciente(),
                "medico", c.getNombreMedico(),
                "especialidad", c.getMedico() != null ? c.getMedico().getEspecialidad() : "",
                "observacion", c.getObservacion());
    }

    private static Map<String, Object> atencionResumen(Atencion a) {
        return Json.m(
                "id", a.getIdAtencion(),
                "fecha", a.getFechaAtencion() != null ? a.getFechaAtencion().toString() : null,
                "paciente", a.getNombrePaciente(),
                "medico", a.getNombreMedico(),
                "diagnostico", a.getDiagnostico());
    }

    // ======================== PACIENTES ========================

    private void pacientes(HttpExchange ex, Usuario u, String[] seg, String metodo)
            throws IOException {
        if (seg.length == 1) {
            if ("GET".equals(metodo)) {
                String dni = parametro(ex, "dni");
                if (dni != null && !dni.trim().isEmpty()) {
                    Paciente p = pacientes.buscarPorDni(dni.trim());
                    if (p == null) {
                        enviar(ex, 200, Json.json(Json.m("datos", new ArrayList<>())));
                        return;
                    }
                    enviar(ex, 200, Json.json(Json.m("datos", Json.a(pacienteJson(p)))));
                } else {
                    List<Object> filas = new ArrayList<>();
                    for (Paciente p : pacientes.listar()) {
                        filas.add(pacienteJson(p));
                    }
                    enviar(ex, 200, Json.json(Json.m("datos", filas)));
                }
                return;
            }
            if ("POST".equals(metodo)) {
                exigir(u.getRol().registrarPaciente(), "registrar pacientes");
                Map<String, Object> c = cuerpoJson(ex);
                Paciente p = pacientes.registrar(
                        Json.txt(c, "nombre"),
                        Json.txt(c, "dni"),
                        Json.txt(c, "telefono"),
                        Json.txt(c, "correo"),
                        Json.txt(c, "direccion"));
                enviar(ex, 201, Json.json(pacienteJson(p)));
                return;
            }
            metodoNoPermitido(ex);
            return;
        }
        int id = idDe(seg[1]);
        if (seg.length == 2) {
            if ("GET".equals(metodo)) {
                enviar(ex, 200, Json.json(pacienteJson(pacientes.buscarPorId(id))));
                return;
            }
            if ("PUT".equals(metodo)) {
                exigir(u.getRol().actualizarPaciente(), "actualizar pacientes");
                Map<String, Object> c = cuerpoJson(ex);
                pacientes.actualizar(id,
                        Json.txt(c, "nombre"),
                        Json.txt(c, "telefono"),
                        Json.txt(c, "correo"),
                        Json.txt(c, "direccion"));
                enviar(ex, 200, Json.json(Json.m("ok", true, "mensaje", "Paciente actualizado.")));
                return;
            }
            metodoNoPermitido(ex);
            return;
        }
        if ("estado".equals(seg[2]) && "PUT".equals(metodo)) {
            exigir(u.getRol().actualizarPaciente(), "cambiar el estado de pacientes");
            Map<String, Object> c = cuerpoJson(ex);
            pacientes.cambiarEstado(id, EstadoPersona.valueOf(Json.txt(c, "estado")));
            enviar(ex, 200, Json.json(Json.m("ok", true, "mensaje", "Estado actualizado.")));
            return;
        }
        noEncontrado(ex, "Recurso no encontrado.");
    }

    private static Map<String, Object> pacienteJson(Paciente p) {
        return Json.m(
                "id", p.getIdPaciente(),
                "nombre", p.getNombre(),
                "dni", p.getDni(),
                "telefono", p.getTelefono(),
                "correo", p.getCorreo(),
                "direccion", p.getDireccion(),
                "fechaRegistro", p.getFechaRegistro() != null ? p.getFechaRegistro().toString() : null,
                "estado", p.getEstado().name());
    }

    // ======================== MEDICOS ========================

    private void medicos(HttpExchange ex, Usuario u, String[] seg, String metodo)
            throws IOException {
        if (seg.length == 1) {
            if ("GET".equals(metodo)) {
                List<Object> filas = new ArrayList<>();
                for (Medico m : medicos.listar()) {
                    filas.add(medicoJson(m));
                }
                enviar(ex, 200, Json.json(Json.m("datos", filas)));
                return;
            }
            if ("POST".equals(metodo)) {
                exigir(u.getRol().gestionarMedicos(), "gestionar medicos");
                Map<String, Object> c = cuerpoJson(ex);
                Medico m = medicos.registrar(
                        Json.txt(c, "nombre"),
                        Json.txt(c, "dni"),
                        Json.txt(c, "especialidad"),
                        Json.txt(c, "telefono"));
                enviar(ex, 201, Json.json(medicoJson(m)));
                return;
            }
            metodoNoPermitido(ex);
            return;
        }
        int id = idDe(seg[1]);
        if (seg.length == 2) {
            if ("GET".equals(metodo)) {
                enviar(ex, 200, Json.json(medicoJson(medicos.buscarPorId(id))));
                return;
            }
            if ("PUT".equals(metodo)) {
                exigir(u.getRol().gestionarMedicos(), "gestionar medicos");
                Map<String, Object> c = cuerpoJson(ex);
                medicos.actualizar(id,
                        Json.txt(c, "nombre"),
                        Json.txt(c, "especialidad"),
                        Json.txt(c, "telefono"));
                enviar(ex, 200, Json.json(Json.m("ok", true, "mensaje", "Medico actualizado.")));
                return;
            }
            metodoNoPermitido(ex);
            return;
        }
        if ("estado".equals(seg[2]) && "PUT".equals(metodo)) {
            exigir(u.getRol().gestionarMedicos(), "gestionar medicos");
            Map<String, Object> c = cuerpoJson(ex);
            medicos.cambiarEstado(id, EstadoPersona.valueOf(Json.txt(c, "estado")));
            enviar(ex, 200, Json.json(Json.m("ok", true, "mensaje", "Estado actualizado.")));
            return;
        }
        noEncontrado(ex, "Recurso no encontrado.");
    }

    private static Map<String, Object> medicoJson(Medico m) {
        return Json.m(
                "id", m.getIdMedico(),
                "nombre", m.getNombre(),
                "dni", m.getDni(),
                "especialidad", m.getEspecialidad(),
                "telefono", m.getTelefono(),
                "fechaRegistro", m.getFechaRegistro() != null ? m.getFechaRegistro().toString() : null,
                "estado", m.getEstado().name());
    }

    // ======================== CITAS ========================

    private void citas(HttpExchange ex, Usuario u, String[] seg, String metodo)
            throws IOException {
        if (seg.length == 1) {
            if ("GET".equals(metodo)) {
                List<Object> filas = new ArrayList<>();
                for (Cita c : u.getRol().esMedico() && u.getIdMedico() != null
                        ? citas.listarPorMedico(u.getIdMedico())
                        : citas.listar()) {
                    filas.add(citaResumen(c));
                }
                enviar(ex, 200, Json.json(Json.m("datos", filas)));
                return;
            }
            if ("POST".equals(metodo)) {
                exigir(u.getRol().programarCita(), "programar citas");
                Map<String, Object> c = cuerpoJson(ex);
                Cita creada = citas.programar(
                        Json.entero(c, "idPaciente"),
                        Json.entero(c, "idMedico"),
                        fecha(Json.txt(c, "fecha")),
                        hora(Json.txt(c, "hora")),
                        Json.txt(c, "observacion"));
                enviar(ex, 201, Json.json(citaResumen(creada)));
                return;
            }
            metodoNoPermitido(ex);
            return;
        }
        int id = idDe(seg[1]);
        if (seg.length == 2) {
            if ("GET".equals(metodo)) {
                enviar(ex, 200, Json.json(citaResumen(citas.buscarPorId(id))));
                return;
            }
            if ("PUT".equals(metodo)) {
                exigir(u.getRol().modificarCita(), "modificar citas");
                Map<String, Object> c = cuerpoJson(ex);
                citas.modificar(id,
                        fecha(Json.txt(c, "fecha")),
                        hora(Json.txt(c, "hora")),
                        EstadoCita.valueOf(Json.txt(c, "estado")),
                        Json.txt(c, "observacion"));
                enviar(ex, 200, Json.json(Json.m("ok", true, "mensaje", "Cita modificada.")));
                return;
            }
            metodoNoPermitido(ex);
            return;
        }
        String accion = seg[2];
        if ("confirmar".equals(accion) && "PUT".equals(metodo)) {
            exigirCitaDeSesion(u, id);
            citas.confirmar(id);
            enviar(ex, 200, Json.json(Json.m("ok", true, "mensaje", "Cita confirmada.")));
            return;
        }
        if ("cancelar".equals(accion) && "PUT".equals(metodo)) {
            exigirCitaDeSesion(u, id);
            citas.cancelar(id);
            enviar(ex, 200, Json.json(Json.m("ok", true, "mensaje",
                    "Cita cancelada. Su horario quedo disponible.")));
            return;
        }
        noEncontrado(ex, "Recurso no encontrado.");
    }

    // ======================== ATENCIONES ========================

    private void atenciones(HttpExchange ex, Usuario u, String[] seg, String metodo)
            throws IOException {
        if (seg.length >= 3 && "paciente".equals(seg[1])) {
            if ("GET".equals(metodo)) {
                int idPaciente = idDe(seg[2]);
                List<Object> filas = new ArrayList<>();
                for (Atencion a : atenciones.historialPorPaciente(idPaciente)) {
                    filas.add(atencionResumen(a));
                }
                enviar(ex, 200, Json.json(Json.m("datos", filas)));
                return;
            }
            metodoNoPermitido(ex);
            return;
        }
        if (seg.length == 1) {
            if ("GET".equals(metodo)) {
                List<Object> filas = new ArrayList<>();
                for (Atencion a : atenciones.listar()) {
                    filas.add(atencionResumen(a));
                }
                enviar(ex, 200, Json.json(Json.m("datos", filas)));
                return;
            }
            if ("POST".equals(metodo)) {
                exigir(u.getRol().registrarAtencion(), "registrar atenciones");
                Map<String, Object> c = cuerpoJson(ex);
                int idCita = Json.entero(c, "idCita");
                exigirCitaDeSesion(u, idCita);
                atenciones.registrar(idCita,
                        Json.txt(c, "diagnostico"),
                        Json.txt(c, "observaciones"),
                        fecha(Json.txt(c, "fecha")));
                enviar(ex, 201, Json.json(Json.m("ok", true, "mensaje", "Atencion registrada.")));
                return;
            }
            metodoNoPermitido(ex);
            return;
        }
        noEncontrado(ex, "Recurso no encontrado.");
    }

    // ======================== PAGOS ========================

    private void pagos(HttpExchange ex, Usuario u, String[] seg, String metodo)
            throws IOException {
        exigir(u.getRol().gestionarPagos(), "gestionar pagos");
        if (seg.length == 1) {
            if ("GET".equals(metodo)) {
                List<Object> filas = new ArrayList<>();
                for (Pago p : pagos.listar()) {
                    filas.add(Json.m(
                            "id", p.getIdPago(),
                            "monto", p.getMonto(),
                            "fecha", p.getFecha() != null ? p.getFecha().toString() : null,
                            "metodo", p.getMetodo().name(),
                            "idAtencion", p.getIdAtencion()));
                }
                enviar(ex, 200, Json.json(Json.m("datos", filas)));
                return;
            }
            if ("POST".equals(metodo)) {
                Map<String, Object> c = cuerpoJson(ex);
                Pago p = pagos.registrar(
                        Json.monto(c, "monto", -1),
                        fecha(Json.txt(c, "fecha")),
                        MetodoPago.valueOf(Json.txt(c, "metodo")),
                        Json.enteroOpcional(c, "idAtencion"));
                enviar(ex, 201, Json.json(Json.m(
                        "ok", true, "mensaje", "Pago registrado.",
                        "id", p.getIdPago(),
                        "monto", p.getMonto())));
                return;
            }
            metodoNoPermitido(ex);
            return;
        }
        noEncontrado(ex, "Recurso no encontrado.");
    }

    // ======================== GASTOS ========================

    private void gastos(HttpExchange ex, Usuario u, String[] seg, String metodo)
            throws IOException {
        exigir(u.getRol().gestionarGastos(), "gestionar gastos");
        if (seg.length == 1) {
            if ("GET".equals(metodo)) {
                List<Object> filas = new ArrayList<>();
                for (Gasto g : gastos.listar()) {
                    filas.add(gastoJson(g));
                }
                enviar(ex, 200, Json.json(Json.m("datos", filas)));
                return;
            }
            if ("POST".equals(metodo)) {
                Map<String, Object> c = cuerpoJson(ex);
                Gasto g = gastos.registrar(
                        Json.txt(c, "descripcion"),
                        Json.monto(c, "monto", -1),
                        fecha(Json.txt(c, "fecha")),
                        CategoriaGasto.valueOf(Json.txt(c, "categoria")));
                enviar(ex, 201, Json.json(Json.m(
                        "ok", true, "mensaje", "Gasto registrado.", "id", g.getIdGasto())));
                return;
            }
            metodoNoPermitido(ex);
            return;
        }
        if (seg.length == 3 && "anular".equals(seg[2]) && "PUT".equals(metodo)) {
            gastos.anular(idDe(seg[1]));
            enviar(ex, 200, Json.json(Json.m("ok", true, "mensaje", "Gasto anulado.")));
            return;
        }
        noEncontrado(ex, "Recurso no encontrado.");
    }

    private static Map<String, Object> gastoJson(Gasto g) {
        return Json.m(
                "id", g.getIdGasto(),
                "descripcion", g.getDescripcion(),
                "monto", g.getMonto(),
                "fecha", g.getFecha() != null ? g.getFecha().toString() : null,
                "categoria", g.getCategoria().name(),
                "estado", g.getEstado().name());
    }

    // ======================== PRESUPUESTO ========================

    private void presupuesto(HttpExchange ex, Usuario u, String metodo) throws IOException {
        if ("GET".equals(metodo)) {
            enviar(ex, 200, Json.json(Json.m("presupuesto", gastos.obtenerPresupuesto())));
            return;
        }
        if ("PUT".equals(metodo)) {
            exigir(u.getRol().gestionarPresupuesto(), "gestionar el presupuesto");
            Map<String, Object> c = cuerpoJson(ex);
            double nuevo = Json.monto(c, "presupuesto", -1);
            gastos.actualizarPresupuesto(nuevo);
            enviar(ex, 200, Json.json(Json.m("ok", true,
                    "mensaje", "Presupuesto actualizado.",
                    "presupuesto", gastos.obtenerPresupuesto())));
            return;
        }
        metodoNoPermitido(ex);
    }

    // ======================== REPORTES ========================

    private void reportes(HttpExchange ex, Usuario u, String[] seg, String metodo)
            throws IOException {
        if (seg.length < 2) {
            noEncontrado(ex, "Recurso no encontrado.");
            return;
        }
        String tipo = seg[1].toLowerCase();
        if ("GET".equals(metodo) && "resumen".equals(tipo)) {
            enviar(ex, 200, Json.json(resumenJson(reportes.obtenerResumen())));
            return;
        }
        if (("ingresos-gastos".equals(tipo) || "citas".equals(tipo)
                || "atenciones".equals(tipo)) && "POST".equals(metodo)) {
            Map<String, Object> c = cuerpoJson(ex);
            LocalDate inicio = fecha(Json.txt(c, "inicio"));
            LocalDate fin = fecha(Json.txt(c, "fin"));
            if (inicio.isAfter(fin)) {
                throw new IllegalArgumentException(
                        "La fecha de inicio debe ser anterior o igual a la de fin.");
            }
            Reporte r = switch (tipo) {
                case "ingresos-gastos" -> reportes.reporteIngresosYGastos(inicio, fin);
                case "citas" -> reportes.reporteCitas(inicio, fin);
                default -> reportes.reporteAtenciones(inicio, fin);
            };
            enviar(ex, 200, Json.json(reporteJson(r)));
            return;
        }
        noEncontrado(ex, "Recurso no encontrado.");
    }

    private static Map<String, Object> reporteJson(Reporte r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tipo", r.getTipo());
        m.put("inicio", r.getFechaInicio() != null ? r.getFechaInicio().toString() : null);
        m.put("fin", r.getFechaFin() != null ? r.getFechaFin().toString() : null);
        m.put("totalIngresos", r.getTotalIngresos());
        m.put("totalGastos", r.getTotalGastos());
        m.put("presupuesto", r.getPresupuesto());
        m.put("saldo", r.getSaldo());
        m.put("saldoPresupuestal", r.getSaldoPresupuestal());
        m.put("excedePresupuesto", r.isExcedePresupuesto());
        m.put("totalCitas", r.getTotalCitas());
        m.put("totalAtenciones", r.getTotalAtenciones());

        if (r.getGastosPorCategoria() != null) {
            Map<Object, Object> porCategoria = new LinkedHashMap<>();
            for (Map.Entry<CategoriaGasto, Double> en : r.getGastosPorCategoria().entrySet()) {
                porCategoria.put(en.getKey().name(), en.getValue());
            }
            m.put("gastosPorCategoria", porCategoria);
        }
        if (r.getCitasPorEstado() != null) {
            Map<Object, Object> porEstado = new LinkedHashMap<>();
            for (Map.Entry<EstadoCita, Integer> en : r.getCitasPorEstado().entrySet()) {
                porEstado.put(en.getKey().name(), en.getValue());
            }
            m.put("citasPorEstado", porEstado);
        }
        if (r.getDetallePagos() != null) {
            List<Object> detalle = new ArrayList<>();
            for (Pago p : r.getDetallePagos()) {
                detalle.add(Json.m(
                        "id", p.getIdPago(), "monto", p.getMonto(),
                        "fecha", p.getFecha() != null ? p.getFecha().toString() : null,
                        "metodo", p.getMetodo().name(), "idAtencion", p.getIdAtencion()));
            }
            m.put("detallePagos", detalle);
        }
        if (r.getDetalleGastos() != null) {
            List<Object> detalle = new ArrayList<>();
            for (Gasto g : r.getDetalleGastos()) {
                detalle.add(gastoJson(g));
            }
            m.put("detalleGastos", detalle);
        }
        if (r.getDetalleCitas() != null) {
            List<Object> detalle = new ArrayList<>();
            for (Cita c : r.getDetalleCitas()) {
                detalle.add(citaResumen(c));
            }
            m.put("detalleCitas", detalle);
        }
        if (r.getDetalleAtenciones() != null) {
            List<Object> detalle = new ArrayList<>();
            for (Atencion a : r.getDetalleAtenciones()) {
                detalle.add(atencionResumen(a));
            }
            m.put("detalleAtenciones", detalle);
        }
        return m;
    }

    // ======================== USUARIOS ========================

    private void usuarios(HttpExchange ex, Usuario u, String[] seg, String metodo)
            throws IOException {
        exigir(u.getRol().gestionarUsuarios(), "gestionar usuarios");
        if (seg.length == 1) {
            if ("GET".equals(metodo)) {
                List<Object> filas = new ArrayList<>();
                for (Usuario us : usuarios.listar()) {
                    filas.add(Json.m(
                            "id", us.getIdUsuario(),
                            "usuario", us.getNombreUsuario(),
                            "nombre", us.getNombreCompleto(),
                            "rol", us.getRol().name(),
                            "idMedico", us.getIdMedico(),
                            "medicoVinculado", us.getNombreMedicoVinculado(),
                            "activo", us.isActivo()));
                }
                enviar(ex, 200, Json.json(Json.m("datos", filas)));
                return;
            }
            if ("POST".equals(metodo)) {
                Map<String, Object> c = cuerpoJson(ex);
                String rol = Json.txt(c, "rol");
                Integer idMedico = "MEDICO".equals(rol) ? Json.enteroOpcional(c, "idMedico") : null;
                Usuario nuevo = usuarios.registrar(
                        Json.txt(c, "usuario"),
                        Json.txt(c, "nombre"),
                        Json.txt(c, "clave"),
                        Rol.valueOf(rol),
                        idMedico);
                enviar(ex, 201, Json.json(Json.m("ok", true,
                        "mensaje", "Usuario registrado.", "id", nuevo.getIdUsuario())));
                return;
            }
            metodoNoPermitido(ex);
            return;
        }
        int id = idDe(seg[1]);
        if (seg.length == 2) {
            if ("GET".equals(metodo)) {
                Usuario us = usuarios.buscarPorId(id);
                enviar(ex, 200, Json.json(Json.m(
                        "id", us.getIdUsuario(),
                        "usuario", us.getNombreUsuario(),
                        "nombre", us.getNombreCompleto(),
                        "rol", us.getRol().name(),
                        "idMedico", us.getIdMedico(),
                        "activo", us.isActivo())));
                return;
            }
            if ("PUT".equals(metodo)) {
                Map<String, Object> c = cuerpoJson(ex);
                String rol = Json.txt(c, "rol");
                Integer idMedico = "MEDICO".equals(rol) ? Json.enteroOpcional(c, "idMedico") : null;
                usuarios.actualizarDatos(id,
                        Json.txt(c, "nombre"),
                        Rol.valueOf(rol),
                        idMedico,
                        Json.booleano(c, "activo", true),
                        u.getIdUsuario());
                enviar(ex, 200, Json.json(Json.m("ok", true, "mensaje", "Usuario actualizado.")));
                return;
            }
            metodoNoPermitido(ex);
            return;
        }
        String accion = seg[2];
        if ("estado".equals(accion) && "PUT".equals(metodo)) {
            Map<String, Object> c = cuerpoJson(ex);
            usuarios.cambiarEstado(id, Json.booleano(c, "activo", true), u.getIdUsuario());
            enviar(ex, 200, Json.json(Json.m("ok", true, "mensaje", "Estado actualizado.")));
            return;
        }
        if ("clave".equals(accion) && "PUT".equals(metodo)) {
            Map<String, Object> c = cuerpoJson(ex);
            usuarios.cambiarClave(id, Json.txt(c, "clave"));
            enviar(ex, 200, Json.json(Json.m("ok", true, "mensaje", "Clave actualizada.")));
            return;
        }
        noEncontrado(ex, "Recurso no encontrado.");
    }

    private void cambiarMiClave(HttpExchange ex, Usuario u, String metodo) throws IOException {
        if (!"POST".equals(metodo)) {
            metodoNoPermitido(ex);
            return;
        }
        Map<String, Object> c = cuerpoJson(ex);
        String claveActual = Json.txt(c, "claveActual");
        String claveNueva = Json.txt(c, "claveNueva");
        if (claveActual == null || claveActual.isEmpty() || claveNueva == null || claveNueva.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe ingresar la clave actual y la nueva clave.");
        }
        usuarios.cambiarMiClave(u.getNombreUsuario(), claveActual, claveNueva);
        enviar(ex, 200, Json.json(Json.m("ok", true, "mensaje", "Contrasena actualizada.")));
    }

    // ======================== EXPORTACION ========================

    private void exportar(HttpExchange ex, String[] seg) throws IOException {
        if (seg.length < 2 || !"GET".equals(ex.getRequestMethod().toUpperCase())) {
            metodoNoPermitido(ex);
            return;
        }
        String tipo = seg[1].toLowerCase();
        String formato = parametro(ex, "formato");
        if (formato == null || formato.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar el formato (PDF o EXCEL).");
        }
        FormatoExportacion f;
        try {
            f = FormatoExportacion.valueOf(formato.toUpperCase());
        } catch (IllegalArgumentException exF) {
            throw new IllegalArgumentException("Formato invalido: " + formato);
        }
        Path archivo = switch (tipo) {
            case "resumen" -> reportes.exportarResumen(f);
            case "dashboard" -> dashboard.exportarEstadisticas(f);
            case "ingresos-gastos" -> reportes.exportarReporteIngresosYGastos(
                    f, fecha(parametro(ex, "inicio")), fecha(parametro(ex, "fin")));
            case "citas" -> reportes.exportarReporteCitas(
                    f, fecha(parametro(ex, "inicio")), fecha(parametro(ex, "fin")));
            case "atenciones" -> reportes.exportarReporteAtenciones(
                    f, fecha(parametro(ex, "inicio")), fecha(parametro(ex, "fin")));
            default -> null;
        };
        if (archivo == null || !Files.exists(archivo)) {
            noEncontrado(ex, "No se pudo generar el archivo de exportacion.");
            return;
        }
        byte[] bytes = Files.readAllBytes(archivo);
        String nombre = archivo.getFileName().toString();
        String tipoContenido = nombre.endsWith(".pdf")
                ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        ex.getResponseHeaders().set("Content-Type", tipoContenido);
        ex.getResponseHeaders().set("Content-Disposition",
                "attachment; filename=\"" + nombre + "\"");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    // ======================== AYUDAS ========================

    private static int idDe(String texto) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El identificador debe ser un numero.");
        }
    }

    private static LocalDate fecha(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar una fecha.");
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Fecha invalida '" + texto + "'. Use el formato aaaa-mm-dd.");
        }
    }

    private static LocalTime hora(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar una hora.");
        }
        try {
            return LocalTime.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Hora invalida '" + texto + "'. Use el formato HH:mm.");
        }
    }

    /** Extension simplificada de la URL: /api/pacientes?dni=123 o query en cuerpo. */
    private static String parametro(HttpExchange ex, String clave) {
        String q = ex.getRequestURI().getRawQuery();
        if (q == null || q.isEmpty()) {
            return null;
        }
        for (String par : q.split("&")) {
            int i = par.indexOf('=');
            if (i > 0 && par.substring(0, i).equals(clave)) {
                try {
                    return java.net.URLDecoder.decode(par.substring(i + 1), UTF8);
                } catch (Exception exU) {
                    return par.substring(i + 1);
                }
            }
        }
        return null;
    }

    private static Map<String, Object> cuerpoJson(HttpExchange ex) throws IOException {
        byte[] bytes = ex.getRequestBody().readAllBytes();
        if (bytes.length == 0) {
            return new LinkedHashMap<>();
        }
        return Json.leerObjeto(new String(bytes, StandardCharsets.UTF_8));
    }

    private static void enviar(HttpExchange ex, int codigo, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=" + UTF8);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void error(HttpExchange ex, int codigo, String mensaje) throws IOException {
        enviar(ex, codigo, Json.json(Json.m("error", mensaje)));
    }

    private static void noEncontrado(HttpExchange ex, String mensaje) throws IOException {
        error(ex, 404, mensaje);
    }

    private static void metodoNoPermitido(HttpExchange ex) throws IOException {
        error(ex, 405, "Metodo HTTP no permitido.");
    }
}