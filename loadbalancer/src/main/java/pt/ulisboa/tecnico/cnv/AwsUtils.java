package pt.ulisboa.tecnico.cnv;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.time.Instant;
import java.util.List;

public class AwsUtils {

    private static long OBS_TIME = 60 * 20;
    private static final String imageId = System.getenv("AWS_IMAGE_ID");
    private static final String securityGroup = System.getenv("AWS_SECURITY_GROUP");
    private static final String keyName = System.getenv("AWS_KEYPAIR_NAME");

    public static Instance launchInstance(Ec2Client ec2) {
        final var runRequest = RunInstancesRequest.builder()
                .instanceType(InstanceType.T2_MICRO)
                .imageId(imageId)
                .securityGroupIds(securityGroup)
                .keyName(keyName)
                .maxCount(1)
                .minCount(1)
                .build();

        final var response = ec2.runInstances(runRequest);
        final var instance = response.instances().get(0);
        ec2.waiter().waitUntilInstanceRunning(r -> r.instanceIds(instance.instanceId()));

        return instance;
    }

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

        for (MetricDataResult item : data) {
            System.out.println("The label is " + item.label());
            System.out.println("The status code is " + item.statusCode().toString());
        }

        return (long) data.get(0).values().get(0).doubleValue();
    }

    public static void deleteInstance(final Ec2Client ec2Client, final String instanceId) {
        ec2Client.terminateInstances(r -> r.instanceIds(instanceId));

        TerminateInstancesRequest terminateRequest = TerminateInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();

        ec2Client.terminateInstances(terminateRequest);

        ec2Client.waiter().waitUntilInstanceTerminated(r -> r.instanceIds(instanceId));
    }


    public static List<Instance> getInstances(final Ec2Client ec2Client) {
        DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();
        DescribeInstancesResponse response = ec2Client.describeInstances(request);

        return response.reservations().stream()
                .flatMap(r -> r.instances().stream())
                .filter(i -> i.state().name().equals(InstanceStateName.RUNNING))
                .toList();
    }
}
