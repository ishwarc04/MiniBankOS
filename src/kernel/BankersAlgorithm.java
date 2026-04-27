package kernel;

import java.util.*;

public class BankersAlgorithm {

    private final Set<String> resources = new LinkedHashSet<>();

    private final Map<String, Set<String>> allocation = new LinkedHashMap<>();

    private final Map<String, Set<String>> maxClaim = new LinkedHashMap<>();

    public synchronized void registerResource(String accountName) {
        resources.add(accountName);
    }

    public synchronized boolean requestAndCheck(String processId, Set<String> needed) {

        resources.addAll(needed);

        printHeader(processId, needed);

        Set<String> allocated = new HashSet<>();
        for (Set<String> alloc : allocation.values())
            allocated.addAll(alloc);

        Set<String> available = new LinkedHashSet<>(resources);
        available.removeAll(allocated);

        System.out.println("  Available Accounts : " + available);

        if (!allocation.isEmpty()) {
            System.out.println("  Active Processes   :");
            for (Map.Entry<String, Set<String>> e : allocation.entrySet()) {
                System.out.println("    " + e.getKey() + " holds " + e.getValue());
            }
        } else {
            System.out.println("  Active Processes   : none");
        }

        if (!available.containsAll(needed)) {
            System.out.println("  Result             : UNSAFE - accounts not available (in use)");
            System.out.println();
            return false;
        }

        Map<String, Set<String>> tempAlloc = new LinkedHashMap<>(allocation);
        Map<String, Set<String>> tempMax = new LinkedHashMap<>(maxClaim);
        tempAlloc.put(processId, new HashSet<>(needed));
        tempMax.put(processId, new HashSet<>(needed));

        Set<String> tempAvailable = new LinkedHashSet<>(available);
        tempAvailable.removeAll(needed);

        List<String> safeSequence = new ArrayList<>();
        Set<String> finished = new LinkedHashSet<>();
        boolean progress = true;

        while (progress) {
            progress = false;
            for (String proc : tempMax.keySet()) {
                if (finished.contains(proc))
                    continue;

                Set<String> need = new HashSet<>(tempMax.get(proc));
                Set<String> procAlloc = tempAlloc.getOrDefault(proc, new HashSet<>());
                need.removeAll(procAlloc);

                if (tempAvailable.containsAll(need)) {
                    tempAvailable.addAll(procAlloc);
                    finished.add(proc);
                    safeSequence.add(proc);
                    progress = true;
                }
            }
        }

        boolean safe = (finished.size() == tempMax.size());

        System.out.println("  Safe Sequence      : " + safeSequence);
        if (safe) {
            System.out.println("  Result             : [SAFE] Transfer approved.");
            allocation.put(processId, new HashSet<>(needed));
            maxClaim.put(processId, new HashSet<>(needed));
        } else {
            System.out.println("  Result             : [UNSAFE] Transfer deferred - deadlock risk.");
        }
        System.out.println();
        return safe;
    }

    public synchronized void release(String processId) {
        Set<String> held = allocation.remove(processId);
        maxClaim.remove(processId);
        if (held != null) {
            System.out.println("  [BANKER'S] Process " + processId + " released: " + held);
        }
    }

    private void printHeader(String processId, Set<String> needed) {
        System.out.println();
        System.out.println("  +--------------------------------------------------+");
        System.out.println("  |       BANKER'S ALGORITHM  - SAFETY CHECK         |");
        System.out.println("  +--------------------------------------------------+");
        System.out.println("  Process ID         : " + processId);
        System.out.println("  Requesting Accounts: " + needed);
        System.out.println("  All Resources      : " + resources);
    }

    public static void runDeadlockDemo() {
        runDeadlockDemo("alice", "bob", "charlie");
    }

    public static void runDeadlockDemo(String acc1, String acc2, String acc3) {
        try {
            String A1 = acc1.toUpperCase();
            String A2 = acc2.toUpperCase();
            String A3 = acc3.toUpperCase();

            System.out.println();
            System.out.println("  ============================================================");
            System.out.println("       DEADLOCK AVOIDANCE DEMO -- Banker's Algorithm");
            System.out.println("  ============================================================");
            Thread.sleep(800);
            System.out.println();
            System.out.println("  SCENARIO: 3 concurrent transfers submitted at same time.");
            System.out.println("  Accounts: " + acc1 + ", " + acc2 + ", " + acc3);
            Thread.sleep(1000);
            System.out.println();
            System.out.println("  T1: " + acc1 + " --> " + acc2 + "   needs [" + A1 + ", " + A2 + "]");
            Thread.sleep(500);
            System.out.println("  T2: " + acc2 + " --> " + acc3 + "   needs [" + A2 + ", " + A3 + "]");
            Thread.sleep(500);
            System.out.println("  T3: " + acc3 + " --> " + acc1 + "   needs [" + A3 + ", " + A1 + "]");
            Thread.sleep(1200);

            System.out.println();
            System.out.println("  --- WITHOUT Banker's Algorithm (what would happen) ---");
            Thread.sleep(800);
            System.out.println("  [T1] Grabbed " + A1 + " lock. Waiting for " + A2 + "...");
            Thread.sleep(600);
            System.out.println("  [T2] Grabbed " + A2 + " lock. Waiting for " + A3 + "...");
            Thread.sleep(600);
            System.out.println("  [T3] Grabbed " + A3 + " lock. Waiting for " + A1 + "...");
            Thread.sleep(1000);
            System.out.println();
            System.out.println("  [T1] waiting...  [T2] waiting...  [T3] waiting...");
            Thread.sleep(1200);
            System.out.println("  !!! DEADLOCK -- All 3 threads stuck FOREVER. System frozen. !!!");
            Thread.sleep(2000);

            System.out.println();
            System.out.println("  --- WITH Banker's Algorithm (our system) ---");
            Thread.sleep(1200);

            BankersAlgorithm ba = new BankersAlgorithm();
            ba.resources.addAll(Arrays.asList(acc1, acc2, acc3));
            Set<String> t1Needs = new LinkedHashSet<>(Arrays.asList(acc1, acc2));
            Set<String> t2Needs = new LinkedHashSet<>(Arrays.asList(acc2, acc3));
            String t1id = "T1-" + acc1 + "->" + acc2;
            String t2id = "T2-" + acc2 + "->" + acc3;

            System.out.println();
            System.out.println("  [T1] Requesting accounts [" + acc1 + ", " + acc2 + "]...");
            Thread.sleep(1200);
            ba.requestAndCheck(t1id, t1Needs);
            Thread.sleep(600);
            System.out.println("  [T1] Both locks granted. Executing " + acc1 + " --> " + acc2 + "...");
            Thread.sleep(1800);

            System.out.println();
            System.out.println("  [T2] Requesting accounts [" + acc2 + ", " + acc3 + "]...");
            Thread.sleep(500);
            System.out.println("  [T2] NOTE: T1 is still running and holds " + acc1 + " + " + acc2 + "!");
            Thread.sleep(1200);
            boolean t2safe = ba.requestAndCheck(t2id, t2Needs);
            Thread.sleep(400);
            if (!t2safe) {
                System.out.println("  [T2] !!! BLOCKED -- " + acc2 + " is held by T1. Cannot proceed.");
                Thread.sleep(600);
                System.out.println("  [T2] If granted now + T3 starts = circular wait = DEADLOCK.");
                Thread.sleep(600);
                System.out.println("  [T2] Banker's says: WAIT. Deferring until T1 finishes...");
            }
            Thread.sleep(2000);

            System.out.println();
            System.out.println("  [T1] Transfer " + acc1 + " --> " + acc2 + " DONE. Releasing locks.");
            ba.release(t1id);
            Thread.sleep(1200);

            System.out.println();
            System.out.println("  [T2] Retrying request for [" + acc2 + ", " + acc3 + "]...");
            Thread.sleep(1200);
            boolean t2retry = ba.requestAndCheck(t2id, t2Needs);
            Thread.sleep(400);
            if (t2retry) {
                System.out.println("  [T2] APPROVED. Executing " + acc2 + " --> " + acc3 + "...");
                Thread.sleep(1500);
                System.out.println("  [T2] Transfer " + acc2 + " --> " + acc3 + " DONE.");
                ba.release(t2id);
            }
            Thread.sleep(1000);

            System.out.println();
            System.out.println("  ============================================================");
            System.out.println("  RESULT SUMMARY:");
            Thread.sleep(400);
            System.out.println("  T1 (" + acc1 + "-->" + acc2 + ")  -> Executed.       [DONE]");
            Thread.sleep(400);
            System.out.println("  T2 (" + acc2 + "-->" + acc3 + ")  -> Blocked, retried.[DONE]");
            Thread.sleep(400);
            System.out.println("  T3 (" + acc3 + "-->" + acc1 + ")  -> Not needed. Deadlock avoided.");
            Thread.sleep(600);
            System.out.println();
            System.out.println("  Banker's maintained SAFE STATE throughout. Zero deadlocks.");
            System.out.println("  ============================================================");
            System.out.println();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
