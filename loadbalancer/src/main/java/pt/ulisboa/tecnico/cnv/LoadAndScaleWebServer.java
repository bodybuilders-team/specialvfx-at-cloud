package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.HttpGet;
import pt.ulisboa.tecnico.cnv.mss.DynamoDbClientManager;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.BlurImageRequestMetricDynamoDbRepositoryImpl;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.EnhanceImageRequestMetricDynamoDbRepositoryImpl;
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
import java.util.logging.Logger;

/**
 * Entry point for the webserver that will act as a load balancer and auto scaler.
 */
public class LoadAndScaleWebServer {
    public static final String AWS_REGION = System.getenv("AWS_DEFAULT_REGION");
    private static final int PORT = 8001;
    private static final Logger logger = Logger.getLogger(LoadAndScaleWebServer.class.getName());

    public static void main(String[] args) throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        final DynamoDbClientManager dynamoDbClientManager = new DynamoDbClientManager();
        final RaytracerRequestMetricRepository raytracerRequestMetricRepository = new RayTracerRequestMetricDynamoDbRepositoryImpl(dynamoDbClientManager);
        final ImageProcessorRequestMetricRepository blurImageRequestMetricDynamoDbRepository = new BlurImageRequestMetricDynamoDbRepositoryImpl(dynamoDbClientManager);
        final ImageProcessorRequestMetricRepository enhanceImageRequestMetricDynamoDbRepository = new EnhanceImageRequestMetricDynamoDbRepositoryImpl(dynamoDbClientManager);

        final LambdaClient lambdaClient = LambdaClient.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

        final Ec2Client ec2Client = Ec2Client.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

        VMWorkersMonitor vmWorkersMonitor = new VMWorkersMonitor();
        final var instancesData = AwsUtils.getRunningInstances(ec2Client);

        logger.info("Found " + instancesData.size() + " instances");
        for (var instance : instancesData) {
            final var vmWorker = new VMWorker(instance, VMWorker.WorkerState.RUNNING);
            vmWorkersMonitor.getVmWorkers().put(instance.instanceId(), vmWorker);
            logger.info("Instance " + instance.instanceId() + " added");
        }

        // Start the auto scaler
        AutoScaler autoScaler = new AutoScaler(vmWorkersMonitor);
        autoScaler.start();

        server.createContext("/", new LoadBalancerHandler(
                vmWorkersMonitor,
                raytracerRequestMetricRepository,
                blurImageRequestMetricDynamoDbRepository,
                enhanceImageRequestMetricDynamoDbRepository,
                ec2Client,
                lambdaClient
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