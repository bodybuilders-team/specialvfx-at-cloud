package pt.ulisboa.tecnico.cnv.javassist.tools;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.analysis.ControlFlow;
import javassist.bytecode.analysis.ControlFlow.Block;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractJavassistTool implements ClassFileTransformer {

    private final List<String> packageNameList;
    private final String writeDestination;

    protected AbstractJavassistTool(List<String> packageNameList, String writeDestination) {
        this.packageNameList = packageNameList;
        this.writeDestination = writeDestination;
    }

    private static List<BasicBlock> getBasicBlocks(CtBehavior behavior) throws BadBytecode {
        CodeAttribute ca = behavior.getMethodInfo().getCodeAttribute();
        LineNumberAttribute ainfo = (LineNumberAttribute) ca.getAttribute(LineNumberAttribute.tag);
        List<BasicBlock> bbs = new ArrayList<>();

        for (Block block : new ControlFlow(behavior.getDeclaringClass(), behavior.getMethodInfo()).basicBlocks()) {
            int[] entrances = new int[block.incomings()];

            for (int i = 0; i < entrances.length; i++) {
                entrances[i] = block.incoming(i).position();
            }

            bbs.add(new BasicBlock(behavior, block.position(), ainfo.toLineNumber(block.position()), block.length(), entrances));
        }

        return bbs;
    }

    protected void transform(BasicBlock block) throws CannotCompileException {
    }

    protected void transform(CtBehavior behavior) throws Exception {
        for (BasicBlock bb : getBasicBlocks(behavior)) {
            transform(bb);
        }
    }

    protected void transform(CtClass clazz) throws Exception {
        for (CtClass nestedClazz : clazz.getDeclaredClasses()) {
            transform(nestedClazz);
        }
        for (CtBehavior behavior : clazz.getDeclaredBehaviors()) {
            if ((AccessFlag.ABSTRACT & behavior.getModifiers()) == 0) {
                transform(behavior);
            }
        }
    }

    public final byte[] transform(String canonicalClassName) {
        byte[] bytecode = null;
        CtClass cc = null;

        try {
            cc = ClassPool.getDefault().get(canonicalClassName);
            transform(cc);
            cc.writeFile(writeDestination);
            bytecode = cc.toBytecode();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cc.detach();
        }

        return bytecode;
    }

    @Override
    public final byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        String canonicalClassName = className.replaceAll("/", "\\.");

        for (String packageName : packageNameList) {
            if (canonicalClassName.startsWith(packageName)) {
                return transform(canonicalClassName);
            }
        }
        return classfileBuffer;
    }

    static class BasicBlock {

        /**
         * Method that contains the basic block.
         */
        final CtBehavior behavior;

        /**
         * Bytecode index corresponding to the start of the basic block.
         */
        final int position;

        /**
         * Closest source code line to the beginning of the basic block.
         */
        final int line;

        /**
         * Length of the basic block.
         */
        final int length;

        /**
         * Bytecode index of incoming basic blocks.
         */
        final int[] entrances;

        BasicBlock(CtBehavior behavior, int position, int line, int length, int[] entrances) {
            this.behavior = behavior;
            this.length = length;
            this.line = line;
            this.position = position;
            this.entrances = entrances;
        }

        public CtBehavior getBehavior() {
            return behavior;
        }

        public int getPosition() {
            return position;
        }

        public int getLine() {
            return line;
        }

        public int getLength() {
            return length;
        }
    }
}