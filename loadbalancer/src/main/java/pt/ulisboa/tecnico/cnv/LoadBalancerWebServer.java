package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpServer;
import pt.ulisboa.tecnico.cnv.mss.DynamoDbClientManager;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.ImageProcessorRequestMetricDynamoDbRepositoryImpl;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.ImageProcessorRequestMetricRepository;
import pt.ulisboa.tecnico.cnv.mss.raytracer.RayTracerRequestMetricDynamoDbRepositoryImpl;
import pt.ulisboa.tecnico.cnv.mss.raytracer.RaytracerRequestMetricRepository;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class LoadBalancerWebServer {
    private static final int PORT = 8001;
    private static final Logger LOGGER = Logger.getLogger(LoadBalancerWebServer.class.getName());

    public static void main(String[] args) throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        DynamoDbClientManager dynamoDbClientManager = new DynamoDbClientManager();

        RaytracerRequestMetricRepository raytracerRequestMetricRepository = new RayTracerRequestMetricDynamoDbRepositoryImpl(dynamoDbClientManager);
        ImageProcessorRequestMetricRepository imageProcessorRequestMetricRepository = new ImageProcessorRequestMetricDynamoDbRepositoryImpl(dynamoDbClientManager);

        server.createContext("/", new LoadBalancerHandler(raytracerRequestMetricRepository,imageProcessorRequestMetricRepository));

        server.start();
        System.out.println("Server started on http://localhost:" + PORT + "/");
    }
}