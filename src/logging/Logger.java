package logging;

import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;

public class Logger{
    private final String logFile="data\\transaction.log";

    public Logger(){
        try{
            File file=new File(logFile);
            //if non existent create new file and directory
            if(!file.exists()){
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        }
        catch(IOException e){
            System.out.println("Error creating log files.");
        }
    }

    public synchronized void log(String entry){
        try(FileWriter fw=new FileWriter(logFile, true);
            PrintWriter out=new PrintWriter(fw)){
                out.println(entry);
            }
            catch(IOException e){
                System.out.println("Logging failed.");
            }
    }
}