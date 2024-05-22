package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpServer;
import pt.ulisboa.tecnico.cnv.mss.DynamoDbClientManager;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.ImageProcessorRequestMetricDynamoDbRepositoryImpl;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.ImageProcessorRequestMetricRepository;
import pt.ulisboa.tecnico.cnv.mss.raytracer.RayTracerRequestMetricDynamoDbRepositoryImpl;
import pt.ulisboa.tecnico.cnv.mss.raytracer.RaytracerRequestMetricRepository;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class LoadBalancerWebServer {
    private static final int PORT = 8001;
    private static final Logger LOGGER = Logger.getLogger(LoadBalancerWebServer.class.getName());

    public static void main(String[] args) throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        DynamoDbClientManager dynamoDbClientManager = new DynamoDbClientManager();

        final Map<String, ServerInstanceInfo> instanceInfoMap = new ConcurrentHashMap<>();
        final Region region = Region.EU_WEST_3;
        final Ec2Client ec2Client = Ec2Client.builder().region(region).build();

        final var instances = AwsUtils.getInstances(ec2Client);

        System.out.println("Found " + instances.size() + " instances");
        for (var instance : instances) {
            instanceInfoMap.put(instance.instanceId(), new ServerInstanceInfo(instance));
            System.out.println("Instance " + instance.instanceId() + " added");
        }

        if (instanceInfoMap.isEmpty()) {
            System.out.println("No instances found, launching a new one");
            final var instance = AwsUtils.launchInstance(ec2Client);
            instanceInfoMap.put(instance.instanceId(), new ServerInstanceInfo(instance));
        }

        RaytracerRequestMetricRepository raytracerRequestMetricRepository = new RayTracerRequestMetricDynamoDbRepositoryImpl(dynamoDbClientManager);
        ImageProcessorRequestMetricRepository imageProcessorRequestMetricRepository = new ImageProcessorRequestMetricDynamoDbRepositoryImpl(dynamoDbClientManager);

        AutoScaler autoScaler = new AutoScaler(instanceInfoMap);

        autoScaler.start();

        server.createContext("/", new LoadBalancerHandler(instanceInfoMap, raytracerRequestMetricRepository, imageProcessorRequestMetricRepository));
        server.start();
        System.out.println("Server started on http://localhost:" + PORT + "/");
    }
}