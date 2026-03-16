package kernel;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteManager {
    private ConcurrentHashMap<String, ReentrantReadWriteLock> locks= new ConcurrentHashMap<>();

    public ReentrantReadWriteLock getLock(String accountName){
        locks.putIfAbsent(accountName, new ReentrantReadWriteLock());

        return locks.get(accountName);
    }
}
