//172.19.48.218
package main;

import java.io.*;
import java.net.*;

public class Client {
	private static BufferedReader in;
    private static PrintWriter out;

    public static void main(String[] args) {
        // Prompt the user for the server IP address
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter the server IP address: ");
        String serverIp = null;
        try {
            serverIp = reader.readLine();
        } catch (IOException e1) {
            System.err.println("Error: " + e1.getMessage());
            System.exit(1);
        }
        
        
        int serverPort = 8000;
        
        try {
            Socket socket = socketConnect(serverIp, serverPort);

            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter Username: ");
            String username = userInput.readLine();
            out.println(username.replace(" ", ""));
            
            while (true) {
                System.out.print("Type your message (type 'exit' to stop): ");
                String message = userInput.readLine();

                if (message == null || message.equalsIgnoreCase("exit")) {
                    break;
                }

                out.println(message);
            }

            socket.close();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
//    For Testing purposes only
    public Socket autoRun() throws UnknownHostException, IOException {
    	Socket socket = socketConnect("localhost", 8000);
    	Thread listenerThread = new Thread(new Listener());
		listenerThread.start();
		return socket;
    }

	public static Socket socketConnect(String serverIp, int serverPort) throws UnknownHostException, IOException {
		Socket socket = new Socket(serverIp, serverPort);
		out = new PrintWriter(socket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		Thread listenerThread = new Thread(new Listener());
		listenerThread.start();
		return socket;
	}

    private static class Listener implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    String message = in.readLine();
                    System.out.println("\n"+ message);
                }
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
}

