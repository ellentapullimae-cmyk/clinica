package clinica.presentacion.grafico;

import clinica.controlador.AtencionController;
import clinica.controlador.CitaController;
import clinica.controlador.DashboardController;
import clinica.controlador.GastoController;
import clinica.controlador.MedicoController;
import clinica.controlador.PacienteController;
import clinica.controlador.PagoController;
import clinica.controlador.ReporteController;
import clinica.controlador.UsuarioController;
import clinica.presentacion.Sesion;
import clinica.modelo.Rol;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Ventana principal de la aplicacion: barra lateral de navegacion, cabecera
 * superior con el usuario, fecha/hora y el cambio de modulo. Solo es interfaz:
 * toda la logica se delega en los controladores existentes.
 */
public final class VentanaPrincipal extends JDialog {

    private static final Color MARINO = new Color(10, 38, 71);
    private static final Color MARINO_OSCURO = new Color(7, 27, 50);
    private static final Color CARGO = new Color(241, 245, 249);

    private final PacienteController pacientes;
    private final MedicoController medicos;
    private final CitaController citas;
    private final AtencionController atenciones;
    private final PagoController pagos;
    private final GastoController gastos;
    private final ReporteController reportes;
    private final UsuarioController usuarios;
    private final DashboardController dashboard;

    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenedor = new JPanel(tarjetas);
    private final JPanel barraLateral;
    private final JLabel tituloCabecera = new JLabel();
    private final JLabel reloj = new JLabel();
    private final Map<String, JComponent> paneles = new LinkedHashMap<>();
    private final List<BotonNavegacion> botonesNav = new ArrayList<>();

    private boolean salirSistema;

    public VentanaPrincipal(PacienteController pacientes, MedicoController medicos,
                            CitaController citas, AtencionController atenciones,
                            PagoController pagos, GastoController gastos,
                            ReporteController reportes, UsuarioController usuarios,
                            DashboardController dashboard) {
        super((Frame) null, "Sistema de Gestion para una Clinica",
                Dialog.ModalityType.APPLICATION_MODAL);
        this.pacientes = pacientes;
        this.medicos = medicos;
        this.citas = citas;
        this.atenciones = atenciones;
        this.pagos = pagos;
        this.gastos = gastos;
        this.reportes = reportes;
        this.usuarios = usuarios;
        this.dashboard = dashboard;
        this.barraLateral = construirBarraLateral();

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salirSistema = true;
                dispose();
            }
        });

        construirUI();
        reproducirReloj();
    }

    // ======================= UI =======================

    private void construirUI() {
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(CARGO);

        raiz.add(barraLateral, BorderLayout.WEST);

        JPanel zona = new JPanel(new BorderLayout());
        zona.setOpaque(false);
        zona.add(cabecera(), BorderLayout.NORTH);
        zona.add(contenedor, BorderLayout.CENTER);
        raiz.add(zona, BorderLayout.CENTER);

        setContentPane(raiz);

        int ancho = Controles.esc(1280);
        int alto = Controles.esc(760);
        setMinimumSize(new Dimension(Controles.esc(980), Controles.esc(600)));
        setSize(ancho, alto);
        setLocationRelativeTo(null);
    }

    private JPanel cabecera() {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBackground(Color.WHITE);
        cabecera.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Controles.BORDE),
                BorderFactory.createEmptyBorder(Controles.esc(14), Controles.esc(24),
                        Controles.esc(14), Controles.esc(18))));

        JPanel izquierda = new JPanel(new GridBagLayout());
        izquierda.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 1.0;
        g.anchor = GridBagConstraints.WEST;
        tituloCabecera.setFont(Controles.fuente(19, java.awt.Font.BOLD));
        tituloCabecera.setForeground(MARINO);
        izquierda.add(tituloCabecera, g);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, Controles.esc(14), 0));
        derecha.setOpaque(false);
        reloj.setFont(Controles.fuente(13, java.awt.Font.PLAIN));
        reloj.setForeground(Controles.TEXTO_SUAVE);
        reloj.setHorizontalAlignment(SwingConstants.TRAILING);
        derecha.add(
                Controles.etiqueta("Usuario: "+Sesion.getNombreUsuario()
                        + "  |  Rol: " + Sesion.getRolActual().name()+"  |  ",
                        Controles.TEXTO, 13, java.awt.Font.BOLD));
        derecha.add(reloj);

        Controles.BotonUI cambio = new Controles.BotonUI("Cambiar mi contrasena", Controles.TipoBoton.SECUNDARIO);
        cambio.addActionListener(e -> abrirDialogoCambiarClave());
        derecha.add(cambio);

        cabecera.add(izquierda, BorderLayout.CENTER);
        cabecera.add(derecha, BorderLayout.EAST);
        return cabecera;
    }

    private void reproducirReloj() {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        Timer t = new Timer(1000, e -> {
            if (reloj.isShowing()) {
                reloj.setText(LocalDateTime.now().format(formato));
            }
        });
        t.start();
    }

    // ======================= BARRA LATERAL =======================

    private JPanel construirBarraLateral() {
        JPanel lateral = new JPanel(new BorderLayout());
        lateral.setPreferredSize(new Dimension(Controles.esc(250), Controles.esc(760)));
        lateral.setOpaque(true);
        lateral.setBackground(MARINO_OSCURO);
        lateral.setBorder(BorderFactory.createEmptyBorder());
        lateral.add(LogicaFondo(), BorderLayout.CENTER);
        return lateral;
    }

    private JPanel LogicaFondo() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                if (getWidth() > 0 && getHeight() > 0) {
                    g2.setPaint(new java.awt.LinearGradientPaint(0, 0, 0, getHeight(),
                            new float[]{0f, 0.55f, 1f},
                            new Color[]{MARINO, new Color(19, 74, 92), MARINO_OSCURO}));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(Controles.esc(22), Controles.esc(16),
                Controles.esc(18), Controles.esc(16)));
        panel.setLayout(new BorderLayout());

        JPanel marca = new JPanel();
        marca.setOpaque(false);
        marca.setLayout(new BoxLayout(marca, BoxLayout.Y_AXIS));
        Controles.PanelIcono logo = new Controles.PanelIcono(Controles.Icono.NUEVO,
                Controles.esc(52));
        logo.color(Color.WHITE);
        logo.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        marca.add(logo);
        marca.add(Box.createVerticalStrut(Controles.esc(10)));
        JLabel nombre = Controles.etiqueta("SISTEMA DE GESTION", Color.WHITE,
                Controles.esc(15), java.awt.Font.BOLD);
        nombre.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        marca.add(nombre);
        JLabel clinica = Controles.etiqueta("PARA UNA CLINICA", new Color(140, 235, 222),
                Controles.esc(12), java.awt.Font.BOLD);
        clinica.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        marca.add(clinica);
        marca.add(Box.createVerticalStrut(Controles.esc(14)));

        panel.add(marca, BorderLayout.NORTH);

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        registrarYAgregar(nav, "Dashboard", Controles.Icono.DASHBOARD, "dashboard", true);
        registrarYAgregar(nav, "Pacientes", Controles.Icono.PACIENTES, "pacientes",
                Sesion.getRolActual().verPacientes());
        registrarYAgregar(nav, "Medicos", Controles.Icono.MEDICOS, "medicos",
                Sesion.getRolActual().gestionarMedicos());
        registrarYAgregar(nav, "Citas", Controles.Icono.CITAS, "citas",
                Sesion.getRolActual().verCitas());
        registrarYAgregar(nav, "Atenciones", Controles.Icono.ATENCIONES, "atenciones",
                Sesion.getRolActual().verAtenciones());
        registrarYAgregar(nav, "Pagos", Controles.Icono.PAGOS, "pagos",
                Sesion.getRolActual().gestionarPagos());
        registrarYAgregar(nav, "Gastos", Controles.Icono.GASTOS, "gastos",
                Sesion.getRolActual().gestionarGastos());
        registrarYAgregar(nav, "Reportes", Controles.Icono.REPORTES, "reportes",
                Sesion.getRolActual().verReportes());
        registrarYAgregar(nav, "Usuarios", Controles.Icono.USUARIOS, "usuarios",
                Sesion.getRolActual().gestionarUsuarios());
        registrarYAgregar(nav, "Configuracion", Controles.Icono.CONFIG, "config",
                Sesion.getRolActual().gestionarPresupuesto());
        nav.add(Box.createVerticalGlue());
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pie.setOpaque(false);
        Controles.BotonUI cerrarSesion = new Controles.BotonUI("Cerrar sesion",
                Controles.TipoBoton.PELIGRO);
        cerrarSesion.addActionListener(e -> {
            salirSistema = false;
            dispose();
        });
        pie.add(cerrarSesion);
        nav.add(pie);

        panel.add(nav, BorderLayout.CENTER);
        return panel;
    }

    private void registrarYAgregar(JPanel nav, String titulo, Controles.Icono icono,
                                   String clave, boolean visible) {
        if (!visible) {
            return;
        }
        if (paneles.isEmpty()) {
            tituloCabecera.setText(titulo);
        }
        BotonNavegacion boton = new BotonNavegacion(titulo, icono, clave);
        botonesNav.add(boton);
        nav.add(boton);
        nav.add(Box.createVerticalStrut(Controles.esc(6)));
        if (!paneles.containsKey(clave)) {
            JComponent panel = crearPanel(clave);
            paneles.put(clave, panel);
            contenedor.add(panel, clave);
        }
    }

    private JComponent crearPanel(String clave) {
        return switch (clave) {
            case "dashboard" -> new PanelDashboard(this, dashboard, citas,
                    atenciones, pagos, reportes);
            case "pacientes" -> new PanelPacientes(this, pacientes);
            case "medicos" -> new PanelMedicos(this, medicos);
            case "citas" -> new PanelCitas(this, citas, pacientes, medicos);
            case "atenciones" -> new PanelAtenciones(this, atenciones, citas, pacientes);
            case "pagos" -> new PanelPagos(this, pagos, atenciones, reportes);
            case "gastos" -> new PanelGastos(this, gastos, reportes);
            case "reportes" -> new PanelReportes(this, reportes, dashboard);
            case "usuarios" -> new PanelUsuarios(this, usuarios, medicos);
            case "config" -> new PanelConfiguracion(this, reportes, gastos);
            default -> new JPanel();
        };
    }

    /** Cambia al modulo indicado y recarga sus datos. */
    public void mostrarModulo(String clave) {
        if (!paneles.containsKey(clave)) {
            return;
        }
        tarjetas.show(contenedor, clave);
        JComponent panel = paneles.get(clave);
        String[] titulos = {
                "dashboard", "Dashboard", "pacientes", "Pacientes", "medicos", "Medicos",
                "citas", "Citas", "atenciones", "Atenciones", "pagos", "Pagos",
                "gastos", "Gastos", "reportes", "Reportes",
                "usuarios", "Usuarios", "config", "Configuracion"};
        for (int i = 0; i < titulos.length; i += 2) {
            if (titulos[i].equals(clave)) {
                tituloCabecera.setText(titulos[i + 1]);
                break;
            }
        }
        for (BotonNavegacion b : botonesNav) {
            b.setActivo(b.getClave().equals(clave));
        }
        if (panel instanceof Recargable recargable) {
            recargable.recargar();
        }
    }

    /** Devuelve true si el usuario cerro desde el sistema (no por cerrar sesion). */
    public boolean quiereSalir() {
        return salirSistema;
    }

    private void abrirDialogoCambiarClave() {
        FormularioDialogo form = new FormularioDialogo(this,
                "Cambiar mi contrasena", 380);
        var actual = Controles.campoClave();
        var nueva = Controles.campoClave();
        var confirma = Controles.campoClave();
        form.campo("Clave actual", actual);
        form.campo("Nueva clave (minimo 6)", nueva);
        form.campo("Confirme la nueva clave", confirma);
        if (form.mostrar()) {
            try {
                String n = Controles.clave(nueva);
                if (!n.equals(Controles.clave(confirma))) {
                    throw new IllegalArgumentException("Las nuevas claves no coinciden.");
                }
                usuarios.cambiarMiClave(Sesion.getNombreUsuario(),
                        Controles.clave(actual), n);
                Controles.informacion(this, "Contrasena",
                        "Su contrasena se actualizo correctamente.");
            } catch (RuntimeException ex) {
                Controles.error(this, "Error", ex.getMessage());
            }
        }
    }

    // ======================= NAVEGACION =======================

    private final class BotonNavegacion extends JPanel {
        private final String texto;
        private final Controles.Icono icono;
        private final String clave;
        private boolean activo;

        BotonNavegacion(String texto, Controles.Icono icono, String clave) {
            this.texto = texto;
            this.icono = icono;
            this.clave = clave;
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setBorder(BorderFactory.createEmptyBorder(Controles.esc(10),
                    Controles.esc(14), Controles.esc(10), Controles.esc(14)));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    mostrarModulo(clave);
                }
            });
            construir();
            setMaximumSize(new Dimension(Controles.esc(250), Controles.esc(46)));
            setPreferredSize(new Dimension(Controles.esc(220), Controles.esc(46)));
        }

        private void construir() {
            Controles.PanelIcono ic = new Controles.PanelIcono(icono, Controles.esc(20));
            Color icColor = activo ? new Color(140, 235, 222) : new Color(200, 216, 232);
            ic.color(icColor);
            add(ic);
            add(Box.createHorizontalStrut(Controles.esc(12)));
            JLabel l = Controles.etiqueta(texto, activo ? Color.WHITE
                    : new Color(210, 224, 238), 13, activo ? java.awt.Font.BOLD
                    : java.awt.Font.PLAIN);
            add(l);
        }

        void setActivo(boolean valor) {
            if (this.activo == valor) {
                return;
            }
            this.activo = valor;
            removeAll();
            construir();
            revalidate();
            repaint();
        }

        String getClave() {
            return clave;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (activo) {
                g2.setColor(new Color(20, 184, 166, 200));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        Controles.esc(10), Controles.esc(10));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}