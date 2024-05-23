package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpServer;
import pt.ulisboa.tecnico.cnv.mss.DynamoDbClientManager;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.ImageProcessorRequestMetricDynamoDbRepositoryImpl;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.ImageProcessorRequestMetricRepository;
import pt.ulisboa.tecnico.cnv.mss.raytracer.RayTracerRequestMetricDynamoDbRepositoryImpl;
import pt.ulisboa.tecnico.cnv.mss.raytracer.RaytracerRequestMetricRepository;
import pt.ulisboa.tecnico.cnv.utils.AwsUtils;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.lambda.LambdaClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class LoadBalancerWebServer {
    public static final String AWS_REGION = "eu-west-3";
    private static final int PORT = 8001;
    private static final Logger logger = Logger.getLogger(LoadBalancerWebServer.class.getName());

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: LoadBalancerWebServer <imageProcessorArn> <raytracerArn>");
            System.exit(1);
        }

        final HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        DynamoDbClientManager dynamoDbClientManager = new DynamoDbClientManager();
        RaytracerRequestMetricRepository raytracerRequestMetricRepository = new RayTracerRequestMetricDynamoDbRepositoryImpl(dynamoDbClientManager);
        ImageProcessorRequestMetricRepository imageProcessorRequestMetricRepository = new ImageProcessorRequestMetricDynamoDbRepositoryImpl(dynamoDbClientManager);

        LambdaClient lambdaClient = LambdaClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

        final Ec2Client ec2Client = Ec2Client.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create()).build();

        final Map<String, ServerInstanceInfo> instanceInfoMap = new ConcurrentHashMap<>();
        final var instances = AwsUtils.getInstances(ec2Client);

        logger.info("Found " + instances.size() + " instances");
        for (var instance : instances) {
            instanceInfoMap.put(instance.instanceId(), new ServerInstanceInfo(instance));
            logger.info("Instance " + instance.instanceId() + " added");
        }

        if (instanceInfoMap.isEmpty()) {
            logger.info("No instances found, launching a new one");
            final var instance = AwsUtils.launchInstance(ec2Client);
            ec2Client.waiter().waitUntilInstanceRunning(r -> r.instanceIds(instance.instanceId()));
            instanceInfoMap.put(instance.instanceId(), new ServerInstanceInfo(instance));
        }

        AutoScaler autoScaler = new AutoScaler(instanceInfoMap);

        autoScaler.start();

        server.createContext("/", new LoadBalancerHandler(instanceInfoMap,
                raytracerRequestMetricRepository,
                imageProcessorRequestMetricRepository,
                ec2Client,
                lambdaClient,
                /*imageProcessorArn=*/args[0],
                /*raytracerArn=*/args[1]
        ));
        server.start();
        logger.info("Server started on http://localhost:" + PORT + "/");
    }
}