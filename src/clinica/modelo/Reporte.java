package clinica.modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Entidad Reporte (RF-07).
 * Atributos del documento: fecha_inicio, fecha_fin, tipo.
 * Contiene el resultado del reporte: ingresos, gastos y saldo del periodo.
 */
public class Reporte {

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String tipo;
    private double totalIngresos;
    private double totalGastos;
    private double presupuesto;
    private double saldo;
    private double saldoPresupuestal;
    private boolean excedePresupuesto;
    private Map<CategoriaGasto, Double> gastosPorCategoria = new EnumMap<>(CategoriaGasto.class);
    private List<Pago> detallePagos = new ArrayList<>();
    private List<Gasto> detalleGastos = new ArrayList<>();
    private List<Cita> detalleCitas = new ArrayList<>();
    private List<Atencion> detalleAtenciones = new ArrayList<>();
    private int totalCitas;
    private int totalAtenciones;
    private Map<EstadoCita, Integer> citasPorEstado = new EnumMap<>(EstadoCita.class);

    public Reporte(LocalDate fechaInicio, LocalDate fechaFin, String tipo) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.tipo = tipo;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public double getTotalIngresos() {
        return totalIngresos;
    }

    public void setTotalIngresos(double totalIngresos) {
        this.totalIngresos = totalIngresos;
    }

    public double getTotalGastos() {
        return totalGastos;
    }

    public void setTotalGastos(double totalGastos) {
        this.totalGastos = totalGastos;
    }

    public double getPresupuesto() {
        return presupuesto;
    }

    public void setPresupuesto(double presupuesto) {
        this.presupuesto = presupuesto;
    }

    public double getSaldo() {
        return saldo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }

    public double getSaldoPresupuestal() {
        return saldoPresupuestal;
    }

    public void setSaldoPresupuestal(double saldoPresupuestal) {
        this.saldoPresupuestal = saldoPresupuestal;
    }

    public boolean isExcedePresupuesto() {
        return excedePresupuesto;
    }

    public void setExcedePresupuesto(boolean excedePresupuesto) {
        this.excedePresupuesto = excedePresupuesto;
    }

    public Map<CategoriaGasto, Double> getGastosPorCategoria() {
        return gastosPorCategoria;
    }

    public void setGastosPorCategoria(Map<CategoriaGasto, Double> gastosPorCategoria) {
        this.gastosPorCategoria = gastosPorCategoria;
    }

    public List<Pago> getDetallePagos() {
        return detallePagos;
    }

    public void setDetallePagos(List<Pago> detallePagos) {
        this.detallePagos = detallePagos;
    }

    public List<Gasto> getDetalleGastos() {
        return detalleGastos;
    }

    public void setDetalleGastos(List<Gasto> detalleGastos) {
        this.detalleGastos = detalleGastos;
    }

    public List<Cita> getDetalleCitas() {
        return detalleCitas;
    }

    public void setDetalleCitas(List<Cita> detalleCitas) {
        this.detalleCitas = detalleCitas;
    }

    public List<Atencion> getDetalleAtenciones() {
        return detalleAtenciones;
    }

    public void setDetalleAtenciones(List<Atencion> detalleAtenciones) {
        this.detalleAtenciones = detalleAtenciones;
    }

    public int getTotalCitas() {
        return totalCitas;
    }

    public void setTotalCitas(int totalCitas) {
        this.totalCitas = totalCitas;
    }

    public int getTotalAtenciones() {
        return totalAtenciones;
    }

    public void setTotalAtenciones(int totalAtenciones) {
        this.totalAtenciones = totalAtenciones;
    }

    public Map<EstadoCita, Integer> getCitasPorEstado() {
        return citasPorEstado;
    }

    public void setCitasPorEstado(Map<EstadoCita, Integer> citasPorEstado) {
        this.citasPorEstado = citasPorEstado;
    }

    @Override
    public String toString() {
        return "Reporte{tipo=" + tipo
                + ", periodo=" + fechaInicio + " al " + fechaFin
                + ", ingresos=" + totalIngresos
                + ", gastos=" + totalGastos
                + ", saldo=" + saldo + '}';
    }
}