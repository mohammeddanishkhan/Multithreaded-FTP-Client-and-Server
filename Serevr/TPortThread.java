//package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class TPortThread extends Thread {

	static Socket socket = null;
	private static ServerSocket server;
	HashMap<Integer, String> getPutMap;

	@Override
	public void run() {
		boolean flag=true;
		while (flag) {
			
			try {
				socket = server.accept();
				ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
				String commandID = (String) inputStream.readObject();
				System.out.println("Terminating Command: " + commandID);
				
				String terminateFlag = "terminate ".concat(getPutMap.get(Integer.parseInt(commandID)));
				getPutMap.put(Integer.parseInt(commandID), terminateFlag);
																			
				System.out.println("Command state" + getPutMap.get(Integer.parseInt(commandID)));

			} catch (Exception e) {
				System.out.println("Error while creating ");
				e.printStackTrace();
			} 

		}

	}
	
	public TPortThread(int tPort, HashMap<Integer, String> getPutMap) throws IOException {
		server = new ServerSocket(tPort);// server
		this.getPutMap = getPutMap;
		

	}

}