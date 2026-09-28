package clinica.presentacion.grafico;

import clinica.controlador.DashboardController;
import clinica.controlador.ReporteController;
import clinica.modelo.Atencion;
import clinica.modelo.Cita;
import clinica.modelo.EstadoCita;
import clinica.modelo.FormatoExportacion;
import clinica.modelo.Gasto;
import clinica.modelo.Pago;
import clinica.modelo.Reporte;
import clinica.modelo.ResumenContable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Modulo de reportes y resumen economico, con filtros de periodo y
 * exportacion a PDF/Excel. Reutiliza ReporteController/DashboardController.
 */
public final class PanelReportes extends JPanel implements Recargable {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReporteController reportes;
    private final DashboardController dashboard;

    private final JLabel resumenIngresos = new JLabel("S/ 0.00");
    private final JLabel resumenGastos = new JLabel("S/ 0.00");
    private final JLabel resumenSaldo = new JLabel("S/ 0.00");
    private final JLabel resumenPresupuesto = new JLabel("S/ 0.00");
    private final JLabel alerta = new JLabel(" ");
    private final Controles.CampoTextoUI campoInicio = new Controles.CampoTextoUI("dd/mm/aaaa");
    private final Controles.CampoTextoUI campoFin = new Controles.CampoTextoUI("dd/mm/aaaa");

    private LocalDate ultimoInicio;
    private LocalDate ultimoFin;

    public PanelReportes(VentanaPrincipal ventana, ReporteController reportes,
                         DashboardController dashboard) {
        super(new BorderLayout());
        this.reportes = reportes;
        this.dashboard = dashboard;
        setBackground(Controles.FONDO);
        setBorder(BorderFactory.createEmptyBorder(
                Controles.esc(20), Controles.esc(24), Controles.esc(18), Controles.esc(24)));
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
        cuerpo.add(new Controles.Encabezado("Reportes",
                "Resumen economico, reportes por periodo y exportacion."), g);

        g.gridy = 1;
        g.insets = new Insets(0, 0, Controles.esc(18), 0);
        cuerpo.add(tarjetasResumen(), g);

        g.gridy = 2;
        g.insets = new Insets(0, 0, Controles.esc(18), 0);
        cuerpo.add(filtrosPeriodo(), g);

        g.gridy = 3;
        g.insets = new Insets(0, 0, 0, 0);
        cuerpo.add(botonesAccion(), g);

        add(cuerpo, BorderLayout.NORTH);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        JLabel nota = Controles.etiqueta(
                "Los reportes por periodo usan las fechas indicadas arriba (dd/mm/aaaa).",
                Controles.TEXTO_SUAVE, 12, Font.PLAIN);
        nota.setBorder(BorderFactory.createEmptyBorder(Controles.esc(14), 0, 0, 0));
        pie.add(nota, BorderLayout.WEST);
        add(pie, BorderLayout.SOUTH);
    }

    private JPanel tarjetasResumen() {
        JPanel fila = new JPanel(new GridLayout(1, 4, Controles.esc(14), 0));
        fila.setOpaque(false);
        fila.add(tarjeta("Ingresos (pagos)", resumenIngresos, new Color(22, 163, 74)));
        fila.add(tarjeta("Gastos activos", resumenGastos, new Color(220, 38, 38)));
        fila.add(tarjeta("Saldo del periodo", resumenSaldo, new Color(37, 99, 235)));
        fila.add(tarjeta("Presupuesto", resumenPresupuesto, new Color(180, 126, 14)));
        return fila;
    }

    private JPanel tarjeta(String titulo, JLabel valor, Color acento) {
        Controles.PanelTarjeta tarjeta = new Controles.PanelTarjeta();
        tarjeta.setLayout(new BorderLayout(0, Controles.esc(6)));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(
                Controles.esc(14), Controles.esc(16), Controles.esc(14), Controles.esc(16)));
        JLabel k = Controles.etiqueta(titulo, Controles.TEXTO_SUAVE, 12, Font.BOLD);
        tarjeta.add(k, BorderLayout.NORTH);
        valor.setFont(Controles.fuente(18, Font.BOLD));
        valor.setForeground(acento);
        tarjeta.add(valor, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel filtrosPeriodo() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Controles.etiqueta("PERIODO DEL REPORTE", Controles.TEXTO_SUAVE,
                11, Font.BOLD));
        panel.add(Box.createVerticalStrut(Controles.esc(8)));
        JPanel fila = new JPanel();
        fila.setOpaque(false);
        fila.add(Controles.etiqueta("Desde:", Controles.TEXTO, 13, Font.PLAIN));
        fila.add(gap());
        campoInicio.setPreferredSize(new java.awt.Dimension(Controles.esc(150),
                Controles.esc(40)));
        campoFin.setPreferredSize(new java.awt.Dimension(Controles.esc(150),
                Controles.esc(40)));
        fila.add(campoInicio);
        fila.add(gap());
        fila.add(Controles.etiqueta("Hasta:", Controles.TEXTO, 13, Font.PLAIN));
        fila.add(gap());
        fila.add(campoFin);
        Controles.BotonUI aplicar = new Controles.BotonUI("Fijar periodo",
                Controles.TipoBoton.NEUTRO);
        aplicar.addActionListener(e -> fijarPeriodo());
        fila.add(gap());
        fila.add(aplicar);
        panel.add(fila);
        return panel;
    }

    private JPanel botonesAccion() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Controles.etiqueta("REPORTES POR PERIODO", Controles.TEXTO_SUAVE,
                11, Font.BOLD));
        panel.add(Box.createVerticalStrut(Controles.esc(8)));
        JPanel fila = new JPanel();
        fila.setOpaque(false);
        agregarBoton(fila, "Reporte ingresos y gastos", e -> reporteIngresosGastos());
        agregarBoton(fila, "Reporte de citas", e -> reporteCitas());
        agregarBoton(fila, "Reporte de atenciones", e -> reporteAtenciones());
        panel.add(fila);

        panel.add(Box.createVerticalStrut(Controles.esc(18)));
        panel.add(Controles.etiqueta("EXPORTAR (PDF / EXCEL)", Controles.TEXTO_SUAVE,
                11, Font.BOLD));
        panel.add(Box.createVerticalStrut(Controles.esc(8)));
        JPanel fila2 = new JPanel();
        fila2.setOpaque(false);
        agregarBoton(fila2, "Resumen economico PDF", e -> exportarResumen(FormatoExportacion.PDF));
        agregarBoton(fila2, "Resumen economico Excel",
                e -> exportarResumen(FormatoExportacion.EXCEL));
        agregarBoton(fila2, "Reporte ingresos/gastos PDF",
                e -> exportarIngresosGastos(FormatoExportacion.PDF));
        agregarBoton(fila2, "Reporte ingresos/gastos Excel",
                e -> exportarIngresosGastos(FormatoExportacion.EXCEL));
        agregarBoton(fila2, "Reporte citas PDF", e -> exportarCitas(FormatoExportacion.PDF));
        agregarBoton(fila2, "Reporte citas Excel", e -> exportarCitas(FormatoExportacion.EXCEL));
        agregarBoton(fila2, "Reporte atenciones PDF",
                e -> exportarAtenciones(FormatoExportacion.PDF));
        agregarBoton(fila2, "Reporte atenciones Excel",
                e -> exportarAtenciones(FormatoExportacion.EXCEL));
        agregarBoton(fila2, "Dashboard PDF", e -> exportarDashboard(FormatoExportacion.PDF));
        agregarBoton(fila2, "Dashboard Excel", e -> exportarDashboard(FormatoExportacion.EXCEL));
        panel.add(fila2);
        return panel;
    }

    private javax.swing.Box.Filler gap() {
        return (javax.swing.Box.Filler) Box.createHorizontalStrut(Controles.esc(8));
    }

    private void agregarBoton(JPanel fila, String texto, java.awt.event.ActionListener accion) {
        Controles.BotonUI b = new Controles.BotonUI(texto, Controles.TipoBoton.SECUNDARIO);
        b.addActionListener(accion);
        fila.add(b);
        fila.add(Box.createHorizontalStrut(Controles.esc(8)));
    }

    private void fijarPeriodo() {
        try {
            ultimoInicio = parseFecha(Controles.texto(campoInicio));
            ultimoFin = parseFecha(Controles.texto(campoFin));
            if (!ultimoInicio.isBefore(ultimoFin.plusDays(1))) {
                throw new IllegalArgumentException(
                        "La fecha de inicio debe ser anterior o igual a la de fin.");
            }
            Controles.informacion(this, "Periodo",
                    "Periodo fijado: " + ultimoInicio.format(FECHA)
                            + " al " + ultimoFin.format(FECHA) + ".");
        } catch (RuntimeException ex) {
            Controles.error(this, "Periodo", ex.getMessage());
        }
    }

    private void exigirPeriodo() {
        if (ultimoInicio == null || ultimoFin == null) {
            throw new IllegalArgumentException(
                    "Fije primero el periodo de fechas (boton 'Fijar periodo').");
        }
    }

    private void reporteIngresosGastos() {
        try {
            exigirPeriodo();
            Reporte r = reportes.reporteIngresosYGastos(ultimoInicio, ultimoFin);
            StringBuilder sb = new StringBuilder();
            sb.append("Periodo: ").append(r.getFechaInicio().format(FECHA))
                    .append(" al ").append(r.getFechaFin().format(FECHA)).append("\n\n");
            sb.append("DETALLE DE PAGOS:\n");
            for (Pago p : r.getDetallePagos()) {
                sb.append("  id=").append(p.getIdPago()).append("  ")
                        .append(Controles.moneda(p.getMonto())).append("  ")
                        .append(p.getFecha().format(FECHA)).append("  ")
                        .append(p.getMetodo()).append('\n');
            }
            sb.append("DETALLE DE GASTOS ACTIVOS:\n");
            for (Gasto gasto : r.getDetalleGastos()) {
                sb.append("  id=").append(gasto.getIdGasto()).append("  ")
                        .append(gasto.getDescripcion()).append("  ")
                        .append(Controles.moneda(gasto.getMonto())).append("  ")
                        .append(gasto.getFecha().format(FECHA)).append('\n');
            }
            sb.append("\nDesglose por categoria:\n");
            for (Map.Entry<clinica.modelo.CategoriaGasto, Double> e
                    : r.getGastosPorCategoria().entrySet()) {
                sb.append("  ").append(e.getKey()).append(": ")
                        .append(Controles.moneda(e.getValue())).append('\n');
            }
            sb.append("\n---------------------------------------------\n");
            sb.append("Total ingresos       : ").append(Controles.moneda(r.getTotalIngresos())).append('\n');
            sb.append("Total gastos (activos): ").append(Controles.moneda(r.getTotalGastos())).append('\n');
            sb.append("Saldo del periodo    : ").append(Controles.moneda(r.getSaldo())).append('\n');
            sb.append("Saldo del presupuesto: ").append(Controles.moneda(r.getSaldoPresupuestal())).append('\n');
            if (r.isExcedePresupuesto()) {
                sb.append("\nALERTA: Los gastos superan el presupuesto establecido.");
            }
            Controles.panelTexto(this, "Reporte de ingresos y gastos", sb.toString());
        } catch (RuntimeException ex) {
            Controles.error(this, "Reporte", ex.getMessage());
        }
    }

    private void reporteCitas() {
        try {
            exigirPeriodo();
            Reporte r = reportes.reporteCitas(ultimoInicio, ultimoFin);
            StringBuilder sb = new StringBuilder();
            sb.append("Periodo: ").append(r.getFechaInicio().format(FECHA))
                    .append(" al ").append(r.getFechaFin().format(FECHA)).append('\n');
            sb.append("Total de citas: ").append(r.getTotalCitas()).append('\n');
            sb.append("Por estado: ");
            boolean primero = true;
            for (Map.Entry<EstadoCita, Integer> e : r.getCitasPorEstado().entrySet()) {
                if (!primero) {
                    sb.append(", ");
                }
                sb.append(e.getKey()).append('=').append(e.getValue());
                primero = false;
            }
            sb.append("\n\n");
            if (r.getDetalleCitas().isEmpty()) {
                sb.append("No hay citas en el periodo.");
            } else {
                for (Cita c : r.getDetalleCitas()) {
                    sb.append(String.format("id=%-4d %-10s %-5s %-25s %-25s %s%n",
                            c.getIdCita(), c.getFecha().format(FECHA),
                            c.getHora().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                            c.getNombrePaciente(), c.getNombreMedico(), c.getEstado()));
                }
            }
            Controles.panelTexto(this, "Reporte de citas", sb.toString());
        } catch (RuntimeException ex) {
            Controles.error(this, "Reporte", ex.getMessage());
        }
    }

    private void reporteAtenciones() {
        try {
            exigirPeriodo();
            Reporte r = reportes.reporteAtenciones(ultimoInicio, ultimoFin);
            StringBuilder sb = new StringBuilder();
            sb.append("Periodo: ").append(r.getFechaInicio().format(FECHA))
                    .append(" al ").append(r.getFechaFin().format(FECHA)).append('\n');
            sb.append("Total de atenciones: ").append(r.getTotalAtenciones()).append("\n\n");
            if (r.getDetalleAtenciones().isEmpty()) {
                sb.append("No hay atenciones en el periodo.");
            } else {
                for (Atencion a : r.getDetalleAtenciones()) {
                    sb.append(String.format("id=%-4d %-10s %-25s %-25s %s%n",
                            a.getIdAtencion(), a.getFechaAtencion().format(FECHA),
                            a.getNombrePaciente(), a.getNombreMedico(),
                            a.getDiagnostico() == null ? "" : a.getDiagnostico()));
                }
            }
            Controles.panelTexto(this, "Reporte de atenciones", sb.toString());
        } catch (RuntimeException ex) {
            Controles.error(this, "Reporte", ex.getMessage());
        }
    }

    private void exportarResumen(FormatoExportacion formato) {
        try {
            Path archivo = reportes.exportarResumen(formato);
            confirmarExportacion(archivo);
        } catch (RuntimeException ex) {
            Controles.error(this, "Exportar", ex.getMessage());
        }
    }

    private void exportarIngresosGastos(FormatoExportacion formato) {
        try {
            exigirPeriodo();
            Path archivo = reportes.exportarReporteIngresosYGastos(formato,
                    ultimoInicio, ultimoFin);
            confirmarExportacion(archivo);
        } catch (RuntimeException ex) {
            Controles.error(this, "Exportar", ex.getMessage());
        }
    }

    private void exportarCitas(FormatoExportacion formato) {
        try {
            exigirPeriodo();
            Path archivo = reportes.exportarReporteCitas(formato, ultimoInicio, ultimoFin);
            confirmarExportacion(archivo);
        } catch (RuntimeException ex) {
            Controles.error(this, "Exportar", ex.getMessage());
        }
    }

    private void exportarAtenciones(FormatoExportacion formato) {
        try {
            exigirPeriodo();
            Path archivo = reportes.exportarReporteAtenciones(formato, ultimoInicio, ultimoFin);
            confirmarExportacion(archivo);
        } catch (RuntimeException ex) {
            Controles.error(this, "Exportar", ex.getMessage());
        }
    }

    private void exportarDashboard(FormatoExportacion formato) {
        try {
            Path archivo = dashboard.exportarEstadisticas(formato);
            confirmarExportacion(archivo);
        } catch (RuntimeException ex) {
            Controles.error(this, "Exportar", ex.getMessage());
        }
    }

    private void confirmarExportacion(Path archivo) {
        Controles.informacion(this, "Exportacion",
                "Reporte exportado correctamente: " + archivo.toAbsolutePath());
    }

    private static LocalDate parseFecha(String texto) {
        try {
            return LocalDate.parse(texto.trim(), FECHA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha invalida. Use dd/mm/aaaa.");
        }
    }

    @Override
    public void recargar() {
        try {
            ResumenContable r = reportes.obtenerResumen();
            resumenIngresos.setText(Controles.moneda(r.getIngresos()));
            resumenGastos.setText(Controles.moneda(r.getGastos()));
            resumenSaldo.setText(Controles.moneda(r.getSaldo()));
            resumenPresupuesto.setText(Controles.moneda(r.getPresupuesto()));
            alerta.setText(r.isExcedePresupuesto()
                    ? "ALERTA: Los gastos superan el presupuesto establecido." : " ");
        } catch (RuntimeException ex) {
            Controles.error(this, "Reportes",
                    "No se pudo cargar el resumen: " + ex.getMessage());
        }
    }
}