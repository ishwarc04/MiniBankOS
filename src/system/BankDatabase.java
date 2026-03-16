package system;

import java.util.HashMap;
import kernel.ReadWriteManager;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BankDatabase{

    private HashMap<String, Account> accounts=new HashMap<>();
    private ReadWriteManager lockManager=new ReadWriteManager();

    public void createAccount(String name, double balance){

        if(accounts.containsKey(name)){
            System.out.println("This account name already exists.");
            return;
        }

        if(balance<0){
            System.out.println("Balance cannot be negative.");
            return;
        }

        //create account and add to hashmap
        accounts.put(name,new Account(name,balance));

        System.out.println("Account created for "+name);
        return;

    }

    public void checkBalance(String name){

        Account acc=accounts.get(name);

        if(acc==null){
            System.out.println("Account not found.");
            return;
        }
        ReentrantReadWriteLock lock= lockManager.getLock(name);
        lock.readLock().lock();

        try{
            System.out.println("Name :"+ name +"\n Balance = "+acc.getBalance());
        }
        finally{
            lock.readLock().unlock();
        }
    }

    public void transfer(String from, String to, double amount){
        if(from.equals(to)){
            System.out.println("Cannot transfer to the same account.");
            return;
        }
        if(amount<=0){
            System.out.println("Invalid amount.");
            return;
        }

        Account sender=accounts.get(from);
        Account receiver=accounts.get(to);

        if(sender==null||receiver==null){
            System.out.println("Account not found.");
            return;
        }
        ReentrantReadWriteLock lock1=lockManager.getLock(from);
        ReentrantReadWriteLock lock2=lockManager.getLock(to);

        lock1.writeLock().lock();
        lock2.writeLock().lock();

        try{
            
            if(sender.getBalance() < amount){
                System.out.println("Insufficient funds.");
                return;
            }
            sender.withdraw(amount);
            receiver.deposit(amount);

            System.out.println("\n"+ amount +" transferred from "+ from +" to "+ to);
            return;
        }
        finally{
            lock1.writeLock().unlock();
            lock2.writeLock().unlock();
        }
    }
}