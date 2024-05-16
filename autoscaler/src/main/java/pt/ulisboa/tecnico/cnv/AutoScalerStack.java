package pt.ulisboa.tecnico.cnv;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.cloudwatch.Metric;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClientBuilder;

import java.util.HashMap;
import java.util.Map;

public class AutoScalerStack extends Stack {
    public Map<Integer, Integer> serverLoad = new HashMap<>();

    public void monitor() {
        // Monitor the server load from cloudwatch
        HostedZone hostedZone = HostedZone.Builder.create(this, "MyHostedZone").zoneName("example.org").build();
        Metric metric = Metric.Builder.create()
                .namespace("AWS/EC2")
                .metricName("CPUUtilization")
                .build();

    }
}
