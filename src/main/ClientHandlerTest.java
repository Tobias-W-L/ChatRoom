package main;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientHandlerTest {

	@BeforeEach
	void setUp() throws Exception {
		
	}
	@Test
	void testRun() throws UnknownHostException, IOException {
		ServerSocket serverSocket = new ServerSocket(8000);
		Client client = new Client();
		Socket clientsocket = client.autoRun();
		ClientHandler handler = new ClientHandler(clientsocket);
		handler.setisKicked(true);
		serverSocket.close();
		assert(serverSocket.isClosed());
	}

	@Test
	void testUsernameProcess() throws IOException {
		ServerSocket serverSocket = new ServerSocket(8000);
		Client client = new Client();
		Socket clientsocket = client.autoRun();
		ClientHandler handler = new ClientHandler(clientsocket);
		assertFalse(handler.getisKicked());
	} 

}
