package kernel;

import system.BankDatabase;
import system.LoanManager;
import logging.Logger;
import kernel.scheduler.*;

import Auth.AuthManager;
import Auth.Session;
import Auth.user;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class TransactionManager{
    private BankDatabase bank;
    private Logger logger;
    private Scheduler scheduler;
    private AuthManager authManager;
    private ModeBit modeBit;
    private LoanManager loanManager;
    private BankersAlgorithm bankersAlgorithm;

    // SLOW MODE: holds resources for 5s after Banker's approval so concurrent
    // clients can see blocking live (for server demo)
    private volatile boolean slowMode = false;

    public void setSlowMode(boolean on) {
        this.slowMode = on;
        System.out.println("  [SLOW-MODE] " + (on ? "ON  -- transfers will hold locks for 5s (demo mode)" : "OFF -- normal speed"));
    }

    public TransactionManager(BankDatabase bank, Scheduler scheduler, AuthManager authManager, ModeBit modeBit){
        this.bank=bank;
        this.scheduler=scheduler;
        this.logger=new Logger();//new logger object for each thread
        this.authManager=authManager;
        this.modeBit=modeBit;
        this.loanManager=new LoanManager();
        this.loanManager.startRealtimeUpdates();
        this.bankersAlgorithm=new BankersAlgorithm();
    }
    public void createAccount(String name, double balance){
        user current=Session.getCurrentUser();
        
        if(current==null || !current.isAdmin()){
            System.out.println("Access denied: Only admin can create accounts.");
            return;
        }
        System.out.println("Warning: account created without login credentials. Use: create <account_name> <password> <opening_balance>");
        submitCreateAccount(name, balance);
    }

    public void createAccount(String name, String password, double balance){
        user current=Session.getCurrentUser();
        
        if(current==null || !current.isAdmin()){
            System.out.println("Access denied: Only admin can create accounts.");
            return;
        }
        if(password==null || password.trim().isEmpty()){
            System.out.println("Password cannot be empty.");
            return;
        }

        boolean accountExists=bank.hasAccount(name);
        boolean loginExists=authManager.getuser(name)!=null;

        if(accountExists && loginExists){
            System.out.println("This account name already exists and already has login credentials.");
            return;
        }

        if(accountExists){
            boolean createdLogin=authManager.register(name, password);
            if(createdLogin){
                System.out.println("Account already exists. Login credentials created for "+name+".");
            }
            else{
                System.out.println("Login user could not be created.");
            }
            return;
        }

        boolean createdLogin=false;
        if(!loginExists){
            createdLogin=authManager.register(name, password);
            if(!createdLogin){
                System.out.println("Login user could not be created.");
                return;
            }
        }
        else{
            System.out.println("Login user already exists. Keeping the existing password.");
        }

        boolean createdAccount=submitCreateAccount(name, balance);
        if(!createdAccount && createdLogin){
            authManager.deleteUser(name);
        }
    }

    public void recoverCreateAccount(String name, double balance){
        submitRecoveryCreateAccount(name, balance);
    }

    private boolean submitCreateAccount(String name, double balance){
        String logEntry="CREATE "+ name+ " " + balance;
        boolean[] success={false};
        TransactionProcess process= new TransactionProcess(
            "TX-"+System.currentTimeMillis(),
            () ->{
            logger.log("BEGIN "+logEntry);
            if(bank.createAccount(name, balance)){
                success[0]=true;
                logger.log("COMMIT "+logEntry);
            }
        },
        2//priority
    );
        submitKernelProcess(process);
        if(success[0]){
            bankersAlgorithm.registerResource(name); // register as resource
        }
        return success[0];
    }

    private void submitRecoveryCreateAccount(String name, double balance){
        TransactionProcess process= new TransactionProcess(
            "REC-"+System.currentTimeMillis(),
            () -> bank.recoverCreateAccount(name, balance),
            2
        );
        submitKernelProcess(process);
    }
    public void transfer(String from, String to, double amount){
        user current = Session.getCurrentUser();
        if(current==null){
            System.out.println("Access denied: not logged in.");
            return;
        }

        //admin cannot transfer
        if(current.isAdmin()){
            System.out.println("Access denied: Admin cannot perform transfers.");
            return;
        }

        //must own source account
        if(!current.getUsername().equals(from)){
            System.out.println("Access denied: Cannot trasnfer from other accounts.");
            return;
        }

        //permission check
        if(!current.canTransfer()){
            System.out.println("Access denied: Insufficient permissions.");
            return;
        }

        submitTransfer(from, to, amount);
    }

    public void recoverTransfer(String from, String to, double amount){
        submitRecoveryTransfer(from, to, amount);
    }

    private void submitTransfer(String from, String to, double amount){
        String logEntry="TRANSFER "+from+" "+to+" "+amount;
        String processId="TX-"+System.currentTimeMillis();

        // Banker's Algorithm: check safe state before granting account locks
        Set<String> needed=new HashSet<>(Arrays.asList(from, to));
        if(!bankersAlgorithm.requestAndCheck(processId, needed)){
            System.out.println("Transfer blocked by Banker's Algorithm (unsafe state).");
            return;
        }

        TransactionProcess process=new TransactionProcess(
            processId,
            () ->{
                logger.log("BEGIN "+logEntry);
                // SLOW-MODE: hold resources long enough for concurrent clients to arrive
                if (slowMode) {
                    System.out.println("  [SLOW-MODE] Holding [" + from + ", " + to + "] for 5s...");
                    try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
                if(bank.transfer(from, to, amount)){
                    logger.log("COMMIT "+logEntry);
                }
            },
            1//higher priority
        );
        submitKernelProcess(process);

        // Release accounts after transfer completes
        bankersAlgorithm.release(processId);
    }

    private void submitRecoveryTransfer(String from, String to, double amount){
        TransactionProcess process=new TransactionProcess(
            "REC-"+System.currentTimeMillis(),
            () -> bank.recoverTransfer(from, to, amount),
            1
        );
        submitKernelProcess(process);
    }

    public void deposit(String name, double amount){
        user current=Session.getCurrentUser();
        if(current==null){
            System.out.println("Access denied: not logged in.");
            return;
        }
        if(!current.isAdmin() && !current.getUsername().equals(name)){
            System.out.println("Access denied: Cannot deposit to other accounts.");
            return;
        }
        submitDeposit(name, amount);
    }

    public void recoverDeposit(String name, double amount){
        submitRecoveryDeposit(name, amount);
    }

    private void submitDeposit(String name, double amount){
        String logEntry="DEPOSIT "+name+" "+amount;
        TransactionProcess process=new TransactionProcess(
            "TX-"+System.currentTimeMillis(),
            () ->{
                logger.log("BEGIN "+logEntry);
                if(bank.deposit(name, amount)){
                    logger.log("COMMIT "+logEntry);
                }
            },
            2
        );
        submitKernelProcess(process);
    }

    private void submitRecoveryDeposit(String name, double amount){
        TransactionProcess process=new TransactionProcess(
            "REC-"+System.currentTimeMillis(),
            () -> bank.recoverDeposit(name, amount),
            2
        );
        submitKernelProcess(process);
    }

    public void withdraw(String name, double amount){
        user current=Session.getCurrentUser();
        if(current==null){
            System.out.println("Access denied: not logged in.");
            return;
        }
        if(!current.isAdmin() && !current.getUsername().equals(name)){
            System.out.println("Access denied: Cannot withdraw from other accounts.");
            return;
        }
        submitWithdraw(name, amount);
    }

    public void recoverWithdraw(String name, double amount){
        submitRecoveryWithdraw(name, amount);
    }

    private void submitWithdraw(String name, double amount){
        String logEntry="WITHDRAW "+name+" "+amount;
        TransactionProcess process=new TransactionProcess(
            "TX-"+System.currentTimeMillis(),
            () ->{
                logger.log("BEGIN "+logEntry);
                if(bank.withdraw(name, amount)){
                    logger.log("COMMIT "+logEntry);
                }
            },
            2
        );
        submitKernelProcess(process);
    }

    private void submitRecoveryWithdraw(String name, double amount){
        TransactionProcess process=new TransactionProcess(
            "REC-"+System.currentTimeMillis(),
            () -> bank.recoverWithdraw(name, amount),
            2
        );
        submitKernelProcess(process);
    }

    private void submitKernelProcess(TransactionProcess process){
        modeBit.enterKernelMode();
        try{
            scheduler.submitProcess(process);
            scheduler.runPendingProcesses();
        }
        finally{
            modeBit.enterUserMode();
        }
    }

    public void grantTransfer(String username){
        user current=Session.getCurrentUser();

        if(current==null|| !current.isAdmin()){
            System.out.println("Access denied: Only admins can grant permissions.");
            return;
        }
        user target=authManager.getuser(username);
        if(target==null){
            System.out.println("User not found.");
            return;
        }
        if(!authManager.setTransferPermission(username, true)){
            System.out.println("Permission update failed.");
            return;
        }
        System.out.println("Permission granted for user "+username);
    }

    public void revokeTransfer(String username){
        user current=Session.getCurrentUser();

        if(current==null || !current.isAdmin()){
            System.out.println("Access denied: Only admins can revoke permissions.");
            return;
        }
        user target=authManager.getuser(username);
        if(target==null){
            System.out.println("User not found.");
            return;
        }
        if(!authManager.setTransferPermission(username, false)){
            System.out.println("Permission update failed.");
            return;
        }
        System.out.println("Transfer permission revoked for user "+username);
    }

    public void deleteUser(String username){
        user currentUser=Session.getCurrentUser();
        if(currentUser==null){
            System.out.println("Please login first.");
            return;
        }

        if(!currentUser.isAdmin()){
            System.out.println("Only admins can delete users.");
            return;
        }
        if(username.equalsIgnoreCase("root")){
            System.out.println("Root user cannot be deleted.");
            return;
        }
        if(currentUser.getUsername().equals(username)){
            System.out.println("Cannot delete youself.");
            return;
        }
        boolean success=authManager.deleteUser(username);
        if(!success){
            System.out.println("User not found.");
            return;
        }
        System.out.println("User "+username+" deleted.");
    }

    public void register(String username, String password){
        if(Session.isLoggedIn()){
            System.out.println("Logout before registering a new user.");
            return;
        }

        boolean success=authManager.register(username, password);
        if(success){
            System.out.println("User registered successfully.");
        }
        else{
            System.out.println("User already exists.");
        }
    }
    public void login(String username, String password){
        if(Session.isLoggedIn()){
            System.out.println("Logout before logging in.");
            return;
        }
        user u=authManager.login(username, password);
        if(u==null){
            System.out.println("Invalid credentials."); 
            return;
        }
        
        Session.login(u);
        System.out.println("Login successful. Welcome "+username );

    }
    public void logout(){
        if(!Session.isLoggedIn()){
            System.out.println("No user is currently logged in.");
            return;
        }
        Session.logout();
        System.out.println("Logout Successfull.");
    }

    public void checkBalance(String name){
        user current=Session.getCurrentUser();

        if(current == null){
            System.out.println("Access denied: not logged in.");
            return;
        }
        
        //admin can only viewe any account balance
        if(current.isAdmin()){
            bank.checkBalance(name);
            return;
        }

        //user can only view their own account balance
        if(!current.getUsername().equals(name)){
            System.out.println("Access denied: not unauthorized access");
            return;
        }
        bank.checkBalance(name);
    }

    public void showTransactions(String name){
        user current=Session.getCurrentUser();

        if(current == null){
            System.out.println("Access denied: not logged in.");
            return;
        }

        if(current.isAdmin()){
            bank.printTransactionsFor(name);
            return;
        }

        if(!current.getUsername().equals(name)){
            System.out.println("Access denied: not unauthorized access");
            return;
        }
        bank.printTransactionsFor(name);
    }

    public boolean hasCommittedStorage(){
        return bank.hasCommittedAccounts();
    }

    public void createLoan(String type, String borrower, double amount, int durationYears){
        user current=Session.getCurrentUser();

        if(current==null){
            System.out.println("Access denied: not logged in.");
            return;
        }

        if(!current.isAdmin() && !current.getUsername().equals(borrower)){
            System.out.println("Access denied: Cannot create loan for other users.");
            return;
        }

        if(current.isAdmin() && authManager.getuser(borrower)==null && !bank.hasAccount(borrower)){
            System.out.println("Borrower not found.");
            return;
        }

        loanManager.createLoan(type, borrower, amount, durationYears);
    }

    public void showLoans(String borrower){
        user current=Session.getCurrentUser();

        if(current==null){
            System.out.println("Access denied: not logged in.");
            return;
        }

        if(!current.isAdmin() && !current.getUsername().equals(borrower)){
            System.out.println("Access denied: Cannot view other users' loans.");
            return;
        }

        loanManager.printLoansFor(borrower);
    }

    public void showLoanRates(){
        loanManager.printRates();
    }

    public void updateLoansNow(){
        user current=Session.getCurrentUser();

        if(current==null || !current.isAdmin()){
            System.out.println("Access denied: Only admins can run loan updates manually.");
            return;
        }
        loanManager.updateAccruedInterest(true);
    }

    public void shutdown(){
        loanManager.shutdown();
    }

    // =========================================================================
    // DISK SCHEDULING
    // =========================================================================

    /**
     * Change the disk scheduling algorithm at runtime.
     * @param name  "FCFS", "SSTF", or "SCAN" (case-insensitive)
     */
    public void setDiskAlgorithm(String name) {
        try {
            DiskScheduler.Algorithm algo =
                DiskScheduler.Algorithm.valueOf(name.toUpperCase());
            DiskScheduler.getInstance().setAlgorithm(algo);
        } catch (IllegalArgumentException e) {
            System.out.println("Unknown algorithm '" + name +
                "'. Choose: FCFS | SSTF | SCAN");
        }
    }

    /** Print cumulative disk scheduler statistics. */
    public void printDiskStats() {
        DiskScheduler.getInstance().printStats();
    }

    /** Reset disk scheduler statistics and head position. */
    public void resetDiskStats() {
        DiskScheduler.getInstance().resetStats();
    }

    // =========================================================================
    // DEADLOCK DEMO -- 3 REAL concurrent threads, real bank.transfer() calls
    // =========================================================================
    public void runDeadlockDemo(String acc1, String acc2, String acc3) {
        // Register the 3 accounts as resources in the REAL bankersAlgorithm
        bankersAlgorithm.registerResource(acc1);
        bankersAlgorithm.registerResource(acc2);
        bankersAlgorithm.registerResource(acc3);

        // Different amounts so net balances visibly change after demo
        double amt1 = 500.0; // T1: acc1 --> acc2  (large)
        double amt2 = 200.0; // T2: acc2 --> acc3  (medium)
        double amt3 = 100.0; // T3: acc3 --> acc1  (small)
        // Net: acc1 loses 400, acc2 gains 300, acc3 gains 100

        System.out.println();
        System.out.println("  ============================================================");
        System.out.println("       DEADLOCK DEMO -- 3 Real Concurrent Transfers");
        System.out.println("  ============================================================");
        System.out.println("  T1: " + acc1 + " --> " + acc2 + "  sends " + amt1);
        System.out.println("  T2: " + acc2 + " --> " + acc3 + "  sends " + amt2);
        System.out.println("  T3: " + acc3 + " --> " + acc1 + "  sends " + amt3);
        System.out.println("  All 3 starting simultaneously. Banker's decides who runs first.");
        System.out.println();

        String t1id = "T1[" + acc1 + "->" + acc2 + "]";
        String t2id = "T2[" + acc2 + "->" + acc3 + "]";
        String t3id = "T3[" + acc3 + "->" + acc1 + "]";

        // Thread 1: REAL transfer acc1 --> acc2
        Thread t1 = new Thread(() -> {
            try {
                Set<String> needs = new LinkedHashSet<>(Arrays.asList(acc1, acc2));
                System.out.println("  " + t1id + " requesting locks [" + acc1 + ", " + acc2 + "]...");
                while (!bankersAlgorithm.requestAndCheck(t1id, needs)) {
                    System.out.println("  " + t1id + " waiting... retrying.");
                    Thread.sleep(300);
                }
                // REAL bank.transfer() — balances actually change
                System.out.println("  " + t1id + " APPROVED. Calling bank.transfer(" + acc1 + ", " + acc2 + ", " + amt1 + ")");
                bank.transfer(acc1, acc2, amt1);
                System.out.println("  " + t1id + " DONE. Releasing locks.");
                bankersAlgorithm.release(t1id);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "T1-Thread");

        // Thread 2: REAL transfer acc2 --> acc3 (starts 300ms after T1)
        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(300); // small delay so T1 grabs resources first
                Set<String> needs = new LinkedHashSet<>(Arrays.asList(acc2, acc3));
                System.out.println("  " + t2id + " requesting locks [" + acc2 + ", " + acc3 + "]...");
                System.out.println("  " + t2id + " NOTE: T1 is running and holds [" + acc1 + ", " + acc2 + "]");
                while (!bankersAlgorithm.requestAndCheck(t2id, needs)) {
                    System.out.println("  " + t2id + " BLOCKED -- " + acc2 + " held by T1! Retrying...");
                    Thread.sleep(300);
                }
                // REAL bank.transfer()
                System.out.println("  " + t2id + " APPROVED. Calling bank.transfer(" + acc2 + ", " + acc3 + ", " + amt2 + ")");
                bank.transfer(acc2, acc3, amt2);
                System.out.println("  " + t2id + " DONE. Releasing locks.");
                bankersAlgorithm.release(t2id);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "T2-Thread");

        // Thread 3: REAL transfer acc3 --> acc1 (starts 600ms after T1)
        Thread t3 = new Thread(() -> {
            try {
                Thread.sleep(600);
                Set<String> needs = new LinkedHashSet<>(Arrays.asList(acc3, acc1));
                System.out.println("  " + t3id + " requesting locks [" + acc3 + ", " + acc1 + "]...");
                while (!bankersAlgorithm.requestAndCheck(t3id, needs)) {
                    System.out.println("  " + t3id + " BLOCKED -- resources held by others. Retrying...");
                    Thread.sleep(300);
                }
                // REAL bank.transfer()
                System.out.println("  " + t3id + " APPROVED. Calling bank.transfer(" + acc3 + ", " + acc1 + ", " + amt3 + ")");
                bank.transfer(acc3, acc1, amt3);
                System.out.println("  " + t3id + " DONE. Releasing locks.");
                bankersAlgorithm.release(t3id);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "T3-Thread");

        // Start all 3 threads simultaneously
        t1.start();
        t2.start();
        t3.start();

        // Wait for all to finish
        try {
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println();
        System.out.println("  ============================================================");
        System.out.println("  All 3 REAL transfers completed. ZERO deadlocks.");
        System.out.println("  Check balances of " + acc1 + ", " + acc2 + ", " + acc3 + " to confirm.");
        System.out.println("  Banker's Algorithm kept system in SAFE STATE throughout.");
        System.out.println("  ============================================================");
        System.out.println();
    }
}
