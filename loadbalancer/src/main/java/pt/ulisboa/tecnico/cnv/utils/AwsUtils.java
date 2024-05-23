package pt.ulisboa.tecnico.cnv.utils;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataResult;
import software.amazon.awssdk.services.cloudwatch.model.MetricStat;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

import java.time.Instant;
import java.util.List;

public class AwsUtils {

    private static final String AWS_IMAGE_ID = System.getenv("AWS_IMAGE_ID");
    private static final String AWS_SECURITY_GROUP = System.getenv("AWS_SECURITY_GROUP");
    private static final String AWS_KEYPAIR_NAME = System.getenv("AWS_KEYPAIR_NAME");
    private static final long OBS_TIME = 60 * 20;

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
        final Dimension dimension = Dimension.builder()
                .name("InstanceId")
                .value(instanceId)
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

        MetricDataQuery dataQUery = MetricDataQuery.builder()
                .metricStat(metStat)
                .id("foo2")
                .returnData(true)
                .build();

        List<MetricDataQuery> dataQueries = List.of(dataQUery);

        GetMetricDataRequest getMetReq = GetMetricDataRequest.builder()
                .startTime(Instant.now().minusSeconds(OBS_TIME))
                .endTime(Instant.now())
                .metricDataQueries(dataQueries)
                .build();

        GetMetricDataResponse response = cw.getMetricData(getMetReq);
        List<MetricDataResult> data = response.metricDataResults();

        final var res = data.stream().findFirst();

        return res.map(metricDataResult ->
                metricDataResult.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0)
        ).orElse(0.0);
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
    public static List<Instance> getInstances(final Ec2Client ec2Client) {
        DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();
        DescribeInstancesResponse response = ec2Client.describeInstances(request);

        return response.reservations().stream()
                .flatMap(r -> r.instances().stream())
                .filter(i -> i.state().name().equals(InstanceStateName.RUNNING))
                .toList();
    }
}
