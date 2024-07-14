package main;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@Test
	void testSocketConnect() throws IOException {
		ServerSocket serverSocket = new ServerSocket(8000);
		Socket socket = Client.socketConnect("localhost", 8000);
		assert(socket.isBound());
	}
}
