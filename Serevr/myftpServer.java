//package server;
//package server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;
public class myftpServer {
	
	// Reentrant lock to lock get and put on a file
	public static ReentrantLock lock = new ReentrantLock();
	
	//HashMap: store list of files using command ID on which get and put operations are being performed  
	public static HashMap<Integer,String> getPutMap;
  
	public static void main(String[] args) throws IOException {
		
     
     	Scanner portInput=new Scanner(System.in);
		System.out.println("Enter the port number for normal commands");
		
		getPutMap = new HashMap<Integer,String>();
		//nPort port number
		int nPort = 0;
		try {
			nPort = Integer.parseInt(portInput.nextLine());
		} catch (NumberFormatException nfe) {
			System.out.println("Error===Invalid nport number");
			return;
		} 
		System.out.println("Enter the port number for terminate commands");
		//tPort port number
		int tPort = 0;
		try {
			tPort = Integer.parseInt(portInput.nextLine());
		} catch (NumberFormatException nfe) {
			System.out.println("Error===Invalid tport number");
			return;
		} 
		
		if(nPort==tPort) {
			System.out.println("Error: nport and tport can not be same");
		}
		
		//nport commands Server Thread
		
		NPortThread nThread = new NPortThread(nPort, tPort, getPutMap);
		nThread.start();
		
		//tport commands Server Thread
		
		TPortThread tThread = new TPortThread(tPort, getPutMap);
		tThread.start();
		
	}

}
