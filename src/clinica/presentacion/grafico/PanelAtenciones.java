package clinica.presentacion.grafico;

import clinica.controlador.AtencionController;
import clinica.controlador.CitaController;
import clinica.controlador.PacienteController;
import clinica.modelo.Atencion;
import clinica.modelo.Cita;
import clinica.modelo.EstadoCita;
import clinica.modelo.Paciente;
import clinica.presentacion.Sesion;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;

/**
 * Modulo de atenciones: registro (ligado a una cita) y consulta del historial.
 * No inventa columnas: muestra los datos existentes (fecha, paciente, medico,
 * diagnostico y observaciones).
 */
public final class PanelAtenciones extends PanelModulo {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AtencionController controlador;
    private final CitaController citas;
    private final PacienteController pacientes;

    public PanelAtenciones(VentanaPrincipal ventana, AtencionController controlador,
                           CitaController citas, PacienteController pacientes) {
        super(ventana);
        this.controlador = controlador;
        this.citas = citas;
        this.pacientes = pacientes;

        if (rol().registrarAtencion()) {
            agregarBoton("Registrar atencion", Controles.TipoBoton.PRIMARIO,
                    e -> registrarAtencion());
        }
        agregarBoton("Historial por paciente", Controles.TipoBoton.SECUNDARIO,
                e -> historialPaciente());
        agregarBotonSeleccion("Ver", Controles.TipoBoton.NEUTRO, e -> verAtencion());
        configurarAncho(new int[]{60, 110, 230, 230, 300});
    }

    @Override
    protected String[] columnas() {
        return new String[]{"ID", "FECHA", "PACIENTE", "MEDICO", "DIAGNOSTICO"};
    }

    @Override
    protected int indiceColumnaEstado() {
        return -1;
    }

    @Override
    protected String titulo() {
        return "Atenciones";
    }

    @Override
    protected String subtitulo() {
        return "Registro de atenciones medicas ligadas a una cita.";
    }

    @Override
    protected void recargarDatos() {
        vaciarTabla();
        for (Atencion a : controlador.listar()) {
            agregarFila(a.getIdAtencion(),
                    a.getFechaAtencion().format(FECHA),
                    a.getNombrePaciente(),
                    a.getNombreMedico(),
                    nulo(a.getDiagnostico()));
        }
    }

    private void registrarAtencion() {
        List<Cita> atender = new ArrayList<>();
        for (Cita c : citasSegunRol()) {
            if (c.getEstado() == EstadoCita.PROGRAMADA
                    || c.getEstado() == EstadoCita.CONFIRMADA) {
                atender.add(c);
            }
        }
        if (atender.isEmpty()) {
            Controles.error(this, "Atenciones",
                    "No hay citas programadas ni confirmadas para atender.");
            return;
        }
        FormularioDialogo form = new FormularioDialogo(ventana, "Registrar atencion", 480);
        form.campo("Cita a atender", nuevoCitaCombo(atender));
        form.campo("Diagnostico", new Controles.CampoTextoUI("Diagnostico"));
        form.campo("Observaciones (opcional)",
                new Controles.CampoTextoUI("Observaciones"));
        form.campo("Fecha de atencion (dd/mm/aaaa)",
                nuevoFecha(LocalDate.now().format(FECHA)));
        if (form.mostrar()) {
            try {
                JComboBox<?> combo = (JComboBox<?>) form.campo(0);
                Cita cita = (Cita) combo.getSelectedItem();
                if (!exigirCitaDeSesion(cita.getIdCita())) {
                    return;
                }
                LocalDate fecha = parseFecha(Controles.texto(form.campo(3)));
                Atencion atencion = controlador.registrar(
                        cita.getIdCita(),
                        Controles.texto(form.campo(1)),
                        Controles.texto(form.campo(2)),
                        fecha);
                Controles.informacion(this, "Atencion",
                        "Atencion registrada con id " + atencion.getIdAtencion()
                                + ". La cita " + cita.getIdCita() + " quedo ATENDIDA.");
                recargarDatos();
            } catch (RuntimeException ex) {
                Controles.error(this, "Error", ex.getMessage());
            }
        }
    }

    private void historialPaciente() {
        List<Paciente> activos = pacientes.listarActivos();
        if (activos.isEmpty()) {
            Controles.error(this, "Atenciones", "No hay pacientes registrados.");
            return;
        }
        FormularioDialogo form = new FormularioDialogo(ventana,
                "Historial de atenciones por paciente", 420);
        form.campo("Paciente", nuevoPacienteCombo(activos));
        if (form.mostrar()) {
            try {
                JComboBox<?> combo = (JComboBox<?>) form.campo(0);
                Paciente paciente = (Paciente) combo.getSelectedItem();
                List<Atencion> historial = controlador.historialPorPaciente(paciente.getIdPaciente());
                if (historial.isEmpty()) {
                    Controles.informacion(this, "Historial",
                            "El paciente no tiene atenciones registradas.");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Historial de: ").append(paciente.getNombre())
                        .append(" (DNI ").append(nulo(paciente.getDni())).append(")\n\n");
                for (Atencion a : historial) {
                    sb.append("- ").append(a.getFechaAtencion().format(FECHA))
                            .append(" | Medico: ").append(a.getNombreMedico())
                            .append(" | Cita ").append(a.getCita() != null
                                    ? a.getCita().getIdCita() : "-")
                            .append("\n  Diagnostico: ").append(nulo(a.getDiagnostico())).append('\n');
                    if (a.getObservaciones() != null && !a.getObservaciones().isEmpty()) {
                        sb.append("  Observaciones: ").append(a.getObservaciones()).append('\n');
                    }
                }
                Controles.panelTexto(this, "Historial de atenciones", sb.toString());
            } catch (RuntimeException ex) {
                Controles.error(this, "Error", ex.getMessage());
            }
        }
    }

    private void verAtencion() {
        Object[] fila = filaSeleccionada();
        if (fila == null) {
            return;
        }
        Atencion a = buscarPorId((int) fila[0]);
        if (a == null) {
            Controles.error(this, "Atencion", "No se encontro la atencion.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("ID          : ").append(a.getIdAtencion()).append('\n');
        if (a.getCita() != null) {
            sb.append("Cita        : ").append(a.getCita().getIdCita()).append('\n');
        }
        sb.append("Paciente    : ").append(a.getNombrePaciente()).append('\n');
        sb.append("Medico      : ").append(a.getNombreMedico()).append('\n');
        sb.append("Fecha       : ").append(a.getFechaAtencion().format(FECHA)).append('\n');
        sb.append("Diagnostico : ").append(nulo(a.getDiagnostico())).append('\n');
        sb.append("Observaciones: ").append(nulo(a.getObservaciones()));
        Controles.panelTexto(this, "Detalle de la atencion", sb.toString());
    }

    private Atencion buscarPorId(int id) {
        for (Atencion a : controlador.listar()) {
            if (a.getIdAtencion() == id) {
                return a;
            }
        }
        return null;
    }

    private List<Cita> citasSegunRol() {
        if (Sesion.getRolActual().esMedico() && Sesion.getIdMedicoSesion() != null) {
            return citas.listarPorMedico(Sesion.getIdMedicoSesion());
        }
        return citas.listar();
    }

    private boolean exigirCitaDeSesion(int idCita) {
        if (Sesion.getRolActual().esMedico() && Sesion.getIdMedicoSesion() != null) {
            boolean propia = false;
            for (Cita c : citas.listarPorMedico(Sesion.getIdMedicoSesion())) {
                if (c.getIdCita() == idCita) {
                    propia = true;
                    break;
                }
            }
            if (!propia) {
                Controles.error(this, "Atenciones",
                        "La cita debe pertenecer al medico en sesion.");
                return false;
            }
        }
        return true;
    }

    private JComboBox<Cita> nuevoCitaCombo(List<Cita> lista) {
        JComboBox<Cita> combo = new JComboBox<>();
        for (Cita c : lista) {
            combo.addItem(c);
        }
        combo.setFont(Controles.fuente(13, java.awt.Font.PLAIN));
        combo.setRenderer(new javax.swing.plaf.basic.BasicComboBoxRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Cita c) {
                    setText("Cita " + c.getIdCita() + " - " + c.getNombrePaciente()
                            + " - " + c.getNombreMedico()
                            + " - " + c.getFecha().format(FECHA)
                            + " " + c.getHora().format(
                                    java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                            + " [" + c.getEstado() + "]");
                }
                return this;
            }
        });
        combo.setPreferredSize(new java.awt.Dimension(Controles.esc(300), Controles.esc(36)));
        return combo;
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

    private Controles.CampoTextoUI nuevoFecha(String valor) {
        Controles.CampoTextoUI campo = new Controles.CampoTextoUI("dd/mm/aaaa");
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

    private static String nulo(String v) {
        return v == null ? "" : v;
    }
}