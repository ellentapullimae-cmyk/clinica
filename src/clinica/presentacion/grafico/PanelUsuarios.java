package clinica.presentacion.grafico;

import clinica.controlador.MedicoController;
import clinica.controlador.UsuarioController;
import clinica.modelo.Atencion;
import clinica.modelo.Medico;
import clinica.modelo.Rol;
import clinica.modelo.Usuario;
import clinica.presentacion.Sesion;
import java.awt.Font;
import java.util.List;
import javax.swing.JComboBox;

/**
 * Modulo de usuarios (login y acceso). Solo ADMINISTRADOR (gestionarUsuarios).
 * La clave nunca se muestra: se registra y se cambia por formulario.
 */
public final class PanelUsuarios extends PanelModulo {

    private final UsuarioController controlador;
    private final MedicoController medicos;

    public PanelUsuarios(VentanaPrincipal ventana, UsuarioController controlador,
                         MedicoController medicos) {
        super(ventana);
        this.controlador = controlador;
        this.medicos = medicos;

        if (rol().gestionarUsuarios()) {
            agregarBoton("Nuevo usuario", Controles.TipoBoton.PRIMARIO,
                    e -> registrarUsuario());
            agregarBotonSeleccion("Editar", Controles.TipoBoton.SECUNDARIO,
                    e -> editarUsuario());
            agregarBotonSeleccion("Cambiar clave", Controles.TipoBoton.NEUTRO,
                    e -> cambiarClaveUsuario());
            agregarBotonSeleccion("Activar/Desactivar", Controles.TipoBoton.NEUTRO,
                    e -> activarDesactivar());
        }
        configurarAncho(new int[]{60, 150, 260, 160, 220, 110});
    }

    @Override
    protected String[] columnas() {
        return new String[]{"ID", "USUARIO", "NOMBRE COMPLETO", "ROL", "MEDICO VINCULADO", "ESTADO"};
    }

    @Override
    protected int indiceColumnaEstado() {
        return 5;
    }

    @Override
    protected String titulo() {
        return "Usuarios";
    }

    @Override
    protected String subtitulo() {
        return "Gestion de cuentas de acceso al sistema.";
    }

    @Override
    protected void recargarDatos() {
        vaciarTabla();
        for (Usuario u : controlador.listar()) {
            agregarFila(u.getIdUsuario(), u.getNombreUsuario(), u.getNombreCompleto(),
                    u.getRol().name(),
                    u.getNombreMedicoVinculado() == null ? "-" : u.getNombreMedicoVinculado(),
                    u.isActivo() ? "ACTIVO" : "INACTIVO");
        }
    }

    private void registrarUsuario() {
        FormularioDialogo form = new FormularioDialogo(ventana, "Nuevo usuario", 440);
        form.campo("Nombre de usuario", new Controles.CampoTextoUI("usuario"));
        form.campo("Nombre completo", new Controles.CampoTextoUI("Nombre completo"));
        form.campo("Clave (minimo 6)", Controles.campoClave());
        form.campo("Confirme la clave", Controles.campoClave());
        form.campo("Rol", nuevoRolCombo());
        if (form.mostrar()) {
            try {
                String clave = Controles.clave(form.campo(2));
                if (!clave.equals(Controles.clave(form.campo(3)))) {
                    throw new IllegalArgumentException("Las claves no coinciden.");
                }
                Rol rol = (Rol) ((JComboBox<?>) form.campo(4)).getSelectedItem();
                Integer idMedico = null;
                if (rol == Rol.MEDICO) {
                    idMedico = leerMedicoVinculado();
                }
                Usuario u = controlador.registrar(
                        Controles.texto(form.campo(0)),
                        Controles.texto(form.campo(1)),
                        clave, rol, idMedico);
                Controles.informacion(this, "Usuario",
                        "Usuario registrado con id " + u.getIdUsuario()
                                + ". La clave se almacena protegida (cifrada).");
                recargarDatos();
            } catch (RuntimeException ex) {
                Controles.error(this, "Error", ex.getMessage());
            }
        }
    }

    private void editarUsuario() {
        Object[] fila = filaSeleccionada();
        if (fila == null) {
            return;
        }
        int id = (int) fila[0];
        Usuario actual = controlador.buscarPorId(id);
        if (actual == null) {
            Controles.error(this, "Usuario", "No se encontro el usuario.");
            return;
        }
        FormularioDialogo form = new FormularioDialogo(ventana,
                "Editar usuario - " + actual.getNombreUsuario(), 440);
        Controles.CampoTextoUI nombreCompleto = new Controles.CampoTextoUI("Nombre completo");
        nombreCompleto.setText(actual.getNombreCompleto() == null ? "" : actual.getNombreCompleto());
        form.campo("Nombre completo", nombreCompleto);
        form.campo("Rol", nuevoRolCombo(actual.getRol()));
        JComboBox<Integer> estadoCombo = new JComboBox<>(new Integer[]{1, 2});
        estadoCombo.setFont(Controles.fuente(13, Font.PLAIN));
        estadoCombo.setPreferredSize(new java.awt.Dimension(Controles.esc(140), Controles.esc(36)));
        estadoCombo.setSelectedItem(actual.isActivo() ? 1 : 2);
        form.campo("Estado (1=Activo, 2=Inactivo)", estadoCombo);
        if (form.mostrar()) {
            try {
                Rol rol = (Rol) ((JComboBox<?>) form.campo(1)).getSelectedItem();
                Integer idMedico = null;
                if (rol == Rol.MEDICO) {
                    idMedico = leerMedicoVinculado();
                }
                boolean activo = (Integer) estadoCombo.getSelectedItem() == 1;
                controlador.actualizarDatos(id,
                        Controles.texto(form.campo(0)), rol, idMedico, activo,
                        Sesion.getIdUsuarioSesion());
                Controles.informacion(this, "Usuario", "Usuario actualizado correctamente.");
                recargarDatos();
            } catch (RuntimeException ex) {
                Controles.error(this, "Error", ex.getMessage());
            }
        }
    }

    private void cambiarClaveUsuario() {
        Object[] fila = filaSeleccionada();
        if (fila == null) {
            return;
        }
        int id = (int) fila[0];
        Usuario actual = controlador.buscarPorId(id);
        if (actual == null) {
            Controles.error(this, "Usuario", "No se encontro el usuario.");
            return;
        }
        FormularioDialogo form = new FormularioDialogo(ventana,
                "Cambiar clave de " + actual.getNombreUsuario(), 420);
        form.campo("Nueva clave (minimo 6)", Controles.campoClave());
        form.campo("Confirme la nueva clave", Controles.campoClave());
        if (form.mostrar()) {
            try {
                String clave = Controles.clave(form.campo(0));
                if (!clave.equals(Controles.clave(form.campo(1)))) {
                    throw new IllegalArgumentException("Las claves no coinciden.");
                }
                controlador.cambiarClave(id, clave);
                Controles.informacion(this, "Usuario", "Clave actualizada correctamente.");
            } catch (RuntimeException ex) {
                Controles.error(this, "Error", ex.getMessage());
            }
        }
    }

    private void activarDesactivar() {
        Object[] fila = filaSeleccionada();
        if (fila == null) {
            return;
        }
        int id = (int) fila[0];
        Usuario actual = controlador.buscarPorId(id);
        if (actual == null) {
            Controles.error(this, "Usuario", "No se encontro el usuario.");
            return;
        }
        boolean nuevo = !actual.isActivo();
        if (!Controles.confirmar(this, "Cambiar estado",
                (nuevo ? "Activar" : "Desactivar") + " la cuenta \""
                        + actual.getNombreUsuario() + "\"?")) {
            return;
        }
        try {
            controlador.cambiarEstado(id, nuevo, Sesion.getIdUsuarioSesion());
            Controles.informacion(this, "Usuario",
                    "Cuenta " + (nuevo ? "activada" : "desactivada") + " correctamente.");
            recargarDatos();
        } catch (RuntimeException ex) {
            Controles.error(this, "Error", ex.getMessage());
        }
    }

    private Integer leerMedicoVinculado() {
        List<Medico> lista = medicos.listarActivos();
        if (lista.isEmpty()) {
            throw new IllegalArgumentException("No hay medicos activos. Registre un medico primero.");
        }
        JComboBox<Medico> combo = new JComboBox<>();
        for (Medico m : lista) {
            combo.addItem(m);
        }
        combo.setFont(Controles.fuente(13, Font.PLAIN));
        combo.setRenderer(new javax.swing.plaf.basic.BasicComboBoxRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Medico m) {
                    setText("id " + m.getIdMedico() + " - " + m.getNombre()
                            + " (" + m.getEspecialidad() + ")");
                }
                return this;
            }
        });
        FormularioDialogo form = new FormularioDialogo(ventana, "Medico vinculado", 420);
        form.campo("Medicos activos", combo);
        if (!form.mostrar()) {
            return null;
        }
        Medico seleccionado = (Medico) combo.getSelectedItem();
        if (seleccionado == null) {
            throw new IllegalArgumentException("Debe seleccionar un medico.");
        }
        return seleccionado.getIdMedico();
    }

    private JComboBox<Rol> nuevoRolCombo() {
        return nuevoRolCombo(null);
    }

    private JComboBox<Rol> nuevoRolCombo(Rol seleccion) {
        JComboBox<Rol> combo = new JComboBox<>(Rol.values());
        combo.setFont(Controles.fuente(13, Font.PLAIN));
        combo.setPreferredSize(new java.awt.Dimension(Controles.esc(200), Controles.esc(36)));
        if (seleccion != null) {
            combo.setSelectedItem(seleccion);
        }
        return combo;
    }
}