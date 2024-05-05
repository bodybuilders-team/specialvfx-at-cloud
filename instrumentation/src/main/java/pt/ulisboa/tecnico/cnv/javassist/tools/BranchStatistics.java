package pt.ulisboa.tecnico.cnv.javassist.tools;

import javassist.CannotCompileException;
import javassist.CtBehavior;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BranchStatistics extends AbstractJavassistTool {

    /**
     * Maps method long names to a map of basic block positions to the number of executions.
     */
    private static final Map<String, Map<Integer, Integer>> counters = new HashMap<>();

    public BranchStatistics(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static void visitBasicBlock(String methodLongName, int position) {
        if (!counters.containsKey(methodLongName)) {
            counters.put(methodLongName, new HashMap<>());
        }

        if (!counters.get(methodLongName).containsKey(position)) {
            counters.get(methodLongName).put(position, 0);
        }

        counters.get(methodLongName).put(position, counters.get(methodLongName).get(position) + 1);
    }


    public static void printStatistics() {
        for (Map.Entry<String, Map<Integer, Integer>> method : counters.entrySet()) {
            for (Map.Entry<Integer, Integer> basicblock : method.getValue().entrySet()) {
                System.out.printf("[%s] %s basic block %s was called %s times%n", BranchStatistics.class.getSimpleName(), method.getKey(), basicblock.getKey(), basicblock.getValue());
            }
        }
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);

        if (behavior.getName().equals("main")) {
            behavior.insertAfter(String.format("%s.printStatistics();", BranchStatistics.class.getName()));
        }
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        for (int incoming : block.entrances) {
            System.out.printf("[%s] Basic block %s has an outgoing edge to basic block %s%n",
                    BranchStatistics.class.getSimpleName(), incoming, block.getPosition());
        }


        block.behavior.insertAt(block.line, String.format("%s.visitBasicBlock(\"%s\", %s);", BranchStatistics.class.getName(), block.getBehavior().getLongName(), block.getPosition()));
    }
}