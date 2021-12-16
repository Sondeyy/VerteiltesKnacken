package de.dhbw;


/**
 * This class is used to enrich logs from different threads with their respective thread name.
 */
public class Logger {
    static boolean DEBUG = true;

    /**
     * Write a text to the console together with the name of the thread that called Log().
     * @param text The text to log
     */
    public static void log(String text){
        if(DEBUG) System.out.println("[".concat(Thread.currentThread().getName()).concat("]: ").concat(text));
    }
}
