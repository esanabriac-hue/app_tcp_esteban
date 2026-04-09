package org.vinni.servidor.gui;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class LoadBalancerSrv {
    private static final int LB_PORT = 12340;
    private static final int[] SERVER_PORTS = {12345, 12346, 12347};
    private static int currentIndex = 0;

    public static void main(String[] args) {
        System.out.println("🚀 Balanceador de Carga iniciado en el puerto " + LB_PORT);

        try (ServerSocket serverSocket = new ServerSocket(LB_PORT)) {
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("Solicitud de conexión recibida desde: " + client.getInetAddress());

                // Asignar el siguiente puerto disponible (Round Robin)
                int puertoAsignado = SERVER_PORTS[currentIndex];
                currentIndex = (currentIndex + 1) % SERVER_PORTS.length;

                // Enviarle el puerto al cliente
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                out.println("REDIRECT:" + puertoAsignado);

                System.out.println("Redirigiendo cliente al puerto " + puertoAsignado);

                // Cerramos la conexión con el balanceador. El cliente ahora irá al servidor real.
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
