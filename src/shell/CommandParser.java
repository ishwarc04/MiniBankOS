package shell;

import system.BankDatabase;
import kernel.TransactionManager;
import kernel.scheduler.*;

public class CommandParser{

    private Scheduler scheduler=new Scheduler();
    private BankDatabase bank=new BankDatabase();
    private TransactionManager tm=new TransactionManager(bank, scheduler);


    public void parse(String command){
        command=command.trim();
        if(command.isEmpty()) return;
        String tokens[]=command.split("\\s+");

        if(tokens[0].equalsIgnoreCase("create")){
            if(tokens.length<3){
                System.out.println("Usage: create <name> <balance>");
                return;
            }
            String name = tokens[1];
            double balance= Double.parseDouble(tokens[2]);

            bank.createAccount(name,balance);

        }
        else if(tokens[0].equalsIgnoreCase("balance")){
            if(tokens.length<2){
                System.out.println("Usage: balance <name>");
                return;
            }
            String name=tokens[1];
            bank.checkBalance(name);
        }
        else if(tokens[0].equalsIgnoreCase("transfer")){
            if(tokens.length<4){
                System.out.println("Usage: tranfer <sender_name> <receiver_name> <amount>");
                return;
            }
            try{
                double amount=Double.parseDouble(tokens[3]);
                tm.transfer(tokens[1],tokens[2],amount);
            }
            catch(NumberFormatException e){
                System.out.println("Invalid amount.");
            }
        }
        else{
            System.out.println(tokens[0]+" is not a valid command.");
            System.out.println("Unknown command. Type 'help' for a list of commands.*Reminder to add help command*");
        }
        return;
    }
}