package pt.ulisboa.tecnico.cnv.javassist.tools;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import pt.ulisboa.tecnico.cnv.shared.Request;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Analyser for image processing requests.
 * <p>
 * It keeps track of the basic block count and instruction count of each request.
 * Each request is stored in a map with the thread id as the key.
 * The request is then printed to a log file when it is completed.
 * <p>
 * A thread only processes one request at a time.
 */
public class RequestAnalyser extends AbstractJavassistTool {

    /* TODO: Should be the web server to log the requests and the metrics, not the tool
            To have your web server code access the statistics gathered by the instrumented code for each specific request, the instrumentation tool classes must expose those metrics, e.g., through public methods, that receive arguments that allow retrieving the metrics of each specific request.
            You should not make the instrumentation tools write to specific files or directly issue requests to cloud storage. It is not very good design to tie the instrumentation tools to a specific task of your project and to make it instrument a specific method (e.g., an image processing operation method) just to output metrics.
            It should be your WebServer class to get these metrics from the tool classes and write them to file (with the request parameters) and, later on, to cloud storage (MSS). Instrumentation code instruments and records metrics in a shared data structure. The WebServer reads the shared data structure and pushes data into a file or remote storage.
    */
    private static final String LOG_FILE = "icountparallel.txt";
    private static final Logger logger = Logger.getLogger(RequestAnalyser.class.getName());
    private static final Map<Long, Request> threadRequests = new ConcurrentHashMap<>();

    static {
        try {
            FileHandler fileHandler = new FileHandler(LOG_FILE);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public RequestAnalyser(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    /**
     * Finalize the request by setting the operation time and marking it as completed.
     * The request is then printed to the log.
     *
     * @param opTime the operation time
     */
    public static void finalizeRequest(long opTime) {
        Long threadId = Thread.currentThread().getId();
        Request storedRequest = threadRequests.get(threadId);
        if (storedRequest != null) {
            storedRequest.setCompleted(true);
            storedRequest.setOpTime(opTime);
        }

        logger.info(String.format("[%s] Request from thread %s: %s%n", RequestAnalyser.class.getSimpleName(), threadId, storedRequest));
    }

    /**
     * Handle the request by storing it in the threadRequests map.
     *
     * @param request the request to be handled
     */
    public static void handleRequest(Request request) {
        threadRequests.put(Thread.currentThread().getId(), request);
    }

    /**
     * Increment the basic block count and instruction count of the current request.
     *
     * @param length the length of the basic block
     */
    public static void incBasicBlock(int length) {
        Request request = threadRequests.get(Thread.currentThread().getId());
        if (request != null) {
            request.setBblCount(request.getBblCount() + 1);
            request.setInstructionCount(request.getInstructionCount() + length);
        }
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);

        if (behavior.getName().equals("process")) {
            behavior.addLocalVariable("startTime", CtClass.longType);
            behavior.insertBefore("startTime = System.nanoTime();");

            behavior.insertBefore(String.format("%s.handleRequest(request);", RequestAnalyser.class.getName()));

            StringBuilder builder = new StringBuilder();
            behavior.addLocalVariable("endTime", CtClass.longType);
            behavior.addLocalVariable("opTime", CtClass.longType);
            builder.append("endTime = System.nanoTime();");
            builder.append("opTime = endTime-startTime;");
            behavior.insertAfter(builder.toString());

            behavior.insertAfter(String.format("%s.finalizeRequest(opTime);", RequestAnalyser.class.getName()));
        }
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s);", RequestAnalyser.class.getName(), block.length));
    }
}
