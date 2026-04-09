package org.vinni.servidor.gui;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class PrincipalSrv extends javax.swing.JFrame {
    private final int PORT = 12345;
    private ServerSocket serverSocket;
    private boolean servidorActivo = false;

    private static final Map<String, PrintWriter> clientes = new ConcurrentHashMap<>();

    private javax.swing.JButton bIniciar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextArea mensajesTxt;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton bReiniciar;

    public PrincipalSrv() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        this.setTitle("Servidor ...");

        bIniciar = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        mensajesTxt = new JTextArea();
        jScrollPane1 = new javax.swing.JScrollPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        bIniciar.setFont(new java.awt.Font("Segoe UI", 0, 18));
        bIniciar.setText("INICIAR SERVIDOR");
        bIniciar.addActionListener(evt -> bIniciarActionPerformed(evt));
        getContentPane().add(bIniciar);
        bIniciar.setBounds(100, 90, 250, 40);

        bReiniciar = new javax.swing.JButton();
        bReiniciar.setText("REINICIAR SERVIDOR");
        bReiniciar.setFont(new java.awt.Font("Segoe UI", 0, 18));
        bReiniciar.setBounds(100, 140, 250, 40);
        bReiniciar.addActionListener(evt -> reiniciarServidor());
        getContentPane().add(bReiniciar);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14));
        jLabel1.setForeground(new java.awt.Color(204, 0, 0));
        jLabel1.setText("SERVIDOR TCP : HOEL");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(150, 10, 160, 17);

        mensajesTxt.setColumns(25);
        mensajesTxt.setRows(5);
        jScrollPane1.setViewportView(mensajesTxt);
        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(20, 160, 410, 70);

        setSize(new java.awt.Dimension(491, 290));
        setLocationRelativeTo(null);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new PrincipalSrv().setVisible(true));
    }

    private void bIniciarActionPerformed(java.awt.event.ActionEvent evt) {
        iniciarServidor();
    }

    private void iniciarServidor() {
        new Thread(() -> {
            try {
                InetAddress addr = InetAddress.getLocalHost();
                serverSocket = new ServerSocket(PORT);
                servidorActivo = true;

                mensajesTxt.append("Servidor iniciado en puerto " + PORT + "\n");

                ExecutorService pool = Executors.newCachedThreadPool();

                // 🔥 MEJORA: Hilo Demonio para Heartbeats (Mantiene vivos los sockets y detecta caídas)
                Thread heartbeatThread = new Thread(() -> {
                    while (servidorActivo) {
                        try {
                            Thread.sleep(10000); // Latido cada 10 segundos
                            for (Map.Entry<String, PrintWriter> entry : clientes.entrySet()) {
                                PrintWriter pw = entry.getValue();
                                if (pw.checkError()) { // Si da true, la conexión física se rompió
                                    logSwing("Latido falló para: " + entry.getKey() + ". Desconectando.");
                                    clientes.remove(entry.getKey());
                                    broadcastUsers();
                                } else {
                                    pw.println("PING:"); // Mantiene la red viva
                                }
                            }
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                });
                heartbeatThread.setDaemon(true);
                heartbeatThread.start();

                // Loop principal de aceptación de clientes
                while (servidorActivo) {
                    try {
                        Socket client = serverSocket.accept();
                        pool.execute(() -> manejarCliente(client));
                    } catch (IOException e) {
                        if (!servidorActivo) break;
                    }
                }

            } catch (IOException e) {
                mensajesTxt.append("Error: " + e.getMessage() + "\n");
            }
        }).start();
    }

    private void manejarCliente(Socket clientSocket) {
        String username = null;
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {

            String first = in.readLine();
            if (first == null || !first.startsWith("USER:")) {
                out.println("ERROR:BAD_HANDSHAKE:Se esperaba USER:<nombre>");
                clientSocket.close();
                return;
            }

            username = first.substring("USER:".length()).trim();
            if (username.isEmpty() || username.contains(":") || username.contains(",") || username.length() > 32) {
                out.println("ERROR:BAD_USERNAME:Nombre inválido");
                clientSocket.close();
                return;
            }

            PrintWriter prev = clientes.putIfAbsent(username, out);
            if (prev != null) {
                out.println("ERROR:USER_TAKEN:Ya existe ese usuario");
                clientSocket.close();
                return;
            }

            out.println("OK:USER_REGISTERED");
            broadcastUsers();
            logSwing("Se conectó: " + username);

            String linea;
            while ((linea = in.readLine()) != null) {
                final String msg = linea;

                // Manejo de envío de archivos
                if (msg.startsWith("FILE:")) {
                    out.println("Mensaje recibido en el server");
                    String[] partes = msg.split(":", 3);
                    if (partes.length < 3) {
                        out.println("ERROR:BAD_FILE_HEADER");
                        continue;
                    }
                    String nombre = partes[1];
                    long tamaño = Long.parseLong(partes[2]);

                    try (FileOutputStream fos = new FileOutputStream("recibido_" + nombre)) {
                        InputStream is = clientSocket.getInputStream();
                        byte[] buffer = new byte[4096];
                        long restantes = tamaño;
                        int bytes;
                        while (restantes > 0 &&
                                (bytes = is.read(buffer, 0, (int) Math.min(buffer.length, restantes))) != -1) {
                            fos.write(buffer, 0, bytes);
                            restantes -= bytes;
                        }
                    }
                    logSwing("Archivo recibido: " + nombre);
                    continue;
                }

                // Manejo de mensajes privados
                if (msg.startsWith("PRIV:")) {
                    String[] partes = msg.split(":", 3);
                    if (partes.length < 3) {
                        out.println("ERROR:BAD_PRIV_FORMAT:Use PRIV:<dest>:<mensaje>");
                        continue;
                    }
                    String dest = partes[1].trim();
                    String cuerpo = partes[2];

                    PrintWriter outDest = clientes.get(dest);
                    if (outDest == null) {
                        out.println("ERROR:USER_NOT_FOUND:" + dest);
                    } else {
                        outDest.println("FROM:" + username + ":" + cuerpo);
                        out.println("FROM:" + username + "->" + dest + ":" + cuerpo);
                    }
                    continue;
                }

                // Manejo de mensajes globales
                if (msg.startsWith("MSG:")) {
                    String cuerpo = msg.substring("MSG:".length());
                    broadcastAll("ALL:" + username + ":" + cuerpo);
                    continue;
                }
                broadcastAll("ALL:" + username + ":" + msg);
            }
        } catch (IOException e) {
            // Error esperado si el cliente cierra abruptamente
        } finally {
            if (username != null) {
                clientes.remove(username);
                broadcastUsers();
                logSwing("Cliente desconectado: " + username);
            } else {
                logSwing("Cliente desconectado (sin usuario)");
            }
        }
    }

    private void broadcastAll(String payload) {
        for (PrintWriter pw : clientes.values()) {
            pw.println(payload);
        }
        if (payload.startsWith("ALL:")) {
            String[] parts = payload.split(":", 3);
            if (parts.length == 3) {
                logSwing(parts[0] + " " + parts[1] + ": " + parts[2]);
            } else {
                logSwing(payload);
            }
        } else {
            logSwing(payload);
        }
    }

    private void detenerServidor() {
        try {
            servidorActivo = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            mensajesTxt.append("Servidor detenido ❌\n");
        } catch (IOException e) {
            mensajesTxt.append("Error al detener: " + e.getMessage() + "\n");
        }
    }

    private void reiniciarServidor() {
        mensajesTxt.append("Reiniciando servidor...\n");
        detenerServidor();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        iniciarServidor();
    }

    private void broadcastUsers() {
        Set<String> users = clientes.keySet();
        String lista = String.join(",", users);
        String frame = "USERS:" + lista;
        for (PrintWriter pw : clientes.values()) {
            pw.println(frame);
        }
    }

    private void logSwing(String text) {
        SwingUtilities.invokeLater(() -> mensajesTxt.append(text + "\n"));
    }
}