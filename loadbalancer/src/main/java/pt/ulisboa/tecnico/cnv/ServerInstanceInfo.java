package pt.ulisboa.tecnico.cnv;

import software.amazon.awssdk.services.ec2.model.Instance;

public class ServerInstanceInfo {
    private final Instance instance;
    private double cpuUsage = 0;
    private long work = 0;

    public ServerInstanceInfo(final Instance instance) {
        this.instance = instance;
    }

    public void addWork(long work) {
        this.work += work;
    }

    public void removeWork(long work) {
        this.work -= work;
    }

    public long getWork() {
        return this.work;
    }

    public double getCpuUsage() {
        return this.cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Instance getInstance() {
        return this.instance;
    }
}
