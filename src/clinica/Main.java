package clinica;

import clinica.controlador.AtencionController;
import clinica.controlador.CitaController;
import clinica.controlador.DashboardController;
import clinica.controlador.GastoController;
import clinica.controlador.MedicoController;
import clinica.controlador.PacienteController;
import clinica.controlador.PagoController;
import clinica.controlador.ReporteController;
import clinica.controlador.UsuarioController;
import clinica.presentacion.Menu;

/**
 * Punto de entrada del Sistema de Gestion para una Clinica.
 * Compone los controladores (arquitectura por capas) y lanza el menu
 * con login y control de acceso por rol.
 */
public class Main {

    public static void main(String[] args) {
        Menu menu = new Menu(
                new PacienteController(),
                new MedicoController(),
                new CitaController(),
                new AtencionController(),
                new PagoController(),
                new GastoController(),
                new ReporteController(),
                new UsuarioController(),
                new DashboardController());
        menu.iniciar();
    }
}