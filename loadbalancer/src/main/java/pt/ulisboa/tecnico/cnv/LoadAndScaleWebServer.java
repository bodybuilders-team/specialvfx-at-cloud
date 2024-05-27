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
import java.util.logging.Logger;

/**
 * Entry point for the webserver that will act as a load balancer and auto scaler.
 */
public class LoadAndScaleWebServer {
    public static final String AWS_REGION = "eu-west-3";
    private static final int PORT = 8001;
    private static final Logger logger = Logger.getLogger(LoadAndScaleWebServer.class.getName());

    public static void main(String[] args) throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        final DynamoDbClientManager dynamoDbClientManager = new DynamoDbClientManager();
        final RaytracerRequestMetricRepository raytracerRequestMetricRepository = new RayTracerRequestMetricDynamoDbRepositoryImpl(dynamoDbClientManager);
        final ImageProcessorRequestMetricRepository imageProcessorRequestMetricRepository = new ImageProcessorRequestMetricDynamoDbRepositoryImpl(dynamoDbClientManager);

        final LambdaClient lambdaClient = LambdaClient.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

        final Ec2Client ec2Client = Ec2Client.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

//        // Get all instances of the VM workers
        VMWorkersMonitor VMWorkersMonitor = new VMWorkersMonitor();
        final var instancesData = AwsUtils.getRunningInstances(ec2Client);

        logger.info("Found " + instancesData.size() + " instances");
        for (var instance : instancesData) {
            final var vmWorker = new VMWorker(instance);
            vmWorker.setInitialized(true);
            VMWorkersMonitor.getVmWorkers().put(instance.instanceId(), vmWorker);
            logger.info("Instance " + instance.instanceId() + " added");
        }

        if (VMWorkersMonitor.getVmWorkers().isEmpty()) {
            logger.info("No instances found, launching a new one");
            final var instance = AwsUtils.launchInstanceAndWait(ec2Client);
            final var vmWorker = new VMWorker(instance);
            vmWorker.setInitialized(true);
            VMWorkersMonitor.getVmWorkers().put(instance.instanceId(), vmWorker);
            logger.info("Instance " + instance.instanceId() + " added");
        }

        // Start the auto scaler
        AutoScaler autoScaler = new AutoScaler(VMWorkersMonitor);
        autoScaler.start();

        server.createContext("/", new LoadBalancerHandler(
                VMWorkersMonitor,
                raytracerRequestMetricRepository,
                imageProcessorRequestMetricRepository,
                ec2Client,
                lambdaClient
        ));
        server.start();
        logger.info("Server started on http://localhost:" + PORT + "/");
    }


}