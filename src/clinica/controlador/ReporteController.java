package clinica.controlador;

import clinica.modelo.Atencion;
import clinica.modelo.FormatoExportacion;
import clinica.modelo.Reporte;
import clinica.modelo.ResumenContable;
import clinica.servicio.ReporteService;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador del modulo de Reportes y resumen economico.
 */
public class ReporteController {

    private final ReporteService servicio;

    public ReporteController() {
        this.servicio = new ReporteService();
    }

    public ResumenContable obtenerResumen() {
        return servicio.obtenerResumen();
    }

    public Reporte reporteIngresosYGastos(LocalDate inicio, LocalDate fin) {
        return servicio.generarReporteIngresosYGastos(inicio, fin);
    }

    public Reporte reporteCitas(LocalDate inicio, LocalDate fin) {
        return servicio.generarReporteCitas(inicio, fin);
    }

    public Reporte reporteAtenciones(LocalDate inicio, LocalDate fin) {
        return servicio.generarReporteAtenciones(inicio, fin);
    }

    public List<Atencion> historialPorPaciente(int idPaciente) {
        return servicio.historialPorPaciente(idPaciente);
    }

    public double obtenerPresupuesto() {
        return servicio.obtenerPresupuesto();
    }

    public void actualizarPresupuesto(double nuevoPresupuesto) {
        servicio.actualizarPresupuesto(nuevoPresupuesto);
    }

    // ---- Exportacion a PDF / Excel ----

    public Path exportarResumen(FormatoExportacion formato) {
        return servicio.exportarResumenEconomico(formato);
    }

    public Path exportarReporteIngresosYGastos(FormatoExportacion formato, LocalDate inicio, LocalDate fin) {
        return servicio.exportarReporteIngresosYGastos(formato, inicio, fin);
    }

    public Path exportarReporteCitas(FormatoExportacion formato, LocalDate inicio, LocalDate fin) {
        return servicio.exportarReporteCitas(formato, inicio, fin);
    }

    public Path exportarReporteAtenciones(FormatoExportacion formato, LocalDate inicio, LocalDate fin) {
        return servicio.exportarReporteAtenciones(formato, inicio, fin);
    }
}