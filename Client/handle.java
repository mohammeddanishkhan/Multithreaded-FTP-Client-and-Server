/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author susha
 */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class handle implements Runnable {

    String[] commandAndValue;
    String sending_line = "";
    ObjectOutputStream outputStream = null;
    ObjectInputStream inputStream = null;
    Socket socketAmperHandler;
    String commandId;
    String dir;

    public handle(String serverName, String input, String directory, int nPort) throws IOException {
        sending_line = input;
        //this.commandAndValue = input.split(" ");
        socketAmperHandler = new Socket(serverName, nPort);
        dir = directory;
        //System.out.println("Inside handle dir " + dir);
    }

    public void run() {
        try {

            //System.out.println("Inside thread 1 dir " + dir);

            outputStream = new ObjectOutputStream(socketAmperHandler.getOutputStream());
            inputStream = new ObjectInputStream(socketAmperHandler.getInputStream());

            if (sending_line.contains("get")) {
                
                String fileName = "";
                if (sending_line.contains("/")) {
                    String[] sending_lineArr = sending_line.split("/");
                    fileName = sending_lineArr[sending_lineArr.length - 1];
                } else {
                    String[] sending_lineArr = sending_line.split(" ");
                    fileName = sending_lineArr[1].trim();
                }
                
                if (fileName.charAt(fileName.length() - 1) == '&') {
                    fileName = fileName.substring(0, fileName.length() - 1);
                } else {
                }
                
                //System.out.println("file name : " + fileName);

                outputStream.writeObject("get " +  fileName);

                commandId = (String) inputStream.readObject();
                System.out.println("CommandID : " + commandId);
                boolean terminated = false;
                int length = inputStream.readInt();

                FileOutputStream fos = new FileOutputStream(fileName);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                byte[] buffer = new byte[1000];
                int count=0;
                long bytesReceived=0;
                String state = (String) inputStream.readObject();
                if (!state.contains("terminated")) {
                    while (bytesReceived<length) {
                        count=inputStream.read(buffer);
                        bos.write(buffer, 0, count);
                        bytesReceived+=count;
                            if(!(bytesReceived>=length)) state = (String) inputStream.readObject();;
                        
                        if (state.contains("terminated")) {
                            terminated = true;
                            break;
                        }
                        
                    }
                }
                if (state.contains("terminated")) {
                    File file = new File(fileName);
                    file.delete();
                }
                bos.close();
                fos.close();           
            }

            if (sending_line.contains("put")) {

                String fileName = "";
                if (sending_line.contains("/")) {
                    String[] sending_lineArr = sending_line.split("/");
                    fileName = sending_lineArr[sending_lineArr.length - 1];
                } else {
                    String[] sending_lineArr = sending_line.split(" ");
                    fileName = sending_lineArr[1];
                }

                if (fileName.charAt(fileName.length() - 1) == '&') {
                    fileName = fileName.substring(0, fileName.length() - 1);
                } else {
                }

                System.out.println("Inside thread put fileName " + fileName);
                System.out.println("Inside thread put dir " + dir);

                boolean terminated = false;
                outputStream.writeObject("put " + fileName);
                commandId = (String) inputStream.readObject();
                System.out.println("Command Id: " + commandId);
                File myfile = new File(fileName);
                int length = (int) myfile.length();
                outputStream.writeInt(length);
                int counter = 0;
                byte[] a = new byte[length];
                int rem = length % 1000;
                int limit = length - rem;
                BufferedInputStream br = new BufferedInputStream(new FileInputStream(myfile));
                br.read(a, 0, a.length);

                outputStream.flush();
                if (length >= 1000) {
                    while (counter < limit) {
                        //System.out.println("b4 isTerminated");
                        String isTerminated = (String) inputStream.readObject();
                        //System.out.println("isTerminated"+isTerminated);
                        if (isTerminated.contains("terminated")) {
                            terminated = true;
                        }

                        if (terminated) {
                            //System.out.println("Put Operation Terminated by user!");
                            break;
                        }

                        outputStream.write(a, counter, 1000);
                        counter += 1000;
                        outputStream.flush();
                    }
                    if (terminated) {
                        
                    } else if (rem != 0) {
                        outputStream.write(a, counter, rem);
                    }
                } else {
                    outputStream.write(a, counter, length);
                }

                br.close();
                outputStream.flush();
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error");
        }

    }
}
