package clinica.presentacion;

import clinica.controlador.UsuarioController;
import clinica.modelo.Usuario;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

/**
 * Ventana grafica de inicio de sesion (Swing) con la imagen de la clinica como
 * panel lateral izquierdo y el formulario de acceso a la derecha.
 * Solo es interfaz: la autenticacion sigue usando UsuarioController y los
 * permisos del resto del sistema no se ven alterados.
 */
public final class LoginVentana {

    private static final int LIMITE_INTENTOS = 3;
    private static final int ANCHO_FORMULARIO = 460;
    private static final int UMBRAL_LATERAL = 760;

    private LoginVentana() {
    }

    /** Excepcion interna: el usuario cerro la ventana o supero los intentos. */
    public static final class SesionCancelada extends RuntimeException {
    }

    /**
     * Muestra el dialogo modal de acceso y devuelve el usuario autenticado.
     * Lanza {@link SesionCancelada} si se cancela o se agotan los intentos; si
     * no existe entorno grafico se propaga una excepcion para que el llamador
     * utilice el login de consola como respaldo.
     */
    public static Usuario autenticar(UsuarioController controlador) {
        final Usuario[] resultado = new Usuario[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                DialogoLogin dialogo = new DialogoLogin(controlador, resultado);
                dialogo.setVisible(true);
            });
        } catch (java.awt.HeadlessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo iniciar la interfaz grafica de acceso.", e);
        }
        if (resultado[0] == null) {
            throw new SesionCancelada();
        }
        return resultado[0];
    }

    /** Resultado de la verificacion del recurso grafico (sin abrir ventanas). */
    public static final class ImageInfo {
        public final Path ruta;
        public final int ancho;
        public final int alto;
        public final String error;

        ImageInfo(Path ruta, int ancho, int alto, String error) {
            this.ruta = ruta;
            this.ancho = ancho;
            this.alto = alto;
            this.error = error;
        }
    }

    /** Localiza y decodifica assets/images/clinica.png sin abrir ninguna ventana. */
    public static ImageInfo verificarRecurso() {
        Path ruta = ubicarImagen();
        if (ruta == null) {
            return new ImageInfo(null, 0, 0, "No se encontro assets/images/clinica.png");
        }
        try {
            BufferedImage bi = ImageIO.read(ruta.toFile());
            if (bi == null) {
                return new ImageInfo(ruta, 0, 0, "El archivo no es una imagen valida: " + ruta);
            }
            return new ImageInfo(ruta, bi.getWidth(), bi.getHeight(), null);
        } catch (IOException e) {
            return new ImageInfo(ruta, 0, 0, "No se pudo leer la imagen: " + e.getMessage());
        }
    }

    private static BufferedImage cargarImagen() {
        Path ruta = ubicarImagen();
        if (ruta == null) {
            return null;
        }
        try {
            return ImageIO.read(ruta.toFile());
        } catch (IOException e) {
            return null;
        }
    }

    private static Path ubicarImagen() {
        Path cursor = Path.of(System.getProperty("user.dir", "."));
        for (int i = 0; i < 5 && cursor != null; i++) {
            Path candidata = cursor.resolve("assets").resolve("images").resolve("clinica.png");
            if (Files.isRegularFile(candidata)) {
                return candidata;
            }
            cursor = cursor.getParent();
        }
        return null;
    }

    // ======================= INTERFAZ =======================

    private static final class DialogoLogin extends JDialog {

        private static final Color TEMA = new Color(20, 184, 166);
        private static final Color TEMA_OSCURO = new Color(15, 118, 110);
        private static final Color AZUL_MARINO = new Color(15, 48, 82);
        private static final Color TEXTO = new Color(30, 41, 59);
        private static final Color TEXTO_SUAVE = new Color(100, 116, 139);
        private static final Color FONDO_CAMPO = new Color(241, 245, 249);
        private static final Color COLOR_BORDE_CAMPO = new Color(226, 232, 240);
        private static final Color ROJO = new Color(220, 38, 38);

        private final UsuarioController controlador;
        private final Usuario[] resultado;
        private final JTextField campoUsuario = new JTextField();
        private final JPasswordField campoClave = new JPasswordField();
        private final JLabel etiquetaError = new JLabel(" ");
        private int intentosRestantes;

        DialogoLogin(UsuarioController controlador, Usuario[] resultado) {
            super((Frame) null, "Acceso al sistema", Dialog.ModalityType.APPLICATION_MODAL);
            this.controlador = controlador;
            this.resultado = resultado;
            this.intentosRestantes = LIMITE_INTENTOS;
            construirUI();
        }

        private void construirUI() {
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    cerrar(null);
                }

                @Override
                public void windowOpened(WindowEvent e) {
                    campoUsuario.requestFocusInWindow();
                }
            });

            PanelFondo raiz = new PanelFondo();
            raiz.setLayout(new GridBagLayout());

            final PanelImagen imgPanel = new PanelImagen(cargarImagen());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            raiz.add(imgPanel, c);

            JPanel contenedorFormulario = new JPanel(new GridBagLayout());
            contenedorFormulario.setOpaque(false);
            contenedorFormulario.add(construirTarjeta());
            c.gridx = 1;
            c.weightx = 0.0;
            c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            raiz.add(contenedorFormulario, c);

            raiz.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    boolean mostrar = raiz.getWidth() >= UMBRAL_LATERAL;
                    if (imgPanel.isVisible() != mostrar) {
                        imgPanel.setVisible(mostrar);
                        raiz.revalidate();
                        raiz.repaint();
                    }
                }
            });

            getRootPane().registerKeyboardAction(e -> cerrar(null),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);

            setUndecorated(false);
            setSize(1120, 680);
            setMinimumSize(new Dimension(480, 600));
            setLocationRelativeTo(null);
            setContentPane(raiz);
        }

        private JPanel construirTarjeta() {
            JPanel tarjeta = new JPanel();
            tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
            tarjeta.setBackground(Color.WHITE);
            tarjeta.setBorder(BorderFactory.createEmptyBorder(46, 44, 40, 44));
            Dimension contorno = new Dimension(ANCHO_FORMULARIO, 600);
            tarjeta.setPreferredSize(contorno);
            tarjeta.setMaximumSize(contorno);
            tarjeta.setMinimumSize(contorno);

            tarjeta.add(literal("ACCESO AL SISTEMA", TEMA, 12, Font.BOLD));
            tarjeta.add(Box.createVerticalStrut(14));
            tarjeta.add(literal("Inicio de sesion", AZUL_MARINO, 26, Font.BOLD));
            tarjeta.add(Box.createVerticalStrut(6));
            tarjeta.add(literal("Ingrese sus credenciales para continuar", TEXTO_SUAVE, 13, Font.PLAIN));

            tarjeta.add(Box.createVerticalStrut(30));
            tarjeta.add(literal("USUARIO", TEXTO_SUAVE, 11, Font.BOLD));
            tarjeta.add(Box.createVerticalStrut(6));
            configurarCampo(campoUsuario);
            tarjeta.add(campoUsuario);

            tarjeta.add(Box.createVerticalStrut(16));
            tarjeta.add(literal("CONTRASENA", TEXTO_SUAVE, 11, Font.BOLD));
            tarjeta.add(Box.createVerticalStrut(6));
            configurarCampo(campoClave);
            tarjeta.add(campoClave);

            tarjeta.add(Box.createVerticalStrut(18));
            etiquetaError.setAlignmentX(Component.CENTER_ALIGNMENT);
            etiquetaError.setForeground(ROJO);
            etiquetaError.setHorizontalAlignment(SwingConstants.CENTER);
            etiquetaError.setFont(fuente(12, Font.PLAIN));
            tarjeta.add(etiquetaError);

            tarjeta.add(Box.createVerticalStrut(10));
            JButton boton = new BotonPrimario("INICIAR SESION");
            boton.setAlignmentX(Component.CENTER_ALIGNMENT);
            boton.addActionListener(e -> intentarLogin());
            campoClave.addActionListener(e -> intentarLogin());
            campoUsuario.addActionListener(e -> campoClave.requestFocusInWindow());
            getRootPane().setDefaultButton(boton);
            tarjeta.add(boton);

            tarjeta.add(Box.createVerticalStrut(26));
            tarjeta.add(literal("Administrador | Recepcionista | Medico", TEXTO_SUAVE, 12, Font.PLAIN));
            return tarjeta;
        }

        private JLabel literal(String texto, Color color, int tamano, int estilo) {
            JLabel l = new JLabel(texto);
            l.setForeground(color);
            l.setFont(fuente(tamano, estilo));
            l.setAlignmentX(Component.CENTER_ALIGNMENT);
            return l;
        }

        private Font fuente(int tamano, int estilo) {
            return new Font("Segoe UI", estilo, tamano);
        }

        private void configurarCampo(JTextField campo) {
            campo.setFont(fuente(15, Font.PLAIN));
            campo.setForeground(TEXTO);
            campo.setCaretColor(TEMA);
            campo.setOpaque(false);
            campo.setPreferredSize(new Dimension(360, 46));
            campo.setMaximumSize(new Dimension(360, 46));
            Border borde = new BordeCampo(FONDO_CAMPO, COLOR_BORDE_CAMPO, TEMA, 12);
            campo.setBorder(BorderFactory.createCompoundBorder(borde, BorderFactory.createEmptyBorder(0, 14, 0, 14)));
        }

        private void intentarLogin() {
            String usuario = campoUsuario.getText().trim();
            String clave = new String(campoClave.getPassword());
            if (usuario.isEmpty() || clave.isEmpty()) {
                mostrarError("Ingrese usuario y contrasena.");
                return;
            }
            intentosRestantes--;
            try {
                Usuario autenticado = controlador.autenticar(usuario, clave);
                campoClave.setText("");
                cerrar(autenticado);
            } catch (RuntimeException ex) {
                campoClave.setText("");
                if (intentosRestantes <= 0) {
                    mostrarError("Acceso denegado. Se superaron los intentos.");
                    Timer temporizador = new Timer(false);
                    temporizador.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            SwingUtilities.invokeLater(() -> cerrar(null));
                            temporizador.cancel();
                        }
                    }, 1400);
                } else {
                    mostrarError(ex.getMessage() + "  (le quedan " + intentosRestantes + " intento(s))");
                }
            }
        }

        private void mostrarError(String mensaje) {
            etiquetaError.setText(mensaje);
            etiquetaError.setToolTipText(mensaje);
        }

        private void cerrar(Usuario usuario) {
            resultado[0] = usuario;
            dispose();
        }
    }

    /** Raiz con fondo en degradado (visible cuando se oculta el panel lateral). */
    private static final class PanelFondo extends JPanel {
        PanelFondo() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            if (getWidth() > 0 && getHeight() > 0) {
                g2.setPaint(new java.awt.LinearGradientPaint(0, 0, getWidth(), getHeight(),
                        new float[]{0f, 0.55f, 1f},
                        new Color[]{new Color(10, 38, 71), new Color(21, 78, 96), new Color(9, 24, 50)}));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();
        }
    }

    /** Panel lateral: dibuja la imagen escalada (cover) con fundido hacia el formulario. */
    private static final class PanelImagen extends JPanel {
        private final BufferedImage imagen;

        PanelImagen(BufferedImage imagen) {
            this.imagen = imagen;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            if (imagen != null) {
                double escala = Math.max((double) w / imagen.getWidth(), (double) h / imagen.getHeight());
                int iw = (int) Math.ceil(imagen.getWidth() * escala);
                int ih = (int) Math.ceil(imagen.getHeight() * escala);
                g2.drawImage(imagen, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
            } else {
                dibujarRespaldo(g2, w, h);
            }
            g2.setPaint(new java.awt.LinearGradientPaint(w * 0.82f, 0, w, 0,
                    new float[]{0f, 1f},
                    new Color[]{new Color(255, 255, 255, 0), new Color(255, 255, 255, 230)}));
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        }

        private void dibujarRespaldo(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(255, 255, 255, 20));
            int tam = 42;
            for (int x = 24; x < w; x += 90) {
                for (int y = 24; y < h; y += 120) {
                    g2.fillRoundRect(x - tam / 2, y - 6, tam, 12, 6, 6);
                    g2.fillRoundRect(x - 6, y - tam / 2, 12, tam, 6, 6);
                }
            }
            g2.setColor(new Color(240, 253, 250, 220));
            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D ecg = new Path2D.Double();
            ecg.moveTo(30, h * 0.70);
            ecg.lineTo(w * 0.40, h * 0.70);
            ecg.lineTo(w * 0.46, h * 0.60);
            ecg.lineTo(w * 0.52, h * 0.76);
            ecg.lineTo(w * 0.58, h * 0.68);
            ecg.lineTo(w * 0.90, h * 0.68);
            g2.draw(ecg);
        }
    }

    /** Borde de campo: pinta el fondo redondeado y una linea que se ilumina al enfocar. */
    private static final class BordeCampo implements Border {
        private final Color fondo;
        private final Color reposo;
        private final Color foco;
        private final int radio;

        BordeCampo(Color fondo, Color reposo, Color foco, int radio) {
            this.fondo = fondo;
            this.reposo = reposo;
            this.foco = foco;
            this.radio = radio;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fondo);
            g2.fillRoundRect(x, y, width - 1, height - 1, radio, radio);
            g2.setColor(c.isFocusOwner() ? foco : reposo);
            g2.drawRoundRect(x, y, width - 1, height - 1, radio, radio);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(0, 0, 0, 0);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    /** Boton primario con degradado verde esmeralda, esquinas redondeadas y hover. */
    private static final class BotonPrimario extends JButton {
        private boolean hover;

        BotonPrimario(String texto) {
            super(texto);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(360, 50));
            setMaximumSize(new Dimension(360, 50));
            setMinimumSize(new Dimension(360, 50));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color desde = hover ? new Color(21, 191, 172) : Tema.TEAL;
            Color hasta = hover ? new Color(13, 158, 156) : Tema.TEAL_OSCURO;
            g2.setPaint(new java.awt.LinearGradientPaint(0, 0, 0, getHeight(),
                    new float[]{0f, 1f}, new Color[]{desde, hasta}));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Paleta cromatica compartida por los controles. */
    private static final class Tema {
        static final Color TEAL = new Color(20, 184, 166);
        static final Color TEAL_OSCURO = new Color(15, 118, 110);
    }
}