package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import org.objectweb.asm.MethodVisitor;



public class InstOrUninstChoosingMV extends MethodVisitor {

    private UninstrumentedCompatMV umv;

    /**
     * Decides weather to instrument a class or uninstrument by choosing a method visitor.
     * @param tmv
     * @param umv
     */
    public InstOrUninstChoosingMV(TaintPassingMV tmv, UninstrumentedCompatMV umv) {
        super(Configuration.ASM_VERSION, tmv);
        this.umv = umv;
    }

    public void disableTainting() {
        this.mv = umv;
    }

}
