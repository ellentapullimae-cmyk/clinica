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
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
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
 * portada de fondo (panel lateral izquierdo) y el formulario de acceso a la
 * derecha. Solo es interfaz: la autenticacion sigue usando UsuarioController y
 * los permisos del resto del sistema no se ven alterados.
 */
public final class LoginVentana {

    private static final int LIMITE_INTENTOS = 3;
    private static final int ANCHO_FORMULARIO = 460;
    private static final int UMBRAL_LATERAL = 760;

    /** Tamano de diseno base (puntos logicos) sobre el que se calcula la escala. */
    private static final int DISENO_ANCHO = 1120;
    private static final int DISENO_ALTO = 680;
    /** Margen reservado alrededor de la ventana respecto al area de pantalla util. */
    private static final int MARGEN_VENTANA = 24;
    /** Altura aproximada de la barra de titulo de la ventana. */
    private static final int MARGEN_TITULO_VENTANA = 44;

    /** Escala entera: valor * factor, con un minimo de 1 para que nada desaparezca. */
    private static int esc(int valor, float factor) {
        return Math.max(1, Math.round(valor * factor));
    }

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

    /** Localiza y decodifica la imagen de portada sin abrir ninguna ventana. */
    public static ImageInfo verificarRecurso() {
        Path ruta = ubicarImagen();
        if (ruta == null) {
            return new ImageInfo(null, 0, 0,
                    "No se encontro la imagen de portada en assets/images/" + nombresImagen());
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

    private static String nombresImagen() {
        return String.join(", ", NOMBRES_IMAGEN);
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

    private static final String[] NOMBRES_IMAGEN = {
            "Gemini_Generated_Image_5fk8yk5fk8yk5fk8.jpg",
            "clinica.png"
    };

    private static Path ubicarImagen() {
        Path cursor = Path.of(System.getProperty("user.dir", "."));
        for (int i = 0; i < 5 && cursor != null; i++) {
            for (String nombre : NOMBRES_IMAGEN) {
                Path candidata = cursor.resolve("assets").resolve("images").resolve(nombre);
                if (Files.isRegularFile(candidata)) {
                    return candidata;
                }
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
        private static final Color COLOR_BORDE_CAMPO = new Color(226, 232, 240);
        private static final Color ROJO = new Color(220, 38, 38);

        private final UsuarioController controlador;
        private final Usuario[] resultado;
        private final JTextField campoUsuario;
        private final JPasswordField campoClave;
        private final JLabel etiquetaError = new JLabel(" ");
        private int intentosRestantes;
        private final float factor;

        DialogoLogin(UsuarioController controlador, Usuario[] resultado) {
            super((Frame) null, "Acceso al sistema", Dialog.ModalityType.APPLICATION_MODAL);
            this.controlador = controlador;
            this.resultado = resultado;
            this.intentosRestantes = LIMITE_INTENTOS;
            this.factor = calcularFactorEscala();
            int radioCampo = Math.max(6, esc(12, factor));
            this.campoUsuario = new CampoTexto(radioCampo, IconoCampo.USUARIO);
            this.campoClave = new CampoClave(radioCampo, IconoCampo.CLAVE);
            construirUI();
        }

        /**
         * Calcula un factor (0-1) que hace que el diseno base quepa completo en el
         * area disponible de la pantalla, respetando la barra de tareas y la escala
         * de Windows (los tamanos de Toolkit ya vienen en unidades logicas).
         */
        private float calcularFactorEscala() {
            GraphicsConfiguration gc = getGraphicsConfiguration();
            if (gc == null) {
                gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice().getDefaultConfiguration();
            }
            Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
            Insets limites = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            int anchoDisponible = Math.max(1,
                    pantalla.width - limites.left - limites.right - MARGEN_VENTANA * 2);
            int altoDisponible = Math.max(1,
                    pantalla.height - limites.top - limites.bottom - MARGEN_VENTANA * 2);
            return Math.min(1f, Math.min((float) anchoDisponible / DISENO_ANCHO,
                    (float) altoDisponible / DISENO_ALTO));
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

            JPanel tarjeta = construirTarjeta();
            JPanel contenedorFormulario = new JPanel(new GridBagLayout());
            contenedorFormulario.setOpaque(false);
            contenedorFormulario.add(tarjeta);

            final PanelLateral panelLateral = new PanelLateral(cargarImagen(), factor);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            raiz.add(panelLateral, c);

            c.gridx = 1;
            c.weightx = 0.0;
            c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            raiz.add(contenedorFormulario, c);

            raiz.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    boolean mostrar = raiz.getWidth() >= UMBRAL_LATERAL;
                    if (panelLateral.isVisible() != mostrar) {
                        panelLateral.setVisible(mostrar);
                        raiz.revalidate();
                        raiz.repaint();
                    }
                }
            });

            getRootPane().registerKeyboardAction(e -> cerrar(null),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);

            setUndecorated(false);
            int anchoVentana = Math.max(1, esc(DISENO_ANCHO, factor));
            int altoVentana = Math.max(1, esc(DISENO_ALTO, factor));
            Dimension prefTarjeta = tarjeta != null ? tarjeta.getPreferredSize() : new Dimension(0, 0);
            int minAncho = Math.min(prefTarjeta.width + esc(20, factor), anchoVentana);
            int minAlto = Math.min(prefTarjeta.height + esc(MARGEN_TITULO_VENTANA, factor), altoVentana);
            setMinimumSize(new Dimension(Math.max(1, minAncho), Math.max(1, minAlto)));
            setSize(anchoVentana, altoVentana);
            setLocationRelativeTo(null);
            setContentPane(raiz);
        }

        private JPanel construirTarjeta() {
            float f = factor;
            int padLateral = Math.max(6, esc(44, f));
            int padSuperior = Math.max(4, esc(32, f));
            int padInferior = Math.max(4, esc(30, f));
            int anchoControl = esc(360, f);
            int altoCampo = esc(46, f);
            int altoBoton = esc(50, f);

            JPanel tarjeta = new JPanel();
            tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
            tarjeta.setBackground(Color.WHITE);
            tarjeta.setBorder(BorderFactory.createEmptyBorder(padSuperior, padLateral, padInferior, padLateral));
            Dimension contorno = new Dimension(esc(ANCHO_FORMULARIO, f), esc(600, f));
            tarjeta.setPreferredSize(contorno);
            tarjeta.setMaximumSize(contorno);
            tarjeta.setMinimumSize(contorno);

            LogoPlaca logo = new LogoPlaca(Math.max(28, esc(42, f)), f);
            logo.setAlignmentX(Component.CENTER_ALIGNMENT);
            tarjeta.add(logo);

            tarjeta.add(Box.createVerticalStrut(Math.max(8, esc(14, f))));
            tarjeta.add(literal("ACCESO AL SISTEMA", TEMA, esc(11, f), Font.BOLD));
            tarjeta.add(Box.createVerticalStrut(Math.max(3, esc(6, f))));
            tarjeta.add(literal("Bienvenido de nuevo", AZUL_MARINO, esc(24, f), Font.BOLD));
            tarjeta.add(Box.createVerticalStrut(Math.max(2, esc(4, f))));
            tarjeta.add(literal("Ingrese sus credenciales para continuar",
                    TEXTO_SUAVE, esc(13, f), Font.PLAIN));

            tarjeta.add(Box.createVerticalStrut(Math.max(14, esc(24, f))));
            tarjeta.add(filaEtiqueta("USUARIO"));
            configurarCampo(campoUsuario, anchoControl, altoCampo, f);
            tarjeta.add(campoUsuario);

            tarjeta.add(Box.createVerticalStrut(Math.max(8, esc(16, f))));
            tarjeta.add(filaEtiqueta("CONTRASENA"));
            configurarCampo(campoClave, anchoControl, altoCampo, f);
            tarjeta.add(campoClave);

            tarjeta.add(Box.createVerticalStrut(Math.max(6, esc(12, f))));
            etiquetaError.setAlignmentX(Component.CENTER_ALIGNMENT);
            etiquetaError.setForeground(ROJO);
            etiquetaError.setHorizontalAlignment(SwingConstants.CENTER);
            etiquetaError.setFont(fuente(esc(12, f), Font.PLAIN));
            etiquetaError.setPreferredSize(new Dimension(anchoControl, esc(20, f)));
            etiquetaError.setMaximumSize(new Dimension(anchoControl, esc(20, f)));
            tarjeta.add(etiquetaError);

            JButton boton = new BotonPrimario("INICIAR SESION", anchoControl, altoBoton, f);
            boton.setAlignmentX(Component.CENTER_ALIGNMENT);
            boton.addActionListener(e -> intentarLogin());
            campoClave.addActionListener(e -> intentarLogin());
            campoUsuario.addActionListener(e -> campoClave.requestFocusInWindow());
            getRootPane().setDefaultButton(boton);
            tarjeta.add(boton);

            tarjeta.add(Box.createVerticalStrut(Math.max(12, esc(20, f))));
            JPanel pivotes = new JPanel();
            pivotes.setOpaque(false);
            pivotes.setLayout(new BoxLayout(pivotes, BoxLayout.X_AXIS));
            pivotes.add(pivote("Administrador"));
            pivotes.add(Box.createHorizontalStrut(Math.max(8, esc(14, f))));
            pivotes.add(pivote("Recepcionista"));
            pivotes.add(Box.createHorizontalStrut(Math.max(8, esc(14, f))));
            pivotes.add(pivote("Medico"));
            pivotes.setAlignmentX(Component.CENTER_ALIGNMENT);
            tarjeta.add(pivotes);

            tarjeta.add(Box.createVerticalStrut(Math.max(6, esc(10, f))));
            tarjeta.add(literal("Sistema de Gestion para una Clinica  v1.0",
                    TEXTO_SUAVE, esc(11, f), Font.PLAIN));
            return tarjeta;
        }

        private JLabel filaEtiqueta(String texto) {
            JLabel l = new JLabel(texto);
            l.setForeground(TEXTO_SUAVE);
            l.setFont(fuente(esc(11, factor), Font.BOLD));
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            int padL = Math.max(2, esc(4, factor));
            l.setBorder(BorderFactory.createEmptyBorder(
                    Math.max(1, esc(2, factor)), padL, Math.max(3, esc(5, factor)), padL));
            return l;
        }

        private JLabel pivote(String texto) {
            JLabel l = new JLabel(texto);
            l.setForeground(TEXTO_SUAVE);
            l.setFont(fuente(esc(11, factor), Font.PLAIN));
            l.setBorder(BorderFactory.createEmptyBorder(Math.max(2, esc(3, factor)),
                    Math.max(6, esc(12, factor)), Math.max(2, esc(3, factor)),
                    Math.max(6, esc(12, factor))));
            l.setOpaque(true);
            l.setBackground(new Color(241, 245, 249));
            return l;
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

        private void configurarCampo(JTextField campo, int ancho, int alto, float f) {
            campo.setFont(fuente(esc(15, f), Font.PLAIN));
            campo.setForeground(TEXTO);
            campo.setCaretColor(TEMA);
            campo.setHorizontalAlignment(SwingConstants.LEFT);
            campo.setOpaque(false);
            campo.setEditable(true);
            campo.setEnabled(true);
            campo.setFocusable(true);
            Dimension tamano = new Dimension(ancho, alto);
            campo.setPreferredSize(tamano);
            campo.setMaximumSize(tamano);
            campo.setMinimumSize(tamano);
            Border borde = new BordeCampo(COLOR_BORDE_CAMPO, TEMA, Math.max(6, esc(12, f)));
            int izq = Math.max(8, esc(42, f));
            int der = Math.max(8, esc(14, f));
            campo.setBorder(BorderFactory.createCompoundBorder(borde,
                    BorderFactory.createEmptyBorder(0, izq, 0, der)));
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

    /**
     * Panel lateral izquierdo con la foto de la clinica como portada a sangre
     * completa, un velo degradado para legibilidad y la marca del sistema por
     * encima. Si no hay imagen disponible se usa un fondo degradado con motivos.
     */
    private static final class PanelLateral extends JPanel {

        private static final Color BLANCO_FUERTE = new Color(255, 255, 255);
        private static final Color TURQUESA_CLARO = new Color(153, 240, 228, 240);
        private static final Color MARINO = new Color(10, 38, 71);
        private static final Color TEAL_MEDIO = new Color(21, 121, 116);
        private static final Color MARINO_FONDO = new Color(8, 30, 56);

        private final BufferedImage imagen;
        private final boolean conImagen;
        private final float f;

        PanelLateral(BufferedImage imagen, float factorLateral) {
            this.imagen = imagen;
            this.conImagen = imagen != null;
            this.f = Math.max(0.62f, factorLateral);
            setOpaque(false);
            construirContenido();
        }

        private void construirContenido() {
            int lado = Math.max(20, esc(28, f));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(
                    Math.max(10, esc(22, f)), Math.max(16, esc(34, f)),
                    Math.max(10, esc(22, f)), Math.max(16, esc(34, f))));

            // franja superior: emblema + marca
            JPanel marca = new JPanel();
            marca.setOpaque(false);
            marca.setLayout(new BoxLayout(marca, BoxLayout.X_AXIS));
            marca.setAlignmentX(Component.LEFT_ALIGNMENT);
            EmblemaPlaca emblema = new EmblemaPlaca(lado);
            emblema.setAlignmentY(Component.CENTER_ALIGNMENT);
            marca.add(emblema);
            marca.add(Box.createHorizontalStrut(Math.max(6, esc(12, f))));
            JLabel nombre = new JLabel("CLINICA   |   SALUD Y BIENESTAR");
            nombre.setForeground(BLANCO_FUERTE);
            nombre.setFont(new Font("Segoe UI", Font.BOLD, Math.max(9, esc(12, f))));
            nombre.setAlignmentY(Component.CENTER_ALIGNMENT);
            marca.add(nombre);
            add(marca);

            add(Box.createVerticalGlue());

            // bloque central
            JLabel eyebrow = new JLabel("SISTEMA DE GESTION HOSPITALARIA");
            eyebrow.setForeground(TURQUESA_CLARO);
            eyebrow.setFont(new Font("Segoe UI", Font.BOLD, Math.max(9, esc(11, f))));
            eyebrow.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(eyebrow);
            add(Box.createVerticalStrut(Math.max(4, esc(8, f))));

            JLabel titulo = new JLabel("Atencion integral para");
            titulo.setForeground(BLANCO_FUERTE);
            titulo.setFont(new Font("Segoe UI", Font.BOLD, Math.max(16, esc(24, f))));
            titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(titulo);
            JLabel titulo2 = new JLabel("tu salud y la de tu familia");
            titulo2.setForeground(BLANCO_FUERTE);
            titulo2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(16, esc(24, f))));
            titulo2.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(titulo2);
            add(Box.createVerticalStrut(Math.max(4, esc(8, f))));

            LineaDecorativa linea = new LineaDecorativa(Math.max(60, esc(120, f)), 2);
            linea.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(linea);
            add(Box.createVerticalStrut(Math.max(8, esc(14, f))));

            JLabel sub = new JLabel("Gestionamos la operacion completa de su clinica:",
                    null, SwingConstants.CENTER);
            sub.setForeground(new Color(236, 245, 248, 235));
            sub.setFont(new Font("Segoe UI", Font.PLAIN, Math.max(9, esc(13, f))));
            sub.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(sub);

            add(Box.createVerticalStrut(Math.max(10, esc(18, f))));

            JPanel features = new JPanel();
            features.setOpaque(false);
            features.setLayout(new BoxLayout(features, BoxLayout.Y_AXIS));
            features.setAlignmentX(Component.CENTER_ALIGNMENT);
            features.setMaximumSize(new Dimension(esc(340, f), Integer.MAX_VALUE));
            features.add(filaFeature("Agenda de pacientes, medicos y especialidades"));
            features.add(Box.createVerticalStrut(Math.max(4, esc(8, f))));
            features.add(filaFeature("Citas, atenciones y control de historial"));
            features.add(Box.createVerticalStrut(Math.max(4, esc(8, f))));
            features.add(filaFeature("Pagos, gastos, presupuesto y reportes"));
            add(features);

            add(Box.createVerticalGlue());

            JLabel pie = new JLabel("SALUD  |  CONFIANZA  |  CALIDAD",
                    null, SwingConstants.CENTER);
            pie.setForeground(TURQUESA_CLARO);
            pie.setFont(new Font("Segoe UI", Font.BOLD, Math.max(7, esc(10, f))));
            pie.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(pie);
        }

        private float factorLateralLado(float f) {
            return f;
        }

        private JPanel filaFeature(String texto) {
            JPanel fila = new JPanel();
            fila.setOpaque(false);
            fila.setLayout(new BoxLayout(fila, BoxLayout.X_AXIS));
            fila.setAlignmentX(Component.LEFT_ALIGNMENT);
            Vineta v = new Vineta(Math.max(10, esc(16, f)), f);
            v.setAlignmentY(Component.CENTER_ALIGNMENT);
            fila.add(v);
            fila.add(Box.createHorizontalStrut(Math.max(6, esc(10, f))));
            JLabel lbl = new JLabel(texto);
            lbl.setForeground(new Color(238, 246, 248, 240));
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, Math.max(8, esc(12, f))));
            lbl.setAlignmentY(Component.CENTER_ALIGNMENT);
            fila.add(lbl);
            return fila;
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

            if (conImagen) {
                double escala = Math.max((double) w / imagen.getWidth(), (double) h / imagen.getHeight());
                int dw = Math.max(1, (int) Math.ceil(imagen.getWidth() * escala));
                int dh = Math.max(1, (int) Math.ceil(imagen.getHeight() * escala));
                g2.drawImage(imagen, (w - dw) / 2, (h - dh) / 2, dw, dh, null);

                // velo: mas denso abajo (donde van textos) y hacia la derecha.
                g2.setPaint(new java.awt.LinearGradientPaint(0, 0, 0, h,
                        new float[]{0f, 0.5f, 1f},
                        new Color[]{new Color(8, 30, 56, 55),
                                new Color(8, 30, 56, 150),
                                new Color(6, 22, 44, 210)}));
                g2.fillRect(0, 0, w, h);
                g2.setPaint(new java.awt.LinearGradientPaint(0, 0, w, 0,
                        new float[]{0f, 0.75f, 1f},
                        new Color[]{new Color(8, 30, 56, 40),
                                new Color(8, 30, 56, 0),
                                new Color(255, 255, 255, 30)}));
                g2.fillRect(0, 0, w, h);
            } else {
                g2.setPaint(new java.awt.LinearGradientPaint(0, 0, w, h,
                        new float[]{0f, 0.55f, 1f},
                        new Color[]{MARINO, TEAL_MEDIO, MARINO_FONDO}));
                g2.fillRect(0, 0, w, h);
                dibujarDecoracion(g2, w, h);
                dibujarElectrocardiograma(g2, w, h);
            }
            g2.dispose();
        }

        /** Patron decorativo sutil que se usa solo cuando no hay foto de portada. */
        private void dibujarDecoracion(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(255, 255, 255, 14));
            int tam = Math.max(8, esc(22, f));
            int margen = Math.max(10, esc(16, f));
            int pasoX = Math.max(36, esc(78, f));
            int pasoY = Math.max(34, esc(74, f));
            for (int x = margen; x <= w - margen - tam; x += pasoX) {
                for (int y = margen; y <= h - margen - tam; y += pasoY) {
                    g2.drawOval(x, y, tam, tam);
                }
            }
        }

        /** Trazo de electrocardiograma inferior, solo cuando no hay foto de portada. */
        private void dibujarElectrocardiograma(Graphics2D g2, int w, int h) {
            int margen = Math.max(6, esc(14, f));
            if (w < margen * 2 + 30 || h < esc(40, f) + margen) {
                return;
            }
            double yBase = h - Math.max(16, esc(26, f));
            g2.setColor(new Color(255, 255, 255, 95));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D ecg = new Path2D.Double();
            ecg.moveTo(margen, yBase);
            ecg.lineTo(w * 0.36, yBase);
            ecg.lineTo(w * 0.41, yBase - Math.max(6, esc(14, f)));
            ecg.lineTo(w * 0.46, yBase + Math.max(5, esc(12, f)));
            ecg.lineTo(w * 0.51, yBase - Math.max(4, esc(8, f)));
            double fin = Math.min(w - margen, Math.max(w * 0.55, w * 0.66));
            ecg.lineTo(fin, yBase - Math.max(2, esc(4, f)));
            ecg.lineTo(fin, yBase);
            g2.draw(ecg);
        }
    }

    /** Iconos pequenos que se dibujan dentro de los campos de texto. */
    private static enum IconoCampo {
        USUARIO,
        CLAVE
    }

    /**
     * Emblema circular con la cruz medica, usado en la marca superior del panel
     * lateral izquierdo.
     */
    private static final class EmblemaPlaca extends JPanel {
        private final int lado;

        EmblemaPlaca(int lado) {
            this.lado = lado;
            Dimension tam = new Dimension(lado, lado);
            setPreferredSize(tam);
            setMaximumSize(tam);
            setMinimumSize(tam);
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
            g2.setPaint(new java.awt.LinearGradientPaint(0, 0, w, h,
                    new float[]{0f, 1f},
                    new Color[]{new Color(23, 172, 158), new Color(8, 40, 78)}));
            g2.fillOval(0, 0, w - 1, h - 1);
            g2.setColor(new Color(255, 255, 255, 70));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(1, 1, w - 3, h - 3);
            int cx = w / 2;
            int cy = h / 2;
            int largo = Math.max(6, (int) (Math.min(w, h) * 0.5));
            int grosor = Math.max(3, (int) (Math.min(w, h) * 0.16));
            g2.setColor(new Color(255, 255, 255, 235));
            g2.fillRoundRect(cx - grosor / 2, cy - largo / 2, grosor, largo, grosor, grosor);
            g2.fillRoundRect(cx - largo / 2, cy - grosor / 2, largo, grosor, grosor, grosor);
            g2.dispose();
        }
    }

    /** Placa circular superior de la tarjeta de formulario, con la marca. */
    private static final class LogoPlaca extends JPanel {
        private final float f;

        LogoPlaca(int lado, float f) {
            this.f = f;
            Dimension tam = new Dimension(lado, lado);
            setPreferredSize(tam);
            setMaximumSize(tam);
            setMinimumSize(tam);
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
            g2.setPaint(new java.awt.LinearGradientPaint(0, 0, 0, h,
                    new float[]{0f, 1f},
                    new Color[]{new Color(28, 196, 177), new Color(13, 108, 116)}));
            g2.fillOval(0, 0, w - 1, h - 1);
            g2.setColor(new Color(255, 255, 255, 90));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(1, 1, w - 3, h - 3);
            int cx = w / 2;
            int cy = h / 2;
            int largo = Math.max(8, (int) (Math.min(w, h) * 0.52));
            int grosor = Math.max(4, (int) (Math.min(w, h) * 0.17));
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Double(cx - grosor / 2d, cy - largo / 2d,
                    grosor, largo, grosor, grosor));
            g2.fill(new RoundRectangle2D.Double(cx - largo / 2d, cy - grosor / 2d,
                    largo, grosor, grosor, grosor));
            g2.dispose();
        }
    }

    /** Vineta de la lista de caracteristicas: cuadrito con paloma blanca. */
    private static final class Vineta extends JPanel {
        private final float f;

        Vineta(int lado, float f) {
            this.f = f;
            Dimension tam = new Dimension(lado, lado);
            setPreferredSize(tam);
            setMaximumSize(tam);
            setMinimumSize(tam);
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
            g2.setColor(new Color(77, 215, 196, 235));
            int radio = Math.max(3, esc(6, f));
            g2.fillRoundRect(0, 0, w, h, radio, radio);
            g2.setColor(new Color(8, 40, 66));
            g2.setStroke(new BasicStroke(Math.max(1.5f, w * 0.12f),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int x1 = Math.round(w * 0.24f);
            int y1 = Math.round(h * 0.52f);
            int x2 = Math.round(w * 0.44f);
            int y2 = Math.round(h * 0.72f);
            int x3 = Math.round(w * 0.80f);
            int y3 = Math.round(h * 0.30f);
            g2.drawLine(x1, y1, x2, y2);
            g2.drawLine(x2, y2, x3, y3);
            g2.dispose();
        }
    }

    /** Linea divisoria decorativa con desvanecido hacia los extremos. */
    private static final class LineaDecorativa extends JPanel {
        LineaDecorativa(int ancho, int alto) {
            Dimension tam = new Dimension(ancho, alto);
            setPreferredSize(tam);
            setMaximumSize(tam);
            setMinimumSize(tam);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new java.awt.LinearGradientPaint(0, 0, w, 0,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(255, 255, 255, 0),
                            new Color(140, 235, 222, 220),
                            new Color(255, 255, 255, 0)}));
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        }
    }

    /**
     * Borde de campo: solo dibuja el contorno redondeado que se ilumina al
     * enfocar. El fondo redondeado lo pinta el propio campo (CampoTexto /
     * CampoClave) en paintComponent, de modo que el texto queda SIEMPRE por
     * encima del relleno (Swing pinta el borde despues del contenido).
     */
    private static final class BordeCampo implements Border {
        private final Color reposo;
        private final Color foco;
        private final int radio;

        BordeCampo(Color reposo, Color foco, int radio) {
            this.reposo = reposo;
            this.foco = foco;
            this.radio = radio;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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

    /** Campo de texto con icono y fondo redondeado propio (el texto queda sobre el fondo). */
    private static final class CampoTexto extends JTextField {
        private final int radio;
        private final IconoCampo icono;

        CampoTexto(int radio, IconoCampo icono) {
            this.radio = radio;
            this.icono = icono;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(241, 245, 249));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radio, radio);
            pintarIcono(g2);
            g2.dispose();
            super.paintComponent(g);
        }

        private void pintarIcono(Graphics2D g2) {
            g2.setColor(new Color(100, 116, 139, 210));
            g2.setStroke(new BasicStroke(Math.max(1.4f, getHeight() * 0.055f),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = getHeight() / 2;
            if (icono == IconoCampo.USUARIO) {
                int r = Math.max(3, getHeight() / 8);
                int yc = getHeight() / 2 - r;
                g2.drawOval(cx - r, yc - r, 2 * r, 2 * r);
                g2.drawArc(cx - 2 * r, yc + r, 4 * r, 2 * r, 200, 140);
            } else if (icono == IconoCampo.CLAVE) {
                int w = Math.max(6, getHeight() / 5);
                int h2 = Math.max(8, getHeight() / 3);
                int bx = cx - w / 2;
                int by = getHeight() / 2 - h2 / 2 + 4;
                g2.drawRoundRect(bx, by, w, h2, Math.max(2, w / 3), Math.max(2, w / 3));
                g2.drawArc(bx - 1, by - h2 / 2, w + 2, h2, 180, 180);
                g2.fillOval(cx - 1, by + h2 / 2 - 2, 2, 2);
            }
        }
    }

    /** Campo de contrasena con icono y fondo redondeado propio. */
    private static final class CampoClave extends JPasswordField {
        private final int radio;
        private final IconoCampo icono;

        CampoClave(int radio, IconoCampo icono) {
            this.radio = radio;
            this.icono = icono;
            setEchoChar('*');
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(241, 245, 249));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radio, radio);
            pintarIcono(g2);
            g2.dispose();
            super.paintComponent(g);
        }

        private void pintarIcono(Graphics2D g2) {
            g2.setColor(new Color(100, 116, 139, 210));
            g2.setStroke(new BasicStroke(Math.max(1.4f, getHeight() * 0.055f),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = getHeight() / 2;
            int w = Math.max(6, getHeight() / 5);
            int h2 = Math.max(8, getHeight() / 3);
            int bx = cx - w / 2;
            int by = getHeight() / 2 - h2 / 2 + 4;
            g2.drawRoundRect(bx, by, w, h2, Math.max(2, w / 3), Math.max(2, w / 3));
            g2.drawArc(bx - 1, by - h2 / 2, w + 2, h2, 180, 180);
            g2.fillOval(cx - 1, by + h2 / 2 - 2, 2, 2);
        }
    }

    /** Boton primario con degradado verde esmeralda, esquinas redondeadas y hover. */
    private static final class BotonPrimario extends JButton {
        private boolean hover;
        private boolean presionado;

        BotonPrimario(String texto, int ancho, int alto, float f) {
            super(texto);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, esc(14, f)));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Dimension tamano = new Dimension(ancho, alto);
            setPreferredSize(tamano);
            setMaximumSize(tamano);
            setMinimumSize(tamano);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    presionado = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    presionado = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    presionado = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int desp = presionado ? 1 : 0;
            Color desde = hover ? new Color(21, 191, 172) : Tema.TEAL;
            Color hasta = hover ? new Color(13, 158, 156) : Tema.TEAL_OSCURO;
            g2.setPaint(new java.awt.LinearGradientPaint(0, 0, 0, getHeight(),
                    new float[]{0f, 1f}, new Color[]{desde, hasta}));
            g2.fillRoundRect(0, desp, getWidth() - 1, getHeight() - 1 - desp, 12, 12);
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