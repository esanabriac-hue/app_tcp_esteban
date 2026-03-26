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

    private final int[] PORTS = {12345, 12346, 12347}; // principal + backup
    private int currentServerIndex = 0;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final int MAX_REINTENTOS = 5;
    private final int TIEMPO_ESPERA = 3000;
    private final int TIMEOUT = 2000;

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
        new Thread(() -> {
            int intentos = 0;
            boolean conectado = false;

            while (!conectado && intentos < MAX_REINTENTOS * PORTS.length) {

                int puertoActual = PORTS[currentServerIndex];

                try {
                    appendMsg("Intentando conectar a puerto " + puertoActual + "...");

                    socket = new Socket();
                    socket.connect(
                            new java.net.InetSocketAddress("localhost", puertoActual),
                            TIMEOUT
                    );

                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    conectado = true;
                    appendMsg("Conectado al servidor en puerto " + puertoActual + " ✅");

                    registrarUsuario();
                    escucharServidor();

                } catch (IOException e) {
                    appendMsg("Fallo en puerto " + puertoActual);

                    currentServerIndex = (currentServerIndex + 1) % PORTS.length;

                    intentos++;

                    try {
                        Thread.sleep(TIEMPO_ESPERA);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            if (!conectado) {
                appendMsg("No se pudo conectar a ningún servidor ❌");
            }
        }).start();
    }

    private void registrarUsuario() throws IOException {
        while (myName == null || myName.trim().isEmpty()) {
            myName = JOptionPane.showInputDialog(this, "Tu nombre:");
            if (myName == null) return;
        }

        out.println("USER:" + myName);
        String ack = in.readLine();

        if (ack == null || !ack.startsWith("OK")) {
            throw new IOException("Error registro: " + ack);
        }
    }

    private void escucharServidor() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    appendMsg(msg);
                }
            } catch (IOException e) {
                appendMsg("Conexión perdida ⚠️");
                reconectar();
            }
        }).start();
    }

    private void reconectar() {
        appendMsg("Intentando reconectar...");

        try {
            if (socket != null) socket.close();
        } catch (IOException e) {}

        // 🔥 cambiar al siguiente servidor
        currentServerIndex = (currentServerIndex + 1) % PORTS.length;

        conectar();
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