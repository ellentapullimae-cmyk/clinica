package clinica.presentacion.grafico;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * Dialogo de formulario reutilizable: etiquetas + campos, botones
 * Aceptar/Cancelar y devolucion del resultado. Solo es interfaz; la logica
 * sigue en los controladores.
 */
public class FormularioDialogo {

    private final JDialog dialogo;
    private final JPanel cuerpo;
    private final List<JComponent> campos = new ArrayList<>();
    private boolean aceptado;

    public FormularioDialogo(Window titular, String titulo, int ancho) {
        Frame frame = null;
        if (titular instanceof Frame f) {
            frame = f;
        }
        this.dialogo = new JDialog(frame, titulo, Dialog.ModalityType.APPLICATION_MODAL);
        this.dialogo.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel contenido = new JPanel(new BorderLayout(0, Controles.esc(18)));
        contenido.setBackground(java.awt.Color.WHITE);
        contenido.setBorder(javax.swing.BorderFactory.createEmptyBorder(
                Controles.esc(24), Controles.esc(28), Controles.esc(22), Controles.esc(28)));

        this.cuerpo = new JPanel(new GridBagLayout());
        this.cuerpo.setOpaque(false);
        contenido.add(this.cuerpo, BorderLayout.CENTER);

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, Controles.esc(8), 0));
        pie.setOpaque(false);
        Controles.BotonUI aceptar = new Controles.BotonUI("Aceptar", Controles.TipoBoton.PRIMARIO);
        Controles.BotonUI cancelar = new Controles.BotonUI("Cancelar", Controles.TipoBoton.SECUNDARIO);
        aceptar.addActionListener(e -> {
            if (validar()) {
                aceptado = true;
                dialogo.dispose();
            }
        });
        cancelar.addActionListener(e -> dialogo.dispose());
        pie.add(aceptar);
        pie.add(cancelar);
        contenido.add(pie, BorderLayout.SOUTH);

        dialogo.setContentPane(contenido);
        dialogo.pack();
        dialogo.setMinimumSize(new java.awt.Dimension(Controles.esc(ancho),
                Controles.esc(90)));
        if (titular != null) {
            dialogo.setLocationRelativeTo(titular);
        } else {
            dialogo.setLocationRelativeTo(null);
        }
    }

    /** Agrega una fila etiqueta + campo. */
    public FormularioDialogo campo(String etiqueta, JComponent campo) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = campos.size();
        g.weightx = 0.0;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(0, 0, Controles.esc(6), Controles.esc(8));
        JLabel l = Controles.etiqueta(etiqueta, Controles.TEXTO_SUAVE, 12, java.awt.Font.BOLD);
        cuerpo.add(l, g);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = campos.size();
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, Controles.esc(6), 0);
        cuerpo.add(campo, c);
        campos.add(campo);
        return this;
    }

    /** Agrega un encabezado a lo ancho del formulario. */
    public FormularioDialogo encabezado(String texto) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 100 + campos.size();
        g.gridwidth = GridBagConstraints.REMAINDER;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(Controles.esc(4), 0, Controles.esc(8), 0);
        JLabel l = Controles.etiqueta(texto, Controles.MARINO, 15,
                java.awt.Font.BOLD);
        cuerpo.add(l, g);
        return this;
    }

    /** Devuelve un campo por indice (orden de adicion). */
    public JComponent campo(int indice) {
        return campos.get(indice);
    }

    public List<JComponent> campos() {
        return campos;
    }

    /** Validacion por defecto (sin restricciones). Sobrescribir en subclases. */
    protected boolean validar() {
        return true;
    }

    /** Muestra el dialogo de forma bloqueante. Devuelve true si se acepto. */
    public boolean mostrar() {
        dialogo.setVisible(true);
        return aceptado;
    }
}