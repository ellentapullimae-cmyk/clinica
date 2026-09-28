package clinica.presentacion.grafico;

import clinica.controlador.MedicoController;
import clinica.modelo.EstadoPersona;
import clinica.modelo.Medico;
import java.time.format.DateTimeFormatter;

/**
 * Modulo de medicos: registro, listado, edicion y cambio de estado.
 * Solo accesible para el rol ADMINISTRADOR (gestionarMedicos).
 */
public final class PanelMedicos extends PanelModulo {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MedicoController controlador;

    public PanelMedicos(VentanaPrincipal ventana, MedicoController controlador) {
        super(ventana);
        this.controlador = controlador;

        if (rol().gestionarMedicos()) {
            agregarBoton("Nuevo medico", Controles.TipoBoton.PRIMARIO,
                    e -> registrarMedico());
        }
        agregarBotonSeleccion("Ver", Controles.TipoBoton.SECUNDARIO, e -> verMedico());
        if (rol().gestionarMedicos()) {
            agregarBotonSeleccion("Editar", Controles.TipoBoton.SECUNDARIO,
                    e -> editarMedico());
            agregarBotonSeleccion("Cambiar estado", Controles.TipoBoton.NEUTRO,
                    e -> cambiarEstado());
        }
        configurarAncho(new int[]{50, 280, 110, 180, 130, 110});
    }

    @Override
    protected String[] columnas() {
        return new String[]{"ID", "NOMBRE", "DNI", "ESPECIALIDAD", "TELEFONO", "ESTADO"};
    }

    @Override
    protected int indiceColumnaEstado() {
        return 5;
    }

    @Override
    protected String titulo() {
        return "Medicos";
    }

    @Override
    protected String subtitulo() {
        return "Registro y control de medicos (profesional de la salud).";
    }

    @Override
    protected void recargarDatos() {
        vaciarTabla();
        for (Medico m : controlador.listar()) {
            agregarFila(m.getIdMedico(), nulo(m.getNombre()), nulo(m.getDni()),
                    nulo(m.getEspecialidad()), nulo(m.getTelefono()), m.getEstado().name());
        }
    }

    private void registrarMedico() {
        FormularioDialogo form = new FormularioDialogo(ventana, "Nuevo medico", 460);
        form.campo("Nombre y apellidos", new Controles.CampoTextoUI("Nombre y apellidos"));
        form.campo("DNI (8 digitos)", new Controles.CampoTextoUI("00000000"));
        form.campo("Especialidad", new Controles.CampoTextoUI("Especialidad"));
        form.campo("Telefono (opcional)", new Controles.CampoTextoUI("Telefono"));
        if (form.mostrar()) {
            try {
                Medico m = controlador.registrar(
                        Controles.texto(form.campo(0)),
                        Controles.texto(form.campo(1)),
                        Controles.texto(form.campo(2)),
                        Controles.texto(form.campo(3)));
                Controles.informacion(this, "Medico",
                        "Medico registrado correctamente con id " + m.getIdMedico() + ".");
                recargarDatos();
            } catch (RuntimeException ex) {
                Controles.error(this, "Error", ex.getMessage());
            }
        }
    }

    private void verMedico() {
        Object[] fila = filaSeleccionada();
        if (fila == null) {
            return;
        }
        int id = (int) fila[0];
        Medico m = controlador.buscarPorId(id);
        if (m == null) {
            Controles.error(this, "Medico", "No se encontro el medico.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("ID            : ").append(m.getIdMedico()).append('\n');
        sb.append("Nombre        : ").append(nulo(m.getNombre())).append('\n');
        sb.append("DNI           : ").append(nulo(m.getDni())).append('\n');
        sb.append("Especialidad  : ").append(nulo(m.getEspecialidad())).append('\n');
        sb.append("Telefono      : ").append(nulo(m.getTelefono())).append('\n');
        if (m.getFechaRegistro() != null) {
            sb.append("Fecha registro: ").append(m.getFechaRegistro().format(FECHA)).append('\n');
        }
        sb.append("Estado        : ").append(m.getEstado());
        Controles.panelTexto(this, "Detalle del medico", sb.toString());
    }

    private void editarMedico() {
        Object[] fila = filaSeleccionada();
        if (fila == null) {
            return;
        }
        int id = (int) fila[0];
        Medico actual = controlador.buscarPorId(id);
        if (actual == null) {
            Controles.error(this, "Medico", "No se encontro el medico.");
            return;
        }
        FormularioDialogo form = new FormularioDialogo(ventana,
                "Editar medico - " + actual.getNombre(), 460);
        Controles.CampoTextoUI nombreU = new Controles.CampoTextoUI("Nombre y apellidos");
        nombreU.setText(nulo(actual.getNombre()));
        Controles.CampoTextoUI espU = new Controles.CampoTextoUI("Especialidad");
        espU.setText(nulo(actual.getEspecialidad()));
        Controles.CampoTextoUI telU = new Controles.CampoTextoUI("Telefono opcional");
        telU.setText(nulo(actual.getTelefono()));
        form.campo("Nombre", nombreU);
        form.campo("Especialidad", espU);
        form.campo("Telefono", telU);
        if (form.mostrar()) {
            try {
                controlador.actualizar(id, Controles.texto(form.campo(0)),
                        Controles.texto(form.campo(1)), Controles.texto(form.campo(2)));
                Controles.informacion(this, "Medico", "Medico actualizado correctamente.");
                recargarDatos();
            } catch (RuntimeException ex) {
                Controles.error(this, "Error", ex.getMessage());
            }
        }
    }

    private void cambiarEstado() {
        Object[] fila = filaSeleccionada();
        if (fila == null) {
            return;
        }
        int id = (int) fila[0];
        Medico actual = controlador.buscarPorId(id);
        if (actual == null) {
            Controles.error(this, "Medico", "No se encontro el medico.");
            return;
        }
        EstadoPersona nuevo = actual.getEstado() == EstadoPersona.ACTIVO
                ? EstadoPersona.INACTIVO : EstadoPersona.ACTIVO;
        if (!Controles.confirmar(this, "Cambiar estado",
                "Cambiar el estado de \"" + actual.getNombre() + "\" a " + nuevo + "?")) {
            return;
        }
        try {
            controlador.cambiarEstado(id, nuevo);
            Controles.informacion(this, "Medico", "Estado actualizado a " + nuevo + ".");
            recargarDatos();
        } catch (RuntimeException ex) {
            Controles.error(this, "Error", ex.getMessage());
        }
    }

    private static String nulo(String v) {
        return v == null ? "" : v;
    }
}