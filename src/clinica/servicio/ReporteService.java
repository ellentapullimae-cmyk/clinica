package clinica.servicio;

import clinica.datos.AtencionDAO;
import clinica.datos.CitaDAO;
import clinica.datos.ConfiguracionDAO;
import clinica.datos.GastoDAO;
import clinica.datos.PagoDAO;
import clinica.modelo.Atencion;
import clinica.modelo.CategoriaGasto;
import clinica.modelo.Cita;
import clinica.modelo.EstadoCita;
import clinica.modelo.FormatoExportacion;
import clinica.modelo.Celda;
import clinica.modelo.Reporte;
import clinica.modelo.ResumenContable;
import clinica.util.ExportadorExcel;
import clinica.util.ExportadorPDF;
import clinica.util.UtilRutas;
import clinica.util.Validaciones;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de reportes y resumen economico (RF-07, RF-08).
 * Reglas: el saldo se calcula como ingresos menos gastos; los gastos
 * anulados no se incluyen; alerta cuando los gastos superan el presupuesto.
 */
public class ReporteService {

    private final PagoDAO pagoDAO;
    private final GastoDAO gastoDAO;
    private final ConfiguracionDAO configuracionDAO;
    private final CitaDAO citaDAO;
    private final AtencionDAO atencionDAO;

    public ReporteService() {
        this.pagoDAO = new PagoDAO();
        this.gastoDAO = new GastoDAO();
        this.configuracionDAO = new ConfiguracionDAO();
        this.citaDAO = new CitaDAO();
        this.atencionDAO = new AtencionDAO();
    }

    /** RF-08: Calcula el saldo disponible como ingresos menos gastos. */
    public ResumenContable obtenerResumen() {
        double ingresos = pagoDAO.totalIngresos();
        double gastos = gastoDAO.totalGastosActivos();
        double presupuesto = configuracionDAO.obtenerPresupuesto();
        double saldo = ingresos - gastos;
        double saldoPresupuestal = presupuesto - gastos;
        return new ResumenContable(ingresos, gastos, presupuesto, saldo, saldoPresupuestal,
                gastos > presupuesto);
    }

    /** RF-07: Reporte de ingresos y gastos del periodo. */
    public Reporte generarReporteIngresosYGastos(LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        Reporte reporte = new Reporte(inicio, fin, "INGRESOS_Y_GASTOS");
        double ingresos = pagoDAO.totalIngresosEnPeriodo(inicio, fin);
        double gastos = gastoDAO.totalGastosActivosEnPeriodo(inicio, fin);
        double presupuesto = configuracionDAO.obtenerPresupuesto();

        reporte.setTotalIngresos(ingresos);
        reporte.setTotalGastos(gastos);
        reporte.setPresupuesto(presupuesto);
        reporte.setSaldo(ingresos - gastos);
        reporte.setSaldoPresupuestal(presupuesto - gastos);
        reporte.setExcedePresupuesto(gastos > presupuesto);
        reporte.setGastosPorCategoria(gastoDAO.totalGastosActivosPorCategoria(inicio, fin));
        reporte.setDetallePagos(pagoDAO.listarEntreFechas(inicio, fin));
        reporte.setDetalleGastos(gastoDAO.listarActivosEntreFechas(inicio, fin));
        return reporte;
    }

    /** RF-07: Reporte de citas del periodo. */
    public Reporte generarReporteCitas(LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        Reporte reporte = new Reporte(inicio, fin, "CITAS");
        List<Cita> citas = citaDAO.listarEntreFechas(inicio, fin);
        reporte.setDetalleCitas(citas);
        reporte.setTotalCitas(citas.size());
        Map<EstadoCita, Integer> porEstado = new EnumMap<>(EstadoCita.class);
        for (EstadoCita estado : EstadoCita.values()) {
            porEstado.put(estado, 0);
        }
        for (Cita cita : citas) {
            porEstado.put(cita.getEstado(), porEstado.get(cita.getEstado()) + 1);
        }
        reporte.setCitasPorEstado(porEstado);
        return reporte;
    }

    /** RF-07: Reporte de atenciones del periodo. */
    public Reporte generarReporteAtenciones(LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        Reporte reporte = new Reporte(inicio, fin, "ATENCIONES");
        List<Atencion> atenciones = atencionDAO.listarEntreFechas(inicio, fin);
        reporte.setDetalleAtenciones(atenciones);
        reporte.setTotalAtenciones(atenciones.size());
        return reporte;
    }

    /** RF-09: Historial de atenciones de un paciente. */
    public List<Atencion> historialPorPaciente(int idPaciente) {
        return atencionDAO.listarPorPaciente(idPaciente);
    }

    public double obtenerPresupuesto() {
        return configuracionDAO.obtenerPresupuesto();
    }

    public void actualizarPresupuesto(double nuevoPresupuesto) {
        configuracionDAO.actualizarPresupuesto(nuevoPresupuesto);
    }

    private void validarRango(LocalDate inicio, LocalDate fin) {
        Validaciones.validarRangoFechas(inicio, fin);
    }

    // =====================================================================
    // EXPORTACION DE REPORTES A PDF Y EXCEL (RF-07)
    // =====================================================================

    public Path exportarResumenEconomico(FormatoExportacion formato) {
        ResumenContable r = obtenerResumen();
        List<List<Celda>> filas = new ArrayList<>();
        filas.add(fila(Celda.texto("Concepto"), Celda.texto("Monto")));
        filas.add(fila(Celda.texto("Ingresos (pagos)"), Celda.moneda(r.getIngresos())));
        filas.add(fila(Celda.texto("Gastos activos"), Celda.moneda(r.getGastos())));
        filas.add(fila(Celda.texto("Presupuesto del periodo"), Celda.moneda(r.getPresupuesto())));
        filas.add(fila(Celda.texto("Saldo (ingresos - gastos)"), Celda.moneda(r.getSaldo())));
        filas.add(fila(Celda.texto("Saldo del presupuesto"), Celda.moneda(r.getSaldoPresupuestal())));
        if (r.isExcedePresupuesto()) {
            filas.add(fila(Celda.texto("ALERTA"), Celda.texto("Los gastos superan el presupuesto establecido.")));
        }
        return guardar(formato, "resumen_economico", "RESUMEN ECONOMICO",
                "Sistema de Gestion para una Clinica - Ingresos, gastos y saldo", filas);
    }

    public Path exportarReporteIngresosYGastos(FormatoExportacion formato, LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        Reporte rep = generarReporteIngresosYGastos(inicio, fin);
        List<List<Celda>> filas = new ArrayList<>();
        filas.add(fila(Celda.texto("Periodo"),
                Celda.texto(rep.getFechaInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + " al " + rep.getFechaFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
        filas.add(fila(Celda.texto("Total ingresos"), Celda.moneda(rep.getTotalIngresos())));
        filas.add(fila(Celda.texto("Total gastos (activos)"), Celda.moneda(rep.getTotalGastos())));
        filas.add(fila(Celda.texto("Presupuesto del periodo"), Celda.moneda(rep.getPresupuesto())));
        filas.add(fila(Celda.texto("Saldo del periodo"), Celda.moneda(rep.getSaldo())));
        filas.add(fila(Celda.texto("Saldo del presupuesto"), Celda.moneda(rep.getSaldoPresupuestal())));
        if (rep.isExcedePresupuesto()) {
            filas.add(fila(Celda.texto("ALERTA"), Celda.texto("Los gastos superan el presupuesto establecido.")));
        }
        filas.add(fila(Celda.texto("")));
        filas.add(fila(Celda.texto("DETALLE DE PAGOS")));
        filas.add(fila(Celda.texto("Id"), Celda.texto("Monto"), Celda.texto("Fecha"), Celda.texto("Metodo")));
        for (clinica.modelo.Pago pago : rep.getDetallePagos()) {
            filas.add(fila(Celda.numero(pago.getIdPago()), Celda.moneda(pago.getMonto()),
                    Celda.texto(pago.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
                    Celda.texto(pago.getMetodo().name())));
        }
        filas.add(fila(Celda.texto("")));
        filas.add(fila(Celda.texto("DETALLE DE GASTOS ACTIVOS")));
        filas.add(fila(Celda.texto("Id"), Celda.texto("Descripcion"), Celda.texto("Monto"),
                Celda.texto("Fecha"), Celda.texto("Categoria")));
        for (clinica.modelo.Gasto gasto : rep.getDetalleGastos()) {
            filas.add(fila(Celda.numero(gasto.getIdGasto()), Celda.texto(gasto.getDescripcion()),
                    Celda.moneda(gasto.getMonto()),
                    Celda.texto(gasto.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
                    Celda.texto(gasto.getCategoria().name())));
        }
        filas.add(fila(Celda.texto("")));
        filas.add(fila(Celda.texto("DESGLOSE POR CATEGORIA")));
        filas.add(fila(Celda.texto("Categoria"), Celda.texto("Monto")));
        for (Map.Entry<CategoriaGasto, Double> entrada : rep.getGastosPorCategoria().entrySet()) {
            filas.add(fila(Celda.texto(entrada.getKey().name()), Celda.moneda(entrada.getValue())));
        }
        return guardar(formato, "reporte_ingresos_y_gastos", "REPORTE DE INGRESOS Y GASTOS",
                "Sistema de Gestion para una Clinica - Periodo "
                        + rep.getFechaInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + " al " + rep.getFechaFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                filas);
    }

    public Path exportarReporteCitas(FormatoExportacion formato, LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        Reporte rep = generarReporteCitas(inicio, fin);
        List<List<Celda>> filas = new ArrayList<>();
        filas.add(fila(Celda.texto("Total de citas"), Celda.numero(rep.getTotalCitas())));
        StringBuilder porEstado = new StringBuilder();
        for (Map.Entry<EstadoCita, Integer> entrada : rep.getCitasPorEstado().entrySet()) {
            if (porEstado.length() > 0) {
                porEstado.append(", ");
            }
            porEstado.append(entrada.getKey()).append("=").append(entrada.getValue());
        }
        filas.add(fila(Celda.texto("Por estado"), Celda.texto(porEstado.toString())));
        filas.add(fila(Celda.texto("")));
        filas.add(fila(Celda.texto("Id"), Celda.texto("Fecha"), Celda.texto("Hora"),
                Celda.texto("Paciente"), Celda.texto("Medico"), Celda.texto("Estado")));
        for (Cita c : rep.getDetalleCitas()) {
            filas.add(fila(Celda.numero(c.getIdCita()),
                    Celda.texto(c.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
                    Celda.texto(c.getHora().format(DateTimeFormatter.ofPattern("HH:mm"))),
                    Celda.texto(c.getNombrePaciente()), Celda.texto(c.getNombreMedico()),
                    Celda.texto(c.getEstado().name())));
        }
        return guardar(formato, "reporte_citas", "REPORTE DE CITAS",
                "Sistema de Gestion para una Clinica - Periodo "
                        + rep.getFechaInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + " al " + rep.getFechaFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                filas);
    }

    public Path exportarReporteAtenciones(FormatoExportacion formato, LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        Reporte rep = generarReporteAtenciones(inicio, fin);
        List<List<Celda>> filas = new ArrayList<>();
        filas.add(fila(Celda.texto("Total de atenciones"), Celda.numero(rep.getTotalAtenciones())));
        filas.add(fila(Celda.texto("")));
        filas.add(fila(Celda.texto("Id"), Celda.texto("Fecha"), Celda.texto("Paciente"),
                Celda.texto("Medico"), Celda.texto("Diagnostico")));
        for (Atencion a : rep.getDetalleAtenciones()) {
            filas.add(fila(Celda.numero(a.getIdAtencion()),
                    Celda.texto(a.getFechaAtencion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
                    Celda.texto(a.getNombrePaciente()), Celda.texto(a.getNombreMedico()),
                    Celda.texto(a.getDiagnostico())));
        }
        return guardar(formato, "reporte_atenciones", "REPORTE DE ATENCIONES",
                "Sistema de Gestion para una Clinica - Periodo "
                        + rep.getFechaInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + " al " + rep.getFechaFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                filas);
    }

    /** Aplica el formato elegido y guarda el archivo en la carpeta reportes/. */
    private Path guardar(FormatoExportacion formato, String nombre, String titulo,
                         String subtitulo, List<List<Celda>> filas) {
        String sello = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS").format(LocalDateTime.now());
        Path carpeta = UtilRutas.directorioReportes();
        boolean esPdf = formato == FormatoExportacion.PDF;
        Path destino = carpeta.resolve(nombre + "_" + sello + (esPdf ? ".pdf" : ".xlsx"));
        try {
            if (esPdf) {
                ExportadorPDF.exportarCeldas(titulo, subtitulo, filas, destino);
            } else {
                ExportadorExcel.exportarCeldas(filas, destino, nombre);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al exportar el reporte: " + e.getMessage(), e);
        }
        return destino;
    }

    private static List<Celda> fila(Celda... celdas) {
        List<Celda> lista = new ArrayList<>(celdas.length);
        for (Celda celda : celdas) {
            lista.add(celda);
        }
        return lista;
    }
}