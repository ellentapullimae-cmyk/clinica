package clinica.presentacion.grafico;

import clinica.presentacion.Sesion;
import clinica.presentacion.grafico.Controles.TablaUI;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;

/**
 * Panel base de los modulos con listado: encabezado, buscador, tabla estilizada
 * con seleccion y una barra de acciones configurables por cada modulo.
 */
public abstract class PanelModulo extends JPanel {

    protected final VentanaPrincipal ventana;
    protected final DefaultTableModel modelo;
    protected final TablaUI tabla;
    protected final JTextField buscador;
    protected final JPanel barraAcciones;

    private TableRowSorter<DefaultTableModel> sorter;
    private final List<JButton> botonesSeleccion = new ArrayList<>();
    private final List<Integer> columnasFiltro = new ArrayList<>();

    protected PanelModulo(VentanaPrincipal ventana) {
        this.ventana = ventana;
        this.modelo = new DefaultTableModel(columnas(), 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        this.tabla = new TablaUI(modelo, indiceColumnaEstado());
        this.buscador = new Controles.CampoTextoUI("Buscar...");
        this.barraAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, Controles.esc(8), 0));
        this.barraAcciones.setOpaque(false);
        construir();
    }

    /** Nombres de las columnas del modulo. */
    protected abstract String[] columnas();

    /** Indice de la columna de estado para renderizar con insignia; -1 si no hay. */
    protected int indiceColumnaEstado() {
        return -1;
    }

    /** Relaciona los datos con el modelo de la tabla. */
    protected abstract void recargarDatos();

    private void construir() {
        setLayout(new BorderLayout());
        setBackground(Controles.FONDO);

        JPanel norte = new JPanel(new GridBagLayout());
        norte.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, Controles.esc(10), 0);
        norte.add(encabezado(), g);

        g.gridy = 1;
        JPanel fila = new JPanel(new BorderLayout(Controles.esc(8), 0));
        fila.setOpaque(false);
        buscarConfig();
        fila.add(buscador, BorderLayout.CENTER);
        fila.add(barraAcciones, BorderLayout.EAST);
        norte.add(fila, g);

        add(norte, BorderLayout.NORTH);

        sorter = new TableRowSorter<>(modelo);
        tabla.setRowSorter(sorter);
        JScrollPane scroll = Controles.enrutador(tabla);
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(true);
        contenedor.setBackground(Controles.BLANCO);
        contenedor.add(scroll, BorderLayout.CENTER);
        scroll.setBorder(javax.swing.BorderFactory.createLineBorder(Controles.BORDE, 1));
        add(contenedor, BorderLayout.CENTER);

        configurarSeleccion();

        recargarDatos();
    }

    /** Encabezado del modulo por defecto (titulo + subtitulo). */
    protected JComponent encabezado() {
        return new Controles.Encabezado(titulo(), subtitulo());
    }

    protected String titulo() {
        return "";
    }

    protected String subtitulo() {
        return "";
    }

    private void buscarConfig() {
        buscador.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filtrar();
            }
        });
    }

    private void filtrar() {
        RowFilter<DefaultTableModel, Object> rf = null;
        String texto = buscador.getText().trim().toLowerCase();
        if (!texto.isEmpty()) {
            final String patron = java.util.regex.Pattern.quote(texto);
            final int[] cols = new int[columnasFiltro.size()];
            for (int i = 0; i < cols.length; i++) {
                cols[i] = columnasFiltro.get(i);
            }
            rf = new RowFilter<>() {
                @Override
                public boolean include(RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entrada) {
                    int n = modelo.getColumnCount();
                    if (cols.length == 0) {
                        for (int c = 0; c < n; c++) {
                            Object v = entrada.getValue(c);
                            if (v != null && v.toString().toLowerCase().contains(patron)) {
                                return true;
                            }
                        }
                        return false;
                    }
                    for (int c : cols) {
                        Object v = entrada.getValue(c);
                        if (v != null && v.toString().toLowerCase().contains(patron)) {
                            return true;
                        }
                    }
                    return false;
                }
            };
        }
        sorter.setRowFilter(rf);
    }

    /** Configura la seleccion de la tabla (una fila a la vez y refresco de botones). */
    protected void configurarSeleccion() {
        tabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refrescarBotones();
            }
        });
        refrescarBotones();
    }

    /** Permite registrar una columna sobre la que filtra el buscador (ademas del resto). */
    protected void agregarColumnaFiltro(int indice) {
        columnasFiltro.add(indice);
    }

    /** Agrega un boton de accion habilitado solo cuando hay fila seleccionada. */
    protected void agregarBotonSeleccion(String texto, Controles.TipoBoton tipo,
                                         java.awt.event.ActionListener accion) {
        JButton boton = new Controles.BotonUI(texto, tipo);
        boton.setEnabled(false);
        boton.addActionListener(accion);
        botonesSeleccion.add(boton);
        barraAcciones.add(boton);
    }

    /** Agrega un boton de accion siempre habilitado (nuevo, exportar, etc.). */
    protected void agregarBoton(String texto, Controles.TipoBoton tipo,
                                java.awt.event.ActionListener accion) {
        JButton boton = new Controles.BotonUI(texto, tipo);
        boton.addActionListener(accion);
        barraAcciones.add(boton);
    }

    /** Devuelve la fila seleccionada o null si no hay seleccion. */
    protected Object[] filaSeleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Controles.error(this, "Seleccion", "Seleccione una fila de la tabla.");
            return null;
        }
        int vista = tabla.convertRowIndexToModel(fila);
        Object[] datos = new Object[modelo.getColumnCount()];
        for (int i = 0; i < datos.length; i++) {
            datos[i] = modelo.getValueAt(vista, i);
        }
        return datos;
    }

    protected void refrescarBotones() {
        boolean hay = tabla.getSelectedRow() >= 0;
        for (JButton b : botonesSeleccion) {
            b.setEnabled(hay);
        }
    }

    protected void vaciarTabla() {
        modelo.setRowCount(0);
        refrescarBotones();
    }

    protected void agregarFila(Object... valores) {
        modelo.addRow(valores);
    }

    protected void configurarAncho(int[] anchos) {
        tabla.setAutoResizeMode(jTabvResizeOff());
        javax.swing.table.TableColumnModel cm = tabla.getColumnModel();
        int total = 0;
        for (int i = 0; i < anchos.length && i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setPreferredWidth(Controles.esc(anchos[i]));
            total += anchos[i];
        }
        Dimension d = new Dimension(Controles.esc(total) + Controles.esc(40),
                tabla.getPreferredSize().height);
        tabla.setPreferredScrollableViewportSize(d);
        tabla.setTableHeader(tabla.getTableHeader());
    }

    private int jTabvResizeOff() {
        return javax.swing.JTable.AUTO_RESIZE_OFF;
    }

    protected boolean haySeleccion() {
        return tabla.getSelectedRow() >= 0;
    }

    protected RolActual rol() {
        return new RolActual();
    }

    protected static final class RolActual {
        public boolean gestionarMedicos() {
            return Sesion.getRolActual().gestionarMedicos();
        }

        public boolean registrarPaciente() {
            return Sesion.getRolActual().registrarPaciente();
        }

        public boolean actualizarPaciente() {
            return Sesion.getRolActual().actualizarPaciente();
        }

        public boolean programarCita() {
            return Sesion.getRolActual().programarCita();
        }

        public boolean modificarCita() {
            return Sesion.getRolActual().modificarCita();
        }

        public boolean registrarAtencion() {
            return Sesion.getRolActual().registrarAtencion();
        }

        public boolean gestionarPagos() {
            return Sesion.getRolActual().gestionarPagos();
        }

        public boolean gestionarGastos() {
            return Sesion.getRolActual().gestionarGastos();
        }

        public boolean gestionarUsuarios() {
            return Sesion.getRolActual().gestionarUsuarios();
        }

        public boolean gestionarPresupuesto() {
            return Sesion.getRolActual().gestionarPresupuesto();
        }
    }
}