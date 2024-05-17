package pt.ulisboa.tecnico.cnv.webserver;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpServer;
import pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler;
import pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler;
import pt.ulisboa.tecnico.cnv.javassist.tools.RequestAnalyzer;
import pt.ulisboa.tecnico.cnv.mss.MSSDynamoDB;
import pt.ulisboa.tecnico.cnv.mss.MSSFile;
import pt.ulisboa.tecnico.cnv.mss.MetricStorageSystem;
import pt.ulisboa.tecnico.cnv.mss.Request;
import pt.ulisboa.tecnico.cnv.raytracer.RaytracerHandler;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

/**
 * The SpecialVFX@Cloud web server that listens for HTTP requests and delegates them to the appropriate handler.
 */
public class WebServer {

    private static final int PORT = 8000;
    private static final Boolean MSS_DYNAMODB = true;
    private static final Logger LOGGER = Logger.getLogger(RequestAnalyzer.class.getName());

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        MetricStorageSystem metricStorageSystem = MSS_DYNAMODB ? new MSSDynamoDB() : new MSSFile();

        Filter metricsRecorderFilter = Filter.afterHandler(
                "Obtains the metrics of the request collected by the instrumentation tool and stores them",
                httpExchange -> {
                    long threadId = Thread.currentThread().getId();
                    Request request = RequestAnalyzer.getThreadRequest(threadId);

                    if (request != null) {
                        LOGGER.info(String.format("[%s] Request from thread %s: %s%n", RequestAnalyzer.class.getSimpleName(), threadId, request));
                        metricStorageSystem.save(request);
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
}
