package pt.ulisboa.tecnico.cnv;

import lombok.Data;
import software.amazon.awssdk.services.ec2.model.Instance;

/**
 * Information about a worker running in a Virtual Machine, on EC2.
 */
@Data
public class VMInstance {
    private Instance instance;
    private double cpuUsage = 0;
    private long work = 0;
    private int numRequests = 0;
    private boolean terminating = false;

    public VMInstance(final Instance instance) {
        this.instance = instance;
    }

    public synchronized void addWork(long work) {
        this.work += work;
    }

    public synchronized void removeWork(long work) {
        this.work -= work;
    }

    public Instance getInstance() {
        return instance;
    }

    public synchronized void setInstance(final Instance instance) {
        this.instance = instance;
    }

    public synchronized void markForTermination() {
        this.terminating = true;
    }

    public synchronized boolean isTerminating() {
        return this.terminating;
    }

    public synchronized void incrementRequests() {
        this.numRequests++;
    }

    public synchronized void decrementRequests() {
        this.numRequests--;
    }

    public synchronized int getNumRequests() {
        return this.numRequests;
    }
}
