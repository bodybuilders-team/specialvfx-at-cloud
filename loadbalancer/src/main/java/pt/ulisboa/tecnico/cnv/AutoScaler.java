package pt.ulisboa.tecnico.cnv;

import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.cnv.utils.AwsUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;

import java.util.Comparator;
import java.util.logging.Logger;

public class AutoScaler {
    public static final long DEFAULT_WORK_RAYTRACER = 500000000;
    public static final long DEFAULT_WORK_IMAGE_PROCESSOR = 430000000;
    public static final long COMPLEX_REQUEST_THRESHOLD = DEFAULT_WORK_RAYTRACER * 2;
    public static final long MAX_WORKLOAD = DEFAULT_WORK_RAYTRACER * 5;
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AutoScaler.class);

    private final VMWorkersMonitor vmWorkersMonitor;

    private final Region region = Region.EU_WEST_3;
    private final CloudWatchClient cloudWatchClient = CloudWatchClient.builder().region(region).build();
    private final Ec2Client ec2Client = Ec2Client.builder().region(region).build();

    private final Logger logger = Logger.getLogger(AutoScaler.class.getName());


    public AutoScaler(final VMWorkersMonitor instances) {
        this.vmWorkersMonitor = instances;
    }

    /**
     * Starts the auto scaler, which will monitor the server load and scale up or down the number of instances.
     * The auto scaler will also terminate instances that are marked for termination and have no requests.
     */
    public void start() {
        logger.info("Starting auto scaler");
        new Thread(() -> {
            try {
                while (true) {
                    monitorInstances();
                    terminateMarkedInstances();
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

    /**
     * Monitors the instances and scales up or down the number of instances if needed.
     */
    private void monitorInstances() {
        final var instances = vmWorkersMonitor.getVmWorkers();
        logger.info("Monitoring " + instances.size() + " instances");
        // Updates the CPU usage of the instances
        AwsUtils.getCpuUsage(cloudWatchClient, instances.keySet().stream().toList())
                .forEach((instanceId, cpuUsage) -> instances.get(instanceId).setCpuUsage(cpuUsage));

        final var averageWork = instances.values().stream()
                .filter(vm -> !vm.isTerminating() && !vm.isInitialized())
                .mapToDouble(VMWorker::getWork)
                .average()
                .orElse(0);

        final var averageCpuUsage = instances.values().stream()
                .filter(vm -> !vm.isTerminating() && !vm.isInitialized())
                .mapToDouble(VMWorker::getCpuUsage)
                .average()
                .orElse(0);

        logger.info("Average work: " + averageWork + " Average CPU usage: " + averageCpuUsage + " Instances: " + instances.size());

        // Check if we need to scale up, down or do nothing
        if (averageWork > 0.7 * MAX_WORKLOAD) {
            Instance instance;
            VMWorker vmWorker;

            synchronized (vmWorkersMonitor) {
                instance = AwsUtils.launchInstance(ec2Client);
                vmWorker = new VMWorker(instance);
                vmWorkersMonitor.getVmWorkers().put(instance.instanceId(), vmWorker);
            }

            instance = AwsUtils.waitForInstance(ec2Client, instance.instanceId());
            vmWorker.setInstance(instance);
            vmWorker.setInitialized(true);
        } else if (averageWork < 0.3 * MAX_WORKLOAD && instances.size() > 1) {
            instances.values().stream()
                    .filter(vm -> !vm.isTerminating() && !vm.isInitialized())
                    .min(Comparator.comparingDouble(VMWorker::getCpuUsage))
                    .ifPresent(VMWorker::markForTermination);
        }
    }

    /**
     * Terminates instances that are marked for termination and has no requests.
     */
    private void terminateMarkedInstances() {
        for (var instance : vmWorkersMonitor.getVmWorkers().values()) {
            if (instance.isTerminating() && instance.getNumRequests() == 0) {
                vmWorkersMonitor.getVmWorkers().remove(instance.getInstance().instanceId());
                AwsUtils.deleteInstance(ec2Client, instance.getInstance().instanceId());
            }
        }
    }
}
