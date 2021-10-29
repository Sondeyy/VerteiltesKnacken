package de.dhbw;

import java.io.*;
import java.net.Socket;

/**
 * This class is used as an example for reading Object
 * from a socket
 */
public class ObjectMessageReader {
    /**
     * this method reads objects from a given socket
     *
     * @param socket socket to read an object from
     * @return the message object or null , in case of an
     * error
     */
    public Message read(Socket socket) {
        Message ret = null;
        try {
            InputStream is = socket.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(is);
            ret = (Message) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void send(Socket socket, Message msg) {
        OutputStream os;
        try {
            os = socket.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(msg);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
