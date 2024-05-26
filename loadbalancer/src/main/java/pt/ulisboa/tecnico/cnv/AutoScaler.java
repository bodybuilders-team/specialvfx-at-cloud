package pt.ulisboa.tecnico.cnv;

import pt.ulisboa.tecnico.cnv.utils.AwsUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.Comparator;
import java.util.Map;
import java.util.logging.Logger;

public class AutoScaler {
    public static final long DEFAULT_WORK_RAYTRACER = 500000000;
    public static final long DEFAULT_WORK_IMAGE_PROCESSOR = 430000000;
    public static final long COMPLEX_REQUEST_THRESHOLD = DEFAULT_WORK_RAYTRACER * 2;
    public static final long MAX_WORKLOAD = DEFAULT_WORK_RAYTRACER * 5;
    public static final int LAST_INSTANCE_MAX_CPU_USAGE = 30;
    private final Map<String, VMWorkerInfo> instances;
    private final Region region = Region.EU_WEST_3;
    private final CloudWatchClient cloudWatchClient = CloudWatchClient.builder().region(region).build();
    private final Ec2Client ec2Client = Ec2Client.builder().region(region).build();
    private final Logger logger = Logger.getLogger(AutoScaler.class.getName());


    public AutoScaler(final Map<String, VMWorkerInfo> instances) {
        this.instances = instances;
    }

    public void start() {
        // Monitor the server load
        new Thread(() -> {
            try {
                while (true) {
                    monitor();
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

    private void monitor() throws InterruptedException {
        if (instances.isEmpty()) {
            return;
        }

        // Updates the CPU usage of the instances
        AwsUtils.getCpuUsage(cloudWatchClient, instances.keySet().stream().toList())
                .forEach((instanceId, cpuUsage) -> instances.get(instanceId).setCpuUsage(cpuUsage));

        final var averageWork = instances.values().stream()
                .mapToDouble(VMWorkerInfo::getWork)
                .average()
                .orElse(0);

//        final var averageCpuUsage = instances.values().stream()
//                .mapToDouble(VMWorkerInfo::getCpuUsage)
//                .average()
//                .orElse(0);

        logger.info("Average work: " + averageWork);
        logger.info("Instances: " + instances.size());

        // Check if we need to scale up, down or do nothing
        if (averageWork > 0.8 * MAX_WORKLOAD) {
            final var instance = AwsUtils.launchInstanceAndWait(ec2Client);
            instances.put(instance.instanceId(), new VMWorkerInfo(instance));
        } else if (averageWork < 0.2 * MAX_WORKLOAD && instances.size() > 1) {
            instances.values().stream()
                    .min(Comparator.comparingDouble(VMWorkerInfo::getCpuUsage))
                    .ifPresent(instance -> {
                                //TODO, only delete after every request has been processed
                                //TODO: Add terminating flag, to avoid this instance getting more requests
                                //TODO: Wait until the work is == 0
                                instances.remove(instance.getInstance().instanceId());
                                AwsUtils.deleteInstance(ec2Client, instance.getInstance().instanceId());
                            }
                    );
        }
    }
}
