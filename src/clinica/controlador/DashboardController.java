package clinica.controlador;

import clinica.modelo.Estadisticas;
import clinica.modelo.FormatoExportacion;
import clinica.servicio.DashboardService;
import java.nio.file.Path;

/**
 * Controlador del Dashboard de estadisticas (RF-10).
 */
public class DashboardController {

    private final DashboardService servicio;

    public DashboardController() {
        this.servicio = new DashboardService();
    }

    public Estadisticas obtenerEstadisticas() {
        return servicio.obtenerEstadisticas();
    }

    public Path exportarEstadisticas(FormatoExportacion formato) {
        return servicio.exportarEstadisticas(formato);
    }
}