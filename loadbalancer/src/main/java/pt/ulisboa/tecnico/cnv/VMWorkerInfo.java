package pt.ulisboa.tecnico.cnv;

import lombok.Data;
import software.amazon.awssdk.services.ec2.model.Instance;

/**
 * Information about a worker running in a Virtual Machine, on EC2.
 */
@Data
public class VMWorkerInfo {
    private final Instance instance;
    private double cpuUsage = 0;
    private long work = 0;

    public VMWorkerInfo(final Instance instance) {
        this.instance = instance;
    }

    public void addWork(long work) {
        this.work += work;
    }

    public void removeWork(long work) {
        this.work -= work;
    }
}
