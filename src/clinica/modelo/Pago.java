package clinica.modelo;

import java.time.LocalDate;

/**
 * Entidad Pago.
 * Atributos del documento: id_pago, monto, fecha, metodo.
 * Regla de negocio: todo pago debe registrar un monto mayor que cero.
 */
public class Pago {

    private int idPago;
    private Integer idAtencion;
    private double monto;
    private LocalDate fecha;
    private MetodoPago metodo;

    public Pago() {
    }

    public Pago(double monto, LocalDate fecha, MetodoPago metodo) {
        this.monto = monto;
        this.fecha = fecha;
        this.metodo = metodo;
    }

    public Pago(Integer idAtencion, double monto, LocalDate fecha, MetodoPago metodo) {
        this.idAtencion = idAtencion;
        this.monto = monto;
        this.fecha = fecha;
        this.metodo = metodo;
    }

    public Pago(int idPago, Integer idAtencion, double monto, LocalDate fecha, MetodoPago metodo) {
        this.idPago = idPago;
        this.idAtencion = idAtencion;
        this.monto = monto;
        this.fecha = fecha;
        this.metodo = metodo;
    }

    public int getIdPago() {
        return idPago;
    }

    public void setIdPago(int idPago) {
        this.idPago = idPago;
    }

    public Integer getIdAtencion() {
        return idAtencion;
    }

    public void setIdAtencion(Integer idAtencion) {
        this.idAtencion = idAtencion;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public MetodoPago getMetodo() {
        return metodo;
    }

    public void setMetodo(MetodoPago metodo) {
        this.metodo = metodo;
    }

    @Override
    public String toString() {
        return "Pago{id=" + idPago
                + ", monto=" + monto
                + ", fecha=" + fecha
                + ", metodo=" + metodo + '}';
    }
}