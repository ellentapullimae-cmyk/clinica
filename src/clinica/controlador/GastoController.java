package clinica.controlador;

import clinica.modelo.CategoriaGasto;
import clinica.modelo.Gasto;
import clinica.servicio.GastoService;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador del modulo Gastos y presupuesto.
 */
public class GastoController {

    private final GastoService servicio;

    public GastoController() {
        this.servicio = new GastoService();
    }

    public Gasto registrar(String descripcion, double monto, LocalDate fecha, CategoriaGasto categoria) {
        return servicio.registrar(descripcion, monto, fecha, categoria);
    }

    public void anular(int idGasto) {
        servicio.anular(idGasto);
    }

    public List<Gasto> listar() {
        return servicio.listar();
    }

    public double obtenerPresupuesto() {
        return servicio.obtenerPresupuesto();
    }

    public void actualizarPresupuesto(double nuevoPresupuesto) {
        servicio.actualizarPresupuesto(nuevoPresupuesto);
    }
}