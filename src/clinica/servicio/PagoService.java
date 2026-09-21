package clinica.servicio;

import clinica.datos.AtencionDAO;
import clinica.datos.PagoDAO;
import clinica.modelo.MetodoPago;
import clinica.modelo.Pago;
import clinica.util.Validaciones;
import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de la entidad Pago (RF-05).
 * Regla: todo pago debe registrar un monto mayor que cero.
 */
public class PagoService {

    private final PagoDAO pagoDAO;
    private final AtencionDAO atencionDAO;

    public PagoService() {
        this.pagoDAO = new PagoDAO();
        this.atencionDAO = new AtencionDAO();
    }

    public Pago registrar(double monto, LocalDate fecha, MetodoPago metodo, Integer idAtencion) {
        if (fecha == null || metodo == null) {
            throw new IllegalArgumentException("La fecha y el metodo de pago son obligatorios.");
        }
        if (!Validaciones.esMontoValido(monto)) {
            throw new IllegalArgumentException("Regla de negocio: el monto de un pago debe ser mayor que cero.");
        }
        if (idAtencion != null && atencionDAO.buscarPorId(idAtencion) == null) {
            throw new IllegalArgumentException("La atencion con id " + idAtencion + " no existe.");
        }
        Pago pago = new Pago(idAtencion, monto, fecha, metodo);
        int id = pagoDAO.insertar(pago);
        pago.setIdPago(id);
        return pago;
    }

    public List<Pago> listar() {
        return pagoDAO.listar();
    }
}