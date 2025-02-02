package pt.ulisboa.tecnico.cnv.webserver;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpServer;
import pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler;
import pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler;
import pt.ulisboa.tecnico.cnv.imageproc.ImageProcessingRequest;
import pt.ulisboa.tecnico.cnv.javassist.Request;
import pt.ulisboa.tecnico.cnv.javassist.tools.RequestAnalyzer;
import pt.ulisboa.tecnico.cnv.mss.DynamoDbClientManager;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.BlurImageRequestMetricDynamoDbRepositoryImpl;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.EnhanceImageRequestMetricDynamoDbRepositoryImpl;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.ImageProcessorRequestMetric;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.ImageProcessorRequestMetricRepository;
import pt.ulisboa.tecnico.cnv.mss.raytracer.RayTracerRequestMetricDynamoDbRepositoryImpl;
import pt.ulisboa.tecnico.cnv.mss.raytracer.RaytracerRequestMetric;
import pt.ulisboa.tecnico.cnv.mss.raytracer.RaytracerRequestMetricRepository;
import pt.ulisboa.tecnico.cnv.raytracer.RaytracerHandler;
import pt.ulisboa.tecnico.cnv.raytracer.RaytracerRequest;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

/**
 * The SpecialVFX@Cloud web server that listens for HTTP requests and delegates them to the appropriate handler.
 */
public class WebServer {

    private static final int PORT = 8000;
    private static final Logger LOGGER = Logger.getLogger(WebServer.class.getName());

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        DynamoDbClientManager dynamoDbClientManager = new DynamoDbClientManager();

        RaytracerRequestMetricRepository raytracerRequestMetricRepository = new RayTracerRequestMetricDynamoDbRepositoryImpl(dynamoDbClientManager);
        final ImageProcessorRequestMetricRepository blurImageRequestMetricDynamoDbRepository = new BlurImageRequestMetricDynamoDbRepositoryImpl(dynamoDbClientManager);
        final ImageProcessorRequestMetricRepository enhanceImageRequestMetricDynamoDbRepository = new EnhanceImageRequestMetricDynamoDbRepositoryImpl(dynamoDbClientManager);

        Filter metricsRecorderFilter = getMetricsRecorderFilter(
                raytracerRequestMetricRepository,
                blurImageRequestMetricDynamoDbRepository,
                enhanceImageRequestMetricDynamoDbRepository
        );

        server.createContext("/", new RootHandler());
        server.createContext("/raytracer", new RaytracerHandler()).getFilters().add(metricsRecorderFilter);
        server.createContext("/blurimage", new BlurImageHandler()).getFilters().add(metricsRecorderFilter);
        server.createContext("/enhanceimage", new EnhanceImageHandler()).getFilters().add(metricsRecorderFilter);

        server.start();

        System.out.println("Server started on http://localhost:" + PORT + "/");
    }

    /**
     * Creates a filter that records the metrics of the request collected by the instrumentation tool and stores them.
     *
     * @param raytracerRequestMetricRepository    the repository where the raytracer request metrics will be stored
     * @param blurImageRequestMetricRepository    the repository where the blur request metrics will be stored
     * @param enhanceImageRequestMetricRepository the repository where the enhance request metrics will be stored
     * @return the filter
     */
    private static Filter getMetricsRecorderFilter(
            RaytracerRequestMetricRepository raytracerRequestMetricRepository,
            ImageProcessorRequestMetricRepository blurImageRequestMetricRepository,
            ImageProcessorRequestMetricRepository enhanceImageRequestMetricRepository
    ) {
        return Filter.afterHandler(
                "Obtains the metrics of the request collected by the instrumentation tool and stores them",
                httpExchange -> {
                    if (!httpExchange.getRequestMethod().equals("POST")) {
                        return;
                    }
                    long threadId = Thread.currentThread().getId();
                    Request request = RequestAnalyzer.getThreadRequest(threadId);

                    if (request != null) {
                        request.setCompleted(true);
                        LOGGER.info(String.format("[%s] Request from thread %s: %s%n", RequestAnalyzer.class.getSimpleName(), threadId, request));

                        if (request instanceof RaytracerRequest raytracerRequest)
                            raytracerRequestMetricRepository.save(raytracerRequestToMetric(raytracerRequest));
                        else if (request instanceof ImageProcessingRequest imageProcessingRequest)
                            if (httpExchange.getRequestURI().getPath().equals("/blurimage"))
                                blurImageRequestMetricRepository.save(imageProcessorRequestToMetric(imageProcessingRequest));
                            else if (httpExchange.getRequestURI().getPath().equals("/enhanceimage"))
                                enhanceImageRequestMetricRepository.save(imageProcessorRequestToMetric(imageProcessingRequest));
                    } else {
                        LOGGER.severe(String.format("[%s] Request from thread %s was not found%n", RequestAnalyzer.class.getSimpleName(), threadId));
                    }
                }
        );
    }

    /**
     * Converts a raytracer request to a raytracer request metric.
     *
     * @param request the raytracer request
     * @return the raytracer request metric
     */
    private static RaytracerRequestMetric raytracerRequestToMetric(final RaytracerRequest request) {
        final var metric = new RaytracerRequestMetric();

        if (request.getInput() != null)
            metric.setSceneSize(request.getInput().length);
        else
            metric.setSceneSize(0);

        metric.setId(request.getId());
        metric.setBblCount(request.getBblCount());
        metric.setInstructionCount(request.getInstructionCount());

        metric.setScols(request.getScols());
        metric.setSrows(request.getSrows());
        metric.setWcols(request.getWcols());
        metric.setWrows(request.getWrows());

        return metric;
    }

    /**
     * Converts an image processor request to an image processor request metric.
     *
     * @param request the image processor request
     * @return the image processor request metric
     */
    private static ImageProcessorRequestMetric imageProcessorRequestToMetric(final ImageProcessingRequest request) {
        final var metric = new ImageProcessorRequestMetric();

        metric.setId(request.getId());
        metric.setBblCount(request.getBblCount());
        metric.setInstructionCount(request.getInstructionCount());

        final var image = request.getImage();
        metric.setWidth(image.getWidth());
        metric.setHeight(image.getHeight());

        return metric;
    }
}
