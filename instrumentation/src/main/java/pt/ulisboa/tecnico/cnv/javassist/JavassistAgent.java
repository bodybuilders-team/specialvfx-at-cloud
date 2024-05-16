package pt.ulisboa.tecnico.cnv.javassist;

import pt.ulisboa.tecnico.cnv.javassist.tools.AbstractJavassistTool;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class JavassistAgent {

    private static final String TOOL_PACKAGE = "pt.ulisboa.tecnico.cnv.javassist.tools.";
    private static final Logger logger = Logger.getLogger(JavassistAgent.class.getName());

    private JavassistAgent() {
    }

    private static AbstractJavassistTool getTransformer(
            String toolName,
            List<String> packageNameList,
            String writeDestination
    ) throws Exception {
        Class<?> transformerClass = Class.forName(TOOL_PACKAGE + toolName);

        try {
            return (AbstractJavassistTool) transformerClass
                    .getDeclaredConstructor(List.class, String.class)
                    .newInstance(packageNameList, writeDestination);
        } catch (Exception e) {
            logger.severe("Failed to instantiate transformer: " + e.getMessage());
            throw e;
        }
    }

    /**
     * This method is invoked before the target 'main' method is invoked.
     *
     * @param agentArgs arguments passed to the agent
     * @param inst      the instrumentation instance
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        String[] argSplits = agentArgs.split(":");
        String toolName = argSplits[0];
        String packageNames = argSplits[1];
        String writeDestination = argSplits[2];
        List<String> packageNameList = Arrays.asList(packageNames.split(","));
        try {
            System.out.println("Adding transformer");
            inst.addTransformer(getTransformer(toolName, packageNameList, writeDestination), true);
        } catch (Exception e) {
            logger.severe("Failed to add transformer: " + e.getMessage());
        }
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }
}
