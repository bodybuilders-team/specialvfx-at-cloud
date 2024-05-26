package pt.ulisboa.tecnico.cnv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestsMonitor {
    private final Map<String, VMInstance> instances = new ConcurrentHashMap<>();
    private long totalWork = 0;

    public RequestsMonitor() {
        super();
    }


    public Map<String, VMInstance> getInstances() {
        return instances;
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

    public void addWork(final VMInstance instance, final long requestComplexity) {
        instance.addWork(requestComplexity);
        instance.incrementRequests();
        addWork(requestComplexity);
    }

    public void removeWork(final VMInstance instance, final long requestComplexity) {
        instance.removeWork(requestComplexity);
        instance.decrementRequests();
        removeWork(requestComplexity);
    }
}
