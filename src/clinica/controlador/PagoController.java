package clinica.controlador;

import clinica.modelo.MetodoPago;
import clinica.modelo.Pago;
import clinica.servicio.PagoService;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador del modulo Pagos.
 */
public class PagoController {

    private final PagoService servicio;

    public PagoController() {
        this.servicio = new PagoService();
    }

    public Pago registrar(double monto, LocalDate fecha, MetodoPago metodo, Integer idAtencion) {
        return servicio.registrar(monto, fecha, metodo, idAtencion);
    }

    public List<Pago> listar() {
        return servicio.listar();
    }
}