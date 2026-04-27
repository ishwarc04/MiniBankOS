package kernel;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * A real disk I/O request queued for scheduling.
 *
 * <p>In MiniBankOS, each CSV file is mapped to a virtual cylinder number so
 * the disk scheduler can compute real seek distances and reorder requests
 * using FCFS / SSTF / SCAN — exactly as a real OS disk scheduler does.
 *
 * <p>Cylinder assignment (deterministic, based on file path hash):
 * <pre>
 *   user.csv         → cylinder  10
 *   auth.csv         → cylinder  30
 *   transactions.csv → cylinder  50
 *   transaction.log  → cylinder  70
 *   house_loans.csv  → cylinder  90
 *   education_loans  → cylinder 110
 *   business_loans   → cylinder 130
 *   (unknown)        → abs(hashCode) % 200
 * </pre>
 */
public class DiskRequest {

    public enum Type { READ, WRITE }

    /** Callable that performs the actual File I/O. */
    private final Callable<Object>  ioTask;

    /** Human-readable label shown in scheduler output. */
    public final String             label;

    /** Virtual cylinder this request targets. */
    public final int                cylinder;

    /** READ or WRITE. */
    public final Type               type;

    /** Filled in by the scheduler after execution. */
    volatile Object                 result;
    volatile Exception              error;

    // ─────────────────────────────────────────────────────────────────────────

    public DiskRequest(File file, Type type, Callable<Object> ioTask) {
        this.ioTask   = ioTask;
        this.type     = type;
        this.label    = file.getName() + " [" + type + "]";
        this.cylinder = cylinderFor(file);
    }

    /** Execute the underlying I/O task and store the result. */
    public void execute() {
        try {
            result = ioTask.call();
        } catch (Exception e) {
            error  = e;
            result = null;
        }
    }

    /** True if the I/O task threw an exception. */
    public boolean hasFailed() {
        return error != null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Virtual cylinder mapping — deterministic per file name
    // ─────────────────────────────────────────────────────────────────────────

    public static int cylinderFor(File file) {
        String name = file.getName().toLowerCase();
        switch (name) {
            case "user.csv":             return  10;
            case "auth.csv":             return  30;
            case "transactions.csv":     return  50;
            case "transaction.log":      return  70;
            case "house_loans.csv":      return  90;
            case "education_loans.csv":  return 110;
            case "business_loans.csv":   return 130;
            default:
                return Math.abs(name.hashCode()) % 200;
        }
    }

    @Override
    public String toString() {
        return label + " @ cylinder " + cylinder;
    }
}
