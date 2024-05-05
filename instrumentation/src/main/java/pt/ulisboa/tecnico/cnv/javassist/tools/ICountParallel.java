package pt.ulisboa.tecnico.cnv.javassist.tools;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import pt.ulisboa.tecnico.cnv.imageproc.ImageProcessingRequest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ICountParallel extends AbstractJavassistTool {

    private static final String LOG_FILE = "icount.txt";
    private static final Map<Long, ImageProcessingRequest> threadRequests = new ConcurrentHashMap<>();

    public ICountParallel(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static void printStatistics(Long id) {
        ImageProcessingRequest request = threadRequests.get(id);
        if (request != null) {
            System.out.printf("[%s] Number of executed basic blocks: %s%n", ICountParallel.class.getSimpleName(), request.getBblCount());
            System.out.printf("[%s] Number of executed instructions: %s%n", ICountParallel.class.getSimpleName(), request.getInstructionCount());
        }
    }

    public static void printStatistics() {
        for (Map.Entry<Long, ImageProcessingRequest> entry : threadRequests.entrySet()) {
            printStatistics(entry.getKey());
        }
    }

    public static void handleRequest(ImageProcessingRequest request) {
        System.out.printf("[%s] Handling request: %s%n", ICountParallel.class.getSimpleName(), request);
        threadRequests.put(Thread.currentThread().getId(), request);
    }

    public static void updateMetrics(ImageProcessingRequest request) {
        System.out.printf("[%s] Updating metrics: %s%n", ICountParallel.class.getSimpleName(), request);
        ImageProcessingRequest storedRequest = threadRequests.get(Thread.currentThread().getId());
        if (storedRequest != null) {
            storedRequest.setBblCount(request.getBblCount());
            storedRequest.setInstructionCount(request.getInstructionCount());
            storedRequest.setCompleted(true);
        }
    }

    public static void incBasicBlock(int length) {
        ImageProcessingRequest request = threadRequests.get(Thread.currentThread().getId());
        if (request != null) {
            System.out.printf("[%s] Incrementing basic block: %s%n", ICountParallel.class.getSimpleName(), length);
            /*request.setBblCount(request.getBblCount() + 1);
            request.setInstructionCount(request.getInstructionCount() + length);*/
        }
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);

        if (behavior.getName().equals("process")) {
            behavior.insertBefore(String.format("%s.handleRequest(request);", ICountParallel.class.getName()));
            behavior.insertAfter(String.format("%s.updateMetrics(request);", ICountParallel.class.getName()));
        }

        if (behavior.getName().equals("main"))
            behavior.insertAfter(String.format("%s.printStatistics();", ICountParallel.class.getName()));
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s);", ICountParallel.class.getName(), block.length));
    }
}
