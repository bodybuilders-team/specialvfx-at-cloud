package pt.ulisboa.tecnico.cnv.webserver;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpServer;
import pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler;
import pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler;
import pt.ulisboa.tecnico.cnv.imageproc.ImageProcessingRequest;
import pt.ulisboa.tecnico.cnv.javassist.Request;
import pt.ulisboa.tecnico.cnv.javassist.tools.RequestAnalyzer;
import pt.ulisboa.tecnico.cnv.mss.DynamoDbClientManager;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.ImageProcessorRequestMetric;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.ImageProcessorRequestMetricDynamoDbRepositoryImpl;
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
    private static final Logger LOGGER = Logger.getLogger(RequestAnalyzer.class.getName());

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        DynamoDbClientManager dynamoDbClientManager = new DynamoDbClientManager();

        RaytracerRequestMetricRepository raytracerRequestMetricRepository = new RayTracerRequestMetricDynamoDbRepositoryImpl(dynamoDbClientManager);
        ImageProcessorRequestMetricRepository imageProcessorRequestMetricRepository = new ImageProcessorRequestMetricDynamoDbRepositoryImpl(dynamoDbClientManager);

        Filter metricsRecorderFilter = Filter.afterHandler(
                "Obtains the metrics of the request collected by the instrumentation tool and stores them",
                httpExchange -> {
                    long threadId = Thread.currentThread().getId();
                    Request request = RequestAnalyzer.getThreadRequest(threadId);


                    if (request != null) {
                        request.setCompleted(true);
                        LOGGER.info(String.format("[%s] Request from thread %s: %s%n", RequestAnalyzer.class.getSimpleName(), threadId, request));

                        if (request instanceof RaytracerRequest)
                            raytracerRequestMetricRepository.save(raytracerRequestToMetric((RaytracerRequest) request));
                        else if (request instanceof ImageProcessingRequest)
                            imageProcessorRequestMetricRepository.save(imageProcessorRequestToMetric((ImageProcessingRequest) request));
                    } else {
                        LOGGER.severe(String.format("[%s] Request from thread %s was not found%n", RequestAnalyzer.class.getSimpleName(), threadId));
                    }
                }
        );

        server.createContext("/", new RootHandler()).getFilters().add(metricsRecorderFilter);
        server.createContext("/raytracer", new RaytracerHandler()).getFilters().add(metricsRecorderFilter);
        server.createContext("/blurimage", new BlurImageHandler()).getFilters().add(metricsRecorderFilter);
        server.createContext("/enhanceimage", new EnhanceImageHandler()).getFilters().add(metricsRecorderFilter);

        server.start();

        System.out.println("Server started on http://localhost:" + PORT + "/");
    }

    private static RaytracerRequestMetric raytracerRequestToMetric(final RaytracerRequest request) {
        final var metric = new RaytracerRequestMetric();

        metric.setId(request.getId());
        metric.setBblCount(request.getBblCount());
        metric.setInstructionCount(request.getInstructionCount());

        metric.setScols(request.getScols());
        metric.setSrows(request.getSrows());
        metric.setWcols(request.getWcols());
        metric.setWrows(request.getWrows());
        metric.setCoff(request.getCoff());
        metric.setRoff(request.getRoff());

        return metric;
    }

    private static ImageProcessorRequestMetric imageProcessorRequestToMetric(final ImageProcessingRequest request) {
        final var metric = new ImageProcessorRequestMetric();

        metric.setId(request.getId());
        metric.setBblCount(request.getBblCount());
        metric.setInstructionCount(request.getInstructionCount());

        final var image = request.getImage();
        final long numPixels = ((long) image.getWidth()) * image.getHeight();
        metric.setNumPixels(numPixels);

        return metric;
    }
}
