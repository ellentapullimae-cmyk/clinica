package clinica.servicio;

import clinica.datos.ConfiguracionDAO;
import clinica.datos.GastoDAO;
import clinica.modelo.CategoriaGasto;
import clinica.modelo.EstadoGasto;
import clinica.modelo.Gasto;
import clinica.util.Validaciones;
import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de la entidad Gasto (RF-06).
 * Reglas de negocio:
 * - Todo gasto debe registrar descripcion, categoria, fecha y monto.
 * - El monto de un gasto debe ser mayor que cero.
 * - Los gastos anulados no se incluyen en el total de gastos activos.
 */
public class GastoService {

    private final GastoDAO gastoDAO;
    private final ConfiguracionDAO configuracionDAO;

    public GastoService() {
        this.gastoDAO = new GastoDAO();
        this.configuracionDAO = new ConfiguracionDAO();
    }

    public Gasto registrar(String descripcion, double monto, LocalDate fecha, CategoriaGasto categoria) {
        if (!Validaciones.esTextoValido(descripcion)) {
            throw new IllegalArgumentException("Regla de negocio: todo gasto debe registrar una descripcion.");
        }
        if (categoria == null) {
            throw new IllegalArgumentException("Regla de negocio: todo gasto debe registrar una categoria.");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("Regla de negocio: todo gasto debe registrar su fecha.");
        }
        if (!Validaciones.esMontoValido(monto)) {
            throw new IllegalArgumentException("Regla de negocio: el monto de un gasto debe ser mayor que cero.");
        }
        Gasto gasto = new Gasto(descripcion.trim(), monto, fecha, categoria);
        int id = gastoDAO.insertar(gasto);
        gasto.setIdGasto(id);
        return gasto;
    }

    public void anular(int idGasto) {
        Gasto gasto = gastoDAO.buscarPorId(idGasto);
        if (gasto == null) {
            throw new IllegalArgumentException("No existe un gasto con id " + idGasto + ".");
        }
        if (gasto.getEstado() == EstadoGasto.ANULADO) {
            throw new IllegalArgumentException("El gasto " + idGasto + " ya se encuentra anulado.");
        }
        gastoDAO.anular(idGasto);
    }

    public List<Gasto> listar() {
        return gastoDAO.listar();
    }

    public double obtenerPresupuesto() {
        return configuracionDAO.obtenerPresupuesto();
    }

    public void actualizarPresupuesto(double nuevoPresupuesto) {
        if (!Validaciones.esMontoValido(nuevoPresupuesto)) {
            throw new IllegalArgumentException(
                    "El presupuesto del periodo debe ser un monto mayor que cero.");
        }
        configuracionDAO.actualizarPresupuesto(nuevoPresupuesto);
    }
}