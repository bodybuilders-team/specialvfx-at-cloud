package pt.ulisboa.tecnico.cnv.utils;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AwsUtils {

    private static final String AWS_IMAGE_ID = System.getenv("AWS_IMAGE_ID");
    private static final String AWS_SECURITY_GROUP = System.getenv("AWS_SECURITY_GROUP");
    private static final String AWS_KEYPAIR_NAME = System.getenv("AWS_KEYPAIR_NAME");
    private static final long OBS_TIME = 60 * 5; // 5 minutes
    private static final Logger logger = Logger.getLogger(AwsUtils.class.getName());

    private AwsUtils() {
        // hide implicit public constructor
    }

    /**
     * Launches an instance with the given parameters
     *
     * @param ec2 the ec2 client
     * @return the instance launched
     */
    public static Instance launchInstance(Ec2Client ec2) {
        final var runRequest = RunInstancesRequest.builder()
                .instanceType(InstanceType.T2_MICRO)
                .imageId(AWS_IMAGE_ID)
                .securityGroupIds(AWS_SECURITY_GROUP)
                .keyName(AWS_KEYPAIR_NAME)
                .monitoring(RunInstancesMonitoringEnabled.builder().enabled(true).build())
                .maxCount(1)
                .minCount(1)
                .build();

        final var response = ec2.runInstances(runRequest);

        return response.instances().get(0);
    }

    /**
     * Get the CPU usage of an instance
     *
     * @param cw         the cloudwatch client
     * @param instanceId the instance id
     * @return the CPU usage
     */
    public static double getCpuUsage(final CloudWatchClient cw, final String instanceId) {
        return getCpuUsage(cw, List.of(instanceId)).get(instanceId);
    }

    public static Map<String, Double> getCpuUsage(final CloudWatchClient cw, final List<String> instanceIds) {
        List<MetricDataQuery> dataQueries = new ArrayList<>();

        for (int i = 0; i < instanceIds.size(); i++) {
            final Dimension dimension = Dimension.builder()
                    .name("InstanceId")
                    .value(instanceIds.get(i))
                    .build();

            final var metric = Metric.builder()
                    .metricName("CPUUtilization")
                    .namespace("AWS/EC2")
                    .dimensions(dimension)
                    .build();

            MetricStat metStat = MetricStat.builder()
                    .stat("Average")
                    .period(60)
                    .metric(metric)
                    .build();

            MetricDataQuery dataQuery = MetricDataQuery.builder()
                    .metricStat(metStat)
                    .id("id" + i)
                    .returnData(true)
                    .build();

            dataQueries.add(dataQuery);
        }

        GetMetricDataRequest getMetReq = GetMetricDataRequest.builder()
                .startTime(Instant.now().minusSeconds(OBS_TIME))
                .endTime(Instant.now())
                .metricDataQueries(dataQueries)
                .build();

        GetMetricDataResponse response = cw.getMetricData(getMetReq);
        List<MetricDataResult> data = response.metricDataResults();

        Map<String, Double> cpuUsageMap = new HashMap<>();
        for (int i = 0; i < data.size(); i++) {
            final var res = data.get(i);
            cpuUsageMap.put(instanceIds.get(i), res.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .findFirst()
                    .orElse(0.0));
        }

        return cpuUsageMap;
    }


    /**
     * Deletes an instance
     *
     * @param ec2Client  the ec2 client
     * @param instanceId the instance id
     */
    public static void deleteInstance(final Ec2Client ec2Client, final String instanceId) {
        ec2Client.terminateInstances(r -> r.instanceIds(instanceId));

        TerminateInstancesRequest terminateRequest = TerminateInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();

        ec2Client.terminateInstances(terminateRequest);

        ec2Client.waiter().waitUntilInstanceTerminated(r -> r.instanceIds(instanceId));
    }

    /**
     * Get all instances
     *
     * @param ec2Client the ec2 client
     * @return the list of instances
     */
    public static List<Instance> getInstances(final Ec2Client ec2Client) { // TODO get only our instances
        DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();
        DescribeInstancesResponse response = ec2Client.describeInstances(request);

        return response.reservations().stream()
                .flatMap(r -> r.instances().stream())
                .filter(i -> i.state().name().equals(InstanceStateName.RUNNING))
                .filter(i -> i.imageId() != null && i.imageId().equals(AWS_IMAGE_ID))
                .toList();
    }

    public static Instance launchInstanceAndWait(final Ec2Client ec2Client) {
        final var tempInstanceInfo = AwsUtils.launchInstance(ec2Client);
        return waitForInstance(ec2Client, tempInstanceInfo.instanceId());
    }

    public static Instance waitForInstance(final Ec2Client ec2Client, final String instanceId) {
        final var instanceWaitResponse = ec2Client.waiter().waitUntilInstanceRunning(r -> r.instanceIds(instanceId));
        final var instance = instanceWaitResponse.matched().response().map(r -> r.reservations().get(0).instances().get(0));

        if (instance.isEmpty()) {
            logger.severe("Failed waiting for instance to be running");
            throw new RuntimeException("Failed waiting for instance to be running");
        }

        return instance.get();
    }

    public static Instance getInstance(final Ec2Client ec2Client, final String instanceId) {
        DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();

        DescribeInstancesResponse response = ec2Client.describeInstances(request);

        if (response.reservations().isEmpty()) {
            logger.severe("Failed to get instance");
            throw new RuntimeException("Failed to get instance");
        }

        return response.reservations().get(0).instances().get(0);
    }
}
