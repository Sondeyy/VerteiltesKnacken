package de.dhbw;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Tests {
    public static void main(String[] args) throws InterruptedException {

        // server
        (new Thread(() -> {
            ServerSocket server = null;
            Socket socket = null;
            try {
                server = new ServerSocket(20000, 100, InetAddress.getByName("localhost"));
                socket = server.accept();
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream()); // blocking
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

                while(true){
                    Thread.sleep(10);

                    while(true){
                        if(ois.available() != 0){
                            System.out.println("datta availave");
                            int delemiter = ois.readInt();

                            Message message = (Message) ois.readObject();
                            System.out.println("Server received message: ".concat(message.getPayload().toString()));

                            // answer with ok
                            Message answer = new Message();
                            answer.setPayload("OK");
                            oos.writeObject(answer);
                        }
                    }
                }

            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                e.printStackTrace();
            }
        })).start();

        Thread.sleep(10);
        // client

        (new Thread(() -> {
            Socket socket = null;
            try {
                socket = new Socket("localhost", 20000);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream()); // blocking


                while(true){
                    Thread.sleep(1000);

                    System.out.println("_____________________________");
                    // answer with ok
                    Message request = new Message();
                    request.setPayload("REQUEST");
                    oos.writeInt(0);
                    oos.flush();
                    oos.writeObject(request);
                    System.out.println("Client send message: REQUEST");

                    Message answer = (Message) ois.readObject();
                    System.out.println("CLient received: ".concat(answer.getPayload().toString()));
                }

            } catch (IOException | InterruptedException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        })).start();
    }
}
