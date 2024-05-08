package pt.ulisboa.tecnico.cnv.javassist.tools;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import pt.ulisboa.tecnico.cnv.imageproc.ImageProcessingRequest;

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
public class ImageProcessingAnalyser extends AbstractJavassistTool {

    private static final String LOG_FILE = "icountparallel.txt";
    private static final Logger logger = Logger.getLogger(ImageProcessingAnalyser.class.getName());
    private static final Map<Long, ImageProcessingRequest> threadRequests = new ConcurrentHashMap<>();

    static {
        try {
            FileHandler fileHandler = new FileHandler(LOG_FILE);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ImageProcessingAnalyser(List<String> packageNameList, String writeDestination) {
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
        ImageProcessingRequest storedRequest = threadRequests.get(threadId);
        if (storedRequest != null) {
            storedRequest.setCompleted(true);
            storedRequest.setOpTime(opTime);
        }

        logger.info(String.format("[%s] Request from thread %s: %s%n", ImageProcessingAnalyser.class.getSimpleName(), threadId, storedRequest));
    }

    /**
     * Handle the request by storing it in the threadRequests map.
     *
     * @param request the request to be handled
     */
    public static void handleRequest(ImageProcessingRequest request) {
        threadRequests.put(Thread.currentThread().getId(), request);
    }

    /**
     * Increment the basic block count and instruction count of the current request.
     *
     * @param length the length of the basic block
     */
    public static void incBasicBlock(int length) {
        ImageProcessingRequest request = threadRequests.get(Thread.currentThread().getId());
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

            behavior.insertBefore(String.format("%s.handleRequest(request);", ImageProcessingAnalyser.class.getName()));

            StringBuilder builder = new StringBuilder();
            behavior.addLocalVariable("endTime", CtClass.longType);
            behavior.addLocalVariable("opTime", CtClass.longType);
            builder.append("endTime = System.nanoTime();");
            builder.append("opTime = endTime-startTime;");
            behavior.insertAfter(builder.toString());

            behavior.insertAfter(String.format("%s.finalizeRequest(opTime);", ImageProcessingAnalyser.class.getName()));
        }
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s);", ImageProcessingAnalyser.class.getName(), block.length));
    }
}
