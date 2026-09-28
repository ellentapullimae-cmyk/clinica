package clinica.presentacion.grafico;

import clinica.controlador.AtencionController;
import clinica.controlador.CitaController;
import clinica.controlador.DashboardController;
import clinica.controlador.PagoController;
import clinica.controlador.ReporteController;
import clinica.modelo.Atencion;
import clinica.modelo.Cita;
import clinica.modelo.EstadoCita;
import clinica.modelo.Estadisticas;
import clinica.modelo.ResumenContable;
import clinica.presentacion.Sesion;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;

/**
 * Dashboard de la clinica: tarjetas estadisticas, proximas citas, actividad
 * reciente y resumen economico. Solo lectura.
 */
public final class PanelDashboard extends JPanel implements Recargable {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final DashboardController dashboard;
    private final CitaController citas;
    private final AtencionController atenciones;
    private final PagoController pagos;
    private final ReporteController reportes;

    private final JLabel vPacientes = new JLabel("0");
    private final JLabel vMedicos = new JLabel("0");
    private final JLabel vCitasDia = new JLabel("0");
    private final JLabel vAtendidas = new JLabel("0");
    private final JLabel vPagos = new JLabel("0");
    private final JLabel vIngresos = new JLabel("S/ 0.00");
    private final JLabel vIngresosR = new JLabel("S/ 0.00");
    private final JLabel vGastosR = new JLabel("S/ 0.00");
    private final JLabel vSaldoR = new JLabel("S/ 0.00");
    private final JLabel vPresupuestoR = new JLabel("S/ 0.00");
    private final JLabel vAlerta = new JLabel("");

    private final DefaultTableModel citasProximas = new DefaultTableModel(
            new String[]{"HORA", "PACIENTE", "MEDICO", "ESPECIALIDAD", "ESTADO"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    private final DefaultTableModel actividad = new DefaultTableModel(
            new String[]{"FECHA", "PACIENTE", "DIAGNOSTICO"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    public PanelDashboard(VentanaPrincipal vp, DashboardController dashboard,
                          CitaController citas, AtencionController atenciones,
                          PagoController pagos, ReporteController reportes) {
        super(new BorderLayout());
        this.dashboard = dashboard;
        this.citas = citas;
        this.atenciones = atenciones;
        this.pagos = pagos;
        this.reportes = reportes;
        setBackground(Controles.FONDO);
        setBorder(BorderFactory.createEmptyBorder(
                Controles.esc(18), Controles.esc(24), Controles.esc(18), Controles.esc(24)));
        construir();
    }

    private void construir() {
        JPanel cuerpo = new JPanel(new GridBagLayout());
        cuerpo.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, Controles.esc(18), 0);
        cuerpo.add(encabezado(), g);

        g.gridy = 1;
        g.insets = new Insets(0, 0, Controles.esc(18), 0);
        cuerpo.add(tarjetas(), g);

        g.gridy = 2;
        g.weighty = 1.0;
        g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(0, 0, 0, Controles.esc(18));
        cuerpo.add(panelProximas(), g);

        g.gridx = 1;
        g.insets = new Insets(0, 0, 0, 0);
        cuerpo.add(panelActividad(), g);

        add(cuerpo, BorderLayout.CENTER);
    }

    private JPanel encabezado() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JPanel izq = new JPanel();
        izq.setOpaque(false);
        izq.setLayout(new BoxLayout(izq, BoxLayout.Y_AXIS));
        izq.add(Controles.etiqueta("Dashboard", Controles.MARINO, 22, Font.BOLD));
        izq.add(Controles.etiqueta("Bienvenido, " + Sesion.getNombreCompleto()
                + "  |  " + Sesion.getRolActual().name(), Controles.TEXTO_SUAVE, 12, Font.PLAIN));
        panel.add(izq, BorderLayout.CENTER);
        JPanel der = new JPanel();
        der.setOpaque(false);
        der.setLayout(new BoxLayout(der, BoxLayout.Y_AXIS));
        der.add(Controles.etiqueta("Fecha: " + LocalDate.now().format(FECHA),
                Controles.TEXTO, 13, Font.PLAIN));
        der.add(Controles.etiqueta("Hora: " + LocalTime.now().format(HORA),
                Controles.TEXTO, 13, Font.PLAIN));
        panel.add(der, BorderLayout.EAST);
        return panel;
    }

    private JPanel tarjetas() {
        JPanel fila = new JPanel(new GridBagLayout());
        fila.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.insets = new Insets(0, 0, 0, Controles.esc(14));
        fila.add(tarjeta("Pacientes registrados", vPacientes, Controles.Icono.PACIENTES,
                new Color(21, 101, 192)), g);
        g.gridx = 1;
        fila.add(tarjeta("Medicos activos", vMedicos, Controles.Icono.MEDICOS,
                new Color(13, 148, 136)), g);
        g.gridx = 2;
        fila.add(tarjeta("Citas del dia", vCitasDia, Controles.Icono.CITAS,
                new Color(180, 83, 9)), g);
        g.gridx = 3;
        fila.add(tarjeta("Atenciones", vAtendidas, Controles.Icono.ATENCIONES,
                new Color(22, 101, 52)), g);
        g.gridx = 4;
        fila.add(tarjeta("Pagos registrados", vPagos, Controles.Icono.PAGOS,
                new Color(126, 34, 206)), g);
        g.gridx = 5;
        g.insets = new Insets(0, 0, 0, 0);
        fila.add(tarjeta("Ingresos del periodo", vIngresos, Controles.Icono.REPORTES,
                new Color(154, 52, 18)), g);
        return fila;
    }

    private JPanel tarjeta(String titulo, JLabel valor, Controles.Icono icono, Color acento) {
        Controles.PanelTarjeta tarjeta = new Controles.PanelTarjeta();
        tarjeta.setLayout(new BorderLayout(Controles.esc(10), 0));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(
                Controles.esc(14), Controles.esc(14), Controles.esc(14), Controles.esc(14)));
        JLabel ic = new JLabel();
        ic.setIcon(iconoSimple(icono, acento));
        tarjeta.add(ic, BorderLayout.WEST);
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        valor.setFont(Controles.fuente(21, Font.BOLD));
        valor.setForeground(Controles.MARINO);
        textos.add(valor);
        textos.add(Controles.etiqueta(titulo, Controles.TEXTO_SUAVE, 11, Font.PLAIN));
        tarjeta.add(textos, BorderLayout.CENTER);
        return tarjeta;
    }

    /** Icono simple de color plano para las tarjetas. */
    private javax.swing.Icon iconoSimple(Controles.Icono icono, Color acento) {
        int tam = Controles.esc(32);
        return new javax.swing.ImageIcon(dibujarIcono(icono, tam, acento));
    }

    private java.awt.image.BufferedImage dibujarIcono(Controles.Icono icono, int tam,
                                                      Color acento) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                tam, tam, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(acento);
        Controles.pintarIcono(g, icono, tam);
        g.dispose();
        return img;
    }

    private JPanel panelProximas() {
        JPanel panel = contenedorTabla("Proximas citas");
        Controles.TablaUI tabla = new Controles.TablaUI(citasProximas, 4);
        tabla.setFillsViewportHeight(true);
        JScrollPane scroll = Controles.enrutador(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Controles.BORDE, 1));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel panelActividad() {
        JPanel contenedor = contenedorTabla("Actividad reciente");
        Controles.TablaUI tabla = new Controles.TablaUI(actividad, -1);
        tabla.setFillsViewportHeight(true);
        JScrollPane scroll = Controles.enrutador(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Controles.BORDE, 1));
        contenedor.add(scroll, BorderLayout.CENTER);

        Controles.PanelTarjeta resumen = new Controles.PanelTarjeta();
        resumen.setLayout(new BoxLayout(resumen, BoxLayout.Y_AXIS));
        resumen.setBorder(BorderFactory.createEmptyBorder(
                Controles.esc(12), Controles.esc(14), Controles.esc(12), Controles.esc(14)));
        resumen.add(Controles.etiqueta("Resumen economico", Controles.MARINO, 13, Font.BOLD));
        resumen.add(Box.createVerticalStrut(Controles.esc(6)));
        resumen.add(filaResumen("Ingresos", vIngresosR));
        resumen.add(filaResumen("Gastos", vGastosR));
        resumen.add(filaResumen("Saldo", vSaldoR));
        resumen.add(filaResumen("Presupuesto", vPresupuestoR));
        resumen.add(Box.createVerticalStrut(Controles.esc(6)));
        vAlerta.setFont(Controles.fuente(11, Font.BOLD));
        vAlerta.setForeground(Controles.ROJO);
        resumen.add(vAlerta);
        contenedor.add(resumen, BorderLayout.SOUTH);
        return contenedor;
    }

    private JPanel filaResumen(String clave, JLabel valor) {
        JPanel fila = new JPanel(new BorderLayout());
        fila.setOpaque(false);
        fila.setBorder(BorderFactory.createEmptyBorder(Controles.esc(2), 0,
                Controles.esc(2), 0));
        fila.add(Controles.etiqueta(clave, Controles.TEXTO_SUAVE, 12, Font.PLAIN),
                BorderLayout.WEST);
        valor.setFont(Controles.fuente(12, Font.BOLD));
        valor.setForeground(Controles.MARINO);
        fila.add(valor, BorderLayout.EAST);
        return fila;
    }

    private JPanel contenedorTabla(String titulo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel t = Controles.etiqueta(titulo, Controles.MARINO, 15, Font.BOLD);
        t.setBorder(BorderFactory.createEmptyBorder(0, 0, Controles.esc(10), 0));
        panel.add(t, BorderLayout.NORTH);
        return panel;
    }

    private List<Cita> citasSegunRol() {
        if (Sesion.getRolActual().esMedico() && Sesion.getIdMedicoSesion() != null) {
            return citas.listarPorMedico(Sesion.getIdMedicoSesion());
        }
        return citas.listar();
    }

    @Override
    public void recargar() {
        try {
            Estadisticas e = dashboard.obtenerEstadisticas();
            vPacientes.setText(String.valueOf(e.getTotalPacientes()));
            vMedicos.setText(String.valueOf(e.getTotalMedicos()));
            vCitasDia.setText(String.valueOf(contarCitasDelDia()));
            vAtendidas.setText(String.valueOf(e.getTotalAtenciones()));
            vPagos.setText(String.valueOf(pagos.listar().size()));
            vIngresos.setText(Controles.moneda(e.getTotalIngresos()));

            citasProximas.setRowCount(0);
            List<Cita> proximas = new ArrayList<>();
            for (Cita c : citasSegunRol()) {
                if (c.getEstado() == EstadoCita.PROGRAMADA
                        || c.getEstado() == EstadoCita.CONFIRMADA) {
                    proximas.add(c);
                }
            }
            proximas.sort(Comparator.comparing(Cita::getFecha).thenComparing(Cita::getHora));
            int tope = Math.min(proximas.size(), 8);
            for (int i = 0; i < tope; i++) {
                Cita c = proximas.get(i);
                citasProximas.addRow(new Object[]{
                        c.getHora().format(HORA), c.getNombrePaciente(),
                        c.getNombreMedico(),
                        c.getMedico() != null ? c.getMedico().getEspecialidad() : "",
                        c.getEstado().name()});
            }

            actividad.setRowCount(0);
            List<?> atencionesTotales = atenciones.listar();
            int topa = Math.min(atencionesTotales.size(), 6);
            for (int i = 0; i < topa; i++) {
                Atencion a = (Atencion) atencionesTotales.get(i);
                actividad.addRow(new Object[]{
                        a.getFechaAtencion().format(FECHA), a.getNombrePaciente(),
                        a.getDiagnostico()});
            }

            ResumenContable r = reportes.obtenerResumen();
            vIngresosR.setText(Controles.moneda(r.getIngresos()));
            vGastosR.setText(Controles.moneda(r.getGastos()));
            vSaldoR.setText(Controles.moneda(r.getSaldo()));
            vPresupuestoR.setText(Controles.moneda(r.getPresupuesto()));
            vAlerta.setText(r.isExcedePresupuesto()
                    ? "ALERTA: Los gastos superan el presupuesto establecido." : "");
        } catch (RuntimeException ex) {
            Controles.error(this, "Dashboard",
                    "No se pudo cargar el dashboard: " + ex.getMessage());
        }
    }

    private int contarCitasDelDia() {
        int n = 0;
        for (Cita c : citasSegunRol()) {
            if (c.getFecha().equals(LocalDate.now())
                    && c.getEstado() != EstadoCita.CANCELADA) {
                n++;
            }
        }
        return n;
    }
}