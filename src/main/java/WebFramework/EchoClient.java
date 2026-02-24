package WebFramework;

import java.io.*;
import java.net.*;

public class EchoClient {

    public static void main(String[] args) {
        Socket echoSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            // Conexión al servidor en localhost:35000
            echoSocket = new Socket("127.0.0.1", 35000);

            // Flujo de salida hacia el servidor
            out = new PrintWriter(echoSocket.getOutputStream(), true);

            // Flujo de entrada desde el servidor
            in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));

            // Flujo de entrada desde la consola del usuario
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

            String userInput;
            System.out.println("Cliente conectado. Escribe mensajes para enviar al servidor:");
            while ((userInput = stdIn.readLine()) != null) {
                out.println(userInput); // enviar al servidor
                System.out.println("echo: " + in.readLine()); // recibir respuesta
            }

            // Cerrar recursos
            out.close();
            in.close();
            stdIn.close();
            echoSocket.close();

        } catch (UnknownHostException e) {
            System.err.println("No se conoce el host: localhost");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error de I/O al intentar conectarse a localhost");
            System.exit(1);
        }
    }
}
