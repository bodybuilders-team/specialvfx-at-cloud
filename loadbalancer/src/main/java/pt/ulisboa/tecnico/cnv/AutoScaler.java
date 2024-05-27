package pt.ulisboa.tecnico.cnv;

import pt.ulisboa.tecnico.cnv.utils.AwsUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;

import java.util.Comparator;
import java.util.Map;
import java.util.logging.Logger;

public class AutoScaler {
    public static final long DEFAULT_WORK_RAYTRACER = 500000000;
    public static final long DEFAULT_WORK_IMAGE_PROCESSOR = 430000000;
    public static final long COMPLEX_REQUEST_THRESHOLD = DEFAULT_WORK_RAYTRACER * 2;
    public static final long MAX_WORKLOAD = DEFAULT_WORK_RAYTRACER * 5;
    public static final double WORKERS_UNDERLOADED_THRESHOLD = 0.3 * MAX_WORKLOAD;
    public static final double WORKERS_OVERLOADED_THRESHOLD = 0.7 * MAX_WORKLOAD;

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
                    vmWorkersMonitor.lock();
                    monitorInstances();
                    terminateMarkedInstances();
                    vmWorkersMonitor.unlock();
                    Thread.sleep(5000);
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
        updateInstances(instances);

        // Updates the CPU usage of the instances
        AwsUtils.getCpuUsage(cloudWatchClient, instances.keySet().stream().toList())
                .forEach((instanceId, cpuUsage) -> instances.get(instanceId).setCpuUsage(cpuUsage));

        final var averageWork = instances.values().stream()
                .filter(VMWorker::isRunning)
                .mapToDouble(VMWorker::getWork)
                .average()
                .orElse(0);

        final var averageCpuUsage = instances.values().stream()
                .filter(VMWorker::isRunning)
                .mapToDouble(VMWorker::getCpuUsage)
                .average()
                .orElse(0);

        logger.info("Average work: " + averageWork + " Average CPU usage: " + averageCpuUsage + " Instances: " + instances.size());

        // Check if we need to scale up, down or do nothing
        if (averageWork > WORKERS_OVERLOADED_THRESHOLD && !vmWorkersMonitor.anyVmWorkerInitializing()) {
            logger.info("VM workers are overloaded and none is initializing, scaling up");

            final var instance = AwsUtils.launchInstance(ec2Client);
            final var vmWorker = new VMWorker(instance, VMWorker.WorkerState.INITIALIZING);
            vmWorkersMonitor.getVmWorkers().put(instance.instanceId(), vmWorker);
        } else if (averageWork < WORKERS_UNDERLOADED_THRESHOLD && instances.size() > 1) {
            logger.info("VM workers are underloaded, scaling down");
            instances.values().stream()
                    .filter(VMWorker::isRunning)
                    .min(Comparator.comparingDouble(VMWorker::getCpuUsage)) // TODO: use work or cpu usage?
                    .ifPresent(VMWorker::markForTermination);
        }
    }

    private void updateInstances(final Map<String, VMWorker> instances) {
        final var awsInstances = AwsUtils.getInstances(ec2Client);

        final var runningInstances = awsInstances.stream()
                .filter(i -> i.state().name().equals(InstanceStateName.RUNNING))
                .toList();

        final var activeInstances = awsInstances.stream()
                .filter(i -> !i.state().name().equals(InstanceStateName.RUNNING) || !i.state().name().equals(InstanceStateName.PENDING))
                .map(Instance::instanceId)
                .toList();

        // Check if the instances are still running
        for (var instance : instances.values()) {
            if (!activeInstances.contains(instance.getInstance().instanceId())) {
                logger.severe("Instance " + instance.getInstance().instanceId() + " is not running, removing it.");
                instances.remove(instance.getInstance().instanceId());
            }
        }

        // Check if the instances are initialized
        for (var instance : instances.values()) {
            if (!instance.isRunning()) {
                // Get the instance data
                final var instanceId = instance.getInstance().instanceId();
                final var newInstance = runningInstances.stream().filter(i -> i.instanceId().equals(instanceId)).findFirst();

                if (newInstance.isEmpty())
                    continue;

                logger.info("Instance " + instanceId + " is now running, updating it.");

                instance.setInstance(newInstance.get());
                instance.setRunning();
            }
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
