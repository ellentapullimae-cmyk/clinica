package clinica.presentacion;

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
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Interfaz de consola del Sistema de Gestion para una Clinica.
 * Vista en la arquitectura por capas (presentacion).
 * Controla el inicio de sesion, los permisos por rol (Administrador,
 * Recepcionista y Medico) y expone todos los modulos del sistema.
 */
public class Menu {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final PacienteController pacienteController;
    private final MedicoController medicoController;
    private final CitaController citaController;
    private final AtencionController atencionController;
    private final PagoController pagoController;
    private final GastoController gastoController;
    private final ReporteController reporteController;
    private final UsuarioController usuarioController;
    private final DashboardController dashboardController;

    private final Scanner lector;
    private boolean salirSistema;

    public Menu(PacienteController pacienteController, MedicoController medicoController,
                CitaController citaController, AtencionController atencionController,
                PagoController pagoController, GastoController gastoController,
                ReporteController reporteController, UsuarioController usuarioController,
                DashboardController dashboardController) {
        this.pacienteController = pacienteController;
        this.medicoController = medicoController;
        this.citaController = citaController;
        this.atencionController = atencionController;
        this.pagoController = pagoController;
        this.gastoController = gastoController;
        this.reporteController = reporteController;
        this.usuarioController = usuarioController;
        this.dashboardController = dashboardController;
        this.lector = new Scanner(System.in);
    }

    /** Arranca el sistema: login y menu principal, permitiendo cambiar de usuario. */
    public void iniciar() {
        while (!salirSistema) {
            Usuario usuario = iniciarLogin();
            if (usuario == null) {
                System.out.println("Sesion cancelada o no autorizada. Acceso denegado.");
                break;
            }
            Sesion.iniciar(usuario);
            ejecutar();
            Sesion.cerrar();
            if (!salirSistema) {
                System.out.println("Sesion cerrada.");
            }
        }
        System.out.println("Fin del programa. Hasta pronto.");
    }

    // ======================= LOGIN =======================

    private Usuario iniciarLogin() {
        try {
            return LoginVentana.autenticar(usuarioController);
        } catch (LoginVentana.SesionCancelada cancelada) {
            salirSistema = true;
            return null;
        } catch (RuntimeException e) {
            System.out.println("No se pudo abrir la ventana de acceso, usando la consola: " + e.getMessage());
            return iniciarLoginConsola();
        }
    }

    private Usuario iniciarLoginConsola() {
        Interfaz.banner("SISTEMA DE GESTION PARA UNA CLINICA", "ACCESO RESTRINGIDO - INICIO DE SESION");
        for (int intento = 1; intento <= 3; intento++) {
            String usuario = leerTexto("Usuario: ");
            String clave = leerClave("Clave: ");
            try {
                Usuario u = usuarioController.autenticar(usuario, clave);
                Interfaz.separador();
                Interfaz.exito("Bienvenido, " + u.getNombreCompleto());
                Interfaz.informacion("Rol: " + u.getRol().name());
                Interfaz.separador();
                return u;
            } catch (RuntimeException e) {
                Interfaz.error("Error: " + e.getMessage());
                if (intento < 3) {
                    Interfaz.aviso("Le quedan " + (3 - intento) + " intento(s).");
                }
            }
        }
        return null;
    }

    private String leerClave(String etiqueta) {
        java.io.Console consola = System.console();
        if (consola != null) {
            char[] clave = consola.readPassword(etiqueta);
            return new String(clave);
        }
        System.out.print(etiqueta);
        return lector.nextLine();
    }

    // ======================= MENU PRINCIPAL =======================

    private void ejecutar() {
        int opcion;
        do {
            System.out.println();
            List<String> opciones = new ArrayList<>();
            agregarSiPermitido(opciones, "  1. Modulo de Pacientes", true);
            agregarSiPermitido(opciones, "  2. Modulo de Medicos", Sesion.getRolActual().gestionarMedicos());
            agregarSiPermitido(opciones, "  3. Modulo de Citas", true);
            agregarSiPermitido(opciones, "  4. Modulo de Atenciones", true);
            agregarSiPermitido(opciones, "  5. Modulo de Pagos", Sesion.getRolActual().gestionarPagos());
            agregarSiPermitido(opciones, "  6. Modulo de Gastos", Sesion.getRolActual().gestionarGastos());
            agregarSiPermitido(opciones, "  7. Reportes y resumen economico", true);
            agregarSiPermitido(opciones, "  8. Presupuesto del periodo", Sesion.getRolActual().gestionarPresupuesto());
            agregarSiPermitido(opciones, "  9. Dashboard (estadisticas)", Sesion.getRolActual().verDashboard());
            agregarSiPermitido(opciones, " 10. Gestion de usuarios", Sesion.getRolActual().gestionarUsuarios());
            agregarSiPermitido(opciones, " 11. Cerrar sesion (cambiar de usuario)", true);
            agregarSiPermitido(opciones, " 12. Cambiar mi contrasena", true);
            agregarSiPermitido(opciones, "  0. Salir del sistema", true);
            Interfaz.informacion("Usuario: " + Sesion.getNombreUsuario()
                    + " | Rol: " + Sesion.getRolActual().name());
            Interfaz.menu("SISTEMA DE GESTION PARA UNA CLINICA", opciones);
            opcion = leerEntero("Seleccione un modulo: ");
            switch (opcion) {
                case 1 -> menuPacientes();
                case 2 -> menuMedicos();
                case 3 -> menuCitas();
                case 4 -> menuAtenciones();
                case 5 -> menuPagos();
                case 6 -> menuGastos();
                case 7 -> menuReportes();
                case 8 -> opcionPresupuesto();
                case 9 -> opcionDashboard();
                case 10 -> menuUsuarios();
                case 11 -> { }
                case 12 -> opCambiarMiClave();
                case 0 -> salirSistema = true;
                default -> Interfaz.aviso("Opcion no valida.");
            }
        } while (opcion != 0 && opcion != 11);
    }

    private void mostrarOpcion(String etiqueta, boolean permitido) {
        if (permitido) {
            System.out.println(etiqueta);
        }
    }

    private void agregarSiPermitido(List<String> opciones, String etiqueta, boolean permitido) {
        if (permitido) {
            opciones.add(etiqueta);
        }
    }

    private void exigirPermiso(boolean permitido) {
        if (!permitido) {
            throw new IllegalStateException("Acceso denegado: su rol no tiene permiso para esta funcion.");
        }
    }

    /** Si la sesion es de un medico, la cita debe pertenecer a ese medico. */
    private void exigirCitaDeSesion(int idCita) {
        if (Sesion.getRolActual().esMedico() && Sesion.getIdMedicoSesion() != null) {
            boolean propia = false;
            for (Cita c : citaController.listarPorMedico(Sesion.getIdMedicoSesion())) {
                if (c.getIdCita() == idCita) {
                    propia = true;
                    break;
                }
            }
            if (!propia) {
                throw new IllegalArgumentException("La cita debe pertenecer al medico en sesion.");
            }
        }
    }

    // ======================= PACIENTES =======================

    private void menuPacientes() {
        int opcion;
        do {
            System.out.println();
            Interfaz.encabezado("MODULO DE PACIENTES (RF-01)");
            mostrarOpcion("  1. Registrar paciente", Sesion.getRolActual().registrarPaciente());
            mostrarOpcion("  2. Listar pacientes", true);
            mostrarOpcion("  3. Buscar paciente por DNI", true);
            mostrarOpcion("  4. Actualizar datos del paciente", Sesion.getRolActual().actualizarPaciente());
            mostrarOpcion("  5. Cambiar estado (activo/inactivo)", Sesion.getRolActual().actualizarPaciente());
            System.out.println("  0. Volver al menu principal");
            opcion = leerEntero("Seleccione una opcion: ");
            switch (opcion) {
                case 1 -> opRegistrarPaciente();
                case 2 -> opListarPacientes();
                case 3 -> opBuscarPacientePorDni();
                case 4 -> opActualizarPaciente();
                case 5 -> opCambiarEstadoPaciente();
                case 0 -> { }
                default -> System.out.println("Opcion no valida.");
            }
        } while (opcion != 0);
    }

    private void opRegistrarPaciente() {
        intentar(() -> {
            exigirPermiso(Sesion.getRolActual().registrarPaciente());
            String nombre = leerTexto("Nombre y apellidos: ");
            String dni = leerTexto("DNI (8 digitos): ");
            String telefono = leerTexto("Telefono (opcional): ");
            String correo = leerTexto("Correo (opcional): ");
            String direccion = leerTexto("Direccion (opcional): ");
            Paciente paciente = pacienteController.registrar(nombre, dni, telefono, correo, direccion);
            System.out.println("Paciente registrado correctamente con id " + paciente.getIdPaciente() + ".");
        });
    }

    private void opListarPacientes() {
        intentar(() -> {
            List<Paciente> pacientes = pacienteController.listar();
            if (pacientes.isEmpty()) {
                System.out.println("No hay pacientes registrados.");
                return;
            }
            System.out.println("Pacientes registrados: " + pacientes.size());
            System.out.printf("%-5s %-28s %-10s %-12s %-30s %-8s%n",
                    "ID", "NOMBRE", "DNI", "TELEFONO", "CORREO", "ESTADO");
            for (Paciente p : pacientes) {
                System.out.printf("%-5d %-28s %-10s %-12s %-30s %-8s%n",
                        p.getIdPaciente(), p.getNombre(), p.getDni(),
                        nulo(p.getTelefono()), nulo(p.getCorreo()), p.getEstado());
            }
        });
    }

    private void opBuscarPacientePorDni() {
        intentar(() -> {
            String dni = leerTexto("DNI a buscar: ");
            Paciente p = pacienteController.buscarPorDni(dni);
            if (p == null) {
                System.out.println("No se encontro un paciente con el DNI " + dni + ".");
                return;
            }
            System.out.println("ID: " + p.getIdPaciente());
            System.out.println("Nombre: " + p.getNombre());
            System.out.println("DNI: " + p.getDni());
            System.out.println("Telefono: " + nulo(p.getTelefono()));
            System.out.println("Correo: " + nulo(p.getCorreo()));
            System.out.println("Direccion: " + nulo(p.getDireccion()));
            System.out.println("Fecha de registro: " + p.getFechaRegistro().format(FORMATO_FECHA));
            System.out.println("Estado: " + p.getEstado());
        });
    }

    private void opActualizarPaciente() {
        intentar(() -> {
            exigirPermiso(Sesion.getRolActual().actualizarPaciente());
            int id = leerEntero("Id del paciente: ");
            Paciente actual = pacienteController.buscarPorId(id);
            System.out.println("Actualizando a: " + actual.getNombre() + " (DNI " + actual.getDni() + ").");
            String nombre = leerTexto("Nuevo nombre: ");
            String telefono = leerTexto("Nuevo telefono (opcional): ");
            String correo = leerTexto("Nuevo correo (opcional): ");
            String direccion = leerTexto("Nueva direccion (opcional): ");
            pacienteController.actualizar(id, nombre, telefono, correo, direccion);
            System.out.println("Paciente actualizado.");
        });
    }

    private void opCambiarEstadoPaciente() {
        intentar(() -> {
            exigirPermiso(Sesion.getRolActual().actualizarPaciente());
            int id = leerEntero("Id del paciente: ");
            EstadoPersona estado = seleccionarEnum("Nuevo estado", EstadoPersona.values());
            pacienteController.cambiarEstado(id, estado);
            System.out.println("Estado actualizado a " + estado + ".");
        });
    }

    // ======================= MEDICOS =======================

    private void menuMedicos() {
        if (Sesion.getRolActual() != Rol.ADMINISTRADOR) {
            Interfaz.error("Acceso denegado: su rol no tiene permiso para esta funcion.");
            return;
        }
        int opcion;
        do {
            System.out.println();
            Interfaz.encabezado("MODULO DE MEDICOS (RF-02)");
            System.out.println("  1. Registrar medico");
            System.out.println("  2. Listar medicos");
            System.out.println("  3. Buscar medico por DNI");
            System.out.println("  4. Actualizar datos del medico");
            System.out.println("  5. Cambiar estado (activo/inactivo)");
            System.out.println("  0. Volver al menu principal");
            opcion = leerEntero("Seleccione una opcion: ");
            switch (opcion) {
                case 1 -> opRegistrarMedico();
                case 2 -> opListarMedicos();
                case 3 -> opBuscarMedicoPorDni();
                case 4 -> opActualizarMedico();
                case 5 -> opCambiarEstadoMedico();
                case 0 -> { }
                default -> System.out.println("Opcion no valida.");
            }
        } while (opcion != 0);
    }

    private void opRegistrarMedico() {
        intentar(() -> {
            String nombre = leerTexto("Nombre y apellidos: ");
            String dni = leerTexto("DNI (8 digitos): ");
            String especialidad = leerTexto("Especialidad: ");
            String telefono = leerTexto("Telefono (opcional): ");
            Medico medico = medicoController.registrar(nombre, dni, especialidad, telefono);
            System.out.println("Medico registrado correctamente con id " + medico.getIdMedico() + ".");
        });
    }

    private void opListarMedicos() {
        intentar(() -> {
            List<Medico> medicos = medicoController.listar();
            if (medicos.isEmpty()) {
                System.out.println("No hay medicos registrados.");
                return;
            }
            System.out.printf("%-5s %-28s %-10s %-20s %-12s %-8s%n",
                    "ID", "NOMBRE", "DNI", "ESPECIALIDAD", "TELEFONO", "ESTADO");
            for (Medico m : medicos) {
                System.out.printf("%-5d %-28s %-10s %-20s %-12s %-8s%n",
                        m.getIdMedico(), m.getNombre(), m.getDni(),
                        m.getEspecialidad(), nulo(m.getTelefono()), m.getEstado());
            }
        });
    }

    private void opBuscarMedicoPorDni() {
        intentar(() -> {
            String dni = leerTexto("DNI a buscar: ");
            Medico m = medicoController.buscarPorDni(dni);
            if (m == null) {
                System.out.println("No se encontro un medico con el DNI " + dni + ".");
                return;
            }
            System.out.println("ID: " + m.getIdMedico());
            System.out.println("Nombre: " + m.getNombre());
            System.out.println("Especialidad: " + m.getEspecialidad());
            System.out.println("Telefono: " + nulo(m.getTelefono()));
            System.out.println("Estado: " + m.getEstado());
        });
    }

    private void opActualizarMedico() {
        intentar(() -> {
            int id = leerEntero("Id del medico: ");
            Medico actual = medicoController.buscarPorId(id);
            System.out.println("Actualizando a: " + actual.getNombre() + ".");
            String nombre = leerTexto("Nuevo nombre: ");
            String especialidad = leerTexto("Nueva especialidad: ");
            String telefono = leerTexto("Nuevo telefono (opcional): ");
            medicoController.actualizar(id, nombre, especialidad, telefono);
            System.out.println("Medico actualizado.");
        });
    }

    private void opCambiarEstadoMedico() {
        intentar(() -> {
            int id = leerEntero("Id del medico: ");
            EstadoPersona estado = seleccionarEnum("Nuevo estado", EstadoPersona.values());
            medicoController.cambiarEstado(id, estado);
            System.out.println("Estado actualizado a " + estado + ".");
        });
    }

    // ======================= CITAS =======================

    private void menuCitas() {
        int opcion;
        do {
            System.out.println();
            Interfaz.encabezado("MODULO DE CITAS (RF-03)");
            mostrarOpcion("  1. Programar cita", Sesion.getRolActual().programarCita());
            mostrarOpcion("  2. Listar citas", true);
            mostrarOpcion("  3. Listar citas por periodo", true);
            mostrarOpcion("  4. Modificar cita", Sesion.getRolActual().modificarCita());
            mostrarOpcion("  5. Confirmar cita", true);
            mostrarOpcion("  6. Cancelar cita", true);
            System.out.println("  0. Volver al menu principal");
            opcion = leerEntero("Seleccione una opcion: ");
            switch (opcion) {
                case 1 -> opProgramarCita();
                case 2 -> opListarCitas();
                case 3 -> opListarCitasPeriodo();
                case 4 -> opModificarCita();
                case 5 -> opConfirmarCita();
                case 6 -> opCancelarCita();
                case 0 -> { }
                default -> System.out.println("Opcion no valida.");
            }
        } while (opcion != 0);
    }

    private void opProgramarCita() {
        intentar(() -> {
            exigirPermiso(Sesion.getRolActual().programarCita());
            List<Paciente> pacientes = pacienteController.listarActivos();
            if (pacientes.isEmpty()) {
                throw new IllegalArgumentException("No hay pacientes activos. Registre un paciente primero.");
            }
            List<Medico> medicos = medicoController.listarActivos();
            if (medicos.isEmpty()) {
                throw new IllegalArgumentException("No hay medicos activos. Registre un medico primero.");
            }
            System.out.println("Pacientes activos:");
            System.out.printf("%-5s %-28s %s%n", "ID", "NOMBRE", "DNI");
            for (Paciente p : pacientes) {
                System.out.printf("%-5d %-28s %s%n", p.getIdPaciente(), p.getNombre(), p.getDni());
            }
            System.out.println("Medicos activos:");
            System.out.printf("%-5s %-28s %s%n", "ID", "NOMBRE", "ESPECIALIDAD");
            for (Medico m : medicos) {
                System.out.printf("%-5d %-28s %s%n", m.getIdMedico(), m.getNombre(), m.getEspecialidad());
            }
            int idPaciente = leerEntero("Id del paciente: ");
            int idMedico = leerEntero("Id del medico: ");
            LocalDate fecha = leerFecha("Fecha de la cita (dd/mm/aaaa): ");
            LocalTime hora = leerHora("Hora de la cita (HH:mm): ");
            String observacion = leerTexto("Observacion (opcional): ");
            Cita cita = citaController.programar(idPaciente, idMedico, fecha, hora, observacion);
            System.out.println("Cita programada con id " + cita.getIdCita() + " (estado " + cita.getEstado() + ").");
        });
    }

    private void opListarCitas() {
        intentar(() -> {
            List<Cita> citas;
            if (Sesion.getRolActual().esMedico() && Sesion.getIdMedicoSesion() != null) {
                citas = citaController.listarPorMedico(Sesion.getIdMedicoSesion());
                System.out.println("Mostrando solo las citas del medico " + Sesion.getNombreCompleto() + ":");
            } else {
                citas = citaController.listar();
            }
            listarCitasImpl(citas);
        });
    }

    private void opListarCitasPeriodo() {
        intentar(() -> {
            LocalDate inicio = leerFecha("Fecha de inicio (dd/mm/aaaa): ");
            LocalDate fin = leerFecha("Fecha de fin (dd/mm/aaaa): ");
            List<Cita> citas;
            if (Sesion.getRolActual().esMedico() && Sesion.getIdMedicoSesion() != null) {
                citas = citaController.listarPorMedicoEntreFechas(Sesion.getIdMedicoSesion(), inicio, fin);
            } else {
                citas = citaController.listarEntreFechas(inicio, fin);
            }
            listarCitasImpl(citas);
        });
    }

    private void listarCitasImpl(List<Cita> citas) {
        if (citas.isEmpty()) {
            System.out.println("No hay citas en la consulta.");
            return;
        }
        System.out.printf("%-5s %-12s %-5s %-25s %-25s %-12s %s%n",
                "ID", "FECHA", "HORA", "PACIENTE", "MEDICO", "ESTADO", "OBSERVACION");
        for (Cita c : citas) {
            System.out.printf("%-5d %-12s %-5s %-25s %-25s %-12s %s%n",
                    c.getIdCita(),
                    c.getFecha().format(FORMATO_FECHA),
                    c.getHora().format(FORMATO_HORA),
                    c.getNombrePaciente(),
                    c.getNombreMedico(),
                    c.getEstado(),
                    nulo(c.getObservacion()));
        }
    }

    private void opModificarCita() {
        intentar(() -> {
            exigirPermiso(Sesion.getRolActual().modificarCita());
            int idCita = leerEntero("Id de la cita: ");
            Cita actual = citaController.buscarPorId(idCita);
            System.out.println("Cita actual: " + actual.getNombrePaciente() + " - "
                    + actual.getNombreMedico() + " - " + actual.getFecha().format(FORMATO_FECHA)
                    + " " + actual.getHora().format(FORMATO_HORA) + " [" + actual.getEstado() + "]");
            LocalDate fecha = leerFecha("Nueva fecha (dd/mm/aaaa): ");
            LocalTime hora = leerHora("Nueva hora (HH:mm): ");
            EstadoCita[] estadosEditables = {EstadoCita.PROGRAMADA, EstadoCita.CONFIRMADA};
            EstadoCita estado = seleccionarEnum("Nuevo estado", estadosEditables);
            String observacion = leerTexto("Observacion (opcional): ");
            citaController.modificar(idCita, fecha, hora, estado, observacion);
            System.out.println("Cita modificada correctamente.");
        });
    }

    private void opConfirmarCita() {
        intentar(() -> {
            int idCita = leerEntero("Id de la cita: ");
            exigirCitaDeSesion(idCita);
            citaController.confirmar(idCita);
            System.out.println("Cita " + idCita + " confirmada.");
        });
    }

    private void opCancelarCita() {
        intentar(() -> {
            int idCita = leerEntero("Id de la cita: ");
            exigirCitaDeSesion(idCita);
            citaController.cancelar(idCita);
            System.out.println("Cita " + idCita + " cancelada. Su horario queda disponible.");
        });
    }

    // ======================= ATENCIONES =======================

    private void menuAtenciones() {
        int opcion;
        do {
            System.out.println();
            Interfaz.encabezado("MODULO DE ATENCIONES (RF-04 / RF-09)");
            mostrarOpcion("  1. Registrar atencion (ligada a una cita)", Sesion.getRolActual().registrarAtencion());
            mostrarOpcion("  2. Listar atenciones", true);
            mostrarOpcion("  3. Listar atenciones por periodo", true);
            mostrarOpcion("  4. Historial de atenciones de un paciente", true);
            System.out.println("  0. Volver al menu principal");
            opcion = leerEntero("Seleccione una opcion: ");
            switch (opcion) {
                case 1 -> opRegistrarAtencion();
                case 2 -> opListarAtenciones();
                case 3 -> opListarAtencionesPeriodo();
                case 4 -> opHistorialPaciente();
                case 0 -> { }
                default -> System.out.println("Opcion no valida.");
            }
        } while (opcion != 0);
    }

    private void opRegistrarAtencion() {
        intentar(() -> {
            exigirPermiso(Sesion.getRolActual().registrarAtencion());
            System.out.println("Citas programadas o confirmadas:");
            List<Cita> citas;
            if (Sesion.getRolActual().esMedico() && Sesion.getIdMedicoSesion() != null) {
                citas = citaController.listarPorMedico(Sesion.getIdMedicoSesion());
            } else {
                citas = citaController.listar();
            }
            boolean hay = false;
            for (Cita c : citas) {
                if (c.getEstado() == EstadoCita.PROGRAMADA || c.getEstado() == EstadoCita.CONFIRMADA) {
                    System.out.printf("  id=%d  %s - %s - %s %s [%s]%n",
                            c.getIdCita(), c.getNombrePaciente(), c.getNombreMedico(),
                            c.getFecha().format(FORMATO_FECHA), c.getHora().format(FORMATO_HORA), c.getEstado());
                    hay = true;
                }
            }
            if (!hay) {
                throw new IllegalArgumentException("No hay citas programadas ni confirmadas para atender.");
            }
            int idCita = leerEntero("Id de la cita a atender: ");
            exigirCitaDeSesion(idCita);
            String diagnostico = leerTexto("Diagnostico: ");
            String observaciones = leerTexto("Observaciones (opcional): ");
            LocalDate fecha = leerFecha("Fecha de la atencion (dd/mm/aaaa): ");
            Atencion atencion = atencionController.registrar(idCita, diagnostico, observaciones, fecha);
            System.out.println("Atencion registrada con id " + atencion.getIdAtencion()
                    + ". La cita " + idCita + " quedo ATENDIDA.");
        });
    }

    private void opListarAtenciones() {
        intentar(() -> listarAtencionesImpl(atencionController.listar()));
    }

    private void opListarAtencionesPeriodo() {
        intentar(() -> {
            LocalDate inicio = leerFecha("Fecha de inicio (dd/mm/aaaa): ");
            LocalDate fin = leerFecha("Fecha de fin (dd/mm/aaaa): ");
            listarAtencionesImpl(atencionController.listarEntreFechas(inicio, fin));
        });
    }

    private void listarAtencionesImpl(List<Atencion> atenciones) {
        if (atenciones.isEmpty()) {
            System.out.println("No hay atenciones registradas.");
            return;
        }
        System.out.printf("%-5s %-5s %-12s %-25s %-25s %s%n",
                "ID", "CITA", "FECHA", "PACIENTE", "MEDICO", "DIAGNOSTICO");
        for (Atencion a : atenciones) {
            System.out.printf("%-5d %-5d %-12s %-25s %-25s %s%n",
                    a.getIdAtencion(),
                    a.getCita().getIdCita(),
                    a.getFechaAtencion().format(FORMATO_FECHA),
                    a.getNombrePaciente(),
                    a.getNombreMedico(),
                    a.getDiagnostico());
        }
    }

    private void opHistorialPaciente() {
        intentar(() -> {
            int idPaciente = leerEntero("Id del paciente: ");
            Paciente paciente = pacienteController.buscarPorId(idPaciente);
            System.out.println("Historial de atenciones de: " + paciente.getNombre()
                    + " (DNI " + paciente.getDni() + ")");
            List<Atencion> atenciones = atencionController.historialPorPaciente(idPaciente);
            if (atenciones.isEmpty()) {
                System.out.println("El paciente no tiene atenciones registradas.");
                return;
            }
            for (Atencion a : atenciones) {
                System.out.println("  - " + a.getFechaAtencion().format(FORMATO_FECHA)
                        + " | Medico: " + a.getNombreMedico()
                        + " | Cita " + a.getCita().getIdCita()
                        + " | Diagnostico: " + a.getDiagnostico());
                if (a.getObservaciones() != null) {
                    System.out.println("    Observaciones: " + a.getObservaciones());
                }
            }
        });
    }

    // ======================= PAGOS =======================

    private void menuPagos() {
        if (!Sesion.getRolActual().gestionarPagos()) {
            Interfaz.error("Acceso denegado: su rol no tiene permiso para esta funcion.");
            return;
        }
        int opcion;
        do {
            System.out.println();
            Interfaz.encabezado("MODULO DE PAGOS (RF-05)");
            System.out.println("  1. Registrar pago");
            System.out.println("  2. Listar pagos");
            System.out.println("  0. Volver al menu principal");
            opcion = leerEntero("Seleccione una opcion: ");
            switch (opcion) {
                case 1 -> opRegistrarPago();
                case 2 -> opListarPagos();
                case 0 -> { }
                default -> System.out.println("Opcion no valida.");
            }
        } while (opcion != 0);
    }

    private void opRegistrarPago() {
        intentar(() -> {
            double monto = leerDouble("Monto del pago (mayor que cero): ");
            LocalDate fecha = leerFecha("Fecha del pago (dd/mm/aaaa): ");
            MetodoPago metodo = seleccionarEnum("Metodos de pago", MetodoPago.values());
            Integer idAtencion = leerAtencionOpcional();
            Pago pago = pagoController.registrar(monto, fecha, metodo, idAtencion);
            System.out.println("Pago registrado con id " + pago.getIdPago() + ".");
        });
    }

    private Integer leerAtencionOpcional() {
        int id = leerEntero("Id de la atencion vinculada (0 = ninguno): ");
        if (id == 0) {
            return null;
        }
        return id;
    }

    private void opListarPagos() {
        intentar(() -> {
            List<Pago> pagos = pagoController.listar();
            if (pagos.isEmpty()) {
                System.out.println("No hay pagos registrados.");
                return;
            }
            System.out.printf("%-5s %-12s %-12s %-14s %s%n",
                    "ID", "MONTO", "FECHA", "METODO", "ID ATENCION");
            double total = 0;
            for (Pago pago : pagos) {
                total += pago.getMonto();
                System.out.printf("%-5d %-12s %-12s %-14s %s%n",
                        pago.getIdPago(), dinero(pago.getMonto()),
                        pago.getFecha().format(FORMATO_FECHA), pago.getMetodo(),
                        pago.getIdAtencion() == null ? "-" : pago.getIdAtencion());
            }
            System.out.println("Total de pagos: " + dinero(total));
        });
    }

    // ======================= GASTOS =======================

    private void menuGastos() {
        if (!Sesion.getRolActual().gestionarGastos()) {
            Interfaz.error("Acceso denegado: su rol no tiene permiso para esta funcion.");
            return;
        }
        int opcion;
        do {
            System.out.println();
            Interfaz.encabezado("MODULO DE GASTOS (RF-06)");
            System.out.println("  1. Registrar gasto");
            System.out.println("  2. Listar gastos");
            System.out.println("  3. Anular gasto");
            System.out.println("  0. Volver al menu principal");
            opcion = leerEntero("Seleccione una opcion: ");
            switch (opcion) {
                case 1 -> opRegistrarGasto();
                case 2 -> opListarGastos();
                case 3 -> opAnularGasto();
                case 0 -> { }
                default -> System.out.println("Opcion no valida.");
            }
        } while (opcion != 0);
    }

    private void opRegistrarGasto() {
        intentar(() -> {
            String descripcion = leerTexto("Descripcion del gasto: ");
            double monto = leerDouble("Monto del gasto (mayor que cero): ");
            LocalDate fecha = leerFecha("Fecha del gasto (dd/mm/aaaa): ");
            CategoriaGasto categoria = seleccionarEnum("Categorias de gasto", CategoriaGasto.values());
            Gasto gasto = gastoController.registrar(descripcion, monto, fecha, categoria);
            System.out.println("Gasto registrado con id " + gasto.getIdGasto() + ".");
            verificarPresupuesto();
        });
    }

    private void opListarGastos() {
        intentar(() -> {
            List<Gasto> gastos = gastoController.listar();
            if (gastos.isEmpty()) {
                System.out.println("No hay gastos registrados.");
                return;
            }
            System.out.printf("%-5s %-30s %-12s %-12s %-14s %s%n",
                    "ID", "DESCRIPCION", "MONTO", "FECHA", "CATEGORIA", "ESTADO");
            for (Gasto gasto : gastos) {
                System.out.printf("%-5d %-30s %-12s %-12s %-14s %s%n",
                        gasto.getIdGasto(), gasto.getDescripcion(), dinero(gasto.getMonto()),
                        gasto.getFecha().format(FORMATO_FECHA), gasto.getCategoria(), gasto.getEstado());
            }
        });
    }

    private void opAnularGasto() {
        intentar(() -> {
            int id = leerEntero("Id del gasto a anular: ");
            gastoController.anular(id);
            System.out.println("Gasto " + id + " anulado. Ya no se incluye en los totales activos.");
            verificarPresupuesto();
        });
    }

    // ======================= REPORTES =======================

    private void menuReportes() {
        int opcion;
        do {
            System.out.println();
            Interfaz.encabezado("REPORTES Y RESUMEN ECONOMICO (RF-07 / RF-08)");
            System.out.println("  1. Resumen economico (ingresos, gastos, saldo)");
            System.out.println("  2. Reporte de ingresos y gastos (por periodo)");
            System.out.println("  3. Reporte de citas (por periodo)");
            System.out.println("  4. Reporte de atenciones (por periodo)");
            System.out.println("  5. Exportar reportes (PDF / Excel)");
            System.out.println("  0. Volver al menu principal");
            opcion = leerEntero("Seleccione una opcion: ");
            switch (opcion) {
                case 1 -> opResumenEconomico();
                case 2 -> opReporteIngresosGastos();
                case 3 -> opReporteCitas();
                case 4 -> opReporteAtenciones();
                case 5 -> opExportarReportes();
                case 0 -> { }
                default -> System.out.println("Opcion no valida.");
            }
        } while (opcion != 0);
    }

    private void opResumenEconomico() {
        intentar(() -> {
            ResumenContable resumen = reporteController.obtenerResumen();
            Interfaz.encabezado("RESUMEN ECONOMICO");
            System.out.println("Ingresos (pagos):          " + dinero(resumen.getIngresos()));
            System.out.println("Gastos activos:            " + dinero(resumen.getGastos()));
            System.out.println("Presupuesto del periodo:   " + dinero(resumen.getPresupuesto()));
            System.out.println("Saldo (ingresos - gastos): " + dinero(resumen.getSaldo()));
            System.out.println("Saldo del presupuesto:     " + dinero(resumen.getSaldoPresupuestal()));
            if (resumen.isExcedePresupuesto()) {
                System.out.println("ALERTA: Los gastos superan el presupuesto establecido.");
            }
        });
    }

    private void opReporteIngresosGastos() {
        intentar(() -> {
            LocalDate inicio = leerFecha("Fecha de inicio (dd/mm/aaaa): ");
            LocalDate fin = leerFecha("Fecha de fin (dd/mm/aaaa): ");
            Reporte reporte = reporteController.reporteIngresosYGastos(inicio, fin);
            Interfaz.encabezado("REPORTE DE INGRESOS Y GASTOS");
            System.out.println("Periodo: " + reporte.getFechaInicio().format(FORMATO_FECHA)
                    + " al " + reporte.getFechaFin().format(FORMATO_FECHA));
            System.out.println("DETALLE DE PAGOS:");
            for (Pago pago : reporte.getDetallePagos()) {
                System.out.printf("  id=%d  %-10s  %s  %s%n", pago.getIdPago(),
                        dinero(pago.getMonto()), pago.getFecha().format(FORMATO_FECHA), pago.getMetodo());
            }
            System.out.println("DETALLE DE GASTOS ACTIVOS:");
            for (Gasto gasto : reporte.getDetalleGastos()) {
                System.out.printf("  id=%d  %-35s  %-10s  %s%n", gasto.getIdGasto(),
                        gasto.getDescripcion(), dinero(gasto.getMonto()),
                        gasto.getFecha().format(FORMATO_FECHA));
            }
            System.out.println("Desglose por categoria:");
            for (Map.Entry<CategoriaGasto, Double> entrada : reporte.getGastosPorCategoria().entrySet()) {
                System.out.printf("  %-14s %s%n", entrada.getKey(), dinero(entrada.getValue()));
            }
            System.out.println("---------------------------------------------");
            System.out.println("Total ingresos:            " + dinero(reporte.getTotalIngresos()));
            System.out.println("Total gastos (activos):    " + dinero(reporte.getTotalGastos()));
            System.out.println("Saldo del periodo:         " + dinero(reporte.getSaldo()));
            System.out.println("Saldo del presupuesto:     " + dinero(reporte.getSaldoPresupuestal()));
            if (reporte.isExcedePresupuesto()) {
                System.out.println("ALERTA: Los gastos superan el presupuesto establecido.");
            }
        });
    }

    private void opReporteCitas() {
        intentar(() -> {
            LocalDate inicio = leerFecha("Fecha de inicio (dd/mm/aaaa): ");
            LocalDate fin = leerFecha("Fecha de fin (dd/mm/aaaa): ");
            Reporte reporte = reporteController.reporteCitas(inicio, fin);
            Interfaz.encabezado("REPORTE DE CITAS");
            System.out.println("Periodo: " + reporte.getFechaInicio().format(FORMATO_FECHA)
                    + " al " + reporte.getFechaFin().format(FORMATO_FECHA));
            System.out.println("Total de citas: " + reporte.getTotalCitas());
            System.out.print("Por estado: ");
            boolean primero = true;
            for (Map.Entry<EstadoCita, Integer> entrada : reporte.getCitasPorEstado().entrySet()) {
                if (!primero) {
                    System.out.print(", ");
                }
                System.out.print(entrada.getKey() + "=" + entrada.getValue());
                primero = false;
            }
            System.out.println();
            if (reporte.getDetalleCitas().isEmpty()) {
                System.out.println("No hay citas en el periodo.");
                return;
            }
            System.out.printf("%-5s %-12s %-5s %-25s %-25s %s%n",
                    "ID", "FECHA", "HORA", "PACIENTE", "MEDICO", "ESTADO");
            for (Cita c : reporte.getDetalleCitas()) {
                System.out.printf("%-5d %-12s %-5s %-25s %-25s %s%n",
                        c.getIdCita(), c.getFecha().format(FORMATO_FECHA),
                        c.getHora().format(FORMATO_HORA), c.getNombrePaciente(),
                        c.getNombreMedico(), c.getEstado());
            }
        });
    }

    private void opReporteAtenciones() {
        intentar(() -> {
            LocalDate inicio = leerFecha("Fecha de inicio (dd/mm/aaaa): ");
            LocalDate fin = leerFecha("Fecha de fin (dd/mm/aaaa): ");
            Reporte reporte = reporteController.reporteAtenciones(inicio, fin);
            Interfaz.encabezado("REPORTE DE ATENCIONES");
            System.out.println("Periodo: " + reporte.getFechaInicio().format(FORMATO_FECHA)
                    + " al " + reporte.getFechaFin().format(FORMATO_FECHA));
            System.out.println("Total de atenciones: " + reporte.getTotalAtenciones());
            if (reporte.getDetalleAtenciones().isEmpty()) {
                System.out.println("No hay atenciones en el periodo.");
                return;
            }
            System.out.printf("%-5s %-12s %-25s %-25s %s%n",
                    "ID", "FECHA", "PACIENTE", "MEDICO", "DIAGNOSTICO");
            for (Atencion a : reporte.getDetalleAtenciones()) {
                System.out.printf("%-5d %-12s %-25s %-25s %s%n",
                        a.getIdAtencion(), a.getFechaAtencion().format(FORMATO_FECHA),
                        a.getNombrePaciente(), a.getNombreMedico(), a.getDiagnostico());
            }
        });
    }

    // ======================= EXPORTAR REPORTES =======================

    private void opExportarReportes() {
        int opcion;
        do {
            System.out.println();
            Interfaz.encabezado("EXPORTAR REPORTES (PDF / EXCEL)");
            System.out.println("  1. Resumen economico");
            System.out.println("  2. Reporte de ingresos y gastos (por periodo)");
            System.out.println("  3. Reporte de citas (por periodo)");
            System.out.println("  4. Reporte de atenciones (por periodo)");
            System.out.println("  5. Dashboard (estadisticas)");
            System.out.println("  0. Volver al menu anterior");
            opcion = leerEntero("Seleccione una opcion: ");
            switch (opcion) {
                case 1 -> exportarResumen();
                case 2 -> exportarIngresosGastos();
                case 3 -> exportarCitas();
                case 4 -> exportarAtenciones();
                case 5 -> exportarDashboard();
                case 0 -> { }
                default -> System.out.println("Opcion no valida.");
            }
        } while (opcion != 0);
    }

    private List<FormatoExportacion> seleccionarFormato() {
        System.out.println("Formato de exportacion:");
        System.out.println("  1. PDF");
        System.out.println("  2. Excel");
        System.out.println("  3. Ambos (PDF y Excel)");
        int opcion = leerEntero("Seleccione: ");
        return switch (opcion) {
            case 1 -> List.of(FormatoExportacion.PDF);
            case 2 -> List.of(FormatoExportacion.EXCEL);
            case 3 -> Arrays.asList(FormatoExportacion.PDF, FormatoExportacion.EXCEL);
            default -> throw new IllegalArgumentException("Formato no valido.");
        };
    }

    private void exportarResumen() {
        intentar(() -> {
            for (FormatoExportacion formato : seleccionarFormato()) {
                Path archivo = reporteController.exportarResumen(formato);
                confirmarExportacion(archivo);
            }
        });
    }

    private void exportarIngresosGastos() {
        intentar(() -> {
            LocalDate inicio = leerFecha("Fecha de inicio (dd/mm/aaaa): ");
            LocalDate fin = leerFecha("Fecha de fin (dd/mm/aaaa): ");
            for (FormatoExportacion formato : seleccionarFormato()) {
                Path archivo = reporteController.exportarReporteIngresosYGastos(formato, inicio, fin);
                confirmarExportacion(archivo);
            }
        });
    }

    private void exportarCitas() {
        intentar(() -> {
            LocalDate inicio = leerFecha("Fecha de inicio (dd/mm/aaaa): ");
            LocalDate fin = leerFecha("Fecha de fin (dd/mm/aaaa): ");
            for (FormatoExportacion formato : seleccionarFormato()) {
                Path archivo = reporteController.exportarReporteCitas(formato, inicio, fin);
                confirmarExportacion(archivo);
            }
        });
    }

    private void exportarAtenciones() {
        intentar(() -> {
            LocalDate inicio = leerFecha("Fecha de inicio (dd/mm/aaaa): ");
            LocalDate fin = leerFecha("Fecha de fin (dd/mm/aaaa): ");
            for (FormatoExportacion formato : seleccionarFormato()) {
                Path archivo = reporteController.exportarReporteAtenciones(formato, inicio, fin);
                confirmarExportacion(archivo);
            }
        });
    }

    private void exportarDashboard() {
        intentar(() -> {
            for (FormatoExportacion formato : seleccionarFormato()) {
                Path archivo = dashboardController.exportarEstadisticas(formato);
                confirmarExportacion(archivo);
            }
        });
    }

    private void confirmarExportacion(Path archivo) {
        System.out.println("Reporte exportado correctamente: " + archivo.toAbsolutePath());
    }

    // ======================= PRESUPUESTO =======================

    private void opcionPresupuesto() {
        if (!Sesion.getRolActual().gestionarPresupuesto()) {
            Interfaz.error("Acceso denegado: su rol no tiene permiso para esta funcion.");
            return;
        }
        intentar(() -> {
            System.out.println("Presupuesto actual del periodo: "
                    + dinero(reporteController.obtenerPresupuesto()));
            double presupuesto = leerDouble("Nuevo presupuesto (mayor que cero; 0 = no cambiar): ");
            if (presupuesto > 0) {
                gastoController.actualizarPresupuesto(presupuesto);
                System.out.println("Presupuesto actualizado a " + dinero(presupuesto) + ".");
            }
            verificarPresupuesto();
        });
    }

    private void verificarPresupuesto() {
        if (reporteController.obtenerResumen().isExcedePresupuesto()) {
            System.out.println("ALERTA: Los gastos superan el presupuesto establecido.");
        }
    }

    // ======================= DASHBOARD =======================

    private void opcionDashboard() {
        intentar(() -> {
            Estadisticas e = dashboardController.obtenerEstadisticas();
            Interfaz.encabezado("DASHBOARD DE LA CLINICA");
            System.out.println(" Pacientes registrados : " + e.getTotalPacientes());
            System.out.println(" Medicos activos       : " + e.getTotalMedicos());
            System.out.println(" Citas registradas     : " + e.getTotalCitas());
            System.out.print(" Citas por estado      : ");
            boolean primero = true;
            for (Map.Entry<EstadoCita, Integer> entrada : e.getCitasPorEstado().entrySet()) {
                if (!primero) {
                    System.out.print(", ");
                }
                System.out.print(entrada.getKey() + "=" + entrada.getValue());
                primero = false;
            }
            System.out.println();
            System.out.println(" Atenciones realizadas : " + e.getTotalAtenciones());
            System.out.println(" Ingresos (pagos)      : " + dinero(e.getTotalIngresos()));
            System.out.println(" Gastos activos        : " + dinero(e.getTotalGastos()));
            System.out.println(" Presupuesto del periodo: " + dinero(e.getPresupuesto()));
            System.out.println(" Saldo (ingresos-gastos): " + dinero(e.getSaldo()));
            System.out.println(" Saldo del presupuesto : " + dinero(e.getSaldoPresupuestal()));
            if (e.isExcedePresupuesto()) {
                System.out.println(" ALERTA: Los gastos superan el presupuesto establecido.");
            }
        });
    }

    // ======================= USUARIOS =======================

    private void menuUsuarios() {
        if (!Sesion.getRolActual().gestionarUsuarios()) {
            Interfaz.error("Acceso denegado: su rol no tiene permiso para esta funcion.");
            return;
        }
        int opcion;
        do {
            System.out.println();
            Interfaz.encabezado("MODULO DE USUARIOS (LOGIN Y ACCESO)");
            System.out.println("  1. Registrar usuario");
            System.out.println("  2. Listar usuarios");
            System.out.println("  3. Editar datos de un usuario");
            System.out.println("  4. Cambiar clave de un usuario");
            System.out.println("  5. Activar o desactivar usuario");
            System.out.println("  0. Volver al menu principal");
            opcion = leerEntero("Seleccione una opcion: ");
            switch (opcion) {
                case 1 -> opRegistrarUsuario();
                case 2 -> opListarUsuarios();
                case 3 -> opEditarUsuario();
                case 4 -> opCambiarClaveUsuario();
                case 5 -> opActivarDesactivarUsuario();
                case 0 -> { }
                default -> System.out.println("Opcion no valida.");
            }
        } while (opcion != 0);
    }

    private void opRegistrarUsuario() {
        intentar(() -> {
            String nombreUsuario = leerTexto("Nombre de usuario: ");
            String nombreCompleto = leerTexto("Nombre completo: ");
            String clave = leerTexto("Clave (minimo 6 caracteres): ");
            String confirmacion = leerTexto("Confirme la clave: ");
            if (!clave.equals(confirmacion)) {
                throw new IllegalArgumentException("Las claves no coinciden.");
            }
            Rol rol = seleccionarEnum("Rol del usuario", Rol.values());
            Integer idMedico = null;
            if (rol == Rol.MEDICO) {
                idMedico = leerMedicoVinculado();
            }
            Usuario usuario = usuarioController.registrar(
                    nombreUsuario, nombreCompleto, clave, rol, idMedico);
            System.out.println("Usuario registrado correctamente con id " + usuario.getIdUsuario() + ".");
            System.out.println("La clave se almacena protegida (cifrada) en la base de datos.");
        });
    }

    private void opListarUsuarios() {
        intentar(() -> {
            List<Usuario> usuarios = usuarioController.listar();
            if (usuarios.isEmpty()) {
                System.out.println("No hay usuarios registrados.");
                return;
            }
            System.out.printf("%-5s %-15s %-30s %-15s %-25s %-8s%n",
                    "ID", "USUARIO", "NOMBRE COMPLETO", "ROL", "MEDICO VINCULADO", "ESTADO");
            for (Usuario u : usuarios) {
                System.out.printf("%-5d %-15s %-30s %-15s %-25s %-8s%n",
                        u.getIdUsuario(), u.getNombreUsuario(), u.getNombreCompleto(),
                        u.getRol(), nulo(u.getNombreMedicoVinculado()),
                        u.isActivo() ? "ACTIVO" : "INACTIVO");
            }
        });
    }

    private void opEditarUsuario() {
        intentar(() -> {
            int id = leerEntero("Id del usuario: ");
            Usuario actual = usuarioController.buscarPorId(id);
            System.out.println("Editando usuario: " + actual.getNombreUsuario()
                    + " [" + actual.getRol() + ", " + (actual.isActivo() ? "ACTIVO" : "INACTIVO") + "]");
            String nombreCompleto = leerTexto("Nuevo nombre completo: ");
            Rol rol = seleccionarEnum("Nuevo rol", Rol.values());
            Integer idMedico = null;
            if (rol == Rol.MEDICO) {
                idMedico = leerMedicoVinculado();
            }
            boolean activo = seleccionarEstadoActivo(actual.isActivo());
            usuarioController.actualizarDatos(id, nombreCompleto, rol, idMedico, activo,
                    Sesion.getIdUsuarioSesion());
            System.out.println("Usuario actualizado correctamente.");
        });
    }

    private void opCambiarClaveUsuario() {
        intentar(() -> {
            int id = leerEntero("Id del usuario: ");
            Usuario actual = usuarioController.buscarPorId(id);
            System.out.println("Cambiando clave de: " + actual.getNombreUsuario()
                    + " (" + actual.getNombreCompleto() + ").");
            String clave = leerTexto("Nueva clave (minimo 6 caracteres): ");
            String confirmacion = leerTexto("Confirme la nueva clave: ");
            if (!clave.equals(confirmacion)) {
                throw new IllegalArgumentException("Las claves no coinciden.");
            }
            usuarioController.cambiarClave(id, clave);
            System.out.println("Clave actualizada correctamente.");
        });
    }

    /** Cambio de contrasena del propio usuario en sesion (exige la clave actual). */
    private void opCambiarMiClave() {
        intentar(() -> {
            String claveActual = leerClave("Clave actual: ");
            String claveNueva = leerClave("Nueva clave (minimo 6 caracteres): ");
            String confirmacion = leerClave("Confirme la nueva clave: ");
            if (!claveNueva.equals(confirmacion)) {
                throw new IllegalArgumentException("Las nuevas claves no coinciden.");
            }
            usuarioController.cambiarMiClave(Sesion.getNombreUsuario(), claveActual, claveNueva);
            Interfaz.exito("Contrasena actualizada correctamente.");
        });
    }

    private void opActivarDesactivarUsuario() {
        intentar(() -> {
            int id = leerEntero("Id del usuario: ");
            Usuario actual = usuarioController.buscarPorId(id);
            System.out.println("Usuario: " + actual.getNombreUsuario() + " | Estado actual: "
                    + (actual.isActivo() ? "ACTIVO" : "INACTIVO"));
            int nuevo = leerEntero("Nuevo estado (1 = Activo, 2 = Inactivo): ");
            if (nuevo != 1 && nuevo != 2) {
                throw new IllegalArgumentException("Opcion no valida.");
            }
            boolean activo = nuevo == 1;
            usuarioController.cambiarEstado(id, activo, Sesion.getIdUsuarioSesion());
            System.out.println("Cuenta " + (activo ? "activada" : "desactivada") + " correctamente.");
        });
    }

    private Integer leerMedicoVinculado() {
        List<Medico> medicos = medicoController.listarActivos();
        if (medicos.isEmpty()) {
            throw new IllegalArgumentException("No hay medicos activos. Registre un medico primero.");
        }
        System.out.println("Medicos activos:");
        System.out.printf("%-5s %-28s %s%n", "ID", "NOMBRE", "ESPECIALIDAD");
        for (Medico m : medicos) {
            System.out.printf("%-5d %-28s %s%n", m.getIdMedico(), m.getNombre(), m.getEspecialidad());
        }
        int idMedico = leerEntero("Id del medico vinculado: ");
        boolean existe = false;
        for (Medico m : medicos) {
            if (m.getIdMedico() == idMedico) {
                existe = true;
                break;
            }
        }
        if (!existe) {
            throw new IllegalArgumentException("El medico seleccionado no existe o esta inactivo.");
        }
        return idMedico;
    }

    private boolean seleccionarEstadoActivo(boolean actual) {
        System.out.println("Nuevo estado (1 = Activo, 2 = Inactivo) [actual: "
                + (actual ? "ACTIVO" : "INACTIVO") + "]: ");
        int opcion = leerEntero("Seleccione: ");
        if (opcion != 1 && opcion != 2) {
            throw new IllegalArgumentException("Opcion no valida.");
        }
        return opcion == 1;
    }

    // ======================= HELPERS =======================

    private void intentar(Runnable operacion) {
        try {
            operacion.run();
        } catch (RuntimeException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return lector.nextLine().trim();
    }

    private int leerEntero(String mensaje) {
        while (true) {
            try {
                System.out.print(mensaje);
                return Integer.parseInt(lector.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Debe ingresar un numero entero valido.");
            }
        }
    }

    private double leerDouble(String mensaje) {
        while (true) {
            try {
                System.out.print(mensaje);
                double valor = Double.parseDouble(lector.nextLine().trim().replace(',', '.'));
                if (valor < 0) {
                    System.out.println("El valor no puede ser negativo.");
                    continue;
                }
                return valor;
            } catch (NumberFormatException e) {
                System.out.println("Debe ingresar un monto valido.");
            }
        }
    }

    private LocalDate leerFecha(String mensaje) {
        while (true) {
            try {
                System.out.print(mensaje);
                return LocalDate.parse(lector.nextLine().trim(), FORMATO_FECHA);
            } catch (DateTimeParseException e) {
                System.out.println("Formato incorrecto. Use dd/mm/aaaa.");
            }
        }
    }

    private LocalTime leerHora(String mensaje) {
        while (true) {
            try {
                System.out.print(mensaje);
                return LocalTime.parse(lector.nextLine().trim(), FORMATO_HORA);
            } catch (DateTimeParseException e) {
                System.out.println("Formato incorrecto. Use HH:mm.");
            }
        }
    }

    private <E extends Enum<E>> E seleccionarEnum(String titulo, E[] valores) {
        System.out.println(titulo + ":");
        for (int i = 0; i < valores.length; i++) {
            System.out.println("  " + (i + 1) + ". " + valores[i]);
        }
        int opcion = leerEntero("Seleccione: ");
        if (opcion < 1 || opcion > valores.length) {
            throw new IllegalArgumentException("Opcion no valida.");
        }
        return valores[opcion - 1];
    }

    private String nulo(String valor) {
        return valor == null ? "-" : valor;
    }

    private String dinero(double valor) {
        return "S/ " + String.format("%.2f", valor);
    }
}