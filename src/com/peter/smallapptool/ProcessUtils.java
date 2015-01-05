package com.peter.smallapptool;

import java.io.IOException;
import java.io.OutputStream;
import android.util.Log;

public class ProcessUtils {
   
    
    public static int executeCommand(final String command, final long timeout) {
        Process process = null;
        //root 获取
        try {
            process = Runtime.getRuntime().exec("su");
            OutputStream out = process.getOutputStream();
            out.write(command.getBytes());
            out.flush();
        } catch (IOException e1) {
            return -1;
        }
        
        Worker worker = new Worker(process);
        worker.start();
        try {
            worker.join(timeout);
            if (worker.exit != null){
                return worker.exit;
            } else{
                return -2;
            }
        } catch (InterruptedException ex) {
            worker.interrupt();
            Thread.currentThread().interrupt();
            Log.i("executeCommand", "InterruptedException");
            return -3;
        } finally {
            try {
                if (process != null) { 
                    // use exitValue() to determine if process is still running.  
                    process.exitValue(); 
                } 
            } catch (IllegalThreadStateException e) { 
                // process is still running, kill it. 
                process.destroy(); 
                return -4;
            } 
        }
    }
   

    private static class Worker extends Thread {
        private final Process process;
        private Integer exit;

        private Worker(Process process) {
            this.process = process;
        }

        public void run() {
            try {
                //0 indicates normal termination
                exit = process.waitFor();
            } catch (InterruptedException ignore) {
                Log.i("worker", "InterruptedException");
                return;
            }
        }
    }

}