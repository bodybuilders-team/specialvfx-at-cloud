package pt.ulisboa.tecnico.cnv.javassist.tools;

import javassist.CtBehavior;
import javassist.bytecode.AccessFlag;

import java.util.List;

public class MethodAnalyser extends AbstractJavassistTool {

    public MethodAnalyser(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static boolean isStatic(int accflags) {
        return (accflags & AccessFlag.STATIC) != 0;
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);
        String modifiers = "";
        switch (behavior.getModifiers()) {
            case AccessFlag.PRIVATE:
                modifiers = "private";
                break;
            case AccessFlag.PUBLIC:
                modifiers = "public";
                break;
            case AccessFlag.PROTECTED:
                modifiers = "protected";
                break;
            default:
                modifiers = "(package)";
                break;
        }

        if (isStatic(behavior.getModifiers())) {
            modifiers += " static";
        }

        System.out.printf("[%s] %s %s%n", MethodAnalyser.class.getSimpleName(), modifiers, behavior.getLongName());
    }
}
