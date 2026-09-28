package clinica.presentacion.grafico;

import clinica.controlador.GastoController;
import clinica.controlador.ReporteController;
import clinica.modelo.ResumenContable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Configuracion del sistema: presupuesto del periodo.
 * Solo ADMINISTRADOR (gestionarPresupuesto).
 */
public final class PanelConfiguracion extends JPanel implements Recargable {

    private final ReporteController reportes;
    private final GastoController gastos;

    private final JLabel presupuestoActual = new JLabel("S/ 0.00");
    private final JLabel ingresos = new JLabel("S/ 0.00");
    private final JLabel gastosTotal = new JLabel("S/ 0.00");
    private final JLabel saldo = new JLabel("S/ 0.00");
    private final JLabel saldoPresupuestal = new JLabel("S/ 0.00");
    private final JLabel alerta = new JLabel(" ");
    private final Controles.CampoNumericoUI campoNuevo = new Controles.CampoNumericoUI("0.00");

    public PanelConfiguracion(VentanaPrincipal ventana, ReporteController reportes,
                              GastoController gastos) {
        super(new BorderLayout());
        this.reportes = reportes;
        this.gastos = gastos;
        setBackground(Controles.FONDO);
        setBorder(BorderFactory.createEmptyBorder(
                Controles.esc(20), Controles.esc(24), Controles.esc(18), Controles.esc(24)));
        construir();
    }

    private void construir() {
        JPanel cuerpo = new JPanel(new GridBagLayout());
        cuerpo.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, Controles.esc(18), 0);
        cuerpo.add(new Controles.Encabezado("Configuracion",
                "Presupuesto del periodo y control financiero."), g);

        g.gridy = 1;
        g.insets = new Insets(0, 0, Controles.esc(18), 0);
        cuerpo.add(tarjetas(), g);

        g.gridy = 2;
        g.insets = new Insets(0, 0, 0, 0);
        cuerpo.add(formularioPresupuesto(), g);

        add(cuerpo, BorderLayout.NORTH);
    }

    private JPanel tarjetas() {
        JPanel fila = new JPanel(new GridLayout(1, 4, Controles.esc(14), 0));
        fila.setOpaque(false);
        fila.add(tarjeta("Presupuesto actual", presupuestoActual, new Color(180, 126, 14)));
        fila.add(tarjeta("Ingresos (pagos)", ingresos, new Color(22, 163, 74)));
        fila.add(tarjeta("Gastos activos", gastosTotal, new Color(220, 38, 38)));
        fila.add(tarjeta("Saldo del presupuesto", saldoPresupuestal, new Color(37, 99, 235)));
        return fila;
    }

    private JPanel tarjeta(String titulo, JLabel valor, Color acento) {
        Controles.PanelTarjeta t = new Controles.PanelTarjeta();
        t.setLayout(new BorderLayout(0, Controles.esc(6)));
        t.setBorder(BorderFactory.createEmptyBorder(
                Controles.esc(14), Controles.esc(16), Controles.esc(14), Controles.esc(16)));
        t.add(Controles.etiqueta(titulo, Controles.TEXTO_SUAVE, 12, Font.BOLD),
                BorderLayout.NORTH);
        valor.setFont(Controles.fuente(18, Font.BOLD));
        valor.setForeground(acento);
        t.add(valor, BorderLayout.CENTER);
        return t;
    }

    private JPanel formularioPresupuesto() {
        Controles.PanelTarjeta panel = new Controles.PanelTarjeta();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(
                Controles.esc(16), Controles.esc(18), Controles.esc(16), Controles.esc(18)));
        panel.add(Controles.etiqueta("ACTUALIZAR PRESUPUESTO DEL PERIODO",
                Controles.MARINO, 13, Font.BOLD));
        panel.add(Box.createVerticalStrut(Controles.esc(10)));

        JPanel fila = new JPanel();
        fila.setOpaque(false);
        fila.add(Controles.etiqueta("Nuevo presupuesto (mayor que cero; 0 = no cambiar):",
                Controles.TEXTO, 13, Font.PLAIN));
        campoNuevo.setPreferredSize(new java.awt.Dimension(Controles.esc(160),
                Controles.esc(40)));
        fila.add(campoNuevo);
        Controles.BotonUI guardar = new Controles.BotonUI("Guardar presupuesto",
                Controles.TipoBoton.PRIMARIO);
        guardar.addActionListener(e -> actualizarPresupuesto());
        fila.add(Box.createHorizontalStrut(Controles.esc(10)));
        fila.add(guardar);
        panel.add(fila);

        panel.add(Box.createVerticalStrut(Controles.esc(10)));
        saldo.setFont(Controles.fuente(13, Font.BOLD));
        saldo.setForeground(Controles.MARINO);
        JPanel filaSaldo = new JPanel(new BorderLayout());
        filaSaldo.setOpaque(false);
        filaSaldo.add(Controles.etiqueta("Saldo (ingresos - gastos):",
                Controles.TEXTO_SUAVE, 12, Font.PLAIN), BorderLayout.WEST);
        filaSaldo.add(saldo, BorderLayout.EAST);
        panel.add(filaSaldo);

        panel.add(Box.createVerticalStrut(Controles.esc(8)));
        alerta.setFont(Controles.fuente(11, Font.BOLD));
        alerta.setForeground(Controles.ROJO);
        panel.add(alerta);
        return panel;
    }

    private void actualizarPresupuesto() {
        try {
            double valor = Controles.numero(Controles.texto(campoNuevo));
            if (valor < 0) {
                throw new IllegalArgumentException("El presupuesto no puede ser negativo.");
            }
            if (valor > 0) {
                gastos.actualizarPresupuesto(valor);
                Controles.informacion(this, "Presupuesto",
                        "Presupuesto actualizado a " + Controles.moneda(valor) + ".");
            }
            recargar();
        } catch (RuntimeException ex) {
            Controles.error(this, "Presupuesto", ex.getMessage());
        }
    }

    @Override
    public void recargar() {
        try {
            ResumenContable r = reportes.obtenerResumen();
            presupuestoActual.setText(Controles.moneda(r.getPresupuesto()));
            ingresos.setText(Controles.moneda(r.getIngresos()));
            gastosTotal.setText(Controles.moneda(r.getGastos()));
            saldo.setText(Controles.moneda(r.getSaldo()));
            saldoPresupuestal.setText(Controles.moneda(r.getSaldoPresupuestal()));
            alerta.setText(r.isExcedePresupuesto()
                    ? "ALERTA: Los gastos superan el presupuesto establecido." : " ");
        } catch (RuntimeException ex) {
            Controles.error(this, "Configuracion",
                    "No se pudo cargar el presupuesto: " + ex.getMessage());
        }
    }
}