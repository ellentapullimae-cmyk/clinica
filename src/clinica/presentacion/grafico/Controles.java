package clinica.presentacion.grafico;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * Libreria de componentes UI reutilizables del tema visual de la clinica
 * (azul marino, turquesa, blanco y gris claro). Todos los tamanos se calculan
 * con {@link #esc(int)} para adaptarse a la resolucion y a la escala de Windows.
 */
public final class Controles {

    private Controles() {
    }

    // ======================= PALETA =======================

    public static final Color MARINO = new Color(10, 38, 71);
    public static final Color MARINO_OSCURO = new Color(7, 26, 49);
    public static final Color TEAL = new Color(16, 178, 166);
    public static final Color TEAL_CLARO = new Color(21, 191, 172);
    public static final Color TEAL_OSCURO = new Color(12, 138, 130);
    public static final Color FONDO = new Color(245, 247, 250);
    public static final Color BLANCO = Color.WHITE;
    public static final Color TEXTO = new Color(30, 41, 59);
    public static final Color TEXTO_SUAVE = new Color(100, 116, 139);
    public static final Color BORDE = new Color(224, 230, 237);
    public static final Color VERDE = new Color(22, 163, 74);
    public static final Color VERDE_CLARO = new Color(220, 252, 231);
    public static final Color ROJO = new Color(220, 38, 38);
    public static final Color ROJO_CLARO = new Color(254, 226, 226);
    public static final Color AMBAR = new Color(180, 126, 14);
    public static final Color AMBAR_CLARO = new Color(254, 243, 199);
    public static final Color AZUL = new Color(37, 99, 235);
    public static final Color AZUL_CLARO = new Color(219, 234, 254);
    public static final Color GRIS = new Color(100, 116, 139);
    public static final Color GRIS_CLARO = new Color(229, 231, 235);

    // ======================= ESCALA =======================

    /** Tamano de diseno base sobre el que se calcula la escala de la ventana. */
    public static final int DISENO_ANCHO = 1420;
    public static final int DISENO_ALTO = 840;
    public static final int MARGEN_VENTANA = 22;
    public static final int MARGEN_TITULO_VENTANA = 40;

    private static float factor = 1f;

    /** Escala entera: valor * factor, con un minimo de 1 para que nada desaparezca. */
    public static int esc(int valor) {
        return Math.max(1, Math.round(valor * factor));
    }

    public static float factor() {
        return factor;
    }

    public static void establecerFactor(float nuevo) {
        factor = Math.max(0.55f, Math.min(1f, nuevo));
    }

    public static Font fuente(int tamano, int estilo) {
        return new Font("Segoe UI", estilo, esc(tamano));
    }

    // ======================= BOTONES =======================

    public enum TipoBoton {
        PRIMARIO, SECUNDARIO, PELIGRO, NEUTRO
    }

    /** Boton con fondo redondeado (degradado), hover y cursor de mano. */
    public static final class BotonUI extends JButton {
        private final TipoBoton tipo;
        private boolean hover;

        public BotonUI(String texto, TipoBoton tipo) {
            super(texto);
            this.tipo = tipo;
            setFont(fuente(13, Font.BOLD));
            setForeground(esTextoBlanco(tipo) ? Color.WHITE : TEXTO);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(esc(10), esc(16), esc(10), esc(16)));
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

        private boolean esTextoBlanco(TipoBoton t) {
            return t == TipoBoton.PRIMARIO || t == TipoBoton.PELIGRO;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color desde;
            Color hasta;
            switch (tipo) {
                case PRIMARIO -> {
                    desde = hover ? TEAL_CLARO : TEAL;
                    hasta = hover ? TEAL : TEAL_OSCURO;
                }
                case PELIGRO -> {
                    desde = hover ? new Color(239, 68, 68) : new Color(225, 45, 45);
                    hasta = new Color(190, 32, 34);
                }
                case NEUTRO -> {
                    desde = hover ? new Color(240, 244, 247) : new Color(235, 240, 245);
                    hasta = new Color(224, 231, 239);
                }
                default -> {
                    desde = hover ? new Color(246, 249, 252) : Color.WHITE;
                    hasta = new Color(230, 237, 244);
                }
            }
            g2.setPaint(new GradientPaint(0, 0, desde, 0, getHeight(), hasta));
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, esc(10), esc(10)));
            if (tipo == TipoBoton.SECUNDARIO || tipo == TipoBoton.NEUTRO) {
                g2.setColor(BORDE);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1.5, getHeight() - 1.5, esc(10), esc(10)));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ======================= CAMPOS =======================

    /** Campo de texto con fondo redondeado y texto gris de ayuda. */
    public static class CampoTextoUI extends JTextField {
        private final String ayuda;
        private final int radio;

        public CampoTextoUI(String ayuda) {
            this.ayuda = ayuda;
            this.radio = esc(10);
            setFont(fuente(13, Font.PLAIN));
            setForeground(TEXTO);
            setCaretColor(TEAL_OSCURO);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(esc(9), esc(12), esc(9), esc(12)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radio, radio);
            g2.setColor(BORDE);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radio, radio);
            g2.dispose();
            super.paintComponent(g);
            if (getText().isEmpty() && ayuda != null && !ayuda.isEmpty()) {
                Graphics2D h = (Graphics2D) g.create();
                h.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                h.setColor(TEXTO_SUAVE);
                h.setFont(getFont());
                h.drawString(ayuda, getInsets().left + esc(2),
                        getBaseline(0, getHeight()) + esc(1));
                h.dispose();
            }
        }
    }

    /** Campo de texto para cifras, alinea a la derecha. */
    public static final class CampoNumericoUI extends CampoTextoUI {
        public CampoNumericoUI(String ayuda) {
            super(ayuda);
            setHorizontalAlignment(JTextField.RIGHT);
        }
    }

    // ======================= TARJETAS =======================

    /** Panel blanco redondeado con borde sutil (tarjeta). */
    public static final class PanelTarjeta extends JPanel {
        public PanelTarjeta() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BLANCO);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, esc(12), esc(12)));
            g2.setColor(BORDE);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1.5, getHeight() - 1.5, esc(12), esc(12)));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Encabezado de modulo: titulo grande + subtitulo. */
    public static final class Encabezado extends JPanel {
        private final JLabel titulo = new JLabel();
        private final JLabel subtitulo = new JLabel();

        public Encabezado(String tituloTexto, String subtituloTexto) {
            setOpaque(false);
            setLayout(new GridBagLayout());
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0;
            g.gridy = 0;
            g.weightx = 1.0;
            g.anchor = GridBagConstraints.WEST;
            titulo.setText(tituloTexto);
            titulo.setFont(fuente(22, Font.BOLD));
            titulo.setForeground(MARINO);
            add(titulo, g);
            if (subtituloTexto != null && !subtituloTexto.isEmpty()) {
                g.gridy = 1;
                g.insets = new Insets(esc(3), 0, 0, 0);
                subtitulo.setText(subtituloTexto);
                subtitulo.setFont(fuente(12, Font.PLAIN));
                subtitulo.setForeground(TEXTO_SUAVE);
                add(subtitulo, g);
            }
        }
    }

    // ======================= INSIGNIAS DE ESTADO =======================

    /** Insignia redondeada coloreada segun el estado del texto. */
    public static final class EtiquetaEstado extends JLabel {
        private final Color fondo;
        private final Color textoColor;

        public EtiquetaEstado(String estado) {
            super(estado == null ? "-" : estado);
            setHorizontalAlignment(JLabel.CENTER);
            setFont(fuente(11, Font.BOLD));
            Color[] paleta = colorDe(estado);
            this.fondo = paleta[0];
            this.textoColor = paleta[1];
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(esc(3), esc(10), esc(3), esc(10)));
        }

        private static Color[] colorDe(String estado) {
            String e = estado == null ? "" : estado.toUpperCase().trim();
            if (e.contains("CANCELADA") || e.contains("ANULADO") || e.contains("INACTIVO")) {
                return new Color[]{ROJO_CLARO, ROJO};
            }
            if (e.contains("ATENDIDA") || e.contains("ACTIVO")
                    || e.contains("CONTABILIZ") || e.contains("CONFIRMADA") || e.contains("PAGADO")) {
                return new Color[]{VERDE_CLARO, VERDE};
            }
            if (e.contains("PROGRAMADA") || e.contains("PENDIENTE")) {
                return new Color[]{AMBAR_CLARO, AMBAR};
            }
            return new Color[]{GRIS_CLARO, GRIS};
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, esc(12), esc(12));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ======================= TABLAS =======================

    /** Tabla estilizada con encabezado destacado, filas legibles y seleccion clara. */
    public static final class TablaUI extends JTable {
        private final int indiceColumnaEstado;

        public TablaUI(DefaultTableModel modelo, int indiceColumnaEstado) {
            super(modelo);
            this.indiceColumnaEstado = indiceColumnaEstado;
            setFillsViewportHeight(true);
            setShowGrid(false);
            setRowHeight(esc(36));
            setIntercellSpacing(new Dimension(0, 0));
            setFont(fuente(13, Font.PLAIN));
            setForeground(TEXTO);
            setSelectionBackground(new Color(224, 242, 254));
            setSelectionForeground(MARINO);
            setDefaultRenderer(Object.class, new CeldaRenderer());
            JTableHeader cabecera = getTableHeader();
            cabecera.setReorderingAllowed(false);
            cabecera.setDefaultRenderer(new CabeceraRenderer());
        }

        private final class CabeceraRenderer extends DefaultTableCellRenderer {
            CabeceraRenderer() {
                setOpaque(true);
                setHorizontalAlignment(JLabel.LEFT);
                setBorder(BorderFactory.createEmptyBorder(esc(8), esc(10), esc(8), esc(10)));
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                l.setText(value == null ? "" : value.toString());
                l.setFont(fuente(12, Font.BOLD));
                l.setForeground(Color.WHITE);
                l.setBackground(MARINO);
                return l;
            }
        }

        private final class CeldaRenderer extends DefaultTableCellRenderer {
            CeldaRenderer() {
                setOpaque(true);
                setBorder(BorderFactory.createEmptyBorder(esc(4), esc(10), esc(4), esc(10)));
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                if (column == indiceColumnaEstado) {
                    EtiquetaEstado insignia = new EtiquetaEstado(value == null ? "-" : value.toString());
                    if (isSelected) {
                        insignia.setBorder(BorderFactory.createCompoundBorder(
                                insignia.getBorder(),
                                BorderFactory.createLineBorder(new Color(125, 211, 252), 1)));
                    }
                    return insignia;
                }
                JLabel l = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                l.setText(value == null ? "" : value.toString());
                l.setFont(fuente(13, Font.PLAIN));
                l.setForeground(TEXTO);
                if (isSelected) {
                    l.setBackground(new Color(224, 242, 254));
                } else {
                    l.setBackground(row % 2 == 0 ? new Color(248, 250, 252) : BLANCO);
                }
                l.setBorder(BorderFactory.createEmptyBorder(esc(7), esc(10), esc(7), esc(10)));
                return l;
            }
        }
    }

    // ======================= HELPERS =======================

    public static JScrollPane enrutador(JComponent contenido) {
        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BLANCO);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(BLANCO);
        scroll.setOpaque(true);
        scroll.setBackground(BLANCO);
        return scroll;
    }

    public static JPanel fila(int eje, JComponent... componentes) {
        JPanel p = new JPanel(new FlowLayout(eje, esc(8), 0));
        p.setOpaque(false);
        for (JComponent c : componentes) {
            p.add(c);
        }
        return p;
    }

    public static String texto(Component c) {
        if (c instanceof JTextField t) {
            return t.getText().trim();
        }
        return "";
    }

    // ======================= DIAGNOSTICOS =======================

    public static JComboBox<String> combo(String[] opciones) {
        JComboBox<String> combo = new JComboBox<>(opciones);
        combo.setFont(fuente(13, Font.PLAIN));
        combo.setForeground(TEXTO);
        combo.setBackground(BLANCO);
        combo.setPreferredSize(new Dimension(esc(220), esc(38)));
        return combo;
    }

    public static JComboBox<Integer> comboEnteros(Integer[] opciones) {
        JComboBox<Integer> combo = new JComboBox<>(opciones);
        combo.setFont(fuente(13, Font.PLAIN));
        combo.setForeground(TEXTO);
        combo.setBackground(BLANCO);
        combo.setPreferredSize(new Dimension(esc(220), esc(38)));
        return combo;
    }

    /** Area de texto multilinea con estilo acorde al tema. */
    public static JComponent areaTexto(int filas, int columnas) {
        JTextArea area = new JTextArea(filas, columnas);
        area.setFont(fuente(13, Font.PLAIN));
        area.setForeground(TEXTO);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE, 1),
                BorderFactory.createEmptyBorder(esc(6), esc(8), esc(6), esc(8))));
        return new JScrollPane(area);
    }

    public static String textoArea(Component c) {
        if (c instanceof JScrollPane sp && sp.getViewport().getView() instanceof JTextArea ta) {
            return ta.getText().trim();
        }
        return "";
    }

    public static JPasswordField campoClave() {
        JPasswordField campo = new JPasswordField();
        campo.setFont(fuente(13, Font.PLAIN));
        campo.setForeground(TEXTO);
        campo.setCaretColor(TEAL_OSCURO);
        campo.setEchoChar('*');
        campo.setOpaque(false);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE, 1),
                BorderFactory.createEmptyBorder(esc(9), esc(12), esc(9), esc(12))));
        return campo;
    }

    public static String clave(JComponent c) {
        if (c instanceof JPasswordField p) {
            return new String(p.getPassword());
        }
        return "";
    }

    public static JLabel etiqueta(String texto, Color color, int tamano, int estilo) {
        JLabel l = new JLabel(texto);
        l.setForeground(color);
        l.setFont(fuente(tamano, estilo));
        return l;
    }

    public static JLabel etiqueta(String texto) {
        return etiqueta(texto, TEXTO, 13, Font.PLAIN);
    }

    // ======================= MENSAJES =======================

    public static void informacion(Component padre, String titulo, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, titulo, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component padre, String titulo, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirmar(Component padre, String titulo, String mensaje) {
        int opcion = JOptionPane.showConfirmDialog(
                padre, mensaje, titulo, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return opcion == JOptionPane.YES_OPTION;
    }

    /** Muestra un texto multilinea (detalle de registro) en una ventana de dialogo. */
    public static void panelTexto(Component padre, String titulo, String contenido) {
        JTextArea area = new JTextArea(contenido == null ? "" : contenido);
        area.setEditable(false);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setForeground(TEXTO);
        area.setBackground(BLANCO);
        area.setBorder(BorderFactory.createEmptyBorder(esc(8), esc(10), esc(8), esc(10)));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(esc(430), esc(260)));
        JOptionPane.showMessageDialog(padre, scroll, titulo, JOptionPane.INFORMATION_MESSAGE);
    }

    /** Dialoga y captura: muestra un dialogo con un mensaje y devuelve el texto ingresado o null. */
    public static String pregunta(Component padre, String titulo, String mensaje,
                                  String inicial, char caracter) {
        JPasswordField campo = new JPasswordField(inicial == null ? "" : inicial);
        campo.setFont(fuente(13, Font.PLAIN));
        campo.setForeground(TEXTO);
        campo.setEchoChar(caracter);
        Object[] seleccion = {mensaje, campo};
        int opcion = JOptionPane.showConfirmDialog(padre, seleccion, titulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion == JOptionPane.OK_OPTION) {
            return new String(campo.getPassword());
        }
        return null;
    }

    /** Parse una fecha dd/MM/yyyy validando el formato. */
    public static java.time.LocalDate fecha(String texto) {
        try {
            return java.time.LocalDate.parse(
                    texto == null ? "" : texto.trim(),
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "Fecha invalida '" + texto + "'. Use el formato dd/mm/aaaa.");
        }
    }

    public static java.time.LocalTime hora(String texto) {
        try {
            return java.time.LocalTime.parse(
                    texto == null ? "" : texto.trim(),
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "Hora invalida '" + texto + "'. Use el formato HH:mm.");
        }
    }

    public static String moneda(double valor) {
        return String.format("S/ %.2f", valor);
    }

    public static double numero(String texto) {
        try {
            return Double.parseDouble(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Monto invalido: '" + texto + "'.");
        }
    }

    public static int entero(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Numero entero invalido: '" + texto + "'.");
        }
    }

    // ======================= ICONOS =======================

    public enum Icono {
        DASHBOARD, PACIENTES, MEDICOS, CITAS, ATENCIONES, PAGOS,
        GASTOS, REPORTES, USUARIOS, CONFIG, SALIR, NUEVO, BUSCAR
    }

    /** Pinta un icono lineal sencillo y consistente. */
    public static void pintarIcono(Graphics2D g3, Icono icono, int tam) {
        if (tam <= 0) {
            return;
        }
        Graphics2D g = (Graphics2D) g3.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color antes = g.getColor();
        g.setColor(g.getColor());
        float s = Math.max(1f, tam / 12f);
        g.setStroke(new BasicStroke(s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double m = tam * 0.18;
        double a = tam * 0.12;
        double x0 = m;
        double y0 = m;
        double w = tam - 2 * m;
        double h = tam - 2 * m;
        switch (icono) {
            case DASHBOARD -> {
                double[] xs = {x0, x0 + w / 2, x0 + w};
                double[] ys = {y0 + h * 0.55, y0, y0 + h * 0.55};
                g.fillPolygon(toInt(xs), toInt(ys), 3);
                g.drawRoundRect((int) Math.round(x0 + w * 0.18), (int) Math.round(y0 + h * 0.35),
                        (int) Math.round(w * 0.64), (int) Math.round(h * 0.3),
                        (int) s * 2, (int) s * 2);
                g.drawLine((int) Math.round(x0 + w * 0.5), (int) Math.round(y0 + h * 0.35),
                        (int) Math.round(x0 + w * 0.5), (int) Math.round(y0 + h * 0.65));
            }
            case PACIENTES -> {
                g.drawArc((int) Math.round(x0 + w * 0.25), (int) Math.round(y0), (int) Math.round(w * 0.5),
                        (int) Math.round(h * 0.55), 0, 360);
                g.drawArc((int) Math.round(x0), (int) Math.round(y0 + h * 0.55), (int) Math.round(w),
                        (int) Math.round(h * 0.45), 0, -180);
            }
            case MEDICOS -> {
                g.drawOval((int) Math.round(x0), (int) Math.round(y0),
                        (int) Math.round(w), (int) Math.round(h));
                g.drawLine((int) Math.round(x0 + w / 2), (int) Math.round(y0 + h * 0.28),
                        (int) Math.round(x0 + w / 2), (int) Math.round(y0 + h * 0.72));
                g.drawLine((int) Math.round(x0 + w * 0.28), (int) Math.round(y0 + h / 2),
                        (int) Math.round(x0 + w * 0.72), (int) Math.round(y0 + h / 2));
            }
            case CITAS -> {
                g.drawRoundRect((int) Math.round(x0), (int) Math.round(y0 + h * 0.22),
                        (int) Math.round(w), (int) Math.round(h * 0.78),
                        (int) s * 2, (int) s * 2);
                g.drawLine((int) Math.round(x0 + w * 0.15), (int) Math.round(y0 + h * 0.22),
                        (int) Math.round(x0 + w * 0.15), (int) Math.round(y0 + h * 0.05));
                g.drawLine((int) Math.round(x0 + w * 0.85), (int) Math.round(y0 + h * 0.22),
                        (int) Math.round(x0 + w * 0.85), (int) Math.round(y0 + h * 0.05));
                g.drawLine((int) Math.round(x0 + w * 0.2), (int) Math.round(y0 + h * 0.5),
                        (int) Math.round(x0 + w * 0.8), (int) Math.round(y0 + h * 0.5));
                g.drawLine((int) Math.round(x0 + w * 0.2), (int) Math.round(y0 + h * 0.7),
                        (int) Math.round(x0 + w * 0.55), (int) Math.round(y0 + h * 0.7));
            }
            case ATENCIONES -> {
                g.drawRoundRect((int) Math.round(x0 - s), (int) Math.round(y0 + h * 0.12),
                        (int) Math.round(w + 2 * s), (int) Math.round(h * 0.62),
                        (int) s * 2, (int) s * 2);
                g.drawRoundRect((int) Math.round(x0 + w * 0.3), (int) Math.round(y0 - h * 0.18),
                        (int) Math.round(w * 0.4), (int) Math.round(h * 0.25),
                        (int) s * 2, (int) s * 2);
                g.drawLine((int) Math.round(x0 + w * 0.25), (int) Math.round(y0 + h * 0.55),
                        (int) Math.round(x0 + w * 0.45), (int) Math.round(y0 + h * 0.72));
                g.drawLine((int) Math.round(x0 + w * 0.45), (int) Math.round(y0 + h * 0.72),
                        (int) Math.round(x0 + w * 0.75), (int) Math.round(y0 + h * 0.4));
            }
            case PAGOS -> {
                g.drawOval((int) Math.round(x0), (int) Math.round(y0),
                        (int) Math.round(w), (int) Math.round(h));
                g.drawString("S", (int) Math.round(x0 + w * 0.38), (int) Math.round(y0 + h * 0.68));
            }
            case GASTOS -> {
                g.drawRoundRect((int) Math.round(x0), (int) Math.round(y0),
                        (int) Math.round(w), (int) Math.round(h), (int) s * 2, (int) s * 2);
                g.drawLine((int) Math.round(x0 + w * 0.25), (int) Math.round(y0 + h / 2),
                        (int) Math.round(x0 + w * 0.75), (int) Math.round(y0 + h / 2));
            }
            case REPORTES -> {
                double bx = x0 + w * 0.2;
                double base = y0 + h;
                g.drawLine((int) Math.round(x0), (int) Math.round(y0 + h),
                        (int) Math.round(x0 + w), (int) Math.round(y0 + h));
                g.fillRect((int) Math.round(bx), (int) Math.round(base - h * 0.62),
                        (int) Math.round(w * 0.18), (int) Math.round(h * 0.62));
                g.fillRect((int) Math.round(bx + w * 0.28), (int) Math.round(base - h * 0.35),
                        (int) Math.round(w * 0.18), (int) Math.round(h * 0.35));
                g.fillRect((int) Math.round(bx + w * 0.56), (int) Math.round(base - h * 0.8),
                        (int) Math.round(w * 0.18), (int) Math.round(h * 0.8));
            }
            case USUARIOS -> {
                g.drawArc((int) Math.round(x0 + w * 0.3), (int) Math.round(y0 - h * 0.08),
                        (int) Math.round(w * 0.4), (int) Math.round(h * 0.5), 0, 360);
                g.drawArc((int) Math.round(x0), (int) Math.round(y0 + h * 0.55), (int) Math.round(w),
                        (int) Math.round(h * 0.45), 0, -180);
            }
            case CONFIG -> {
                g.drawOval((int) Math.round(x0 + w * 0.3), (int) Math.round(y0 + h * 0.3),
                        (int) Math.round(w * 0.4), (int) Math.round(h * 0.4));
                for (int i = 0; i < 4; i++) {
                    double ang = Math.toRadians(i * 90);
                    double cx = x0 + w / 2 + Math.cos(ang) * w * 0.3;
                    double cy = y0 + h / 2 + Math.sin(ang) * h * 0.3;
                    g.drawLine((int) Math.round(cx - w * 0.08), (int) Math.round(cy - h * 0.08),
                            (int) Math.round(cx + w * 0.08), (int) Math.round(cy + h * 0.08));
                }
            }
            case SALIR -> {
                g.drawRoundRect((int) Math.round(x0 + w * 0.15), (int) Math.round(y0 + h * 0.12),
                        (int) Math.round(w * 0.5), (int) Math.round(h * 0.76),
                        (int) s * 2, (int) s * 2);
                g.drawLine((int) Math.round(x0 + w * 0.15), (int) Math.round(y0 + h / 2),
                        (int) Math.round(x0 + w * 0.8), (int) Math.round(y0 + h / 2));
                g.drawLine((int) Math.round(x0 + w * 0.72), (int) Math.round(y0 + h * 0.35),
                        (int) Math.round(x0 + w * 0.9), (int) Math.round(y0 + h / 2));
                g.drawLine((int) Math.round(x0 + w * 0.72), (int) Math.round(y0 + h * 0.65),
                        (int) Math.round(x0 + w * 0.9), (int) Math.round(y0 + h / 2));
            }
            case NUEVO -> {
                g.drawOval((int) Math.round(x0), (int) Math.round(y0),
                        (int) Math.round(w), (int) Math.round(h));
                g.drawLine((int) Math.round(x0 + w / 2), (int) Math.round(y0 + h * 0.3),
                        (int) Math.round(x0 + w / 2), (int) Math.round(y0 + h * 0.7));
                g.drawLine((int) Math.round(x0 + w * 0.3), (int) Math.round(y0 + h / 2),
                        (int) Math.round(x0 + w * 0.7), (int) Math.round(y0 + h / 2));
            }
            case BUSCAR -> {
                g.drawOval((int) Math.round(x0 + w * 0.05), (int) Math.round(y0 + h * 0.05),
                        (int) Math.round(w * 0.6), (int) Math.round(h * 0.6));
                g.drawLine((int) Math.round(x0 + w * 0.55), (int) Math.round(y0 + h * 0.55),
                        (int) Math.round(x0 + w * 0.95), (int) Math.round(y0 + h * 0.95));
            }
            default -> { }
        }
        g.setColor(antes);
        g.dispose();
    }

    /** Panel que pinta un icono del tema. */
    public static final class PanelIcono extends JPanel {
        private final Icono icono;
        private final int tam;
        private Color color = BLANCO;

        public PanelIcono(Icono icono, int tamLogico) {
            this.icono = icono;
            this.tam = tamLogico;
            setOpaque(false);
            int t = esc(tam);
            setPreferredSize(new Dimension(t, t));
        }

        public void color(Color c) {
            this.color = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getWidth() <= 0) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(color);
            int t = Math.min(getWidth(), getHeight());
            pintarIcono(g2, icono, t);
            g2.dispose();
        }
    }

    private static int[] toInt(double[] v) {
        int[] r = new int[v.length];
        for (int i = 0; i < v.length; i++) {
            r[i] = (int) Math.round(v[i]);
        }
        return r;
    }
}