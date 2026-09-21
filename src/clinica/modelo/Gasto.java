package clinica.modelo;

import java.time.LocalDate;

/**
 * Entidad Gasto.
 * Atributos del documento: id_gasto, descripcion, monto, fecha, categoria, estado.
 */
public class Gasto {

    private int idGasto;
    private String descripcion;
    private double monto;
    private LocalDate fecha;
    private CategoriaGasto categoria;
    private EstadoGasto estado = EstadoGasto.CONTABILIZADO;

    public Gasto() {
    }

    public Gasto(String descripcion, double monto, LocalDate fecha, CategoriaGasto categoria) {
        this.descripcion = descripcion;
        this.monto = monto;
        this.fecha = fecha;
        this.categoria = categoria;
    }

    public Gasto(int idGasto, String descripcion, double monto, LocalDate fecha,
                 CategoriaGasto categoria, EstadoGasto estado) {
        this.idGasto = idGasto;
        this.descripcion = descripcion;
        this.monto = monto;
        this.fecha = fecha;
        this.categoria = categoria;
        this.estado = estado;
    }

    public int getIdGasto() {
        return idGasto;
    }

    public void setIdGasto(int idGasto) {
        this.idGasto = idGasto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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

    public CategoriaGasto getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaGasto categoria) {
        this.categoria = categoria;
    }

    public EstadoGasto getEstado() {
        return estado;
    }

    public void setEstado(EstadoGasto estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "Gasto{id=" + idGasto
                + ", descripcion='" + descripcion + '\''
                + ", monto=" + monto
                + ", fecha=" + fecha
                + ", categoria=" + categoria
                + ", estado=" + estado + '}';
    }
}