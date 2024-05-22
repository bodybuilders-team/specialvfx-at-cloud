package pt.ulisboa.tecnico.cnv;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AutoScaler {

    private final Map<String, ServerInstanceInfo> instanceInfoMap;
    private final ReentrantLock monitor = new ReentrantLock();
    private final Condition condition = monitor.newCondition();
    private final Region region = Region.EU_WEST_3;
    private final CloudWatchClient cloudWatchClient = CloudWatchClient.builder().region(region).build();
    private final Ec2Client ec2Client = Ec2Client.builder().region(region).build();


    public AutoScaler(final Map<String, ServerInstanceInfo> instanceInfoMap) {
        this.instanceInfoMap = instanceInfoMap;
    }

    public void start() {
        new Thread(() -> {
            monitor.lock();
            try {
                while (true) {
                    monitor();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                monitor.unlock();
            }
        }).start();

        //TODO: Temporary, remove after notifying in the loadbalancer
        new Thread(() -> {
            monitor.lock();
            try {
                while (true) {
                    Thread.sleep(1000);

                    condition.signal();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                monitor.unlock();
            }
        }).start();
    }

    private void monitor() throws InterruptedException {
        condition.await();

        // Monitor the server load from cloudwatch
        for (var instance : instanceInfoMap.values()) {
            final var instanceId = instance.getInstance().instanceId();

            final var cpuUsage = AwsUtils.getCpuUsage(cloudWatchClient, instanceId);
            instance.setCpuUsage(cpuUsage);
        }

        // Check if we need to scale up or down
        final var averageCpuUsage = instanceInfoMap.values().stream().mapToDouble(ServerInstanceInfo::getCpuUsage).average().orElse(0);

        if (averageCpuUsage > 0.6) {
            final var instance = AwsUtils.launchInstance(ec2Client);
            instanceInfoMap.put(instance.instanceId(), new ServerInstanceInfo(instance));
        } else if (averageCpuUsage < 0.3) {
            instanceInfoMap
                    .values().stream()
                    .min(Comparator.comparingDouble(ServerInstanceInfo::getCpuUsage))
                    .ifPresent(instance -> {
                                //TODO, only delete after every request has been processed
                                instanceInfoMap.remove(instance.getInstance().instanceId());
                                AwsUtils.deleteInstance(ec2Client, instance.getInstance().instanceId());
                            }
                    );
        }
    }
}
