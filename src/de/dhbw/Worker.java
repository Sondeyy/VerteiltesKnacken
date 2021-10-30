package de.dhbw;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Worker implements Runnable{
    private int id;
    private Role role;
    private Socket socket;
    private final List<Connection> connectionList = new ArrayList<>();
    private final List<Message> messageList = new ArrayList<>();
    private boolean active = true;

    public Worker(Role role) {
    }

    private void send(Message message, Socket socket) {

    }

    private void reactToMessage(Message message, Socket socket) {
        MessageType messageType = message.getType();
        Message answer = new Message();

        if (messageType == MessageType.WRITE) {
            messageList.add(message);
            answer.setType(MessageType.OK);
        }
        else if (messageType == MessageType.GET) {
            int howMany = Integer.parseInt(message.getSender());
            int messageListLength = messageList.size();
            List<Message> requested = messageList.subList(messageListLength - howMany, messageListLength);
        }

        send(answer, socket);
    }

    @Override
    public void run() {
        if (role == Role.FOLLOWER) {
            System.out.println("Im a follower!");
        }
        else if (role == Role.LEADER) {
            System.out.println("Im a leader");
            ServerSocket server;
            try {
                server = new ServerSocket(8080);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            while (active) {
                try {
                    Socket newSocket = server.accept();

                    connectionList.add(new Connection(newSocket));

                    for (Connection connection : connectionList) {
                        if (connection.available() != 0) {
                            Message newMessage = connection.read();
                            reactToMessage(newMessage, connection.getSocket());
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            //???
            System.out.println("I dont know, what I am.");
        }
    }
}
