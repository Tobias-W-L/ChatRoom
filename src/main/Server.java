package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

// Defining class
public class Server {
    // list of connected clients
    static List<ClientHandler> clients = new ArrayList<>();

    // Main method
    public static void main(String[] args) {
        // Define the port number for the server to listen on
        int portNumber = 8000;

        try {
            // Create a new ServerSocket on the specified port
            ServerSocket serverSocket = new ServerSocket(portNumber);

            // Keep listening for and accepting new client connections
            while (true) {
                handleClient(serverSocket);
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

	public static ClientHandler handleClient(ServerSocket serverSocket) throws IOException {
		System.out.println("Waiting for a client...");
		Socket clientSocket = serverSocket.accept();
		System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

		// Create a new ClientHandler for the connected client
		ClientHandler handler = new ClientHandler(clientSocket);
		// Add the handler to the list of clients
		clients.add(handler);
		// Create and start a new thread for the handler
		createThread(handler);		
		return handler;
	}
	
	public static Thread createThread(ClientHandler newHandler) {
		Thread thread = new Thread(newHandler);
		thread.start();
		return thread;
	}
}