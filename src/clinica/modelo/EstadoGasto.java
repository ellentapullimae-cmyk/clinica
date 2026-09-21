package clinica.modelo;

/**
 * Estado de un gasto (entidad Gasto).
 * Regla de negocio: los gastos ANULADOS no se incluyen en los totales activos.
 */
public enum EstadoGasto {
    CONTABILIZADO,
    ANULADO
}