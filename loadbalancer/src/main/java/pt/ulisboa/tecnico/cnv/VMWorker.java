package pt.ulisboa.tecnico.cnv;

import software.amazon.awssdk.services.ec2.model.Instance;

/**
 * Information about a worker running in a Virtual Machine, on EC2.
 */
public class VMWorker {
    private Instance instance;
    private double cpuUsage = 0;
    private long work = 0;
    private int numRequests = 0;
    private boolean terminating = false;
    private boolean initialized = false;

    public VMWorker(final Instance instance) {
        this.instance = instance;
    }

    public synchronized void addWork(long work) {
        this.work += work;
    }

    public synchronized void removeWork(long work) {
        this.work -= work;
    }

    public synchronized Instance getInstance() {
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

    public synchronized double getCpuUsage() {
        return cpuUsage;
    }

    public synchronized void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public synchronized long getWork() {
        return work;
    }

    public synchronized void setWork(long work) {
        this.work = work;
    }

    public synchronized boolean isInitialized() {
        return initialized;
    }

    public synchronized void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
}
