package pt.ulisboa.tecnico.cnv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Monitors the workers running in the Virtual Machines, including their work and requests.
 */
public class VMWorkersMonitor {
    private final Map<String, VMWorker> vmWorkers = new ConcurrentHashMap<>();
    private long totalWork = 0;
    private ReentrantLock lock = new ReentrantLock();

    public VMWorkersMonitor() {
        super();
    }


    public Map<String, VMWorker> getVmWorkers() {
        return vmWorkers;
    }

    public Boolean anyVmWorkerInitializing() {
        return vmWorkers.values().stream().anyMatch(VMWorker::isInitializing);
    }

    public long getTotalWork() {
        return totalWork;
    }

    public void addWork(final long work) {
        this.totalWork += work;
    }

    public void removeWork(final long work) {
        this.totalWork -= work;
    }

    public void addWork(final VMWorker instance, final long requestComplexity) {
        instance.addWork(requestComplexity);
        instance.incrementRequests();
        addWork(requestComplexity);
    }

    public void removeWork(final VMWorker instance, final long requestComplexity) {
        instance.removeWork(requestComplexity);
        instance.decrementRequests();
        removeWork(requestComplexity);
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }
}
