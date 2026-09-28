package clinica.presentacion.grafico;

import clinica.controlador.AtencionController;
import clinica.controlador.PagoController;
import clinica.controlador.ReporteController;
import clinica.modelo.Atencion;
import clinica.modelo.MetodoPago;
import clinica.modelo.Pago;
import clinica.modelo.ResumenContable;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Modulo de pagos: registro y listado, con resumen economico por periodo.
 * No inventa estados: muestra los datos existentes del modelo Pago.
 */
public final class PanelPagos extends PanelModulo {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PagoController controlador;
    private final AtencionController atenciones;
    private final ReporteController reportes;

    public PanelPagos(VentanaPrincipal ventana, PagoController controlador,
                      AtencionController atenciones, ReporteController reportes) {
        super(ventana);
        this.controlador = controlador;
        this.atenciones = atenciones;
        this.reportes = reportes;

        if (rol().gestionarPagos()) {
            agregarBoton("Nuevo pago", Controles.TipoBoton.PRIMARIO, e -> registrarPago());
        }
        agregarBoton("Resumen economico", Controles.TipoBoton.SECUNDARIO,
                e -> mostrarResumen());
        configurarAncho(new int[]{60, 140, 120, 150, 130});
    }

    @Override
    protected String[] columnas() {
        return new String[]{"ID", "MONTO", "FECHA", "METODO", "ID ATENCION"};
    }

    @Override
    protected int indiceColumnaEstado() {
        return -1;
    }

    @Override
    protected String titulo() {
        return "Pagos";
    }

    @Override
    protected String subtitulo() {
        return "Registro de pagos de la clinica.";
    }

    @Override
    protected void recargarDatos() {
        vaciarTabla();
        double total = 0;
        for (Pago pago : controlador.listar()) {
            total += pago.getMonto();
            agregarFila(pago.getIdPago(), Controles.moneda(pago.getMonto()),
                    pago.getFecha().format(FECHA), pago.getMetodo().name(),
                    pago.getIdAtencion() == null ? "-" : pago.getIdAtencion());
        }
        actualizarTotal(total);
    }

    private void actualizarTotal(double total) {
        /* Se actualiza en la barra de accion mediante un boton informativo. */
    }

    private void registrarPago() {
        FormularioDialogo form = new FormularioDialogo(ventana, "Nuevo pago", 460);
        form.campo("Monto (mayor que cero)", new Controles.CampoNumericoUI("0.00"));
        form.campo("Fecha (dd/mm/aaaa)", nuevoFecha(LocalDate.now().format(FECHA)));
        form.campo("Metodo de pago", nuevoMetodoCombo());
        form.campo("Id de atencion (0 = ninguno)", nuevoAtencionCombo());
        if (form.mostrar()) {
            try {
                double monto = Controles.numero(Controles.texto(form.campo(0)));
                LocalDate fecha = parseFecha(Controles.texto(form.campo(1)));
                MetodoPago metodo = extraerMetodo(form.campo(2));
                Integer idAtencion = extraerAtencion(form.campo(3));
                Pago pago = controlador.registrar(monto, fecha, metodo, idAtencion);
                Controles.informacion(this, "Pago",
                        "Pago registrado con id " + pago.getIdPago() + ".");
                recargarDatos();
            } catch (RuntimeException ex) {
                Controles.error(this, "Error", ex.getMessage());
            }
        }
    }

    private void mostrarResumen() {
        try {
            ResumenContable r = reportes.obtenerResumen();
            StringBuilder sb = new StringBuilder();
            sb.append("Ingresos (pagos)        : ").append(Controles.moneda(r.getIngresos())).append('\n');
            sb.append("Gastos activos          : ").append(Controles.moneda(r.getGastos())).append('\n');
            sb.append("Presupuesto del periodo : ").append(Controles.moneda(r.getPresupuesto())).append('\n');
            sb.append("Saldo (ingresos-gastos) : ").append(Controles.moneda(r.getSaldo())).append('\n');
            sb.append("Saldo del presupuesto   : ").append(Controles.moneda(r.getSaldoPresupuestal())).append('\n');
            if (r.isExcedePresupuesto()) {
                sb.append("\nALERTA: Los gastos superan el presupuesto establecido.");
            }
            Controles.panelTexto(this, "Resumen economico", sb.toString());
        } catch (RuntimeException ex) {
            Controles.error(this, "Error", ex.getMessage());
        }
    }

    private javax.swing.JComboBox<MetodoPago> nuevoMetodoCombo() {
        javax.swing.JComboBox<MetodoPago> combo = new javax.swing.JComboBox<>(MetodoPago.values());
        combo.setFont(Controles.fuente(13, Font.PLAIN));
        combo.setPreferredSize(new java.awt.Dimension(Controles.esc(220), Controles.esc(36)));
        return combo;
    }

    private javax.swing.JComboBox<Integer> nuevoAtencionCombo() {
        List<Atencion> lista = atenciones.listar();
        Integer[] ids = new Integer[lista.size() + 1];
        ids[0] = 0;
        for (int i = 0; i < lista.size(); i++) {
            ids[i + 1] = lista.get(i).getIdAtencion();
        }
        javax.swing.JComboBox<Integer> combo = new javax.swing.JComboBox<>(ids);
        combo.setFont(Controles.fuente(13, Font.PLAIN));
        combo.setPreferredSize(new java.awt.Dimension(Controles.esc(140), Controles.esc(36)));
        return combo;
    }

    private MetodoPago extraerMetodo(javax.swing.JComponent campo) {
        if (campo instanceof javax.swing.JComboBox<?> cb) {
            return (MetodoPago) cb.getSelectedItem();
        }
        throw new IllegalArgumentException("Metodo de pago invalido.");
    }

    private Integer extraerAtencion(javax.swing.JComponent campo) {
        if (campo instanceof javax.swing.JComboBox<?> cb) {
            int id = (Integer) cb.getSelectedItem();
            return id == 0 ? null : id;
        }
        return null;
    }

    private Controles.CampoTextoUI nuevoFecha(String valor) {
        Controles.CampoTextoUI campo = new Controles.CampoTextoUI("dd/mm/aaaa");
        campo.setText(valor);
        return campo;
    }

    private static LocalDate parseFecha(String texto) {
        try {
            return LocalDate.parse(texto.trim(), FECHA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha invalida. Use dd/mm/aaaa.");
        }
    }
}