package clinica.controlador;

import clinica.modelo.Atencion;
import clinica.servicio.AtencionService;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador del modulo Atenciones.
 */
public class AtencionController {

    private final AtencionService servicio;

    public AtencionController() {
        this.servicio = new AtencionService();
    }

    public Atencion registrar(int idCita, String diagnostico, String observaciones, LocalDate fechaAtencion) {
        return servicio.registrar(idCita, diagnostico, observaciones, fechaAtencion);
    }

    public List<Atencion> listar() {
        return servicio.listar();
    }

    public List<Atencion> listarEntreFechas(LocalDate inicio, LocalDate fin) {
        return servicio.listarEntreFechas(inicio, fin);
    }

    public List<Atencion> historialPorPaciente(int idPaciente) {
        return servicio.historialPorPaciente(idPaciente);
    }
}