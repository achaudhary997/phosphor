package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FrameNode;

/**
 * Hanldes phosphor tagged methods so that they are hidden from the real jvm and all opcodes are executed
 * keeping in mind that there may be tags associated with variables.
 */
public class HidePhosphorFromASMCV extends ClassVisitor {
    private boolean upgradeVersion;

    public HidePhosphorFromASMCV(ClassVisitor cv, boolean upgradeVersion) {
        super(Configuration.ASM_VERSION, cv);
        this.upgradeVersion = upgradeVersion;
    }

    boolean enabled;
    String className;

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (upgradeVersion)
            version = 51;
        super.visit(version, access, name, signature, superName, interfaces);
        enabled = name.equals("org/springframework/core/type/classreading/AnnotationMetadataReadingVisitor");
        this.className = name;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (enabled && name.equals("visitMethod")) {
            mv = new MethodVisitor(Configuration.ASM_VERSION, mv) {
                @Override
                public void visitCode() {
                    super.visitCode();

                    super.visitVarInsn(Opcodes.ALOAD, 2);
                    // Pushes the value of variable $$PHOSPHORTAGGED into the stack
                    super.visitLdcInsn("$$PHOSPHORTAGGED");
                    // Check if it ends with $$PHOSPHORTAGGED
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false);
                    Label lbl = new Label(); // Create a new label 
                    /*
                     * Generates an if condition of the type <- Not sure guessed this
                     * if (!endWith("$$PHOSPHORTAGGED")) {
                     *      return null;
                     * } else {
                     *      goto lbl; <- To handle tag related data as it is tagged.
                     * }
                     */
                    super.visitJumpInsn(Opcodes.IFEQ, lbl);
                    super.visitInsn(Opcodes.ACONST_NULL);
                    super.visitInsn(Opcodes.ARETURN);
                    super.visitLabel(lbl);

                    NeverNullArgAnalyzerAdapter na = new NeverNullArgAnalyzerAdapter(className, access, name, descriptor, null);
                    na.visitCode();
                    FrameNode fn = TaintAdapter.getCurrentFrameNode(na);
                    fn.type = Opcodes.F_NEW;
                    fn.accept(this);

                }
            };
        }
        return mv;
    }
}
