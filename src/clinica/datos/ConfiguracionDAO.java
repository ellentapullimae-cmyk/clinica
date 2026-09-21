package clinica.datos;

import clinica.util.UtilErrores;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Acceso a la configuracion de la clinica (presupuesto del periodo).
 */
public class ConfiguracionDAO {

    private static final String CLAVE_PRESUPUESTO = "presupuesto_periodo";
    private static final String CONSULTAR =
            "SELECT valor FROM configuracion WHERE clave = '" + CLAVE_PRESUPUESTO + "'";
    private static final String ACTUALIZAR =
            "UPDATE configuracion SET valor = ? WHERE clave = '" + CLAVE_PRESUPUESTO + "'";

    public double obtenerPresupuesto() {
        try (Statement st = ConexionBD.obtenerInstancia().getConexion().createStatement();
             ResultSet rs = st.executeQuery(CONSULTAR)) {
            if (rs.next()) {
                return rs.getDouble("valor");
            }
        } catch (SQLException e) {
            throw UtilErrores.errorBD("consultar el presupuesto", e);
        }
        return 0;
    }

    public void actualizarPresupuesto(double nuevoPresupuesto) {
        if (nuevoPresupuesto <= 0) {
            throw new IllegalArgumentException(
                    "El presupuesto del periodo debe ser mayor que cero.");
        }
        try (PreparedStatement ps = ConexionBD.obtenerInstancia().getConexion().prepareStatement(ACTUALIZAR)) {
            ps.setDouble(1, nuevoPresupuesto);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw UtilErrores.errorBD("actualizar el presupuesto", e);
        }
    }
}