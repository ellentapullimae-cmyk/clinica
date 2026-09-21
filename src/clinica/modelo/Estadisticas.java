package clinica.modelo;

import java.util.EnumMap;
import java.util.Map;

/**
 * Estadisticas generales del dashboard (RF-10):
 * conteo de pacientes, medicos, citas, atenciones, ingresos, gastos y saldos.
 */
public class Estadisticas {

    private int totalPacientes;
    private int totalMedicos;
    private int totalCitas;
    private Map<EstadoCita, Integer> citasPorEstado = new EnumMap<>(EstadoCita.class);
    private int totalAtenciones;
    private double totalIngresos;
    private double totalGastos;
    private double presupuesto;
    private double saldo;
    private double saldoPresupuestal;
    private boolean excedePresupuesto;

    public int getTotalPacientes() {
        return totalPacientes;
    }

    public void setTotalPacientes(int totalPacientes) {
        this.totalPacientes = totalPacientes;
    }

    public int getTotalMedicos() {
        return totalMedicos;
    }

    public void setTotalMedicos(int totalMedicos) {
        this.totalMedicos = totalMedicos;
    }

    public int getTotalCitas() {
        return totalCitas;
    }

    public void setTotalCitas(int totalCitas) {
        this.totalCitas = totalCitas;
    }

    public Map<EstadoCita, Integer> getCitasPorEstado() {
        return citasPorEstado;
    }

    public void setCitasPorEstado(Map<EstadoCita, Integer> citasPorEstado) {
        this.citasPorEstado = citasPorEstado;
    }

    public int getTotalAtenciones() {
        return totalAtenciones;
    }

    public void setTotalAtenciones(int totalAtenciones) {
        this.totalAtenciones = totalAtenciones;
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

    @Override
    public String toString() {
        return "Estadisticas{pacientes=" + totalPacientes
                + ", medicos=" + totalMedicos
                + ", citas=" + totalCitas
                + ", atenciones=" + totalAtenciones
                + ", ingresos=" + totalIngresos
                + ", gastos=" + totalGastos + '}';
    }
}