package pt.ulisboa.tecnico.cnv.javassist.tools;

import javassist.CtBehavior;
import javassist.CtClass;

import java.util.List;

public class MethodExecutionTimer extends CodeDumper {

    public MethodExecutionTimer(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);
        behavior.addLocalVariable("startTime", CtClass.longType);
        behavior.insertBefore("startTime = System.nanoTime();");
        StringBuilder builder = new StringBuilder();
        behavior.addLocalVariable("endTime", CtClass.longType);
        behavior.addLocalVariable("opTime", CtClass.longType);
        builder.append("endTime = System.nanoTime();");
        builder.append("opTime = endTime-startTime;");
        builder.append(String.format("System.out.println(\"[%s] %s method call completed in: \" + opTime + \" ns!\");",
                this.getClass().getSimpleName(), behavior.getLongName()));
        behavior.insertAfter(builder.toString());
    }
}
