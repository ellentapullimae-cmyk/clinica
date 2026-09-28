package clinica.presentacion.grafico;

import clinica.controlador.CitaController;
import clinica.controlador.MedicoController;
import clinica.controlador.PacienteController;
import clinica.modelo.Cita;
import clinica.modelo.EstadoCita;
import clinica.modelo.Medico;
import clinica.modelo.Paciente;
import clinica.presentacion.Sesion;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.JComboBox;

/**
 * Modulo de citas: programacion, confirmacion, cancelacion, listado y
 * modificacion. Respeta las reglas de negocio del CitaService.
 */
public final class PanelCitas extends PanelModulo {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final CitaController controlador;
    private final PacienteController pacientes;
    private final MedicoController medicos;

    public PanelCitas(VentanaPrincipal ventana, CitaController controlador,
                      PacienteController pacientes, MedicoController medicos) {
        super(ventana);
        this.controlador = controlador;
        this.pacientes = pacientes;
        this.medicos = medicos;

        if (rol().programarCita() || Sesion.getRolActual().esMedico()) {
            /* La programacion la usa admin/recepcionista; el medico solo si su rol lo permite */
        }
        if (rol().programarCita()) {
            agregarBoton("Nueva cita", Controles.TipoBoton.PRIMARIO, e -> programarCita());
        }
        agregarBotonSeleccion("Ver", Controles.TipoBoton.SECUNDARIO, e -> verCita());
        if (rol().modificarCita()) {
            agregarBotonSeleccion("Modificar", Controles.TipoBoton.SECUNDARIO,
                    e -> modificarCita());
        }
        agregarBotonSeleccion("Confirmar", Controles.TipoBoton.NEUTRO, e -> confirmarCita());
        agregarBotonSeleccion("Cancelar", Controles.TipoBoton.PELIGRO, e -> cancelarCita());
        configurarAncho(new int[]{50, 110, 70, 220, 220, 160, 120, 200});
    }

    @Override
    protected String[] columnas() {
        return new String[]{"ID", "FECHA", "HORA", "PACIENTE", "MEDICO", "ESPECIALIDAD", "ESTADO", "OBSERVACION"};
    }

    @Override
    protected int indiceColumnaEstado() {
        return 6;
    }

    @Override
    protected String titulo() {
        return "Citas";
    }

    @Override
    protected String subtitulo() {
        return "Programacion y gestion de citas medicas.";
    }

    @Override
    protected void recargarDatos() {
        vaciarTabla();
        List<Cita> citas;
        if (Sesion.getRolActual().esMedico() && Sesion.getIdMedicoSesion() != null) {
            citas = controlador.listarPorMedico(Sesion.getIdMedicoSesion());
        } else {
            citas = controlador.listar();
        }
        for (Cita c : citas) {
            agregarFila(c.getIdCita(),
                    c.getFecha().format(FECHA),
                    c.getHora().format(HORA),
                    c.getNombrePaciente(),
                    c.getNombreMedico(),
                    c.getMedico() != null ? c.getMedico().getEspecialidad() : "",
                    c.getEstado().name(),
                    nulo(c.getObservacion()));
        }
    }

    private void programarCita() {
        List<Paciente> activosP = pacientes.listarActivos();
        if (activosP.isEmpty()) {
            Controles.error(this, "Citas",
                    "No hay pacientes activos. Registre un paciente primero.");
            return;
        }
        List<Medico> activosM = medicos.listarActivos();
        if (activosM.isEmpty()) {
            Controles.error(this, "Citas",
                    "No hay medicos activos. Registre un medico primero.");
            return;
        }
        FormularioDialogo form = new FormularioDialogo(ventana, "Nueva cita", 480);
        form.campo("Paciente", nuevoPacienteCombo(activosP));
        form.campo("Medico", nuevoMedicoCombo(activosM));
        form.campo("Fecha (dd/mm/aaaa)",
                nuevoFecha(LocalDate.now().plusDays(1).format(FECHA)));
        form.campo("Hora (HH:mm)", nuevoHora("08:00"));
        form.campo("Observacion (opcional)", new Controles.CampoTextoUI("Observacion"));
        if (form.mostrar()) {
            try {
                JComboBox<?> comboP = (JComboBox<?>) form.campo(0);
                JComboBox<?> comboM = (JComboBox<?>) form.campo(1);
                Paciente p = (Paciente) comboP.getSelectedItem();
                Medico m = (Medico) comboM.getSelectedItem();
                LocalDate fecha = parseFecha(Controles.texto(form.campo(2)));
                LocalTime hora = parseHora(Controles.texto(form.campo(3)));
                Cita cita = controlador.programar(p.getIdPaciente(), m.getIdMedico(),
                        fecha, hora, Controles.texto(form.campo(4)));
                Controles.informacion(this, "Cita",
                        "Cita programada con id " + cita.getIdCita()
                                + " (estado " + cita.getEstado() + ").");
                recargarDatos();
            } catch (RuntimeException ex) {
                Controles.error(this, "Error", ex.getMessage());
            }
        }
    }

    private void verCita() {
        Object[] fila = filaSeleccionada();
        if (fila == null) {
            return;
        }
        Cita c = controlador.buscarPorId((int) fila[0]);
        if (c == null) {
            Controles.error(this, "Cita", "No se encontro la cita.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("ID          : ").append(c.getIdCita()).append('\n');
        sb.append("Paciente    : ").append(c.getNombrePaciente()).append('\n');
        sb.append("Medico      : ").append(c.getNombreMedico()).append('\n');
        if (c.getMedico() != null) {
            sb.append("Especialidad: ").append(c.getMedico().getEspecialidad()).append('\n');
        }
        sb.append("Fecha       : ").append(c.getFecha().format(FECHA)).append('\n');
        sb.append("Hora        : ").append(c.getHora().format(HORA)).append('\n');
        sb.append("Estado      : ").append(c.getEstado()).append('\n');
        sb.append("Observacion : ").append(nulo(c.getObservacion()));
        Controles.panelTexto(this, "Detalle de la cita", sb.toString());
    }

    private void modificarCita() {
        Object[] fila = filaSeleccionada();
        if (fila == null) {
            return;
        }
        int id = (int) fila[0];
        Cita actual = controlador.buscarPorId(id);
        if (actual == null) {
            Controles.error(this, "Cita", "No se encontro la cita.");
            return;
        }
        FormularioDialogo form = new FormularioDialogo(ventana,
                "Modificar cita " + id + " - " + actual.getNombrePaciente(), 460);
        form.campo("Fecha (dd/mm/aaaa)", nuevoFecha(actual.getFecha().format(FECHA)));
        form.campo("Hora (HH:mm)", nuevoHora(actual.getHora().format(HORA)));

        JComboBox<String> estados = Controles.combo(new String[]{
                EstadoCita.PROGRAMADA.name(), EstadoCita.CONFIRMADA.name()});
        estados.setSelectedItem(actual.getEstado().name());
        form.campo("Estado", estados);

        Controles.CampoTextoUI obs = new Controles.CampoTextoUI("Observacion opcional");
        obs.setText(nulo(actual.getObservacion()));
        form.campo("Observacion", obs);
        if (form.mostrar()) {
            try {
                LocalDate fecha = parseFecha(Controles.texto(form.campo(0)));
                LocalTime hora = parseHora(Controles.texto(form.campo(1)));
                EstadoCita estado = EstadoCita.valueOf((String) estados.getSelectedItem());
                controlador.modificar(id, fecha, hora, estado, Controles.texto(form.campo(3)));
                Controles.informacion(this, "Cita", "Cita modificada correctamente.");
                recargarDatos();
            } catch (RuntimeException ex) {
                Controles.error(this, "Error", ex.getMessage());
            }
        }
    }

    private void confirmarCita() {
        Object[] fila = filaSeleccionada();
        if (fila == null) {
            return;
        }
        int id = (int) fila[0];
        if (!exigirCitaDeSesion(id)) {
            return;
        }
        try {
            controlador.confirmar(id);
            Controles.informacion(this, "Cita", "Cita " + id + " confirmada.");
            recargarDatos();
        } catch (RuntimeException ex) {
            Controles.error(this, "Error", ex.getMessage());
        }
    }

    private void cancelarCita() {
        Object[] fila = filaSeleccionada();
        if (fila == null) {
            return;
        }
        int id = (int) fila[0];
        if (!exigirCitaDeSesion(id)) {
            return;
        }
        if (!Controles.confirmar(this, "Cancelar cita",
                "Cancelar la cita " + id + "? Su horario quedara disponible.")) {
            return;
        }
        try {
            controlador.cancelar(id);
            Controles.informacion(this, "Cita", "Cita " + id + " cancelada.");
            recargarDatos();
        } catch (RuntimeException ex) {
            Controles.error(this, "Error", ex.getMessage());
        }
    }

    /** Si la sesion es de un medico, la cita debe pertenecer a ese medico. */
    private boolean exigirCitaDeSesion(int idCita) {
        if (Sesion.getRolActual().esMedico() && Sesion.getIdMedicoSesion() != null) {
            boolean propia = false;
            for (Cita c : controlador.listarPorMedico(Sesion.getIdMedicoSesion())) {
                if (c.getIdCita() == idCita) {
                    propia = true;
                    break;
                }
            }
            if (!propia) {
                Controles.error(this, "Citas",
                        "La cita debe pertenecer al medico en sesion.");
                return false;
            }
        }
        return true;
    }

    private JComboBox<Paciente> nuevoPacienteCombo(List<Paciente> lista) {
        JComboBox<Paciente> combo = new JComboBox<>();
        for (Paciente p : lista) {
            combo.addItem(p);
        }
        combo.setFont(Controles.fuente(13, java.awt.Font.PLAIN));
        combo.setRenderer(new javax.swing.plaf.basic.BasicComboBoxRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Paciente p) {
                    setText("id " + p.getIdPaciente() + " - " + p.getNombre());
                }
                return this;
            }
        });
        combo.setPreferredSize(new java.awt.Dimension(Controles.esc(260), Controles.esc(36)));
        return combo;
    }

    private JComboBox<Medico> nuevoMedicoCombo(List<Medico> lista) {
        JComboBox<Medico> combo = new JComboBox<>();
        for (Medico m : lista) {
            combo.addItem(m);
        }
        combo.setFont(Controles.fuente(13, java.awt.Font.PLAIN));
        combo.setRenderer(new javax.swing.plaf.basic.BasicComboBoxRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Medico m) {
                    setText("id " + m.getIdMedico() + " - " + m.getNombre()
                            + " (" + m.getEspecialidad() + ")");
                }
                return this;
            }
        });
        combo.setPreferredSize(new java.awt.Dimension(Controles.esc(260), Controles.esc(36)));
        return combo;
    }

    private Controles.CampoTextoUI nuevoFecha(String valor) {
        Controles.CampoTextoUI campo = new Controles.CampoTextoUI("dd/mm/aaaa");
        campo.setText(valor);
        return campo;
    }

    private Controles.CampoTextoUI nuevoHora(String valor) {
        Controles.CampoTextoUI campo = new Controles.CampoTextoUI("HH:mm");
        campo.setText(valor);
        return campo;
    }

    private static LocalDate parseFecha(String texto) {
        try {
            return LocalDate.parse(texto.trim(), FECHA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha invalida. Use dd/mm/aaaa.");
        }
    }

    private static LocalTime parseHora(String texto) {
        try {
            return LocalTime.parse(texto.trim(), HORA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Hora invalida. Use HH:mm.");
        }
    }

    private static String nulo(String v) {
        return v == null ? "" : v;
    }
}