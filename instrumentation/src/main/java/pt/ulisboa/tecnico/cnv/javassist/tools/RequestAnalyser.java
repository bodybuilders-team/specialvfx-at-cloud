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
    private static final Map<Long, Request> threadRequests = new ConcurrentHashMap<>();

    public RequestAnalyser(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    /**
     * Get the request associated with the current thread.
     *
     * @return the request associated with the current thread
     */
    public static Request getThreadRequest(Long threadId) {
        return threadRequests.get(threadId);
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
