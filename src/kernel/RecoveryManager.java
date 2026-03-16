package kernel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import system.BankDatabase;

public class RecoveryManager{
    private BankDatabase bank;
    
    public RecoveryManager(BankDatabase bank){
        this.bank=bank;

    }

    public void recover(){
        System.out.println("Recovering transactions...");
        try(BufferedReader br= new BufferedReader(new FileReader("data/transaction.log"))){
            String line;
            while((line=br.readLine())!=null){
                if(line.startsWith("COMMIT")){
                    String command =line.substring(7);//slicing till just after COMMIT word
                    replay(command);
                
                }
            }
        }
        catch(IOException e){
            System.out.println("Recovery failed.");
        }
    }
    private void replay(String command){
        String[] tokens=command.split("\\s+");
        if(tokens[0].equalsIgnoreCase(command)){
            String name=tokens[1];
            double balance=Double.parseDouble(tokens[2]);
            bank.createAccount(name, balance);
            
        }
        if(tokens[0].equalsIgnoreCase("TRANSFER")){
            String from=tokens[1];
            String to=tokens[2];
            double amount=Double.parseDouble(to);

            bank.transfer(from, to, amount);
        }
    }
}