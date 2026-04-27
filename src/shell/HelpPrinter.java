package shell;

public class HelpPrinter {
    private static final String[][] COMMANDS = {
        {"help",          "help [command]",                                                          "Guest/User/Admin", "Show this table or details for one command."},
        {"exit",          "exit",                                                                    "Guest/User/Admin", "Shutdown MiniBankOS safely."},
        {"register",      "register <username> <password>",                                         "Guest",            "Create a new login user."},
        {"login",         "login <username> <password>",                                            "Guest",            "Start a user session."},
        {"logout",        "logout",                                                                  "User/Admin",       "End the current session."},
        {"create",        "create <account_name> <password> <opening_balance>",                     "Admin",            "Create account/login; repair missing login."},
        {"balance",       "balance <account_name>",                                                 "Owner/Admin",      "Show account balance."},
        {"deposit",       "deposit <account_name> <amount>",                                        "Owner/Admin",      "Deposit money into an account."},
        {"withdraw",      "withdraw <account_name> <amount>",                                       "Owner/Admin",      "Withdraw money from an account."},
        {"transfer",      "transfer <from_account> <to_account> <amount>",                          "Owner",            "Move money between accounts."},
        {"transactions",  "transactions <account_name>",                                            "Owner/Admin",      "Show committed transactions for an account."},
        {"history",       "history <account_name>",                                                 "Owner/Admin",      "Alias for transactions."},
        {"loan",          "loan <education|house|business> <borrower> <loan_amount> <duration_years>", "Owner/Admin",   "Create a loan with type-based yearly interest."},
        {"loans",         "loans <borrower>",                                                       "Owner/Admin",      "Show committed loans for a borrower."},
        {"loanrates",     "loanrates",                                                              "User/Admin",       "Show yearly interest rates for all loan types."},
        {"loanupdate",    "loanupdate",                                                             "Admin",            "Apply due monthly loan interest immediately."},
        {"grant",         "grant <username>",                                                       "Admin",            "Allow a user to transfer money."},
        {"revoke",        "revoke <username>",                                                      "Admin",            "Remove a user's transfer permission."},
        {"delete",        "delete <username>",                                                      "Admin",            "Delete a non-root user."},
        {"disk-schedule", "disk-schedule <FCFS|SSTF|SCAN>",                                        "Admin",            "Change the disk I/O scheduling algorithm."},
        {"disk-stats",    "disk-stats",                                                             "User/Admin",       "Show disk scheduler stats and access log."},
        {"disk-reset",    "disk-reset",                                                             "Admin",            "Reset disk head position and statistics."}
    };

    public static void printAll(){
        System.out.println("MiniBankOS shell commands");
        System.out.println("Usage style: command <required_value> [optional_value]");
        System.out.println();
        printLine();
        System.out.printf("| %-13s | %-74s | %-16s | %-48s |%n", "Command", "Usage", "Access", "Description");
        printLine();
        for(String[] command : COMMANDS){
            System.out.printf("| %-13s | %-74s | %-16s | %-48s |%n", command[0], command[1], command[2], command[3]);
        }
        printLine();
        System.out.println("Examples:");
        System.out.println("  login root root123");
        System.out.println("  create user2 pass123 10000");
        System.out.println("  transfer user2 user3 500");
        System.out.println("  loan education user2 12000 2");
        System.out.println("  loans user2");
        System.out.println("  disk-schedule SSTF");
        System.out.println("  disk-stats");
        System.out.println();
        System.out.println("Tip: run 'help <command>' for one command, for example: help loan");
    }

    public static void printCommand(String commandName){
        for(String[] command : COMMANDS){
            if(command[0].equalsIgnoreCase(commandName)){
                System.out.println(command[0]);
                System.out.println("  Usage:       "+command[1]);
                System.out.println("  Access:      "+command[2]);
                System.out.println("  Description: "+command[3]);
                return;
            }
        }
        System.out.println(commandName+" is not a known command.");
        System.out.println("Run 'help' to see all commands.");
    }

    public static void printUsage(String commandName){
        for(String[] command : COMMANDS){
            if(command[0].equalsIgnoreCase(commandName)){
                System.out.println("Usage: "+command[1]);
                return;
            }
        }
        System.out.println("Usage: help [command]");
    }

    private static void printLine(){
        System.out.println("+---------------+----------------------------------------------------------------------------+------------------+--------------------------------------------------+");
    }
}
