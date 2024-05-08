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

    public static void printStatistics(long opTime) {
        Long threadId = Thread.currentThread().getId();
        ImageProcessingRequest storedRequest = threadRequests.get(threadId);
        if (storedRequest != null) {
            storedRequest.setCompleted(true);
            storedRequest.setOpTime(opTime);
        }

        logger.info(String.format("[%s] Request from thread %s: %s%n", ImageProcessingAnalyser.class.getSimpleName(), threadId, storedRequest));
    }

    public static void handleRequest(ImageProcessingRequest request) {
        threadRequests.put(Thread.currentThread().getId(), request);
    }

    public static void incBasicBlock(int length) {
        ImageProcessingRequest request = threadRequests.get(Thread.currentThread().getId());
        if (request != null) {
            /*TODO: Eu nao entendo porque é que os setters nao estavam a funcionar, estão a criar algum tipo de recursividade que nao entendo,
                por isso comentei o setter e fiz o incremento diretamente na variavel*/
            request.bblCount++;
            request.instructionCount += length;
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

            behavior.insertAfter(String.format("%s.printStatistics(opTime);", ImageProcessingAnalyser.class.getName()));
        }
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s);", ImageProcessingAnalyser.class.getName(), block.length));
    }
}
