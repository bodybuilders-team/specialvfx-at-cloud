package pt.ulisboa.tecnico.cnv.javassist.tools;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtMethod;
import pt.ulisboa.tecnico.cnv.javassist.Request;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Analyser for processing requests.
 * <p>
 * It keeps track of the basic block count and instruction count of each request.
 * Each request is stored in a map with the thread id as the key.
 * The request is then printed to a log file when it is completed.
 * <p>
 * A thread only processes one request at a time.
 */
public class RequestAnalyzer extends AbstractJavassistTool {
    public static final Map<Long, Request> threadRequests = new ConcurrentHashMap<>();

    public RequestAnalyzer(List<String> packageNameList, String writeDestination) {
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
     * Handle the request by storing it in the threadRequests map.
     *
     * @param request the request to be handled
     */
    public static void handleRequest(Request request) {
        threadRequests.put(Thread.currentThread().getId(), request);
        Long threadId = Thread.currentThread().getId();
        Request storedRequest = threadRequests.get(threadId);
        if (storedRequest != null) {
            storedRequest.setCompleted(true);
        }
    }

    /**
     * Increment the basic block count and instruction count of the current request.
     *
     * @param length the length of the basic block
     */
    public static void incBasicBlock(int length) {
        Request request = threadRequests.get(Thread.currentThread().getId());
        if (request != null) {
            // Iterate over the block mem accesses
            request.setBblCount(request.getBblCount() + 1);
            request.setInstructionCount(request.getInstructionCount() + length);
        }
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);

        if (behavior.getName().equals("process")) {
            behavior.insertBefore(String.format("%s.handleRequest(request);", RequestAnalyzer.class.getName()));
        }
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        if (!(block.behavior instanceof CtMethod method))
            return;

        block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s);", RequestAnalyzer.class.getName(), block.length));
    }

}
