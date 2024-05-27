package pt.ulisboa.tecnico.cnv;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.ImageProcessorRequestMetric;
import pt.ulisboa.tecnico.cnv.mss.imageprocessor.ImageProcessorRequestMetricRepository;
import pt.ulisboa.tecnico.cnv.mss.raytracer.RaytracerRequestMetric;
import pt.ulisboa.tecnico.cnv.mss.raytracer.RaytracerRequestMetricRepository;
import pt.ulisboa.tecnico.cnv.utils.AwsUtils;
import pt.ulisboa.tecnico.cnv.utils.ExceptionUtils;
import pt.ulisboa.tecnico.cnv.utils.Utils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.lambda.LambdaClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.cnv.AutoScaler.COMPLEX_REQUEST_THRESHOLD;
import static pt.ulisboa.tecnico.cnv.AutoScaler.DEFAULT_WORK_IMAGE_PROCESSOR;
import static pt.ulisboa.tecnico.cnv.AutoScaler.DEFAULT_WORK_RAYTRACER;
import static pt.ulisboa.tecnico.cnv.AutoScaler.MAX_WORKLOAD;

public class LoadBalancerHandler implements HttpHandler {
    private static final double EPSILON = 1e-6;
    private static final int PORT = 8000;
    public static final String RAYTRACER_PATH = "/raytracer";
    public static final String BLURIMAGE_PATH = "/blurimage";
    public static final String ENHANCEIMAGE_PATH = "/enhanceimage";

    private static final ObjectMapper mapper = new ObjectMapper();

    private final ImageProcessorRequestMetricRepository imageProcessorRequestMetricRepository;
    private final RaytracerRequestMetricRepository raytracerRequestMetricRepository;
    private final VMWorkersMonitor vmWorkersMonitor;

    private final Ec2Client ec2Client;
    private final LambdaClient lambdaClient;

    private static final Logger logger = Logger.getLogger(LoadBalancerHandler.class.getName());

    private final String blurArn = System.getenv("BLUR_LAMBDA_ARN");
    private final String enhanceArn = System.getenv("ENHANCE_LAMBDA_ARN");
    private final String raytracerArn = System.getenv("RAYTRACER_LAMBDA_ARN");

    public LoadBalancerHandler(
            final VMWorkersMonitor vmWorkersMonitor,
            RaytracerRequestMetricRepository raytracerRequestMetricRepository,
            final ImageProcessorRequestMetricRepository imageProcessorRequestMetricRepository,
            final Ec2Client ec2Client,
            final LambdaClient lambdaClient
    ) {
        this.vmWorkersMonitor = vmWorkersMonitor;
        this.raytracerRequestMetricRepository = raytracerRequestMetricRepository;
        this.imageProcessorRequestMetricRepository = imageProcessorRequestMetricRepository;
        this.ec2Client = ec2Client;
        this.lambdaClient = lambdaClient;
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        try {
            logger.info("Received request for " + exchange.getRequestURI());
            if (!exchange.getRequestMethod().equals(HttpPost.METHOD_NAME)) {
                exchange.sendResponseHeaders(HttpStatusCode.METHOD_NOT_ALLOWED, -1);
                return;
            }

            final var requestURI = exchange.getRequestURI();
            final var requestBody = exchange.getRequestBody().readAllBytes();

            final var requestWork = estimateComplexity(requestURI, requestBody);
            logger.info("Received request for " + requestURI + " with complexity " + requestWork);

            final var vmWorkerWithLeastWork = vmWorkersMonitor.getVmWorkers().values().stream()
                    .filter(vmWorker -> !vmWorker.isTerminating() && vmWorker.isInitialized())
                    .filter(vmWorker -> MAX_WORKLOAD - vmWorker.getWork() > requestWork || vmWorker.getNumRequests() == 0)
                    .min(Comparator.comparingDouble(VMWorker::getWork));

            // TODO: Add fault tolerance when for example we redirect to a terminating instance or smthg like that.
            if (vmWorkerWithLeastWork.isPresent()) {
                logger.info("Redirecting to VM worker: " + vmWorkerWithLeastWork.get().getInstance().instanceId());
                redirectToVMWorker(exchange, vmWorkerWithLeastWork.get(), requestURI, requestWork, requestBody);
                return;
            }

            final var initializingVmWorkerOpt = vmWorkersMonitor.getVmWorkers().values().stream()
                    .filter(vm -> !vm.isInitialized())
                    .findAny();

            logger.info("Initializing VM worker: " + initializingVmWorkerOpt.map(vm -> vm.getInstance().instanceId()).orElse("none"));

            if (requestWork > COMPLEX_REQUEST_THRESHOLD) {
                logger.info("Complex request, redirecting to a vm worker");
                if (initializingVmWorkerOpt.isPresent()) {
                    logger.info("An instance is initializing, waiting for it to be ready");
                    var initializingVmWorker = initializingVmWorkerOpt.get();
                    final var instance = AwsUtils.waitForInstance(ec2Client, initializingVmWorker.getInstance().instanceId());

                    initializingVmWorker.setInstance(instance);
                    initializingVmWorker.setInitialized(true);

                    redirectToVMWorker(exchange, initializingVmWorker, requestURI, requestWork, requestBody);
                } else {
                    logger.info("Launching a new instance to handle the request");
                    Instance instance;
                    VMWorker vmWorker;

                    synchronized (vmWorkersMonitor) {
                        instance = AwsUtils.launchInstance(ec2Client);
                        vmWorker = new VMWorker(instance);
                        vmWorkersMonitor.getVmWorkers().put(instance.instanceId(), vmWorker);
                    }

                    instance = AwsUtils.waitForInstance(ec2Client, instance.instanceId());
                    vmWorker.setInstance(instance);
                    vmWorker.setInitialized(true);

                    redirectToVMWorker(exchange, vmWorker, requestURI, requestWork, requestBody);
                }
            } else {
                logger.info("Simple request, redirecting to a lambda worker");
                if (initializingVmWorkerOpt.isEmpty()) {
                    logger.info("Launching a new instance to handle requests in a near future");
                    synchronized (vmWorkersMonitor) {
                        final var instance = AwsUtils.launchInstance(ec2Client);
                        final var vmWorker = new VMWorker(instance);
                        vmWorkersMonitor.getVmWorkers().put(instance.instanceId(), vmWorker);
                    }
                }

                redirectToLambdaWorker(exchange, requestURI, requestBody);
            }
        } catch (Exception e) {
            logger.severe(ExceptionUtils.getStackTrace(e));
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
        } finally {
            logger.info("Closing exchange");
            exchange.close();
        }
    }


    /**
     * Redirect the request to the instance that will process it.
     * It will increment the work of the instance and decrement it after the request is processed.
     */
    private void redirectToVMWorker(
            HttpExchange exchange,
            VMWorker instance,
            URI requestURI,
            long requestComplexity,
            byte[] requestBody
    ) throws URISyntaxException, IOException {
        final var instanceUri = new URI("http://" + instance.getInstance().publicDnsName() + ":" + PORT + requestURI);

        vmWorkersMonitor.addWork(instance, requestComplexity);
        try {
            final var connection = (HttpURLConnection) instanceUri.toURL().openConnection();

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
        } finally {
            vmWorkersMonitor.removeWork(instance, requestComplexity);
        }
    }


    /**
     * Redirect the request to a lambda worker, according to the request URI.
     */
    private void redirectToLambdaWorker(
            final HttpExchange exchange,
            final URI requestURI,
            final byte[] requestBody
    ) throws IOException {
        final String functionName;
        final Map<String, String> payload;
        final String outputFormat;

        switch (requestURI.getPath()) {
            case BLURIMAGE_PATH:
                functionName = blurArn;
                payload = encodeImageProcLambdaRequest(requestBody);
                outputFormat = payload.get("fileFormat");
                break;
            case ENHANCEIMAGE_PATH:
                functionName = enhanceArn;
                payload = encodeImageProcLambdaRequest(requestBody);
                outputFormat = payload.get("fileFormat");
                break;
            case RAYTRACER_PATH:
                functionName = raytracerArn;
                final Map<String, String> parameters = URLEncodedUtils.parse(requestURI, Charset.defaultCharset())
                        .stream()
                        .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
                payload = encodeRaytracerLambdaRequest(requestBody, parameters);
                outputFormat = "bmp";
                break;
            default:
                exchange.sendResponseHeaders(HttpStatusCode.BAD_REQUEST, -1);
                return;
        }

        final var requestJson = new GsonBuilder().disableHtmlEscaping().create().toJson(payload);

        final var response = lambdaClient.invoke(req -> req
                .functionName(functionName)
                .payload(SdkBytes.fromUtf8String(requestJson))
        );

        // TODO: Stream the response
        final var responseBody = response.payload().asByteArray();
        final var responseBodyStr = new String(responseBody);
        final var output = String.format("data:image/%s;base64,%s", outputFormat, responseBodyStr.substring(1, responseBodyStr.length() - 1));

        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(response.statusCode(), output.getBytes().length);

        try (var os = exchange.getResponseBody()) {
            os.write(output.getBytes());
        }
    }

    private Map<String, String> encodeRaytracerLambdaRequest(final byte[] requestBody, final Map<String, String> parameters) throws IOException {
        final Map<String, String> requestPayload = new HashMap<>(parameters);

        Map<String, Object> body = mapper.readValue(requestBody, new TypeReference<>() {
        });

        byte[] input = ((String) body.get("scene")).getBytes();
        byte[] texmap = null;
        if (body.containsKey("texmap")) {
            // Convert ArrayList<Integer> to byte[]
            ArrayList<Integer> texmapBytes = (ArrayList<Integer>) body.get("texmap");
            texmap = new byte[texmapBytes.size()];
            for (int i = 0; i < texmapBytes.size(); i++) {
                texmap[i] = texmapBytes.get(i).byteValue();
            }
        }

        requestPayload.put("input", Base64.getEncoder().encodeToString(input));
        if (texmap != null)
            requestPayload.put("texmap", Base64.getEncoder().encodeToString(texmap));

        return requestPayload;
    }

    private static Map<String, String> encodeImageProcLambdaRequest(final byte[] requestBody) {
        Map<String, String> requestPayload = new HashMap<>();
        String result = new String(requestBody).lines().collect(Collectors.joining("\n"));
        String[] resultSplits = result.split(",");

        final var format = resultSplits[0].split("/")[1].split(";")[0];

        requestPayload.put("fileFormat", format);
        requestPayload.put("body", resultSplits[1]);

        return requestPayload;
    }

    /**
     * Estimate the complexity of the request, based on the request type and its parameters.
     */
    private long estimateComplexity(final URI requestURI, final byte[] requestBody) throws IOException {
        switch (requestURI.getPath()) {
            case BLURIMAGE_PATH: {
                final var image = Utils.readImage(requestBody);
                final var numPixels = image.getWidth() * image.getHeight();

                final var regression = new CNVMultipleLinearRegression();
                final var requests = imageProcessorRequestMetricRepository.getAllRequests();

                if (requests.size() <= 1)
                    return DEFAULT_WORK_IMAGE_PROCESSOR;

                final var y = requests.stream().mapToDouble(ImageProcessorRequestMetric::getInstructionCount).toArray();

                final var x = requests.stream().map(r -> new double[]{r.getNumPixels()})
                        .toArray(double[][]::new);

                final var normalizedInput = normalize(x, new double[]{numPixels});

                regression.newSampleData(y, x);

                return (long) regression.predict(normalizedInput);
            }
            case RAYTRACER_PATH: {
                final Map<String, String> parameters = URLEncodedUtils.parse(requestURI, Charset.defaultCharset())
                        .stream()
                        .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));

                int scols = Integer.parseInt(parameters.get("scols"));
                int srows = Integer.parseInt(parameters.get("srows"));
                int wcols = Integer.parseInt(parameters.get("wcols"));
                int wrows = Integer.parseInt(parameters.get("wrows"));
                int coff = Integer.parseInt(parameters.get("coff"));
                int roff = Integer.parseInt(parameters.get("roff"));

                final var regression = new CNVMultipleLinearRegression();
                final var requests = raytracerRequestMetricRepository.getAllRequests();

                if (requests.size() <= 1)
                    return DEFAULT_WORK_RAYTRACER;

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

    private static void applyFunctionToColumn(final double[][] x, final int col, final Consumer<double[]> function) {
        final var colData = Arrays.stream(x).mapToDouble(row -> row[col]).toArray();

        function.accept(colData);

        for (int i = 0; i < x.length; ++i) {
            x[i][col] = colData[i];
        }
    }
}
