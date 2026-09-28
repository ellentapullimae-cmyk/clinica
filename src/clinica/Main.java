package clinica;

import clinica.web.ServidorWeb;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;

/**
 * Punto de entrada del Sistema de Gestion para una Clinica.
 * Pone en marcha el servidor web que sirve la interfaz en el navegador y la
 * API que reutiliza toda la logica de negocio existente (controlador ->
 * servicio -> DAO). La terminal se usa unicamente para ejecutar el servidor.
 */
public class Main {

    public static void main(String[] args) {
        try {
            HttpServer servidor = ServidorWeb.iniciar();
            // El servidor ya corre en segundo plano; avisamos de la guia.
            System.out.println("Abre el sistema en tu navegador. Presiona Ctrl+C " +
                    "o cierra esta ventana para detener el servidor.");
            Runtime.getRuntime().addShutdownHook(new Thread(
                    () -> System.out.println("\nServidor detenido."),
                    "shutdown-web"));
        } catch (IOException e) {
            System.err.println("No se pudo iniciar el servidor web: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}