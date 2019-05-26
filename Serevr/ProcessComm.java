//package server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ProcessComm extends Thread {
	Socket socket;
	int clientNo;
	String workingDirectory;
	int tPort;
	int commandId;
	Path path;
	HashMap<Integer, String> getPutMap;
	ObjectInputStream inputStream;
	ObjectOutputStream outputStream;

	public ProcessComm(Socket inSocket, int count, int tPort, HashMap<Integer, String> getPutMap)
			throws ClassNotFoundException, IOException {
		socket = inSocket;
		clientNo = count;
		this.tPort = tPort;
		this.getPutMap = getPutMap;
		this.path = Paths.get(System.getProperty("user.dir"));
		workingDirectory = System.getProperty("user.dir");// curemt workimg dirctory
		inputStream = new ObjectInputStream(socket.getInputStream());
		outputStream = new ObjectOutputStream(socket.getOutputStream());
	}

	public void ls() throws Exception {

		try {
			String outputFile = "";
			File folder = new File(workingDirectory);
			File[] fileList = folder.listFiles();
			for (File allFileList : fileList) {
				if (allFileList.isFile()) {
					outputFile = outputFile + "File - " + allFileList.getName() + "\n";
				} else if (allFileList.isDirectory()) {
					outputFile = outputFile + "Directory - " + allFileList.getName() + "\n";
				}
			}
			outputStream.writeObject(outputFile);
		} catch (Exception e) {
			outputStream.writeObject("Error: listing contents of the directory" + "\n");
		}
	}

	public void cd(String dir) throws Exception {
		try {
			// cd
			if (dir == "") {
				path = Paths.get(System.getProperty("user.dir"));
				// outputStream.writeObject("\n");
			}
			// cd ..
			else if (dir.equals("..")) {
				if (path.getParent() != null)
					path = path.getParent();

				// outputStream.writeObject("\n");
			}
			// cd somedirectory
			else {
				// not a directory or file
				if (Files.notExists(path.resolve(dir))) {
					outputStream.writeObject("cd: " + dir + ": No such file or directory" + "\n");
				}
				// is a directory
				else if (Files.isDirectory(path.resolve(dir))) {
					path = path.resolve(dir);
					// outputStream.writeObject("\n");
				}
				// is a file
				else {
					outputStream.writeObject("cd: " + dir + ": Not a directory" + "\n");
				}
			}
			workingDirectory = path.toString();
			System.out.println("Present directory is " + workingDirectory);
			outputStream.writeObject(workingDirectory);
		} catch (Exception e) {
			outputStream.writeObject("cd: " + dir + ": Error" + "\n");
		}
	}

	public void pwd() throws Exception {
		// send path
		outputStream.writeObject(path + "\n");
	}

	public void mkdir(String dir) throws Exception {
		try {
			System.out.println("mkdir path"+path.toString());
			Files.createDirectory(path.resolve(dir));
			outputStream.writeObject("Directory created");
		} catch (Exception e) {
			outputStream.writeObject("mkdir: cannot create directory `" + dir + "': Permission denied" + "\n");
		}
	}

	public void delete(String delFile) {
		try {
			File file = new File(delFile);
			if (file.delete()) {
				outputStream.writeObject(delFile + " File deleted");
			} else
				outputStream.writeObject("File" + delFile + "doesn't exists in project root directory");
		} catch (Exception e) {
			System.out.println("Error while deleting file");
		}
	}

	public void quit() throws Exception {
		try {
			// outputStream.writeObject("quit");
			inputStream.close();
			outputStream.flush();
			outputStream.close();
			socket.close();
		} catch (Exception e) {
			System.out.println("Error while closing the connection");
		} finally {
			socket.close();
		}

	}

	public void get(String file) {
		
		
		try {

			//file=path.toString()+"/"+file;
			//generate random command ID
			int commandId = ThreadLocalRandom.current().nextInt(1, 99 + 1);
			outputStream.writeObject("The command ID for your get "+file+"request is "+String.valueOf(commandId));
			getPutMap.put(commandId, "Stop " + "get " + file); // 1 is running
			
			while (true) {
				
				boolean operationOnThisFile=false; 
				for (Map.Entry<Integer,String > entry : myftpServer.getPutMap.entrySet()) {
					if(entry.getValue().equals("active put "+file)) operationOnThisFile=true;
			    }
				
				if (!operationOnThisFile) {

					System.out.println("Starting get request of : " + clientNo+" for file "+file);
					getPutMap.put(commandId, "active " + "get " + file); // 1 is running
					//Thread.sleep(10000);
					File getFile = new File(file);
					System.out.println("before getFile ");
					BufferedInputStream br = new BufferedInputStream(new FileInputStream(getFile));
					
					System.out.println("before buffer ");
					byte[] buffer = new byte[1000];
					int len=0;
					outputStream.writeInt((int) getFile.length());
					System.out.println("before while loop");

					while((len=br.read(buffer))>0) {
						System.out.println("in while loop");

						String commandState = getPutMap.get(Integer.parseInt(String.valueOf(commandId)));
						if(commandState.contains("terminate")) {
							System.out.println("User terminated" + commandId+ " which is get "+file);
							outputStream.writeObject("terminated");
							break;
						}
						else {
							outputStream.writeObject("Still Runnuing");
						}
						outputStream.write(buffer, 0, len);
						//TimeUnit.SECONDS.sleep(30);
					}
					br.close();
					outputStream.flush();

					System.out.println("Sent file " + file+" successfully to client "+clientNo);
					getPutMap.put(commandId, "Sent " + file); // 1 is running
					break;
				}
				System.out.println("active PUT operation on file "+file+" client "+clientNo+" needs to wait");
				TimeUnit.SECONDS.sleep(30);
			}

			

		
		}
		catch (Exception e) {
			System.out.println("Error occured while getting file "+file+" for client "+clientNo);
		}
	}

	public void put(String putFile) {
		try {
			
			int commandId = ThreadLocalRandom.current().nextInt(1, 99 + 1);
			getPutMap.put(commandId, "Stop " + "put " + putFile); // 1 is running
			outputStream.writeObject(String.valueOf(commandId));
			
			while (true) {
				boolean operationOnThisFile=false; 
				for (Map.Entry<Integer,String > entry : myftpServer.getPutMap.entrySet()) {
					if(entry.getValue().equals("active put "+putFile)) operationOnThisFile=true;
			    }
				if (!operationOnThisFile) {

					System.out.println("Starting put request of : " + clientNo+" for file "+putFile);
					boolean terminate = false;

					getPutMap.put(commandId, "active " + "put " + putFile); // 1 is running

					int fileLength = inputStream.readInt();
					System.out.println("fileLength"+fileLength);
					byte[] a = new byte[fileLength];
					int counter = 0;
					int rem = fileLength % 1000;
					int limit = fileLength - rem;

					FileOutputStream fos = new FileOutputStream(putFile);
					BufferedOutputStream bos = new BufferedOutputStream(fos);
					outputStream.flush();
					
					byte[] buffer = new byte[1000];
					long bytesReceived = 0;
					int count=0;
					String state="";
					/*while(bytesReceived < fileLength) {
						state = getPutMap.get(Integer.parseInt(String.valueOf(commandId)));
						if (state.contains("terminate")) {
							outputStream.writeObject("terminated");
							break;
						}
						else outputStream.writeObject("Still Runnuing");
						count = inputStream.read(buffer);
						bos.write(buffer, 0, count);
						bytesReceived += count;
					}
					
					if(state.contains("terminate")) {
						System.out.println("Process with command Id " + commandId
								+ " terminated by user! Deleting the garbage file creadted during the process..");
						File file = new File(putFile);
						file.delete();
					}
					bos.close();
					fos.close();*/
					
					if (fileLength >= 1000) {
						//System.out.println("in first if");
						while (counter < limit) {
							System.out.println("in counter < limit");
							state = getPutMap.get(Integer.parseInt(String.valueOf(commandId)));
							if (state.contains("terminate"))
								terminate = true;
							System.out.println("in terminate");
							if (terminate) {
								//System.out.println("in if of terminate");
								outputStream.writeObject("terminated");
								break;
							}

							else {
								//System.out.println("in else before still running");
								outputStream.writeObject("Still Runnuing");

								inputStream.read(a, counter, 1000);
								counter += 1000;
							}
								
							
						}
						if (terminate) {
							System.out.println("User terminated " + commandId
									+ " which is for "+putFile+"deleting temp created file");
							File file = new File(putFile);
							file.delete();

						} else if (rem != 0) {
							//System.out.println("in rem != 0");
							// System.out.println("in if");
							// counter-=1000;
							// System.out.println("The value of counter in IF is "+counter);
							inputStream.read(a, counter, rem);
						}
					} else {
						//System.out.println("in else before inputStream.read(a, counter, fileLength);");
						//System.out.println("in else if");
						inputStream.read(a, counter, fileLength);
					}
					bos.write(a, 0, a.length);
					bos.close();
					fos.close();

					System.out.println("put executed" + putFile);
					getPutMap.put(commandId, "Finished " + "put " + putFile); // 1 is running
					break;
				}
				System.out.println("active PUT operation on file "+putFile+" client "+clientNo+" needs to wait");
				TimeUnit.SECONDS.sleep(30);

			}
			
		} catch (Exception e) {
			System.out.println("In exception of catch");
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated constructor stub
		try {

			while (true) {
				// get command(request) from client
				String command = (String) inputStream.readObject();

				String[] splitCommand = command.split(" ");
				System.out.println("command recieved - > at client no ->" + clientNo + " - > " + command);

				workingDirectory.concat("/");

				switch (splitCommand[0]) {
				case "get":
					if(splitCommand[1].contains("/")) get(splitCommand[1]);
					else get(workingDirectory + "/" + splitCommand[1]);
					break;
				case "put":
					if(splitCommand[1].contains("/")) put(splitCommand[1]);
					else put(workingDirectory + "/" + splitCommand[1]);
					break;
				case "delete":
					delete(workingDirectory + "/" + splitCommand[1]);
					break;
				case "ls":
					ls();
					break;
				case "cd":
					String dir = "";
					if (splitCommand.length > 1)
						dir = splitCommand[1];
					cd(dir);
					break;
				case "mkdir":
					mkdir(splitCommand[1]);
					break;
				case "pwd":
					pwd();
					break;
				case "quit":
					quit();
					break;
				default:
					System.out.println("invalid command");
				}

			}

		} catch (Exception e) {
			System.out.println("exiting");

		}
	}
}