package de.dhbw;

public class Logger {
    static boolean DEBUG = true;

    public static void log(String text){
        if(DEBUG) System.out.println("[".concat(Thread.currentThread().getName()).concat("]: ").concat(text));
    }
}
