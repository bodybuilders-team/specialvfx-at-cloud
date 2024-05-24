package pt.ulisboa.tecnico.cnv;

import pt.ulisboa.tecnico.cnv.utils.AwsUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AutoScaler {

    private final Map<String, VMWorkerInfo> instanceInfoMap;
    private final ReentrantLock monitor = new ReentrantLock();
    private final Condition condition = monitor.newCondition();
    private final Region region = Region.EU_WEST_3;
    private final CloudWatchClient cloudWatchClient = CloudWatchClient.builder().region(region).build();
    private final Ec2Client ec2Client = Ec2Client.builder().region(region).build();


    public AutoScaler(final Map<String, VMWorkerInfo> instanceInfoMap) {
        this.instanceInfoMap = instanceInfoMap;
    }

    public void start() {
        // Monitor the server load
        new Thread(() -> {
            monitor.lock();
            try {
                while (true) {
                    monitor();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                monitor.unlock();
            }
        }).start();

        //TODO: Temporary, remove after notifying in the loadbalancer
        // Notify the monitor to check the server load
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(1000);

                    monitor.lock();
                    condition.signalAll();
                    monitor.unlock();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                e.printStackTrace();
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
        final var averageCpuUsage = instanceInfoMap.values().stream()
                .mapToDouble(VMWorkerInfo::getCpuUsage)
                .average()
                .orElse(0);

        System.out.println("averageCpuUsage = " + averageCpuUsage);
        if (averageCpuUsage > 60) {
            final var instance = AwsUtils.launchInstance(ec2Client);
            ec2Client.waiter().waitUntilInstanceRunning(r -> r.instanceIds(instance.instanceId()));
            instanceInfoMap.put(instance.instanceId(), new VMWorkerInfo(instance));
        } else if (averageCpuUsage < 30 && instanceInfoMap.size() > 1) {
            instanceInfoMap.values().stream()
                    .min(Comparator.comparingDouble(VMWorkerInfo::getCpuUsage))
                    .ifPresent(instance -> {
                                //TODO, only delete after every request has been processed
                                instanceInfoMap.remove(instance.getInstance().instanceId());
                                AwsUtils.deleteInstance(ec2Client, instance.getInstance().instanceId());
                            }
                    );
        }
    }
}
