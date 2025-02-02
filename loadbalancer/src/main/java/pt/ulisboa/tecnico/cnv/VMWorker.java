package pt.ulisboa.tecnico.cnv;

import software.amazon.awssdk.services.ec2.model.Instance;

import java.time.Instant;

import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * Information about a worker running in a Virtual Machine, on EC2.
 */
public class VMWorker {
    public static final int FRESH_MACHINE_TIME = 2;
    private Instance instance;
    private volatile double cpuUsage = 0;
    private volatile long work = 0;
    private volatile int numRequests = 0;
    private volatile WorkerState state;
    private Instant startedRunningTime = null;

    public VMWorker(final Instance instance, WorkerState state) {
        this.instance = instance;
        this.state = state;
        if (state == WorkerState.RUNNING)
            this.startedRunningTime = Instant.now().minus(FRESH_MACHINE_TIME, MINUTES);
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
        this.state = WorkerState.TERMINATING;
    }

    public synchronized boolean isTerminating() {
        return this.state == WorkerState.TERMINATING;
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
        return this.cpuUsage;
    }

    public synchronized void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public synchronized long getWork() {
        return this.work;
    }

    public synchronized void setWork(long work) {
        this.work = work;
    }

    public synchronized boolean isRunning() {
        return this.state == WorkerState.RUNNING;
    }

    public synchronized boolean isInitializing() {
        return this.state == WorkerState.INITIALIZING;
    }

    public synchronized void setRunning() {
        this.state = WorkerState.RUNNING;
        this.startedRunningTime = Instant.now();
    }

    public boolean isFresh() {
        return startedRunningTime == null || Instant.now().isBefore(startedRunningTime.plus(FRESH_MACHINE_TIME, MINUTES));
    }

    public enum WorkerState {
        INITIALIZING,
        RUNNING,
        TERMINATING
    }
}
