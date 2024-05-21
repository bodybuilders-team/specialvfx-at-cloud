package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.ImageProcessorRequestMetricRepository;
import pt.ulisboa.tecnico.cnv.mss.raytracer.RaytracerRequestMetric;
import pt.ulisboa.tecnico.cnv.mss.raytracer.RaytracerRequestMetricRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LoadBalancerHandler implements HttpHandler {
    private final List<String> servers = new LinkedList<>(
            List.of("http://localhost:8000")
    );
    private final double EPSILON = 1e-2;
    private final AtomicInteger currentServer = new AtomicInteger();
    private final RaytracerRequestMetricRepository raytracerRequestMetricRepository;
    private final ImageProcessorRequestMetricRepository imageProcessorRequestMetricRepository;
    private final Logger logger = Logger.getLogger(LoadBalancerHandler.class.getName());
    private static final long DEFAULT_COMPLEXITY_RAYTRACER = 900000;
    private static final long DEFAULT_COMPLEXITY_IMAGE_PROCESSOR = 91;
    private static final int RAYTRACER_NUM_VARS = 4;

    public LoadBalancerHandler(RaytracerRequestMetricRepository raytracerRequestMetricRepository, final ImageProcessorRequestMetricRepository imageProcessorRequestMetricRepository) {
        this.raytracerRequestMetricRepository = raytracerRequestMetricRepository;
        this.imageProcessorRequestMetricRepository = imageProcessorRequestMetricRepository;
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equals("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            currentServer.set((currentServer.get() + 1) % servers.size());

            // Send http request to the selected server
            final var requestURI = exchange.getRequestURI();
            final var uri = URI.create(servers.get(currentServer.get()) + requestURI);

            final var requestBody = exchange.getRequestBody().readAllBytes();

            final var requestComplexity = estimateComplexity(requestURI, requestBody);

            System.out.println("requestComplexity = " + requestComplexity);

            resendTo(exchange, requestBody, uri);
        } catch (Exception e) {
            logger.severe(ExceptionUtils.getStackTrace(e));
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
        } finally {
            exchange.close();
        }
    }

    private long estimateComplexity(final URI requestURI, final byte[] requestBody) throws IOException {
        switch (requestURI.getPath()) {
            case "/blurimage": {
                final var image = readImage(requestBody);
                final var numPixels = image.getWidth() * image.getHeight();

                final var regression = new CNVOLSMultipleLinearRegression(0.0);
                final var requests = raytracerRequestMetricRepository.getAllDistinctRequests();

                if (requests.size() <= 1)
                    return DEFAULT_COMPLEXITY_IMAGE_PROCESSOR;


                // TODO: Extract to function
                final var y = requests.stream().mapToDouble(RaytracerRequestMetric::getInstructionCount).toArray();

                final var x = requests.stream().map(r -> new double[]{numPixels})
                        .toArray(double[][]::new);

                final var input = new double[]{numPixels};

                final var normalizedInput = new double[1];

                for (int col = 0; col < x[0].length; col++) {
                    DescriptiveStatistics stats = new DescriptiveStatistics();

                    for (final double[] sample : x)
                        stats.addValue(sample[col]);

                    double mean = stats.getMean();
                    double standardDeviation = stats.getStandardDeviation();

                    applyFunctionToColumn(x, col, r -> {
                        for (int i = 0; i < r.length; ++i) {
                            r[i] = (r[i] - mean) / (standardDeviation + EPSILON) + EPSILON;
                        }
                    });

                    normalizedInput[col] = (input[col] - mean) / (standardDeviation + EPSILON) + EPSILON;
                }

                regression.newSampleData(y, x);

                return (long) regression.predict(normalizedInput);
            }
            case "/raytracer": {
                final Map<String, String> parameters = URLEncodedUtils.parse(requestURI, Charset.defaultCharset())
                        .stream()
                        .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));

                int scols = Integer.parseInt(parameters.get("scols"));
                int srows = Integer.parseInt(parameters.get("srows"));
                int wcols = Integer.parseInt(parameters.get("wcols"));
                int wrows = Integer.parseInt(parameters.get("wrows"));
                int coff = Integer.parseInt(parameters.get("coff"));
                int roff = Integer.parseInt(parameters.get("roff"));

                final var regression = new CNVOLSMultipleLinearRegression(0.0);
                final var requests = raytracerRequestMetricRepository.getAllDistinctRequests();

                if (requests.size() <= RAYTRACER_NUM_VARS)
                    return DEFAULT_COMPLEXITY_RAYTRACER;

                final var y = requests.stream().mapToDouble(RaytracerRequestMetric::getInstructionCount).toArray();

                final var x = requests.stream().map(r -> new double[]{r.getScols(), r.getSrows(), r.getWcols(), r.getWrows()})
                        .toArray(double[][]::new);

                final var input = new double[]{scols, srows, wcols, wrows};

                final var normalizedInput = new double[RAYTRACER_NUM_VARS];

                for (int col = 0; col < x[0].length; col++) {
                    DescriptiveStatistics stats = new DescriptiveStatistics();

                    for (final double[] sample : x)
                        stats.addValue(sample[col]);

                    double mean = stats.getMean();
                    double standardDeviation = stats.getStandardDeviation();

                    applyFunctionToColumn(x, col, r -> {
                        for (int i = 0; i < r.length; ++i) {
                            r[i] = (r[i] - mean) / (standardDeviation + EPSILON) + EPSILON;
                        }
                    });

                    normalizedInput[col] = (input[col] - mean) / (standardDeviation + EPSILON) + EPSILON;
                }

                regression.newSampleData(y, x);

                return (long) regression.predict(normalizedInput);
            }
            default:
                return 1;
        }
    }

    private static void applyFunctionToColumn(final double[][] x, final int col, final Consumer<double[]> function) {
        final var colData = Arrays.stream(x).mapToDouble(row -> row[col]).toArray();

        function.accept(colData);

        for (int i = 0; i < x.length; ++i) {
            x[i][col] = colData[i];
        }
    }


    private void resendTo(final HttpExchange exchange, final byte[] requestBody, final URI uri) throws IOException {
        final var connection = (HttpURLConnection) uri.toURL().openConnection();

        connection.setRequestMethod(exchange.getRequestMethod());

        exchange.getRequestHeaders()
                .forEach((k, v) -> v.forEach(h -> connection.setRequestProperty(k, h)));

        connection.setDoOutput(true);

        try (var os = connection.getOutputStream()) {
            os.write(requestBody);
        }

        connection.connect();

        connection.getHeaderFields().

                forEach((k, v) ->

                {
                    if (k != null) {
                        v.forEach(h -> exchange.getResponseHeaders().add(k, h));
                    }
                });

        exchange.sendResponseHeaders(connection.getResponseCode(), connection.getContentLengthLong());

        try (
                var os = exchange.getResponseBody();
                var is = connection.getInputStream()) {
            is.transferTo(os);
        }

        connection.disconnect();
    }


    private BufferedImage readImage(final byte[] data) throws IOException {
        // Result syntax: data:image/<format>;base64,<encoded image>
        String result = new String(data).lines().collect(Collectors.joining("\n"));
        String[] resultSplits = result.split(",");

        byte[] decoded = Base64.getDecoder().decode(resultSplits[1]);
        ByteArrayInputStream bais = new ByteArrayInputStream(decoded);

        return ImageIO.read(bais);
    }


}
