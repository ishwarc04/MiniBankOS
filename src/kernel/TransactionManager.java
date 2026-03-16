package kernel;

import system.BankDatabase;
import logging.Logger;

public class TransactionManager{
    private BankDatabase bank;
    private Logger logger;

    public TransactionManager(BankDatabase bank){
        this.bank=bank;
        this.logger=new Logger();//new logger object for each thread
    }
    public void createAccount(String name, double balance){
        String logEntry="CREATE "+ name+ " " + balance;
        logger.log("BEGIN "+logEntry);
        bank.createAccount(name, balance);
        logger.log("COMMIT "+logEntry);
    }
    public void transfer(String from, String to, double amount){
        String logEntry="TRANSFER"+from+" "+to+" "+amount;
        logger.log("BEGIN "+logEntry);
        Thread t=new Thread(() -> {bank.transfer(from, to, amount);});
        t.start();
        try{
            t.join();

            logger.log("COMMIT "+logEntry);
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
    }
    public void checkBalance(String name){
        bank.checkBalance(name);
    }
}