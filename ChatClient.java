import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    
    // GUI Components
    private JFrame chatFrame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JFrame cvFrame;
    
    public ChatClient() {
        // Get username first
        username = getUsername();
        if (username == null) {
            System.exit(0);
        }
        
        // Initialize GUI and connection
        initializeGUI();
        connectToServer();
    }
    
    private String getUsername() {
        return JOptionPane.showInputDialog(
            null,
            "Enter your username:",
            "Login",
            JOptionPane.QUESTION_MESSAGE
        );
    }
    
    private void initializeGUI() {
        // Chat Window
        chatFrame = new JFrame("Chat - " + username);
        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chatFrame.setSize(400, 600);
        
        // Chat Components
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setWrapStyleWord(true);
        chatArea.setLineWrap(true);
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        messageField = new JTextField();
        JButton sendButton = new JButton("Send");
        
        // Layout
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        
        chatFrame.add(scrollPane, BorderLayout.CENTER);
        chatFrame.add(bottomPanel, BorderLayout.SOUTH);
        
        // Action Listeners
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        
        chatFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
        
        // Position the window
        chatFrame.setLocationRelativeTo(null);
        chatFrame.setVisible(true);
    }
    
    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Request CV
            out.println("GET_CV");
            
            // Read and display CV
            String response = in.readLine();
            if ("CV_CONTENT".equals(response)) {
                displayCV();
            }
            
            // Send username to server
            out.println(username);
            
            // Start message receiving thread
            new Thread(this::receiveMessages).start();
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                chatFrame,
                "Could not connect to server: " + e.getMessage(),
                "Connection Error",
                JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }
    }
    
    private void displayCV() {
        try {
            StringBuilder cvContent = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null && !line.equals("END_CV_CONTENT")) {
                cvContent.append(line).append("\n");
            }
            
            SwingUtilities.invokeLater(() -> {
                cvFrame = new JFrame("Student CV");
                JEditorPane cvPane = new JEditorPane("text/html", cvContent.toString());
                cvPane.setEditable(false);
                
                JScrollPane scrollPane = new JScrollPane(cvPane);
                cvFrame.add(scrollPane);
                cvFrame.setSize(800, 600);
                cvFrame.setLocationRelativeTo(chatFrame);
                cvFrame.setVisible(true);
                
                cvFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        cvFrame.dispose();
                    }
                });
            });
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                chatFrame,
                "Error displaying CV: " + e.getMessage(),
                "CV Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            messageField.setText("");
            chatArea.append("You: " + message + "\n");
        }
    }
    
    private void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                final String finalMessage = message;
                SwingUtilities.invokeLater(() -> {
                    chatArea.append(finalMessage + "\n");
                    chatArea.setCaretPosition(chatArea.getDocument().getLength());
                });
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                JOptionPane.showMessageDialog(
                    chatFrame,
                    "Connection to server lost: " + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
    
    private void disconnect() {
        try {
            if (out != null) {
                out.println("EXIT");
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient());
    }
}