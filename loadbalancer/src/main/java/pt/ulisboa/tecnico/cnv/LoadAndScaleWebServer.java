package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.HttpGet;
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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Entry point for the webserver that will act as a load balancer and auto scaler.
 */
public class LoadAndScaleWebServer {
    public static final String AWS_REGION = "eu-west-3";
    private static final int PORT = 8001;
    private static final Logger logger = Logger.getLogger(LoadAndScaleWebServer.class.getName());

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: LoadAndScaleManager <imageProcessorArn> <raytracerArn>");
            System.exit(1);
        }

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

        // Get all instances of the VM workers
        final Map<String, VMWorkerInfo> vmWorkersInfo = new ConcurrentHashMap<>();
        final var instances = AwsUtils.getInstances(ec2Client);

        logger.info("Found " + instances.size() + " instances");
        for (var instance : instances) {
            vmWorkersInfo.put(instance.instanceId(), new VMWorkerInfo(instance));
            logger.info("Instance " + instance.instanceId() + " added");
        }

        if (vmWorkersInfo.isEmpty()) {
            logger.info("No instances found, launching a new one");
            final var instance = AwsUtils.launchInstance(ec2Client);
            ec2Client.waiter().waitUntilInstanceRunning(r -> r.instanceIds(instance.instanceId()));
            vmWorkersInfo.put(instance.instanceId(), new VMWorkerInfo(instance));
        }

        // Start the auto scaler
        AutoScaler autoScaler = new AutoScaler(vmWorkersInfo);
        autoScaler.start();

        server.createContext("/", new LoadBalancerHandler(
                vmWorkersInfo,
                raytracerRequestMetricRepository,
                imageProcessorRequestMetricRepository,
                ec2Client,
                lambdaClient,
                /*imageProcessorArn=*/args[0],
                /*raytracerArn=*/args[1]
        ));

        // Health check endpoint
        server.createContext("/test", new HealthCheckHandler());

        server.start();
        logger.info("Server started on http://localhost:" + PORT + "/");
    }


    static class HealthCheckHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (HttpGet.METHOD_NAME.equals(exchange.getRequestMethod())) {
                String response = "OK";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }
}