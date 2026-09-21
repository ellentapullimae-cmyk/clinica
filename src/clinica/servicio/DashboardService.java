package clinica.servicio;

import clinica.datos.ConfiguracionDAO;
import clinica.datos.DashboardDAO;
import clinica.modelo.Celda;
import clinica.modelo.Estadisticas;
import clinica.modelo.EstadoCita;
import clinica.modelo.FormatoExportacion;
import clinica.util.ExportadorExcel;
import clinica.util.ExportadorPDF;
import clinica.util.UtilRutas;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Servicio del Dashboard (RF-10): estadisticas de pacientes, medicos,
 * citas, atenciones, ingresos y gastos.
 */
public class DashboardService {

    private final DashboardDAO dashboardDAO;
    private final ConfiguracionDAO configuracionDAO;

    public DashboardService() {
        this.dashboardDAO = new DashboardDAO();
        this.configuracionDAO = new ConfiguracionDAO();
    }

    public Estadisticas obtenerEstadisticas() {
        Estadisticas e = new Estadisticas();
        e.setTotalPacientes(dashboardDAO.contarPacientes());
        e.setTotalMedicos(dashboardDAO.contarMedicos());
        e.setTotalCitas(dashboardDAO.contarCitas());
        e.setCitasPorEstado(dashboardDAO.citasPorEstado());
        e.setTotalAtenciones(dashboardDAO.contarAtenciones());

        double ingresos = dashboardDAO.totalIngresos();
        double gastos = dashboardDAO.totalGastosActivos();
        double presupuesto = configuracionDAO.obtenerPresupuesto();

        e.setTotalIngresos(ingresos);
        e.setTotalGastos(gastos);
        e.setPresupuesto(presupuesto);
        e.setSaldo(ingresos - gastos);
        e.setSaldoPresupuestal(presupuesto - gastos);
        e.setExcedePresupuesto(gastos > presupuesto);
        return e;
    }

    /** Exporta las estadisticas del dashboard a PDF o Excel. */
    public Path exportarEstadisticas(FormatoExportacion formato) {
        Estadisticas e = obtenerEstadisticas();
        List<List<Celda>> filas = new ArrayList<>();
        filas.add(fila(Celda.texto("Indicador"), Celda.texto("Valor")));
        filas.add(fila(Celda.texto("Pacientes registrados"), Celda.numero(e.getTotalPacientes())));
        filas.add(fila(Celda.texto("Medicos activos"), Celda.numero(e.getTotalMedicos())));
        filas.add(fila(Celda.texto("Citas registradas"), Celda.numero(e.getTotalCitas())));
        StringBuilder porEstado = new StringBuilder();
        for (Map.Entry<EstadoCita, Integer> entrada : e.getCitasPorEstado().entrySet()) {
            if (porEstado.length() > 0) {
                porEstado.append(", ");
            }
            porEstado.append(entrada.getKey()).append("=").append(entrada.getValue());
        }
        filas.add(fila(Celda.texto("Citas por estado"), Celda.texto(porEstado.toString())));
        filas.add(fila(Celda.texto("Atenciones realizadas"), Celda.numero(e.getTotalAtenciones())));
        filas.add(fila(Celda.texto("Ingresos (pagos)"), Celda.moneda(e.getTotalIngresos())));
        filas.add(fila(Celda.texto("Gastos activos"), Celda.moneda(e.getTotalGastos())));
        filas.add(fila(Celda.texto("Presupuesto del periodo"), Celda.moneda(e.getPresupuesto())));
        filas.add(fila(Celda.texto("Saldo (ingresos - gastos)"), Celda.moneda(e.getSaldo())));
        filas.add(fila(Celda.texto("Saldo del presupuesto"), Celda.moneda(e.getSaldoPresupuestal())));
        if (e.isExcedePresupuesto()) {
            filas.add(fila(Celda.texto("ALERTA"), Celda.texto("Los gastos superan el presupuesto establecido.")));
        }

        String sello = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS").format(LocalDateTime.now());
        Path carpeta = UtilRutas.directorioReportes();
        boolean esPdf = formato == FormatoExportacion.PDF;
        Path destino = carpeta.resolve("dashboard_" + sello + (esPdf ? ".pdf" : ".xlsx"));
        try {
            if (esPdf) {
                ExportadorPDF.exportarCeldas("DASHBOARD - ESTADISTICAS DE LA CLINICA",
                        "Sistema de Gestion para una Clinica - Indicadores generales", filas, destino);
            } else {
                ExportadorExcel.exportarCeldas(filas, destino, "dashboard");
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error al exportar el dashboard: " + ex.getMessage(), ex);
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