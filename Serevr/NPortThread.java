//package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class NPortThread extends Thread {

	private static ServerSocket server;
	private static Socket socket = null;
	private int tPort;
	private static int clientCount = 0;

	HashMap<Integer, String> getPutMap;

	public NPortThread(int nPort, int tPort, HashMap<Integer, String> getPutMap) throws IOException {

		server = new ServerSocket(nPort);
		this.tPort = tPort;
		this.getPutMap = getPutMap;
	}

	@Override
	public void run() {
		boolean flag=true;
		//true to allow any number of clients to connect
		while (flag) {
			clientCount=clientCount+1;

			System.out.println("Waiting for client connection request");
			try {
				socket = server.accept();
				System.out.println("A new client has been connected");
				System.out.println("Client:- " + clientCount + " has connected");
				ProcessComm processComm = new ProcessComm(socket, clientCount, tPort, getPutMap); 
				processComm.start();
			} catch (Exception e) {
				System.out.println("Error while providing a connection to the client");
				e.printStackTrace();
			}

		}

	}

}