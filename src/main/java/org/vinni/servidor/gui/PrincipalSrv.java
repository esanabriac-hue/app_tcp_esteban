package org.vinni.servidor.gui;


import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Author: Vinni
 */
public class PrincipalSrv extends javax.swing.JFrame {
    private final int PORT = 12345;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private static Set<PrintWriter> clientes = ConcurrentHashMap.newKeySet();


    /**
     * Creates new form Principal1
     */
    public PrincipalSrv() {
        initComponents();

    }
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {
        this.setTitle("Servidor ...");

        bIniciar = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        mensajesTxt = new JTextArea();
        jScrollPane1 = new javax.swing.JScrollPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        bIniciar.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        bIniciar.setText("INICIAR SERVIDOR");
        bIniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bIniciarActionPerformed(evt);
            }
        });
        getContentPane().add(bIniciar);
        bIniciar.setBounds(100, 90, 250, 40);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
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
    }// </editor-fold>

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PrincipalSrv().setVisible(true);
            }
        });

    }
    private void bIniciarActionPerformed(java.awt.event.ActionEvent evt) {
        iniciarServidor();
    }

    private void iniciarServidor() {
        JOptionPane.showMessageDialog(this, "Iniciando servidor");
        new Thread(new Runnable() {
            public void run() {
                try {
                    InetAddress addr = InetAddress.getLocalHost();
                    serverSocket = new ServerSocket( PORT);
                    mensajesTxt.append("Servidor TCP en ejecución: "+ addr + " ,Puerto " + serverSocket.getLocalPort()+ "\n");
                    ExecutorService pool = Executors.newCachedThreadPool();
                    while (true) {
                        Socket client = serverSocket.accept();
                        pool.execute(() -> manejarCliente(client));
                    }

                } catch (IOException ex) {
                    ex.printStackTrace();
                    mensajesTxt.append("Error en el servidor: " + ex.getMessage() + "\n");
                }
            }
        }).start();
    }

    private void manejarCliente(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(
                        clientSocket.getOutputStream(), true)
        ) {
            clientes.add(out);
            String linea;

            while ((linea = in.readLine()) != null) {
                String finalLinea = linea;
                if (finalLinea.startsWith("FILE:")) {

                    out.println("Mensaje recibido en el server");

                    String[] partes = linea.split(":");
                    String nombre = partes[1];
                    long tamaño = Long.parseLong(partes[2]);

                    FileOutputStream fos = new FileOutputStream("recibido_" + nombre);
                    InputStream is = clientSocket.getInputStream();

                    byte[] buffer = new byte[4096];
                    long restantes = tamaño;
                    int bytes;

                    while (restantes > 0 &&
                            (bytes = is.read(buffer, 0,
                                    (int)Math.min(buffer.length, restantes))) != -1) {

                        fos.write(buffer, 0, bytes);
                        restantes -= bytes;
                    }

                    fos.close();

                    System.out.println("Archivo recibido: " + nombre);

                    continue;
                }



                SwingUtilities.invokeLater(() ->
                        mensajesTxt.append("Cliente: " + finalLinea + "\n")

                );
                for (PrintWriter cliente : clientes) {
                    cliente.println(finalLinea);
                }

                out.println("Mensaje recibido en el server");
            }

        } catch (IOException e) {
            SwingUtilities.invokeLater(() ->
                    mensajesTxt.append("Cliente desconectado\n")
            );
        }
    }


    // Variables declaration - do not modify
    private javax.swing.JButton bIniciar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextArea mensajesTxt;
    private javax.swing.JScrollPane jScrollPane1;
}
