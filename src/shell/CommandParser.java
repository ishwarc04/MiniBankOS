package shell;

import java.util.*;
import kernel.TransactionManager;

public class CommandParser{
    private TransactionManager tm;

    public CommandParser(TransactionManager tm){
        this.tm=tm;
    }

    public void shutdown(){
        tm.shutdown();
    }


    public void parse(String command){
        command=command.trim();
        if(command.isEmpty()){
            return;
        }
        String tokens[]=command.split("\\s+");

        if(tokens[0].equalsIgnoreCase("help")){
            if(tokens.length==1){
                HelpPrinter.printAll();
                return;
            }
            if(tokens.length==2){
                HelpPrinter.printCommand(tokens[1]);
                return;
            }
            HelpPrinter.printUsage("help");
        }
        else if(tokens[0].equalsIgnoreCase("create")){
            if(tokens.length!=4){
                HelpPrinter.printUsage("create");
                return;
            }
            String name = tokens[1];
            double balance;
            try{
                balance= Double.parseDouble(tokens[3]);
            }
            catch(NumberFormatException e){
                System.out.println("Invalid balance.");
                return;
            }

            tm.createAccount(name, tokens[2], balance);

        }
        else if(tokens[0].equalsIgnoreCase("balance")){
            if(tokens.length!=2){
                HelpPrinter.printUsage("balance");
                return;
            }
            String name=tokens[1];
            tm.checkBalance(name);
        }
        else if(tokens[0].equalsIgnoreCase("transfer")){
            if(tokens.length!=4){
                HelpPrinter.printUsage("transfer");
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
        else if(tokens[0].equalsIgnoreCase("deposit")){
            if(tokens.length!=3){
                HelpPrinter.printUsage("deposit");
                return;
            }
            try{
                double amount=Double.parseDouble(tokens[2]);
                tm.deposit(tokens[1], amount);
            }
            catch(NumberFormatException e){
                System.out.println("Invalid amount.");
            }
        }
        else if(tokens[0].equalsIgnoreCase("withdraw")){
            if(tokens.length!=3){
                HelpPrinter.printUsage("withdraw");
                return;
            }
            try{
                double amount=Double.parseDouble(tokens[2]);
                tm.withdraw(tokens[1], amount);
            }
            catch(NumberFormatException e){
                System.out.println("Invalid amount.");
            }
        }
        else if(tokens[0].equalsIgnoreCase("transactions") || tokens[0].equalsIgnoreCase("history")){
            if(tokens.length!=2){
                HelpPrinter.printUsage(tokens[0]);
                return;
            }
            tm.showTransactions(tokens[1]);
        }
        else if(tokens[0].equalsIgnoreCase("loan")){
            if(tokens.length!=5){
                HelpPrinter.printUsage("loan");
                return;
            }
            try{
                double amount=Double.parseDouble(tokens[3]);
                int durationYears=Integer.parseInt(tokens[4]);
                tm.createLoan(tokens[1], tokens[2], amount, durationYears);
            }
            catch(NumberFormatException e){
                System.out.println("Invalid loan amount or duration.");
            }
        }
        else if(tokens[0].equalsIgnoreCase("loans")){
            if(tokens.length!=2){
                HelpPrinter.printUsage("loans");
                return;
            }
            tm.showLoans(tokens[1]);
        }
        else if(tokens[0].equalsIgnoreCase("loanrates")){
            if(tokens.length!=1){
                HelpPrinter.printUsage("loanrates");
                return;
            }
            tm.showLoanRates();
        }
        else if(tokens[0].equalsIgnoreCase("loanupdate")){
            if(tokens.length!=1){
                HelpPrinter.printUsage("loanupdate");
                return;
            }
            tm.updateLoansNow();
        }
        else if(tokens[0].equalsIgnoreCase("register")){
            if(tokens.length!=3){
                HelpPrinter.printUsage("register");
                return;
            }
            tm.register(tokens[1],tokens[2]);
        }
        else if(tokens[0].equalsIgnoreCase("login")){
            if(tokens.length!=3){
                HelpPrinter.printUsage("login");
                return;
            }
            tm.login(tokens[1],tokens[2]);
        }
        else if(tokens[0].equalsIgnoreCase("logout")){
            if(tokens.length!=1){
                HelpPrinter.printUsage("logout");
                return;
            }
            tm.logout();
        }
        else if(tokens[0].equalsIgnoreCase("grant")){
            if(tokens.length!=2){
                HelpPrinter.printUsage("grant");
                return;
            }
            tm.grantTransfer(tokens[1]);
        }
        else if(tokens[0].equalsIgnoreCase("revoke")){
            if(tokens.length!=2){
                HelpPrinter.printUsage("revoke");
                return;
            }
            tm.revokeTransfer(tokens[1]);
        }
        else if(tokens[0].equalsIgnoreCase("delete")){
            if(tokens.length!=2){
                HelpPrinter.printUsage("delete");
                return;
            }
            tm.deleteUser(tokens[1]);
        }
        else if(tokens[0].equalsIgnoreCase("deadlock-demo")){
            if(tokens.length == 4){
                tm.runDeadlockDemo(tokens[1], tokens[2], tokens[3]);
            } else {
                System.out.println("Usage: deadlock-demo <account1> <account2> <account3>");
                System.out.println("Example: deadlock-demo alice bob charlie");
            }
        }
        else if(tokens[0].equalsIgnoreCase("slow-mode")){
            if(tokens.length==2 && tokens[1].equalsIgnoreCase("on")){
                tm.setSlowMode(true);
            } else if(tokens.length==2 && tokens[1].equalsIgnoreCase("off")){
                tm.setSlowMode(false);
            } else {
                System.out.println("Usage: slow-mode on | slow-mode off");
            }
        }
        else if(tokens[0].equalsIgnoreCase("disk-schedule")){
            // disk-schedule <FCFS|SSTF|SCAN>
            if(tokens.length != 2){
                HelpPrinter.printUsage("disk-schedule");
                return;
            }
            tm.setDiskAlgorithm(tokens[1]);
        }
        else if(tokens[0].equalsIgnoreCase("disk-stats")){
            tm.printDiskStats();
        }
        else if(tokens[0].equalsIgnoreCase("disk-reset")){
            tm.resetDiskStats();
        }
        else{
            System.out.println(tokens[0]+" is not a valid command.");
            System.out.println("Run 'help' to see all commands.");
        }
        return;
    }
}
