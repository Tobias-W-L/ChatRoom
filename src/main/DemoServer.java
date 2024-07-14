package main;

// libraries
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Defining class
public class DemoServer {
    // list of connected clients
    private static List<ClientHandler> clients = new ArrayList<>();

    // Main method
    public static void main(String[] args) {
        // Define the port number for the server to listen on
        int portNumber = 8000;

        try {
            // Create a new ServerSocket on the specified port
            ServerSocket serverSocket = new ServerSocket(portNumber);

            // Keep listening for and accepting new client connections
            while (true) {
                System.out.println("Waiting for a client...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                // Create a new ClientHandler for the connected client
                ClientHandler handler = new ClientHandler(clientSocket);
                // Add the handler to the list of clients
                clients.add(handler);
                // Create and start a new thread for the handler
                Thread thread = new Thread(handler);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    // Define the ClientHandler class, which is responsible for handling individual client connections
    private static class ClientHandler implements Runnable {
        private String username;
        private Socket clientSocket;
        private PrintWriter out;
        private boolean isCoordinator = false;
        private boolean isKicked = false;
        private BufferedReader in;
        private long lastActive;

        // ClientHandler constructor
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            // If this is the first client, set them as the coordinator
            if (clients.isEmpty()) {
                isCoordinator = true;
            }
        }

        // Run method
        public void run() {
            try {
                // Initialize input and output streams
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                // scheduler to check for user inactivity every minute
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                scheduler.scheduleAtFixedRate(() -> checkActivity(), 1, 1, TimeUnit.MINUTES);

                // Process incoming messages from the client
                String message;
                while ((message = in.readLine()) != null && !isKicked) {
                    // If the client has not set a username, process the message as a potential username
                    if (username == null) {
                        String potentialUsername = message;
                        // Check if the username is already taken
                        if (checkUsername(potentialUsername)) {
                            sendMessage("[SERVER] Username already taken, please choose another.");
                            return;
                        } else {
                            // Set the username and send a welcome message
                            username = potentialUsername;
                            sendMessage("[SERVER] Welcome to the chat, " + username + "!");

                            // If the client is the coordinator, tell them
                            if (isCoordinator) {
                                sendMessage("[SERVER] You are the group coordinator.");
                            } else {
                                // Otherwise, request the list of group members from the coordinator
                                sendMessage("[SERVER] Requesting group members from coordinator...");
                                for (ClientHandler client : clients) {
                                    if (client.isCoordinator) {
                                        client.sendMessage("[NEW MEMBER] " + username);
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        // Process the message and perform the actions needed based on its content
                        System.out.println("Received message from client: " + message);
                        // Getting the timestamp
                        String timestamp = getTimestamp();
                        // private messages
                        if (message.startsWith("/msg")) {
                            String[] tokens = message.split(" ");
                            String recipient = tokens[1];
                            String privateMessage = message.substring(5 + recipient.length());
                            for (ClientHandler client : clients) {
                                if (client.username != null && client.username.equals(recipient)) {
                                    client.sendMessage("[" + timestamp + "][PM from " + username + "] " + privateMessage);
                                }
                            }
                        }
                        // requests for the list of connected users
                        else if (message.equalsIgnoreCase("/users")) {
                        	// this list comes from the coordinator
                            if (isCoordinator) {
                                sendUserList();
                            } else {
                                for (ClientHandler client : clients) {
                                    if (client.isCoordinator) {
                                        client.sendMessage("[REQUEST_USERS] " + username);
                                        sendUserList();
                                        break;
                                    }
                                }
                            }
                        }
                        // requests to kick a user from the chat, can only be done by the coord
                        else if (message.startsWith("/kick")) {
                            String targetUsername = message.substring(6);
                            kickUser(targetUsername);
                        }
                        // requests for the list of connected users from other clients
                        else if (message.startsWith("[REQUEST_USERS]")) {
                            if (isCoordinator) {
                                String requestingUser = message.substring(16);
                                for (ClientHandler client : clients) {
                                    if (client.username.equals(requestingUser)) {
                                        client.sendUserList();
                                        break;
                                    }
                                }
                            }
                        }
                        // notifications pop up when a new members joins the chat, only for the coord
                        else if (message.startsWith("[ADD_MEMBER]")) {
                            if (isCoordinator) {
                                for (ClientHandler client : clients) {
                                    if (client != this) {
                                        client.sendMessage("[SERVER] " + message.substring(12) + " has joined the chat.");
                                    }
                                }
                            }
                        } else {
                            // for all other messages, send the message to all connected clients
                            for (ClientHandler client : clients) {
                                if (client != this) {
                                    client.sendMessage("[" + timestamp + "][" + username + "] " + message);
                                }else {
                                	//client.sendMessage("[YOU] " + message);
                                }
                            }
                        }
                        // Update the last active timestamp for the user when they send a message
                        lastActive = System.currentTimeMillis();
                    }
                }

                // If the client disconnects, remove them from the clients list
                System.out.println("Client disconnected.");
                clients.remove(this);

                // If the client was the coordinator, assign coordinator status to the next client in the list
                if (isCoordinator && !clients.isEmpty()) {
                    clients.get(0).isCoordinator = true;
                    clients.get(0).sendMessage("[SERVER] You are now the group coordinator.");
                    // Tell all the other clients that the coordinator has left the chat
                    for (ClientHandler client : clients) {
                        if (client != this) {
                            client.sendMessage("[SERVER] " + username + " has left the chat.");
                        }
                    }
                }

                // Close the client socket
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            }
        }

        // mthod for sending a message to the client
        public void sendMessage(String message) {
            if (!isKicked) {
                out.println(message);
            }
        }

        // method for checking if a username is already in use
        private boolean checkUsername(String username) {
        	// this function is ridiculous but keep it because without it, username check doesn't work
            for (ClientHandler client : clients) {
                if (client.username != null && client.username.equals(username)) {
                    return true;
                }
            }
            return false;
        }

        // method for sending the list of connected users to the client
        public void sendUserList() {
            StringBuilder userList = new StringBuilder("[SERVER] Connected Users:\n");
            for (ClientHandler client : clients) {
                String userDescription = client.username + " (" + client.clientSocket.getInetAddress().getHostAddress() + ":" + client.clientSocket.getPort() + ")";
                if (client.isCoordinator) {
                    userDescription += " [Coordinator]";
                }
                userList.append(userDescription).append("\n");
            }
            sendMessage(userList.toString());
        }

        // kick user method
        public void kickUser(String targetUsername) {
            if (isCoordinator) {
                for (ClientHandler client : clients) {
                    if (client.username != null && client.username.equals(targetUsername)) {
                        client.isKicked = true;
                        client.sendMessage("[SERVER] You have been kicked from the chat by the coordinator.");
                        client.out.close();
                        try {
                            client.clientSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        clients.remove(client);
                        break;
                    }
                }
                // tell all clients that the user has been kicked
                for (ClientHandler client : clients) {
                    if (client != this) {
                        client.sendMessage("[SERVER] " + targetUsername + " has been kicked from the chat by the coordinator.");
                    }
                }
            } else {
                // If the client is not the coordinator, tell them that they can not kick users
                sendMessage("[SERVER] Only the coordinator can kick users.");
            }
        }

        // Method to check for user inactivity
        public void checkActivity() {
            long currentTime = System.currentTimeMillis();
            long inactiveTime = currentTime - lastActive;
            // Check if the user has been inactive for more than 1 minute
            if (inactiveTime > 60000) { 
                sendMessage("[SERVER] You have been inactive for more than 1 minute.");
                // Notify the coordinator of the inactive user
                for (ClientHandler client : clients) {
                    if (client.isCoordinator && client != this) {
                        client.sendMessage("[SERVER] User " + username + " has been inactive for more than 1 minute.");
                    }
                }
            }
        }
        // method to get the timestamp
        public String getTimestamp() {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            return now.format(formatter);
        }
    }
}