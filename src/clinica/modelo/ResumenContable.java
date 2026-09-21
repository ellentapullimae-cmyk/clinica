package clinica.modelo;

/**
 * Resumen contable del periodo (RF-08).
 * Permite conocer ingresos, gastos, saldo y presupuesto de la clinica.
 */
public class ResumenContable {

    private final double ingresos;
    private final double gastos;
    private final double presupuesto;
    private final double saldo;
    private final double saldoPresupuestal;
    private final boolean excedePresupuesto;

    public ResumenContable(double ingresos, double gastos, double presupuesto,
                           double saldo, double saldoPresupuestal, boolean excedePresupuesto) {
        this.ingresos = ingresos;
        this.gastos = gastos;
        this.presupuesto = presupuesto;
        this.saldo = saldo;
        this.saldoPresupuestal = saldoPresupuestal;
        this.excedePresupuesto = excedePresupuesto;
    }

    public double getIngresos() {
        return ingresos;
    }

    public double getGastos() {
        return gastos;
    }

    public double getPresupuesto() {
        return presupuesto;
    }

    public double getSaldo() {
        return saldo;
    }

    public double getSaldoPresupuestal() {
        return saldoPresupuestal;
    }

    public boolean isExcedePresupuesto() {
        return excedePresupuesto;
    }

    @Override
    public String toString() {
        return "ResumenContable{ingresos=" + ingresos
                + ", gastos=" + gastos
                + ", presupuesto=" + presupuesto
                + ", saldo=" + saldo
                + ", excedePresupuesto=" + excedePresupuesto + '}';
    }
}