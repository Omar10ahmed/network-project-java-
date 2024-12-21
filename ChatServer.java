import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static final String CV_PATH = "student_cv.html";
    private static Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    
    public static void main(String[] args) {
        // First check if CV file exists
        if (!Files.exists(Paths.get(CV_PATH))) {
            System.err.println("Error: student_cv.html file not found!");
            System.err.println("Please make sure student_cv.html is in the same directory as the server.");
            return;
        }
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            System.out.println("Using CV file at: " + new File(CV_PATH).getAbsolutePath());
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(sender.getUsername() + ": " + message);
            }
        }
    }
    
    static void removeClient(ClientHandler client) {
        clients.remove(client);
        broadcast(client.getUsername() + " has left the chat.", null);
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private static final String CV_PATH = "student_cv.html";
    
    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        try {
            String request = in.readLine();
            if (request.startsWith("GET_CV")) {
                sendCV();
                username = in.readLine();
                ChatServer.broadcast(username + " has joined the chat.", null);
            }
            
            String message;
            while ((message = in.readLine()) != null) {
                if (message.equals("EXIT")) {
                    break;
                }
                ChatServer.broadcast(message, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }
    
    private void sendCV() {
        try {
            String cv = Files.readString(Paths.get(CV_PATH));
            out.println("CV_CONTENT");
            out.println(cv);
            out.println("END_CV_CONTENT");  // Add an end marker
        } catch (IOException e) {
            e.printStackTrace();
            out.println("ERROR");
            out.println("Could not read CV file");
        }
    }
    
    void sendMessage(String message) {
        out.println(message);
    }
    
    String getUsername() {
        return username;
    }
    
    private void cleanup() {
        ChatServer.removeClient(this);
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}