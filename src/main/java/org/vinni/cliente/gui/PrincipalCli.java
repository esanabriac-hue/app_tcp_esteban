package org.vinni.cliente.gui;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * author: Vinni 2024
 * Modificado: Soporta USER/MSG/PRIV y selector de destinatario
 */
public class PrincipalCli extends javax.swing.JFrame {

    private final int PORT = 12345;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    //Nuevos componentes
    private javax.swing.JButton bConectar;
    private javax.swing.JButton btEnviar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea mensajesTxt;
    private JTextField mensajeTxt;
    private javax.swing.JButton btEnviarArchivo;

    private JComboBox<String> destinatarioCmb;
    private JLabel destinatarioLbl;

    // Estado
    private String myName = null;

    public PrincipalCli() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        this.setTitle("Cliente ");
        bConectar = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        mensajesTxt = new javax.swing.JTextArea();
        mensajeTxt = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        btEnviar = new javax.swing.JButton();
        btEnviarArchivo = new javax.swing.JButton();

        destinatarioCmb = new JComboBox<>();
        destinatarioLbl = new JLabel("Enviar a:");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        bConectar.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        bConectar.setText("CONECTAR CON SERVIDOR");
        bConectar.addActionListener(evt -> bConectarActionPerformed(evt));
        getContentPane().add(bConectar);
        bConectar.setBounds(260, 40, 210, 40);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(204, 0, 0));
        jLabel1.setText("CLIENTE TCP : DFRACK");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(110, 10, 250, 17);

        mensajesTxt.setColumns(20);
        mensajesTxt.setRows(5);
        mensajesTxt.setEnabled(false);
        jScrollPane1.setViewportView(mensajesTxt);
        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(30, 210, 410, 140);

        mensajeTxt.setFont(new java.awt.Font("Verdana", 0, 14)); // NOI18N
        getContentPane().add(mensajeTxt);
        mensajeTxt.setBounds(40, 120, 350, 30);

        jLabel2.setFont(new java.awt.Font("Verdana", 0, 14)); // NOI18N
        jLabel2.setText("Mensaje:");
        getContentPane().add(jLabel2);
        jLabel2.setBounds(20, 90, 120, 30);

        btEnviar.setFont(new java.awt.Font("Verdana", 0, 14)); // NOI18N
        btEnviar.setText("Enviar");
        btEnviar.addActionListener(evt -> btEnviarActionPerformed(evt));
        getContentPane().add(btEnviar);
        btEnviar.setBounds(327, 160, 120, 27);

        btEnviarArchivo.setFont(new java.awt.Font("Verdana", 0, 14));
        btEnviarArchivo.setText("Enviar Archivo");
        btEnviarArchivo.addActionListener(evt -> enviarArchivoActionPerformed(evt));
        getContentPane().add(btEnviarArchivo);
        btEnviarArchivo.setBounds(40, 160, 150, 27);

        // Selector de destinatario
        destinatarioLbl.setFont(new java.awt.Font("Verdana", 0, 14));
        getContentPane().add(destinatarioLbl);
        destinatarioLbl.setBounds(40, 60, 80, 20);

        destinatarioCmb.setFont(new java.awt.Font("Verdana", 0, 12));
        destinatarioCmb.addItem("Todos");
        getContentPane().add(destinatarioCmb);
        destinatarioCmb.setBounds(120, 60, 130, 22);

        setSize(new java.awt.Dimension(491, 430));
        setLocationRelativeTo(null);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new PrincipalCli().setVisible(true));
    }

    private void bConectarActionPerformed(java.awt.event.ActionEvent evt) {
        conectar();
    }

    private void btEnviarActionPerformed(java.awt.event.ActionEvent evt) {
        enviarMensaje();
    }

    private void enviarArchivoActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser();
        int opcion = fileChooser.showOpenDialog(this);
        if (opcion == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            enviarArchivo(archivo);
        }
    }

    // ------------------ Lógica de red ------------------

    private void conectar() {
        JOptionPane.showMessageDialog(this, "Conectando con servidor");
        try {
            if (socket == null || socket.isClosed()) {
                socket = new Socket("localhost", PORT);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            }

            // Pedir nombre de usuario
            while (myName == null || myName.trim().isEmpty()) {
                myName = JOptionPane.showInputDialog(this, "Tu nombre de usuario:");
                if (myName == null) { // Cancelado
                    return;
                }
                myName = myName.trim();
            }

            // Enviar USER y validar respuesta
            out.println("USER:" + myName);
            String ack = in.readLine();
            if (ack == null || !ack.startsWith("OK:")) {
                String err = (ack == null ? "Sin respuesta del servidor" : ack);
                JOptionPane.showMessageDialog(this, "No se pudo registrar: " + err, "Error", JOptionPane.ERROR_MESSAGE);
                // Reset para permitir reintentar
                myName = null;
                return;
            }

            // Hilo lector
            new Thread(() -> {
                try {
                    String fromServer;
                    while ((fromServer = in.readLine()) != null) {
                        final String linea = fromServer;
                        if (linea.startsWith("USERS:")) {
                            actualizarUsuarios(linea.substring("USERS:".length()));
                        } else if (linea.startsWith("ALL:")) {
                            // ALL:origen:mensaje
                            String[] parts = linea.split(":", 3);
                            String origen = parts.length > 1 ? parts[1] : "¿?";
                            String cuerpo = parts.length > 2 ? parts[2] : "";
                            appendMsg(origen + ": " + cuerpo);
                        } else if (linea.startsWith("FROM:")) {
                            // FROM:origen:mensaje (privado)
                            String[] parts = linea.split(":", 3);
                            String origen = parts.length > 1 ? parts[1] : "¿?";
                            String cuerpo = parts.length > 2 ? parts[2] : "";
                            appendMsg("[Privado] " + origen + ": " + cuerpo);
                        } else if (linea.startsWith("ERROR:")) {
                            appendMsg("[Servidor] " + linea);
                        } else {
                            appendMsg("Servidor: " + linea);
                        }
                    }
                } catch (IOException ex) {
                    appendMsg("Conexión cerrada.");
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error conectando: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void enviarMensaje() {
        if (out == null) {
            JOptionPane.showMessageDialog(this, "No estás conectado.");
            return;
        }
        String texto = mensajeTxt.getText().trim();
        if (texto.isEmpty()) return;

        String destinatario = (String) destinatarioCmb.getSelectedItem();
        if (destinatario == null || "Todos".equals(destinatario)) {
            out.println("MSG:" + texto);
        } else {
            out.println("PRIV:" + destinatario + ":" + texto);
        }

        mensajeTxt.setText("");
    }

    private void enviarArchivo(File archivo) {
        if (socket == null || socket.isClosed()) {
            JOptionPane.showMessageDialog(this, "Conéctate antes de enviar archivo.");
            return;
        }
        try {
            OutputStream os = socket.getOutputStream();
            FileInputStream fis = new FileInputStream(archivo);

            out.println("FILE:" + archivo.getName() + ":" + archivo.length());

            byte[] buffer = new byte[4096];
            int bytes;
            while ((bytes = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytes);
            }
            os.flush();
            fis.close();

            appendMsg("Archivo enviado: " + archivo.getName());
        } catch (IOException e) {
            appendMsg("Error enviando archivo: " + e.getMessage());
        }
    }

    // ------------------ Helpers UI/Modelo ------------------

    private void actualizarUsuarios(String listaCsv) {
        SwingUtilities.invokeLater(() -> {
            // Mantener "Todos" fijo + usuarios únicos
            Set<String> nuevos = new LinkedHashSet<>();
            nuevos.add("Todos");
            if (listaCsv != null && !listaCsv.isEmpty()) {
                nuevos.addAll(Arrays.asList(listaCsv.split(",")));
            }

            String seleccionado = (String) destinatarioCmb.getSelectedItem();
            destinatarioCmb.removeAllItems();
            for (String u : nuevos) {
                destinatarioCmb.addItem(u);
            }

            // Volver a seleccionar si es posible
            if (seleccionado != null && nuevos.contains(seleccionado)) {
                destinatarioCmb.setSelectedItem(seleccionado);
            } else {
                destinatarioCmb.setSelectedItem("Todos");
            }
        });
    }

    private void appendMsg(String msg) {
        SwingUtilities.invokeLater(() -> mensajesTxt.append(msg + "\n"));
    }
}