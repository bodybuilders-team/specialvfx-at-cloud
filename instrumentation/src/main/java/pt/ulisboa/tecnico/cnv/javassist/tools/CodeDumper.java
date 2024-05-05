package pt.ulisboa.tecnico.cnv.javassist.tools;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;

import java.util.List;

public class CodeDumper extends AbstractJavassistTool {

    public CodeDumper(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    @Override
    protected void transform(CtClass clazz) throws Exception {
        System.out.printf("[%s] Intercepting class %s%n", CodeDumper.class.getSimpleName(), clazz.getName());
        super.transform(clazz);
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        System.out.printf("[%s] Intercepting method %s%n", CodeDumper.class.getSimpleName(), behavior.getName());
        super.transform(behavior);
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        System.out.printf("[%s] Intercepting basicblock position=%s, length=%s, line=%s%n",
                CodeDumper.class.getSimpleName(), block.getPosition(), block.getLength(), block.getLine());
        super.transform(block);
    }
}
