package de.dhbw;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

/**
 * This class represents a single connection
 */
public class Connection implements Serializable{
    transient private Socket socket;
    private Role role = Role.UNKNOWN;
    private int ListenerPort;
    private final InetAddress address;
    transient private ObjectOutputStream objectOutputStream;
    transient private ObjectInputStream objectInputStream;

    public Connection(Socket socket) {
        this.socket = socket;
        this.address = socket.getInetAddress();
    }

    public void connectStreamsClient(){
        // create input and output streams
        try {
            this.objectOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
            // this.hello();
            this.objectOutputStream.flush();

            // wait for server hello
            this.objectInputStream = new ObjectInputStream(this.socket.getInputStream()); // block until corresponding outputstream has flushed
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connectStreamsServer(){
        // create input and output streams
        try {
            // wait for first hello message
            this.objectInputStream = new ObjectInputStream(this.socket.getInputStream()); // block until corresponding outputstream has flushed

            this.objectOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
            // this.hello();
            objectOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String concatAddressPort(InetAddress address, int port){
        return address.toString().concat(":").concat(Integer.toString(port));
    }

    public int getlistenerPort() {
        return ListenerPort;
    }

    public void setListenerPort(int port){
        this.ListenerPort = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public Role getRole(){
        return role;
    }
    public boolean available() {

        try {
            return objectInputStream.available() != 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized Message read() {
        try {
            int delimiter = this.objectInputStream.readInt();
            return (Message) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized void write(Message message){
        message.setReceiver(concatAddressPort(socket.getInetAddress(), socket.getPort()));
        try {
            Logger.log("Write Message: ".concat(message.toString().concat("--> ").concat(Integer.toString(socket.getPort()))));
            this.objectOutputStream.writeInt(0);
            this.objectOutputStream.writeObject(message);
            this.objectOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void close(){
        try {
            objectOutputStream.close();
            objectInputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString(){
        if(socket != null){
            return String.format("[Port: %d, EndpointListener: %d, Connected: %b]",socket.getPort(), getlistenerPort(), socket.isConnected());
        } else {
            return "Not connected";
        }

    }

}
