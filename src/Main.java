import kernel.RecoveryManager;
import shell.Terminal;
import system.BankDatabase;
public class Main {
    public static void main(String args[]){
        BankDatabase bank=new BankDatabase();


        RecoveryManager recovery=new RecoveryManager(bank);
        recovery.recover();


        Terminal terminal=new Terminal();
        terminal.start();
    }
}
