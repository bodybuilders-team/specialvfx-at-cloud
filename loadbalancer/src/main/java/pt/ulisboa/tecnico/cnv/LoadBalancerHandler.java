package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.ImageProcessorRequestMetric;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.ImageProcessorRequestMetricRepository;
import pt.ulisboa.tecnico.cnv.mss.raytracer.RaytracerRequestMetric;
import pt.ulisboa.tecnico.cnv.mss.raytracer.RaytracerRequestMetricRepository;
import pt.ulisboa.tecnico.cnv.utils.AwsUtils;
import pt.ulisboa.tecnico.cnv.utils.ExceptionUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LoadBalancerHandler implements HttpHandler {
    private static final double EPSILON = 1e-6;
    private static final int PORT = 8000;
    private static final long DEFAULT_COMPLEXITY_RAYTRACER = 500000000;
    private static final long DEFAULT_COMPLEXITY_IMAGE_PROCESSOR = 91;

    private final Map<String, ServerInstanceInfo> instanceInfoMap;
    private final RaytracerRequestMetricRepository raytracerRequestMetricRepository;
    private final ImageProcessorRequestMetricRepository imageProcessorRequestMetricRepository;
    private final Ec2Client ec2Client;
    private final LambdaClient lambdaClient;
    private final Logger logger = Logger.getLogger(LoadBalancerHandler.class.getName());
    private final String imageProcessorArn;
    private final String raytracerArn;

    public LoadBalancerHandler(
            final Map<String, ServerInstanceInfo> instanceInfoMap,
            RaytracerRequestMetricRepository raytracerRequestMetricRepository,
            final ImageProcessorRequestMetricRepository imageProcessorRequestMetricRepository,
            final Ec2Client ec2Client,
            final LambdaClient lambdaClient,
            final String imageProcessorArn,
            final String raytracerArn
    ) {
        this.instanceInfoMap = instanceInfoMap;
        this.raytracerRequestMetricRepository = raytracerRequestMetricRepository;
        this.imageProcessorRequestMetricRepository = imageProcessorRequestMetricRepository;
        this.ec2Client = ec2Client;
        this.lambdaClient = lambdaClient;
        this.imageProcessorArn = imageProcessorArn;
        this.raytracerArn = raytracerArn;
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equals("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            final var requestURI = exchange.getRequestURI();
            final var requestBody = exchange.getRequestBody().readAllBytes();

            // Estimate the complexity of the request
            final var requestComplexity = estimateComplexity(requestURI, requestBody);
            logger.info("Received request for " + requestURI + " with complexity " + requestComplexity);

            // Check if the vms are overloaded
            final var averageCpuUsage = instanceInfoMap.values().stream()
                    .mapToDouble(ServerInstanceInfo::getCpuUsage)
                    .average()
                    .orElse(0);

            if (averageCpuUsage > 80) {
                logger.info("Average CPU usage is " + averageCpuUsage + ", launching a new instance");
                final var instance = AwsUtils.launchInstance(ec2Client);
                instanceInfoMap.put(instance.instanceId(), new ServerInstanceInfo(instance));

                if (requestComplexity > 100) { // TODO: Use a better threshold
                    logger.info("Request is complex, sending to new instance");
                    ec2Client.waiter().waitUntilInstanceRunning(r -> r.instanceIds(instance.instanceId())); // TODO: Use a better way to wait
                    redirectToVM(exchange, instanceInfoMap.get(instance.instanceId()), requestURI, requestComplexity, requestBody);
                } else {
                    logger.info("Request is simple, sending to lambda");

                    SdkBytes payload = SdkBytes.fromByteArray(requestBody);
                    final var response = lambdaClient.invoke(req -> req
                            .functionName(requestURI.getPath().equals("/raytracer") ? raytracerArn : imageProcessorArn)
                            .payload(payload)
                    );
                    // TODO: Stream the response
                    exchange.sendResponseHeaders(response.statusCode(), response.payload().asByteArray().length);
                    exchange.getResponseBody().write(response.payload().asByteArray());
                }
                return;
            }

            // Send the request to the instance with the least CPU usage
            final var instanceWithLeastCpuUsage = instanceInfoMap.values().stream()
                    .min(Comparator.comparingDouble(ServerInstanceInfo::getCpuUsage))// TODO: Use instance work and implement work left
                    .orElseThrow();

            redirectToVM(exchange, instanceWithLeastCpuUsage, requestURI, requestComplexity, requestBody);

        } catch (Exception e) {
            logger.severe(ExceptionUtils.getStackTrace(e));
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
        } finally {
            exchange.close();
        }
    }

    /**
     * Redirect the request to the instance that will process it.
     * It will increment the work of the instance and decrement it after the request is processed.
     */
    private void redirectToVM(
            HttpExchange exchange,
            ServerInstanceInfo instance,
            URI requestURI,
            long requestComplexity,
            byte[] requestBody
    ) throws URISyntaxException, IOException {
        final var instanceUri = new URI("http://" + instance.getInstance().publicDnsName() + ":" + PORT + requestURI);

        instance.addWork(requestComplexity);
        redirect(exchange, requestBody, instanceUri);
        instance.removeWork(requestComplexity);
    }

    /**
     * Estimate the complexity of the request.
     *
     * @param requestURI  The request URI
     * @param requestBody The request body
     * @return The estimated complexity
     * @throws IOException If an error occurs
     */
    private long estimateComplexity(final URI requestURI, final byte[] requestBody) throws IOException {
        switch (requestURI.getPath()) {
            case "/blurimage": {
                final var image = readImage(requestBody);
                final var numPixels = image.getWidth() * image.getHeight();

                System.out.println("numPixels = " + numPixels);

                final var regression = new CNVMultipleLinearRegression();
                final var requests = imageProcessorRequestMetricRepository.getAllRequests();

                if (requests.size() <= 1)
                    return DEFAULT_COMPLEXITY_IMAGE_PROCESSOR;

                final var y = requests.stream().mapToDouble(ImageProcessorRequestMetric::getInstructionCount).toArray();

                final var x = requests.stream().map(r -> new double[]{r.getNumPixels()})
                        .toArray(double[][]::new);

                final var normalizedInput = normalize(x, new double[]{numPixels});

                regression.newSampleData(y, x);

                return (long) regression.predict(normalizedInput);
            }
            case "/raytracer": {
                final Map<String, String> parameters = URLEncodedUtils.parse(requestURI, Charset.defaultCharset())
                        .stream()
                        .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));

                System.out.println("parameters = " + parameters);

                int scols = Integer.parseInt(parameters.get("scols"));
                int srows = Integer.parseInt(parameters.get("srows"));
                int wcols = Integer.parseInt(parameters.get("wcols"));
                int wrows = Integer.parseInt(parameters.get("wrows"));
                int coff = Integer.parseInt(parameters.get("coff"));
                int roff = Integer.parseInt(parameters.get("roff"));

                final var regression = new CNVMultipleLinearRegression();
                final var requests = raytracerRequestMetricRepository.getAllRequests();

                if (requests.size() <= 1)
                    return DEFAULT_COMPLEXITY_RAYTRACER;

                final var y = requests.stream().mapToDouble(RaytracerRequestMetric::getInstructionCount).toArray();

                final var x = requests.stream().map(r -> new double[]{r.getScols(), r.getSrows(), r.getWcols(), r.getWrows(), r.getCoff(), r.getRoff()})
                        .toArray(double[][]::new);

                final var input = new double[]{scols, srows, wcols, wrows, coff, roff};

                final var normalizedInput = normalize(x, input);

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

    private double[] normalize(final double[][] x, final double[] input) {
        final var normalizedInput = new double[x[0].length];

        for (int col = 0; col < x[0].length; col++) {
            DescriptiveStatistics stats = new DescriptiveStatistics();

            for (final double[] sample : x)
                stats.addValue(sample[col]);

            double mean = stats.getMean();
            double standardDeviation = stats.getStandardDeviation();

            applyFunctionToColumn(x, col, r -> {
                for (int i = 0; i < r.length; ++i) {
                    r[i] = (r[i] - mean) / (standardDeviation + EPSILON);
                }
            });

            normalizedInput[col] = (input[col] - mean) / (standardDeviation + EPSILON);
        }
        return normalizedInput;
    }

    /**
     * Redirect the request to the instance that will process it.
     *
     * @param exchange    The exchange
     * @param requestBody The request body
     * @param uri         The URI of the instance
     * @throws IOException If an error occurs
     */
    private void redirect(final HttpExchange exchange, final byte[] requestBody, final URI uri) throws IOException {
        final var connection = (HttpURLConnection) uri.toURL().openConnection();

        connection.setRequestMethod(exchange.getRequestMethod());

        exchange.getRequestHeaders().forEach((k, v) -> v.forEach(h -> connection.setRequestProperty(k, h)));

        connection.setDoOutput(true);

        try (var os = connection.getOutputStream()) {
            os.write(requestBody);
        }

        connection.connect();

        connection.getHeaderFields().forEach((k, v) -> {
            if (k != null)
                v.forEach(h -> exchange.getResponseHeaders().add(k, h));
        });

        exchange.sendResponseHeaders(connection.getResponseCode(), connection.getContentLengthLong());

        try (
                var os = exchange.getResponseBody();
                var is = connection.getInputStream()
        ) {
            is.transferTo(os);
        }

        connection.disconnect();
    }


    /**
     * Read an image from a byte array.
     *
     * @param data The byte array
     * @return The image
     * @throws IOException If an error occurs
     */
    private BufferedImage readImage(final byte[] data) throws IOException {
        // Result syntax: data:image/<format>;base64,<encoded image>
        String result = new String(data).lines().collect(Collectors.joining("\n"));
        String[] resultSplits = result.split(",");

        byte[] decoded = Base64.getDecoder().decode(resultSplits[1]);
        ByteArrayInputStream bais = new ByteArrayInputStream(decoded);

        return ImageIO.read(bais);
    }
}
