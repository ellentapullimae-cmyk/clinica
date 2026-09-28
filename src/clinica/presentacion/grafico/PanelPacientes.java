package clinica.presentacion.grafico;

import clinica.controlador.PacienteController;
import clinica.modelo.EstadoPersona;
import clinica.modelo.Paciente;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Modulo de pacientes: registro, listado, edicion y cambio de estado.
 * Toda la logica de negocio se delega en PacienteService/PacienteController.
 */
public final class PanelPacientes extends PanelModulo {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PacienteController controlador;

    public PanelPacientes(VentanaPrincipal ventana, PacienteController controlador) {
        super(ventana);
        this.controlador = controlador;

        if (rol().registrarPaciente()) {
            agregarBoton("Nuevo paciente", Controles.TipoBoton.PRIMARIO,
                    e -> registrarPaciente());
        }
        agregarBotonSeleccion("Ver", Controles.TipoBoton.SECUNDARIO, e -> verPaciente());
        if (rol().actualizarPaciente()) {
            agregarBotonSeleccion("Editar", Controles.TipoBoton.SECUNDARIO,
                    e -> editarPaciente());
            agregarBotonSeleccion("Cambiar estado", Controles.TipoBoton.NEUTRO,
                    e -> cambiarEstado());
        }
        configurarAncho(new int[]{50, 300, 110, 130, 230, 110});
    }

    @Override
    protected String[] columnas() {
        return new String[]{"ID", "NOMBRE", "DNI", "TELEFONO", "CORREO", "ESTADO"};
    }

    @Override
    protected int indiceColumnaEstado() {
        return 5;
    }

    @Override
    protected String titulo() {
        return "Pacientes";
    }

    @Override
    protected String subtitulo() {
        return "Registro, consulta y control de pacientes.";
    }

    @Override
    protected void recargarDatos() {
        vaciarTabla();
        for (Paciente p : controlador.listar()) {
            agregarFila(p.getIdPaciente(), nulo(p.getNombre()), nulo(p.getDni()),
                    nulo(p.getTelefono()), nulo(p.getCorreo()), p.getEstado().name());
        }
    }

    private void registrarPaciente() {
        FormularioDialogo form = new FormularioDialogo(ventana, "Nuevo paciente", 460);
        form.campo("Nombre y apellidos", new Controles.CampoTextoUI("Nombre y apellidos"));
        form.campo("DNI (8 digitos)", new Controles.CampoTextoUI("00000000"));
        form.campo("Telefono (opcional)", new Controles.CampoTextoUI("Telefono"));
        form.campo("Correo (opcional)", new Controles.CampoTextoUI("correo@ejemplo.com"));
        form.campo("Direccion (opcional)", new Controles.CampoTextoUI("Direccion"));
        if (form.mostrar()) {
            try {
                Paciente p = controlador.registrar(
                        Controles.texto(form.campo(0)),
                        Controles.texto(form.campo(1)),
                        Controles.texto(form.campo(2)),
                        Controles.texto(form.campo(3)),
                        Controles.texto(form.campo(4)));
                Controles.informacion(this, "Paciente",
                        "Paciente registrado correctamente con id " + p.getIdPaciente() + ".");
                recargarDatos();
            } catch (RuntimeException ex) {
                Controles.error(this, "Error", ex.getMessage());
            }
        }
    }

    private void verPaciente() {
        Object[] fila = filaSeleccionada();
        if (fila == null) {
            return;
        }
        int id = (int) fila[0];
        Paciente p = controlador.buscarPorId(id);
        if (p == null) {
            Controles.error(this, "Paciente", "No se encontro el paciente.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("ID              : ").append(p.getIdPaciente()).append('\n');
        sb.append("Nombre          : ").append(nulo(p.getNombre())).append('\n');
        sb.append("DNI             : ").append(nulo(p.getDni())).append('\n');
        sb.append("Telefono        : ").append(nulo(p.getTelefono())).append('\n');
        sb.append("Correo          : ").append(nulo(p.getCorreo())).append('\n');
        sb.append("Direccion       : ").append(nulo(p.getDireccion())).append('\n');
        if (p.getFechaRegistro() != null) {
            sb.append("Fecha de registro: ").append(p.getFechaRegistro().format(FECHA)).append('\n');
        }
        sb.append("Estado          : ").append(p.getEstado());
        Controles.panelTexto(this, "Detalle del paciente", sb.toString());
    }

    private void editarPaciente() {
        Object[] fila = filaSeleccionada();
        if (fila == null) {
            return;
        }
        int id = (int) fila[0];
        Paciente actual = controlador.buscarPorId(id);
        if (actual == null) {
            Controles.error(this, "Paciente", "No se encontro el paciente.");
            return;
        }
        FormularioDialogo form = new FormularioDialogo(ventana,
                "Editar paciente - " + actual.getNombre(), 460);
        Controles.CampoTextoUI nombreU = new Controles.CampoTextoUI("Nombre");
        nombreU.setText(nulo(actual.getNombre()));
        Controles.CampoTextoUI telefonoU = new Controles.CampoTextoUI("Telefono opcional");
        telefonoU.setText(nulo(actual.getTelefono()));
        Controles.CampoTextoUI correoU = new Controles.CampoTextoUI("Correo opcional");
        correoU.setText(nulo(actual.getCorreo()));
        Controles.CampoTextoUI direccionU = new Controles.CampoTextoUI("Direccion opcional");
        direccionU.setText(nulo(actual.getDireccion()));
        form.campo("Nombre", nombreU);
        form.campo("Telefono", telefonoU);
        form.campo("Correo", correoU);
        form.campo("Direccion", direccionU);
        if (form.mostrar()) {
            try {
                controlador.actualizar(id, Controles.texto(form.campo(0)),
                        Controles.texto(form.campo(1)), Controles.texto(form.campo(2)),
                        Controles.texto(form.campo(3)));
                Controles.informacion(this, "Paciente", "Paciente actualizado correctamente.");
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
        Paciente actual = controlador.buscarPorId(id);
        if (actual == null) {
            Controles.error(this, "Paciente", "No se encontro el paciente.");
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
            Controles.informacion(this, "Paciente", "Estado actualizado a " + nuevo + ".");
            recargarDatos();
        } catch (RuntimeException ex) {
            Controles.error(this, "Error", ex.getMessage());
        }
    }

    private static String nulo(String v) {
        return v == null ? "" : v;
    }
}