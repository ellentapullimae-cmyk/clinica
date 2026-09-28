package clinica.presentacion.grafico;

import clinica.controlador.GastoController;
import clinica.controlador.ReporteController;
import clinica.modelo.CategoriaGasto;
import clinica.modelo.Gasto;
import clinica.modelo.ResumenContable;
import java.awt.Font;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Modulo de gastos y presupuesto: registro, listado, anulacion y control
 * del presupuesto del periodo. Solo ADMINISTRADOR (gestionarGastos).
 */
public final class PanelGastos extends PanelModulo {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final GastoController controlador;
    private final ReporteController reportes;

    public PanelGastos(VentanaPrincipal ventana, GastoController controlador,
                       ReporteController reportes) {
        super(ventana);
        this.controlador = controlador;
        this.reportes = reportes;

        if (rol().gestionarGastos()) {
            agregarBoton("Nuevo gasto", Controles.TipoBoton.PRIMARIO, e -> registrarGasto());
        }
        agregarBotonSeleccion("Anular", Controles.TipoBoton.PELIGRO, e -> anularGasto());
        agregarBoton("Presupuesto", Controles.TipoBoton.SECUNDARIO,
                e -> gestionarPresupuesto());
        configurarAncho(new int[]{60, 320, 140, 120, 150, 120});
    }

    @Override
    protected String[] columnas() {
        return new String[]{"ID", "DESCRIPCION", "MONTO", "FECHA", "CATEGORIA", "ESTADO"};
    }

    @Override
    protected int indiceColumnaEstado() {
        return 5;
    }

    @Override
    protected String titulo() {
        return "Gastos";
    }

    @Override
    protected String subtitulo() {
        return "Registro y control de gastos de la clinica.";
    }

    @Override
    protected void recargarDatos() {
        vaciarTabla();
        for (Gasto gasto : controlador.listar()) {
            agregarFila(gasto.getIdGasto(), nulo(gasto.getDescripcion()),
                    Controles.moneda(gasto.getMonto()),
                    gasto.getFecha().format(FECHA),
                    gasto.getCategoria().name(),
                    gasto.getEstado().name());
        }
    }

    private void registrarGasto() {
        FormularioDialogo form = new FormularioDialogo(ventana, "Nuevo gasto", 460);
        form.campo("Descripcion", new Controles.CampoTextoUI("Descripcion del gasto"));
        form.campo("Monto (mayor que cero)", new Controles.CampoNumericoUI("0.00"));
        form.campo("Fecha (dd/mm/aaaa)", nuevoFecha(LocalDate.now().format(FECHA)));
        javax.swing.JComboBox<CategoriaGasto> categorias =
                new javax.swing.JComboBox<>(CategoriaGasto.values());
        categorias.setFont(Controles.fuente(13, Font.PLAIN));
        form.campo("Categoria", categorias);
        if (form.mostrar()) {
            try {
                double monto = Controles.numero(Controles.texto(form.campo(1)));
                LocalDate fecha = parseFecha(Controles.texto(form.campo(2)));
                CategoriaGasto categoria = (CategoriaGasto) categorias.getSelectedItem();
                Gasto gasto = controlador.registrar(
                        Controles.texto(form.campo(0)), monto, fecha, categoria);
                Controles.informacion(this, "Gasto",
                        "Gasto registrado con id " + gasto.getIdGasto() + ".");
                verificarPresupuesto();
                recargarDatos();
            } catch (RuntimeException ex) {
                Controles.error(this, "Error", ex.getMessage());
            }
        }
    }

    private void anularGasto() {
        Object[] fila = filaSeleccionada();
        if (fila == null) {
            return;
        }
        int id = (int) fila[0];
        if (!Controles.confirmar(this, "Anular gasto",
                "Anular el gasto " + id + "? Ya no se incluira en los totales activos.")) {
            return;
        }
        try {
            controlador.anular(id);
            Controles.informacion(this, "Gasto", "Gasto " + id + " anulado.");
            verificarPresupuesto();
            recargarDatos();
        } catch (RuntimeException ex) {
            Controles.error(this, "Error", ex.getMessage());
        }
    }

    private void gestionarPresupuesto() {
        FormularioDialogo form = new FormularioDialogo(ventana, "Presupuesto del periodo", 420);
        form.campo("Presupuesto actual", nuevoMonto(reportes.obtenerPresupuesto()));
        form.campo("Nuevo presupuesto (0 = no cambiar)", new Controles.CampoNumericoUI("0.00"));
        if (form.mostrar()) {
            try {
                double nuevo = Controles.numero(Controles.texto(form.campo(1)));
                if (nuevo > 0) {
                    controlador.actualizarPresupuesto(nuevo);
                    Controles.informacion(this, "Presupuesto",
                            "Presupuesto actualizado a " + Controles.moneda(nuevo) + ".");
                }
                verificarPresupuesto();
            } catch (RuntimeException ex) {
                Controles.error(this, "Error", ex.getMessage());
            }
        }
    }

    private void verificarPresupuesto() {
        ResumenContable r = reportes.obtenerResumen();
        if (r.isExcedePresupuesto()) {
            Controles.error(this, "Presupuesto",
                    "ALERTA: Los gastos superan el presupuesto establecido.");
        }
    }

    private Controles.CampoTextoUI nuevoFecha(String valor) {
        Controles.CampoTextoUI campo = new Controles.CampoTextoUI("dd/mm/aaaa");
        campo.setText(valor);
        return campo;
    }

    private Controles.CampoNumericoUI nuevoMonto(double valor) {
        Controles.CampoNumericoUI campo = new Controles.CampoNumericoUI("0.00");
        campo.setText(String.format("%.2f", valor));
        campo.setEnabled(false);
        return campo;
    }

    private static LocalDate parseFecha(String texto) {
        try {
            return LocalDate.parse(texto.trim(), FECHA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha invalida. Use dd/mm/aaaa.");
        }
    }

    private static String nulo(String v) {
        return v == null ? "" : v;
    }
}