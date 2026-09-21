package clinica.datos;

import clinica.modelo.Rol;
import clinica.modelo.Usuario;
import clinica.util.UtilErrores;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la entidad Usuario (login y control de acceso).
 */
public class UsuarioDAO {

    private static final String SELECT =
            "SELECT u.id_usuario, u.nombre_usuario, u.nombre_completo, u.clave_hash, u.sal, "
            + "u.rol, u.id_medico, u.estado, u.fecha_registro, "
            + "m.nombre AS nombre_medico "
            + "FROM usuario u "
            + "LEFT JOIN medico m ON m.id_medico = u.id_medico ";

    private static final String POR_NOMBRE = SELECT + "WHERE u.nombre_usuario = ?";
    private static final String POR_ID = SELECT + "WHERE u.id_usuario = ?";
    private static final String LISTAR = SELECT + "ORDER BY u.id_usuario";
    private static final String INSERTAR =
            "INSERT INTO usuario (nombre_usuario, nombre_completo, clave_hash, sal, rol, id_medico, estado) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String ACTUALIZAR_DATOS =
            "UPDATE usuario SET nombre_completo = ?, rol = ?, id_medico = ?, estado = ? WHERE id_usuario = ?";
    private static final String ACTUALIZAR_CLAVE =
            "UPDATE usuario SET clave_hash = ?, sal = ? WHERE id_usuario = ?";
    private static final String EXISTE_NOMBRE =
            "SELECT COUNT(*) FROM usuario WHERE nombre_usuario = ? AND id_usuario <> ?";
    private static final String EXISTE_VINCULO_MEDICO =
            "SELECT COUNT(*) FROM usuario WHERE id_medico = ? AND rol = 'MEDICO' AND id_usuario <> ?";
    private static final String CONTRAR_ADMIN_ACTIVOS =
            "SELECT COUNT(*) FROM usuario WHERE rol = 'ADMINISTRADOR' AND estado = 'ACTIVO'";

    public Usuario obtenerPorNombreUsuario(String nombreUsuario) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(POR_NOMBRE)) {
            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("consultar el usuario", e);
        }
        return null;
    }

    public Usuario buscarPorId(int idUsuario) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(POR_ID)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("consultar el usuario", e);
        }
        return null;
    }

    public List<Usuario> listar() {
        List<Usuario> usuarios = new ArrayList<>();
        try (Statement st = ConexionBD.obtenerInstancia().getConexion().createStatement();
             ResultSet rs = st.executeQuery(LISTAR)) {
            while (rs.next()) {
                usuarios.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("listar los usuarios", e);
        }
        return usuarios;
    }

    public int insertar(Usuario usuario) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, usuario.getNombreUsuario());
            ps.setString(2, usuario.getNombreCompleto());
            ps.setString(3, usuario.getClaveHash());
            ps.setString(4, usuario.getSal());
            ps.setString(5, usuario.getRol().name());
            if (usuario.getIdMedico() == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, usuario.getIdMedico());
            }
            ps.setString(7, usuario.isActivo() ? "ACTIVO" : "INACTIVO");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw UtilErrores.errorBD("registrar el usuario", e);
        }
    }

    public void actualizarDatos(Usuario usuario) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(ACTUALIZAR_DATOS)) {
            ps.setString(1, usuario.getNombreCompleto());
            ps.setString(2, usuario.getRol().name());
            if (usuario.getIdMedico() == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, usuario.getIdMedico());
            }
            ps.setString(4, usuario.isActivo() ? "ACTIVO" : "INACTIVO");
            ps.setInt(5, usuario.getIdUsuario());
            if (ps.executeUpdate() == 0) {
                throw new RuntimeException("El usuario con id " + usuario.getIdUsuario() + " no existe.");
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("actualizar el usuario", e);
        }
    }

    public void actualizarClave(int idUsuario, String claveHash, String sal) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(ACTUALIZAR_CLAVE)) {
            ps.setString(1, claveHash);
            ps.setString(2, sal);
            ps.setInt(3, idUsuario);
            if (ps.executeUpdate() == 0) {
                throw new RuntimeException("El usuario con id " + idUsuario + " no existe.");
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("actualizar la clave", e);
        }
    }

    public boolean existeMedicoVinculado(int idMedico, int excluirId) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(EXISTE_VINCULO_MEDICO)) {
            ps.setInt(1, idMedico);
            ps.setInt(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("verificar el vinculo medico", e);
        }
        return false;
    }

    public boolean existeNombreUsuario(String nombreUsuario, int excluirId) {
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion()
                .prepareStatement(EXISTE_NOMBRE)) {
            ps.setString(1, nombreUsuario);
            ps.setInt(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("verificar el nombre de usuario", e);
        }
        return false;
    }

    public int contarAdministradoresActivos() {
        return contar(CONTRAR_ADMIN_ACTIVOS);
    }

    private int contar(String sql) {
        try (Statement st = ConexionBD.obtenerInstancia().getConexion().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("consultar la base de datos", e);
        }
        return 0;
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(rs.getInt("id_usuario"));
        usuario.setNombreUsuario(rs.getString("nombre_usuario"));
        usuario.setNombreCompleto(rs.getString("nombre_completo"));
        usuario.setClaveHash(rs.getString("clave_hash"));
        usuario.setSal(rs.getString("sal"));
        usuario.setRol(Rol.valueOf(rs.getString("rol")));
        usuario.setIdMedico(rs.getObject("id_medico") == null ? null : rs.getInt("id_medico"));
        usuario.setActivo("ACTIVO".equals(rs.getString("estado")));
        Timestamp registro = rs.getTimestamp("fecha_registro");
        if (registro != null) {
            usuario.setFechaRegistro(registro.toLocalDateTime());
        }
        usuario.setNombreMedicoVinculado(rs.getString("nombre_medico"));
        return usuario;
    }
}