package de.dhbw.connection;

import de.dhbw.messages.Message;
import de.dhbw.worker.Role;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

/**
 * This class represents a single connection and contains the socket and connected streams.
 * Furthermore, it contains information about the listenerPort of the peer and it´s address.
 * It also contains the role of the connected peer, which can be either: CLIENT, WORKER or UNKNOWN.
 */
public class Connection{
    final private Socket socket;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private int ListenerPort;
    private final InetAddress address;

    private Role role = Role.UNKNOWN;

    private boolean isInterrupted = false;

    /**
     * Create a connection object based on a socket.
     * @param socket The socket the connection is based on.
     */
    public Connection(Socket socket) {
        this.socket = socket;
        this.address = socket.getInetAddress();
    }

    /**
    * This method first creates the ObjectOutputStream and then the ObjectInputStream (Blocking) on the
     * client side of the application (The peer, who initiated the connection).
    */
    public void connectStreamsClient(){
        // create input and output streams
        try {
            this.objectOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
            this.objectOutputStream.flush();

            // wait for server message, blocks until corresponding outputstream has flushed
            this.objectInputStream = new ObjectInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method first creates the ObjectInputStream (Blocking) and then the ObjectOutputStream on the
     * client server of the application (The peer, who accepted the connection). This is in reverse order
     * of connectStreamsClient().
     */
    public void connectStreamsServer(){
        // create input and output streams
        try {
            // wait for client message, blocks until corresponding outputstream has flushed
            this.objectInputStream = new ObjectInputStream(this.socket.getInputStream());

            this.objectOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
            objectOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read a message from the ObjectInputstream. This Operation is blocking!
     * @return The message object.
     * */
    public synchronized Message read() throws IOException {
        try {
            this.objectInputStream.readInt();
            return (Message) objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Write a message to the ObjectOutputStream, to the connected peer.
     * @param message The message to write
     */
    public synchronized void write(Message message){
        String receiver = address.toString().concat(":").concat(Integer.toString(socket.getPort()));
        message.setReceiver(receiver);

        try {
            this.objectOutputStream.writeInt(0);
            this.objectOutputStream.writeObject(message);
            this.objectOutputStream.flush();
        } catch (IOException e) {
            // If the connection has been closed and an IOException occurs, set isInterrupted to true
            this.isInterrupted = true;
        }
    }

    /**
     * Check, if a write error occured while trying to send a message.
     * @return True, if writing a message did fail.
     */
    public boolean isInterrupted() {
        return isInterrupted;
    }

    /**
     * Check if the InputStream contains a new message, this operation is non-blocking.
     * @return True, if new message has arrived, else false.
     */
    public boolean available() {
        try {
            return objectInputStream.available() != 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Close the Input/OutputStream and the socket.
     */
    public void close(){
        try {
            objectOutputStream.close();
            objectInputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Return the ListenerPort of the remote peer.
     * @return The listenerPort on the remote.
     */
    public int getlistenerPort() {
        return ListenerPort;
    }

    /**
     * Set the listenerPort of the remote peer.
     * @param port The new listenerPort of the remote peer
     */
    public void setListenerPort(int port){
        this.ListenerPort = port;
    }

    /**
     * Get role of the remote peer.
     * @return Role of the remote peer.
     */
    public Role getRole(){
        return role;
    }

    /**
     * Get the network address of the remote peer.
     * @return network address of the remote peer.
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Set the role of the remote peer.
     * @param role The role of the remote peer.
     */
    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String toString(){
        if(socket != null){
            return String.format("[%s:%d]", address, getlistenerPort());
        } else {
            return "Not connected";
        }
    }
}
