package de.dhbw;

public class Main {

    public static void main(String[] args) {
        // init client
        Client client = new Client();
        Thread client_thread = new Thread(client);

        // init follower
        Worker follower = new Worker(Role.FOLLOWER);
        Thread follower_thread = new Thread(follower);

        // init leader
        Worker leader = new Worker(Role.LEADER);
        Thread leader_thread = new Thread(leader);

        client_thread.start();
        follower_thread.start();
        leader_thread.start();
        
        try {
            client_thread.join();
            follower_thread.join();
            leader_thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
