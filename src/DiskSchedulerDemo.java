import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * ============================================================
 *  MiniBankOS - Disk Scheduling Algorithm Demo  (DYNAMIC)
 *
 *  Reads REAL data from the live database CSVs:
 *    data/user.csv          -> cylinder  10
 *    data/auth.csv          -> cylinder  30
 *    data/transactions.csv  -> cylinder  50
 *    data/transaction.log   -> cylinder  70
 *    data/house_loans.csv   -> cylinder  90
 *    data/education_loans.csv -> cylinder 110
 *    data/business_loans.csv  -> cylinder 130
 *
 *  For every COMMITTED transaction in transactions.csv the demo
 *  reconstructs the exact disk I/O sequence that BankDatabase
 *  would have generated (READ user.csv -> WRITE user.csv ->
 *  WRITE transactions.csv), then runs FCFS / SSTF / SCAN on
 *  that real request list.
 *
 *  Compile :  javac DiskSchedulerDemo.java
 *  Run     :  java  DiskSchedulerDemo
 * ============================================================
 */
public class DiskSchedulerDemo {

    // -- Virtual cylinder map (mirrors DiskRequest.java exactly) --------------
    static final Map<String, Integer> CYLINDER_MAP = new LinkedHashMap<>();
    static {
        CYLINDER_MAP.put("user.csv",             10);
        CYLINDER_MAP.put("auth.csv",             30);
        CYLINDER_MAP.put("transactions.csv",     50);
        CYLINDER_MAP.put("transaction.log",      70);
        CYLINDER_MAP.put("house_loans.csv",      90);
        CYLINDER_MAP.put("education_loans.csv", 110);
        CYLINDER_MAP.put("business_loans.csv",  130);
    }

    // -- A disk I/O request reconstructed from real transaction data -----------
    static class IORequest {
        final String file;
        final int    cylinder;
        final String op;       // READ / WRITE
        final String reason;   // e.g. "TRANSFER alice->bob $500"

        IORequest(String file, String op, String reason) {
            this.file     = file;
            this.op       = op;
            this.reason   = reason;
            this.cylinder = CYLINDER_MAP.getOrDefault(file,
                                Math.abs(file.hashCode()) % 200);
        }

        @Override public String toString() {
            return String.format("%-25s cyl=%3d  [%-5s]  <- %s",
                                 file, cylinder, op, reason);
        }
    }

    // -- Simple account snapshot read from user.csv ----------------------------
    static class AccountSnapshot {
        final String name;
        final double balance;
        AccountSnapshot(String name, double balance) {
            this.name = name; this.balance = balance;
        }
    }

    // -- Raw transaction row from transactions.csv -----------------------------
    static class TxRow {
        final String id, type, from, to, amount, state;
        TxRow(String[] cols) {
            id     = cols.length > 0 ? cols[0] : "";
            type   = cols.length > 1 ? cols[1] : "";
            from   = cols.length > 2 ? cols[2] : "";
            to     = cols.length > 3 ? cols[3] : "";
            amount = cols.length > 4 ? cols[4] : "";
            state  = cols.length > 5 ? cols[5] : "";
        }
        boolean isCommitted() { return "COMMIT".equalsIgnoreCase(state); }
    }

    // =========================================================================
    //  MAIN
    // =========================================================================
    public static void main(String[] args) {

        File dataDir = findDataDir();

        // -- 1. Load real data from CSVs ---------------------------------------
        List<AccountSnapshot> accounts  = loadAccounts(dataDir);
        List<TxRow>           txHistory = loadTransactions(dataDir);

        if (txHistory.isEmpty()) {
            System.out.println("\n  [!] No committed transactions found in data/transactions.csv");
            System.out.println("      Run the MiniBankOS shell first to create some transactions.");
            System.out.println("      Example: create alice 5000 | deposit alice 200 | transfer alice bob 100\n");
            return;
        }

        // -- 2. Reconstruct the disk I/O request queue from real transactions --
        //
        //  BankDatabase I/O per operation:
        //    CREATE   -> READ user.csv, WRITE user.csv, WRITE transactions.csv
        //    DEPOSIT  -> READ user.csv, WRITE user.csv, WRITE transactions.csv
        //    WITHDRAW -> READ user.csv, WRITE user.csv, WRITE transactions.csv
        //    TRANSFER -> READ user.csv (x2 accounts), WRITE user.csv (x2), WRITE transactions.csv
        //
        List<IORequest> requests = new ArrayList<>();
        for (TxRow tx : txHistory) {
            if (!tx.isCommitted()) continue;
            String label = tx.type + " " + tx.from + "->" + tx.to + " $" + tx.amount;

            switch (tx.type.toUpperCase()) {
                case "CREATE":
                    requests.add(new IORequest("user.csv",         "READ",  label));
                    requests.add(new IORequest("user.csv",         "WRITE", label));
                    requests.add(new IORequest("transactions.csv", "WRITE", label));
                    break;
                case "DEPOSIT":
                    requests.add(new IORequest("user.csv",         "READ",  label));
                    requests.add(new IORequest("user.csv",         "WRITE", label));
                    requests.add(new IORequest("transactions.csv", "WRITE", label));
                    break;
                case "WITHDRAW":
                    requests.add(new IORequest("user.csv",         "READ",  label));
                    requests.add(new IORequest("user.csv",         "WRITE", label));
                    requests.add(new IORequest("transactions.csv", "WRITE", label));
                    break;
                case "TRANSFER":
                    // Two account reads + two account writes + one tx append
                    requests.add(new IORequest("user.csv",         "READ",  label + " (sender)"));
                    requests.add(new IORequest("user.csv",         "READ",  label + " (receiver)"));
                    requests.add(new IORequest("user.csv",         "WRITE", label + " (sender)"));
                    requests.add(new IORequest("user.csv",         "WRITE", label + " (receiver)"));
                    requests.add(new IORequest("transactions.csv", "WRITE", label));
                    break;
                default:
                    requests.add(new IORequest("transactions.csv", "READ", label));
            }
        }

        // -- 3. Print real database snapshot -----------------------------------
        System.out.println();
        System.out.println("+==============================================================+");
        System.out.println("|     MiniBankOS - Disk Scheduling Demo  [DYNAMIC / LIVE]      |");
        System.out.println("+==============================================================+");

        System.out.println("\n  +-- Live Account Balances (from data/user.csv) --------------+");
        if (accounts.isEmpty()) {
            System.out.println("  |  (no accounts found)                                       |");
        } else {
            for (AccountSnapshot a : accounts) {
                System.out.printf("  |  %-15s  Balance = %-10.2f                   |%n",
                                  a.name, a.balance);
            }
        }
        System.out.println("  +------------------------------------------------------------+");

        System.out.println("\n  +-- Committed Transactions (from data/transactions.csv) -----+");
        for (TxRow tx : txHistory) {
            if (!tx.isCommitted()) continue;
            System.out.printf("  |  %-8s  from=%-10s to=%-12s  amt=%-8s|%n",
                              tx.type, tx.from, tx.to, tx.amount);
        }
        System.out.println("  +------------------------------------------------------------+");

        printCylinderMap();

        System.out.printf("%n  >  %d disk I/O requests reconstructed from %d committed transactions%n",
                          requests.size(), txHistory.size());
        System.out.println("  >  Head starts at cylinder 0 (system boot position)");
        System.out.println("\n  Reconstructed I/O queue (arrival order):");
        for (int i = 0; i < requests.size(); i++) {
            System.out.printf("    [%2d] %s%n", i + 1, requests.get(i));
        }

        int startHead = 0;  // head at boot = cylinder 0

        // -- 4. Run all three algorithms on real data ---------------------------
        long fcfsDist = runFCFS(new ArrayList<>(requests), startHead);
        long sstfDist = runSSTF(new ArrayList<>(requests), startHead);
        long scanDist = runSCAN(new ArrayList<>(requests), startHead, true);

        // -- 5. Comparison summary ----------------------------------------------
        System.out.println();
        System.out.println("+==============================================================+");
        System.out.println("|              COMPARISON SUMMARY  (on LIVE data)              |");
        System.out.println("+==============================================================+");
        System.out.printf ("|  Requests processed  : %-38d|%n", requests.size());
        System.out.printf ("|  Transactions in DB  : %-38d|%n", txHistory.size());
        System.out.printf ("|  Accounts in DB      : %-38d|%n", accounts.size());
        System.out.println("+==============================================================+");
        System.out.printf ("|  FCFS  -> Total Seek = %-5d  (no optimization)             |%n", fcfsDist);
        System.out.printf ("|  SSTF  -> Total Seek = %-5d  (fastest, may starve)         |%n", sstfDist);
        System.out.printf ("|  SCAN  -> Total Seek = %-5d  (fair, elevator style)        |%n", scanDist);
        System.out.println("+==============================================================+");

        long best = Math.min(fcfsDist, Math.min(sstfDist, scanDist));
        String winner = fcfsDist == best ? "FCFS" : sstfDist == best ? "SSTF" : "SCAN";
        long saved    = Math.max(fcfsDist, Math.max(sstfDist, scanDist)) - best;
        System.out.printf ("|  *  Best algorithm on this data : %-25s  |%n", winner);
        System.out.printf ("|  *  Cylinders saved vs worst    : %-25d  |%n", saved);
        System.out.println("+==============================================================+");
        System.out.println("|  MiniBankOS default : SSTF                                   |");
        System.out.println("|  Switch in shell    : disk-algo fcfs / sstf / scan           |");
        System.out.println("|  View live stats    : disk-stats                             |");
        System.out.println("+==============================================================+");
        System.out.println();
    }

    // =========================================================================
    //  FCFS - First Come First Served
    // =========================================================================
    static long runFCFS(List<IORequest> requests, int head) {
        printHeader("FCFS - First Come First Served  [arrival order, no reordering]", head);
        long total = 0;
        for (IORequest r : requests) {
            int seek = Math.abs(r.cylinder - head);
            total   += seek;
            printStep(head, r, seek, total);
            head = r.cylinder;
        }
        printFooter(total);
        return total;
    }

    // =========================================================================
    //  SSTF - Shortest Seek Time First
    // =========================================================================
    static long runSSTF(List<IORequest> requests, int head) {
        printHeader("SSTF - Shortest Seek Time First  [greedy nearest cylinder]", head);
        List<IORequest> remaining = new ArrayList<>(requests);
        long total = 0;
        while (!remaining.isEmpty()) {
            final int cur = head;
            IORequest nearest = remaining.stream()
                .min(Comparator.comparingInt(r -> Math.abs(r.cylinder - cur)))
                .orElseThrow();
            int seek = Math.abs(nearest.cylinder - head);
            total   += seek;
            printStep(head, nearest, seek, total);
            head = nearest.cylinder;
            remaining.remove(nearest);
        }
        printFooter(total);
        return total;
    }

    // =========================================================================
    //  SCAN - Elevator Algorithm
    // =========================================================================
    static long runSCAN(List<IORequest> requests, int head, boolean dirUp) {
        printHeader("SCAN - Elevator  [direction: " + (dirUp ? "UP" : "DOWN") + "]", head);
        List<IORequest> sorted = new ArrayList<>(requests);
        sorted.sort(Comparator.comparingInt(r -> r.cylinder));

        List<IORequest> ordered = new ArrayList<>();
        if (dirUp) {
            for (IORequest r : sorted) if (r.cylinder >= head) ordered.add(r);
            List<IORequest> lower = new ArrayList<>();
            for (IORequest r : sorted) if (r.cylinder <  head) lower.add(r);
            for (int i = lower.size() - 1; i >= 0; i--) ordered.add(lower.get(i));
        } else {
            for (int i = sorted.size() - 1; i >= 0; i--)
                if (sorted.get(i).cylinder <= head) ordered.add(sorted.get(i));
            for (IORequest r : sorted) if (r.cylinder > head) ordered.add(r);
        }

        long total = 0;
        for (IORequest r : ordered) {
            int seek = Math.abs(r.cylinder - head);
            total   += seek;
            printStep(head, r, seek, total);
            head = r.cylinder;
        }
        printFooter(total);
        return total;
    }

    // =========================================================================
    //  CSV Readers - read the actual live files
    // =========================================================================

    static List<AccountSnapshot> loadAccounts(File dataDir) {
        List<AccountSnapshot> list = new ArrayList<>();
        File f = new File(dataDir, "user.csv");
        if (!f.exists()) return list;
        try {
            List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                String[] cols = line.split(",", -1);
                if (cols.length >= 3 && "COMMIT".equalsIgnoreCase(cols[2].trim())) {
                    try {
                        list.add(new AccountSnapshot(
                            cols[0].trim(),
                            Double.parseDouble(cols[1].trim())));
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException e) {
            System.out.println("  [!] Could not read user.csv: " + e.getMessage());
        }
        return list;
    }

    static List<TxRow> loadTransactions(File dataDir) {
        List<TxRow> list = new ArrayList<>();
        File f = new File(dataDir, "transactions.csv");
        if (!f.exists()) return list;
        try {
            List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                String[] cols = line.split(",", -1);
                if (cols.length >= 6) {
                    TxRow tx = new TxRow(cols);
                    if (tx.isCommitted()) list.add(tx);
                }
            }
        } catch (IOException e) {
            System.out.println("  [!] Could not read transactions.csv: " + e.getMessage());
        }
        return list;
    }

    // -- Find data directory (mirrors PageTable.findDataDirectory()) -----------
    static File findDataDir() {
        File src = new File("src" + File.separator + "data");
        if (src.exists()) return src;
        File d = new File("data");
        if (d.exists()) return d;
        return src; // will fail gracefully in loaders
    }

    // =========================================================================
    //  Print helpers
    // =========================================================================

    static void printHeader(String title, int head) {
        System.out.println();
        System.out.println("  +--------------------------------------------------------------+");
        System.out.printf ("  |  %-60s|%n", title);
        System.out.printf ("  |  Head starts at cylinder %-36d|%n", head);
        System.out.println("  +--------+------------------------------------+--------+-------+");
        System.out.println("  |  From  |  Request                           |  Seek  | Total |");
        System.out.println("  +--------+------------------------------------+--------+-------+");
    }

    static void printStep(int from, IORequest r, int seek, long total) {
        String label = r.file + " (" + r.cylinder + ") [" + r.op + "]";
        System.out.printf("  |  %4d  |  %-34s  |  %4d  | %5d |%n",
                          from, label, seek, total);
    }

    static void printFooter(long total) {
        System.out.println("  +--------+------------------------------------+--------+-------+");
        System.out.printf ("  |  *  Total Seek Distance = %-35d|%n", total);
        System.out.println("  +--------------------------------------------------------------+");
    }

    static void printCylinderMap() {
        System.out.println();
        System.out.println("  +-- Virtual Disk Layout (MiniBankOS) ------------------------+");
        for (Map.Entry<String, Integer> e : CYLINDER_MAP.entrySet()) {
            System.out.printf("  |  %-25s -> cylinder %3d                   |%n",
                              e.getKey(), e.getValue());
        }
        System.out.println("  +------------------------------------------------------------+");
    }
}
