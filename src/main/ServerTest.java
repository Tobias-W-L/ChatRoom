package main;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServerTest {
	
	@BeforeEach
	void setUp() throws Exception {
	}

	@Test
	void testHandleClient() throws IOException {
		ServerSocket serverSocket = new ServerSocket(8000);
		Client client = new Client();
		client.autoRun();
		ClientHandler clientHandler = Server.handleClient(serverSocket);
		assertNotNull(clientHandler.getClientSocket());	
		serverSocket.close();
	}

	@Test
	void testCreateThread() throws IOException {
		ServerSocket serverSocket = new ServerSocket(8000);
		Client client = new Client();
		client.autoRun();
		ClientHandler clientHandler = Server.handleClient(serverSocket);
		Thread serverThread = Server.createThread(clientHandler);
		assertTrue(serverThread.isAlive());
		serverSocket.close();
	}

}
