/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author susha
 */
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class myftp {

    private Socket socket = null;
    private ObjectOutputStream outputStream = null;
    private ObjectInputStream inputStream = null;
    private Scanner input = null;
    private String directory;

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {

        System.out.println("Enter the server machine name : ");
        Scanner sca = new Scanner(System.in);
        String serverName = sca.nextLine();
        System.out.println("Enter the port number for Normal Commands : ");
        int nPort = sca.nextInt();
        System.out.println("Enter the port number for Trminate Command : ");
        int tPort = sca.nextInt();
        myftp client = new myftp(serverName, nPort, tPort);
    }

    public myftp(String serverName, int nPort, int tPort) throws IOException, InterruptedException, ClassNotFoundException {
        //System.out.println("first--->" + serverName);
        input = new Scanner(System.in);
        socket = new Socket(serverName, nPort);
        String sending_line = "";
        String commandId;
        handle handleThread;

        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
        while (!sending_line.equals("quit")) {
            System.out.print("myftp>");
            sending_line = input.nextLine();
            //System.out.println(sending_line);
            String[] sent = sending_line.split(" ");

            if (sending_line.contains("get") && !(sending_line.contains("&"))) {
                String fileName = "";
                if (sending_line.contains("/")) {
                    String[] sending_lineArr = sending_line.split("/");
                    fileName = sending_lineArr[sending_lineArr.length - 1];
                } else {
                    String[] sending_lineArr = sending_line.split(" ");
                    fileName = sending_lineArr[1].trim();
                }
                //System.out.println("file name : " + fileName);

                outputStream.writeObject(sending_line);

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
                //System.out.println("flength : " + length);

                /*byte[] a = new byte[length];
                 int counter = 0;
                 int rem = length % 1000;
                 int limit = length - rem;
                 System.out.println("limit : " + limit );

                 FileOutputStream fos = new FileOutputStream(fileName);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 if (length >= 1000) {
                 while (counter < limit) {
                 //System.out.println("counter in loop : " + counter );
                 String state = (String) inputStream.readObject();
                 if (state.contains("terminated")) {
                 terminated = true;
                 }

                 if (terminated) {
                 break;
                 }

                 inputStream.read(a, counter, 1000);
                 counter += 1000;
                 }
                 if (terminated) {
                 File file = new File(fileName);
                 file.delete();
                 } else if (rem != 0) {
                 inputStream.read(a, counter, rem);
                 }

                 } else {
                 inputStream.read(a, counter, length);
                 }
                 bos.write(a, 0, a.length);*/
                

            }

            if (sending_line.contains("put") && !(sending_line.contains("&"))) {
                String fileName = "";
                if (sending_line.contains("/")) {
                    String[] sending_lineArr = sending_line.split("/");
                    fileName = sending_lineArr[sending_lineArr.length - 1];
                } else {
                    String[] sending_lineArr = sending_line.split(" ");
                    fileName = sending_lineArr[1];
                }
                //System.out.println("fileName  : " + fileName);

                boolean terminated = false;
                outputStream.writeObject("put " + fileName);
                commandId = (String) inputStream.readObject();
                System.out.println("CommandID: " + commandId);
                
                File myfile = new File(fileName);
                int length = (int) myfile.length();
                outputStream.writeInt(length);
                //System.out.println("file length"+length);
                int counter = 0;
                byte[] a = new byte[length];
                int rem = length % 1000;
                int limit = length - rem;
                BufferedInputStream br = new BufferedInputStream(new FileInputStream(myfile));
                /*int len=0;
                byte[] buffer=new byte[1000];
                while((len=br.read(buffer))>0) {
                    //System.out.println("Inside While");
                     String state = (String) inputStream.readObject();
                     //System.out.println("State : " + state );
                     if(state.contains("terminate")) {
                         break;
                    }
                     else outputStream.write(buffer, 0, len);
                }
                */
                
                br.read(a, 0, a.length);

                outputStream.flush();
                if (length >= 1000) {
                    while (counter < limit) {
                        String state = (String) inputStream.readObject();
                        if (state.contains("terminated")) {
                            terminated = true;
                        }

                        if (terminated) {
                            System.out.println("Terminated by user!");
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

            /*if (sending_line.contains("cd")) {
             System.out.println("Inside cd");
             directory = (String) inputStream.readObject();
             System.out.println("Inside cd directory : " + directory);
             }*/
            
            if (sending_line.contains("&")) {
                //System.out.println("Inside &");

                handle runnable = new handle(serverName,sending_line, directory, nPort);

                //System.out.println("runnable : " + runnable);

                Thread thread = new Thread(runnable, "thread");
                thread.start();
                Thread.sleep(100);

            }

            if (sending_line.contains("terminate")) {
                //System.out.println("Terminated");
                Socket socketTerminate = new Socket(serverName, tPort);
                ObjectOutputStream outputStreamTerminate = new ObjectOutputStream(socketTerminate.getOutputStream());
                //System.out.println("sent[1] : " + sent[1]);
                outputStreamTerminate.writeObject(sent[1]);
                socketTerminate.close();
                Thread.sleep(1000);

            }

            if (!(sending_line.contains("get") || sending_line.contains("put") || sending_line.contains("quit") || sending_line.contains("terminate"))) {
                outputStream.writeObject(sending_line);
                String message = (String) inputStream.readObject();
                System.out.println(message);

                if (sending_line.contains("cd")) {
                    directory = message;
                }

            }

        }

        if (sending_line.equals("quit")) {
            System.out.println("GoodBye!");
            outputStream.close();
            inputStream.close();
            socket.close();

        }
        Thread.sleep(1);
    }
}
