package pt.ulisboa.tecnico.cnv.javassist;

import pt.ulisboa.tecnico.cnv.javassist.tools.AbstractJavassistTool;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;

public class JavassistAgent {

    private static AbstractJavassistTool getTransformer(
            String toolName,
            List<String> packageNameList,
            String writeDestination
    ) throws Exception {
        Class<?> transformerClass = Class.forName("pt.ulisboa.tecnico.cnv.javassist.tools." + toolName);

        return (AbstractJavassistTool) transformerClass
                .getDeclaredConstructor(List.class, String.class)
                .newInstance(packageNameList, writeDestination);
    }

    /**
     * This method is invoked before the target 'main' method is invoked.
     *
     * @param agentArgs arguments passed to the agent
     * @param inst      the instrumentation instance
     * @throws Exception if an error occurs
     */
    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        String[] argSplits = agentArgs.split(":");
        String toolName = argSplits[0];
        String packageNames = argSplits[1];
        String writeDestination = argSplits[2];
        List<String> packageNameList = Arrays.asList(packageNames.split(","));
        inst.addTransformer(getTransformer(toolName, packageNameList, writeDestination), true);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
        premain(agentArgs, inst);
    }
}
