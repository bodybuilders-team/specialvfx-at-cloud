package pt.ulisboa.tecnico.cnv.utils;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import software.amazon.awssdk.core.exception.SdkClientException;
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
import software.amazon.awssdk.services.ec2.model.RunInstancesMonitoringEnabled;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class AwsUtils {

    public static final int WAIT_DELAY = 2000;
    private static final String AWS_IMAGE_ID = System.getenv("AWS_IMAGE_ID");
    private static final String AWS_SECURITY_GROUP = System.getenv("AWS_SECURITY_GROUP");
    private static final String AWS_KEYPAIR_NAME = System.getenv("AWS_KEYPAIR_NAME");
    private static final long OBS_TIME = 60 * 5; // 5 minutes
    private static final Logger logger = Logger.getLogger(AwsUtils.class.getName());
    private static final int MAX_TRIES = 60;
    private static final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3 * 1000).build();
    private static final CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

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
    }

    /**
     * Get all instances
     *
     * @param ec2Client the ec2 client
     * @return the list of instances
     */
    public static List<Instance> getRunningInstances(final Ec2Client ec2Client) { // TODO get only our instances
        DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();
        DescribeInstancesResponse response = ec2Client.describeInstances(request);

        return response.reservations().stream()
                .flatMap(r -> r.instances().stream())
                .filter(i -> i.state().name().equals(InstanceStateName.RUNNING))
                .filter(i -> i.imageId() != null && i.imageId().equals(AWS_IMAGE_ID))
                .toList();
    }

    public static List<Instance> getInstances(final Ec2Client ec2Client) { // TODO get only our instances
        DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();
        DescribeInstancesResponse response = ec2Client.describeInstances(request);

        return response.reservations().stream()
                .flatMap(r -> r.instances().stream())
                .filter(i -> i.imageId() != null && i.imageId().equals(AWS_IMAGE_ID))
                .toList();
    }

    public static Instance waitForInstance(final Ec2Client ec2Client, final String instanceId) throws CnvIOException {
        Optional<Instance> instance;
        try {
            final var instanceWaitResponse = ec2Client.waiter().waitUntilInstanceRunning(r -> r.instanceIds(instanceId));
            instance = instanceWaitResponse.matched().response().map(r -> r.reservations().get(0).instances().get(0));
        } catch (SdkClientException e) {
            throw new CnvIOException("Failed waiting for instance to be running");
        }

        if (instance.isEmpty()) {
            logger.severe("Failed waiting for instance to be running");
            throw new CnvIOException("Failed waiting for instance to be running");
        }

        // Wait for the webserver to be up by sending requests to /
        var i = 0;
        while (true) {
            try {
                i++;

                final var isReady = isWebserverReady(instance.get());

                if (isReady)
                    break;

                Thread.sleep(WAIT_DELAY);

                if (i >= MAX_TRIES)
                    throw new CnvIOException("Failed to connect to instance " + instance.get().instanceId());

            } catch (InterruptedException e) {
                logger.warning("Failed to connect to instance " + instance.get().instanceId());
            }
        }

        return instance.get();
    }


    public static boolean isWebserverReady(final Instance instance) {
        try {
            final var request = new HttpGet("http://" + instance.publicDnsName() + ":8000/");

            final CloseableHttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                return true;
            }
        } catch (Exception e) {
            //logger.info("Exception while checking web server readiness, error: " + e.getMessage());
        }

        return false;
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
