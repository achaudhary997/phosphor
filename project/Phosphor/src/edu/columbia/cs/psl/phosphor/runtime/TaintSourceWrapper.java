package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.*;

import java.lang.reflect.Array;

/**
 * This class handles dynamically doing source-based tainting.
 * <p>
 * If you want to replace the *value* dynamically, then: 1. extend this class 2.
 * Set Configuration.autoTainter to be an instance of your new
 * TaintSourceWrapper 3. Override autoTaint..., change the value, then call
 * super.autoTaint... in order to set the taint correctly
 * <p>
 * Example:
 * <p>
 * <p>
 * public TaintedIntWithObjTag autoTaint(TaintedIntWithObjTag ret, String
 * source, int argIdx) { ret.val = 100; //Change value to be 100 instead of
 * whatever it was normally ret = super.autoTaint(ret, source, argIdx); //will
 * set the taint return ret; }
 *
 * @author jon
 */
public class TaintSourceWrapper<T extends AutoTaintLabel> {

    public boolean shouldInstrumentMethodForImplicitLightTracking(String className, String methodName, String methodDescriptor) {
        return className.equals("edu/columbia/cs/psl/test/phosphor/SelectiveLightImplicitObjTagITCase") && methodName.equals("hasImplicitTracking");
    }

    /* For multi-tainting/object tags. */
    @SuppressWarnings({"unchecked", "unused"})
    public void combineTaintsOnArray(Object inputArray, Taint tag) {
        if (inputArray instanceof LazyArrayObjTags) {
            LazyArrayObjTags array = ((LazyArrayObjTags) inputArray);
            if (array.taints == null) {
                array.taints = new Taint[array.getLength()];
            }
            for (int i = 0; i < array.getLength(); i++) {
                if (array.taints[i] == null) {
                    array.taints[i] = tag.copy();
                } else {
                    array.taints[i].addDependency(tag);
                }
            }
        } else if (inputArray instanceof Object[]) {
            for (int i = 0; i < ((Object[]) inputArray).length; i++) {
                Object o = ((Object[]) inputArray)[i];
                if (o instanceof TaintedWithObjTag) {
                    Taint existing = (Taint) ((TaintedWithObjTag) o).getPHOSPHOR_TAG();
                    if (existing != null) {
                        existing.addDependency(tag);
                    } else {
                        ((TaintedWithObjTag) o).setPHOSPHOR_TAG(tag.copy());
                    }
                }
            }
        }
    }

    /* For int tags. */
    @SuppressWarnings("unused")
    public void combineTaintsOnArray(Object obj, int tag) {
        if (obj instanceof LazyArrayIntTags) {
            ((LazyArrayIntTags) obj).setTaints(tag);
        } else if (obj instanceof Object[]) {
            for (int i = 0; i < ((Object[]) obj).length; i++) {
                if (((Object[]) obj)[i] instanceof TaintedWithIntTag) {
                    ((TaintedWithIntTag) ((Object[]) obj)[i]).setPHOSPHOR_TAG(tag);
                }
            }
        }
    }

    /* Called by taintThrough methods. */
    @SuppressWarnings({"unchecked", "unused"})
    public void addTaint(Object obj, Taint<? extends AutoTaintLabel> tag) {
        if (tag == null || obj == null) {
            return;
        }
        if (obj instanceof LazyArrayObjTags) {
            combineTaintsOnArray(obj, tag);
        } else if (obj instanceof TaintedWithObjTag) {
            TaintedWithObjTag tainted = (TaintedWithObjTag) obj;
            Taint prevTag = (Taint) tainted.getPHOSPHOR_TAG();
            if (prevTag != null) {
                prevTag.addDependency(tag);
            } else {
                tainted.setPHOSPHOR_TAG(tag);
            }
        } else if (obj instanceof TaintedPrimitiveWithObjTag) {
            TaintedPrimitiveWithObjTag tainted = (TaintedPrimitiveWithObjTag) obj;
            if (tainted.taint != null) {
                tainted.taint.addDependency(tag);
            } else {
                tainted.taint = tag;
            }
            autoTaint((TaintedPrimitiveWithObjTag) obj, tag);
        } else if (obj.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(obj); i++) {
                addTaint(Array.get(obj, i), tag);
            }
        }
    }

    /* Called by sources for the arguments and return value. */
    @SuppressWarnings("unused")
    public static void autoTaint(Object obj, int tag) {
        if (obj instanceof LazyArrayIntTags) {
            ((LazyArrayIntTags) obj).setTaints(tag);
        } else if (obj instanceof Object[]) {
            for (int i = 0; i < ((Object[]) obj).length; i++) {
                autoTaint(((Object[]) obj)[i], tag);
            }
        } else if (obj instanceof TaintedWithIntTag) {
            ((TaintedWithIntTag) obj).setPHOSPHOR_TAG(tag);
        } else if (obj instanceof TaintedPrimitiveWithIntTag) {
            ((TaintedPrimitiveWithIntTag) obj).taint = tag;
        }
    }

    public Taint<AutoTaintLabel> generateTaint(String source) {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        StackTraceElement[] s = new StackTraceElement[st.length - 3];
        System.arraycopy(st, 3, s, 0, s.length);
        return new Taint<>(new AutoTaintLabel(source, s));
    }

    /* Called by sources for the arguments and return value. */
    @SuppressWarnings("unused")
    public Object autoTaint(Object obj, String baseSource, String actualSource, int argIdx) {
        return autoTaint(obj, generateTaint(baseSource));
    }

    /* Adds the specified tag to the specified object. */
    public Object autoTaint(Object obj, Taint<? extends AutoTaintLabel> tag) {
        if (obj == null) {
            return null;
        } else if (obj instanceof LazyArrayObjTags) {
            return autoTaint((LazyArrayObjTags) obj, tag);
        } else if (obj instanceof TaintedWithObjTag) {
            return autoTaint((TaintedWithObjTag) obj, tag);
        } else if (obj instanceof TaintedPrimitiveWithObjTag) {
            return autoTaint((TaintedPrimitiveWithObjTag) obj, tag);
        } else if (obj.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(obj); i++) {
                Array.set(obj, i, autoTaint(Array.get(obj, i), tag));
            }
            return obj;
        }
        return obj;
    }

    @SuppressWarnings("unchecked")
    public TaintedWithObjTag autoTaint(TaintedWithObjTag ret, Taint<? extends AutoTaintLabel> tag) {
        Taint prevTag = (Taint) ret.getPHOSPHOR_TAG();
        if (prevTag != null) {
            prevTag.addDependency(tag);
        } else {
            ret.setPHOSPHOR_TAG(tag);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public LazyArrayObjTags autoTaint(LazyArrayObjTags ret, Taint<? extends AutoTaintLabel> tag) {
        Taint[] taintArray = ret.taints;
        if (taintArray != null) {
            for (int i = 0; i < taintArray.length; i++) {
                if (taintArray[i] == null)
                    taintArray[i] = tag.copy();
                else
                    taintArray[i].addDependency(tag);
            }
        } else {
            ret.setTaints(tag);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public TaintedPrimitiveWithObjTag autoTaint(TaintedPrimitiveWithObjTag ret, Taint<? extends AutoTaintLabel> tag) {
        if (ret.taint != null)
            ret.taint.addDependency(tag);
        else
            ret.taint = tag;
        return ret;
    }

    public static void setStringValueTag(String str, LazyCharArrayObjTags tags) {
        if (str != null) {
            str.valuePHOSPHOR_TAG = tags;
        }
    }

    public static LazyCharArrayObjTags getStringValueTag(String str) {
        if (str == null) {
            return null;
        } else {
            return str.valuePHOSPHOR_TAG;

        }
    }

    public static Taint[] getStringValueTaints(String str) {
        return getStringValueTag(str).taints;
    }

    /* Called by sink methods. */
    @SuppressWarnings("unused")
    public void checkTaint(Object self, Object[] arguments, String baseSink, String actualSink) {
        if (arguments != null) {
            for (Object argument : arguments) {
                checkTaint(argument, baseSink, actualSink);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void checkTaint(Object obj, String baseSink, String actualSink) {
        if (obj instanceof String) {
            Taint[] taints = getStringValueTaints((String) obj);
            if (taints != null) {
                SimpleHashSet<String> reported = new SimpleHashSet<>();
                for (Taint t : taints) {
                    if (t != null) {
                        String _t = new String(t.toString().getBytes());
                        if (reported.add(_t))
                            taintViolation(t, obj, baseSink, actualSink);
                    }
                }
            }
        } else if (obj instanceof TaintedWithIntTag) {
            checkTaint(((TaintedWithIntTag) obj).getPHOSPHOR_TAG(), actualSink);
        } else if (obj instanceof TaintedWithObjTag) {
            if (((TaintedWithObjTag) obj).getPHOSPHOR_TAG() != null) {
                taintViolation((Taint<T>) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG(), obj, baseSink, actualSink);
            }
        } else if (obj instanceof LazyArrayIntTags) {
            checkTaints(((LazyArrayIntTags) obj).taints, actualSink);
        } else if (obj instanceof LazyArrayObjTags) {
            LazyArrayObjTags tags = ((LazyArrayObjTags) obj);
            if (tags.taints != null) {
                for (Object i : tags.taints) {
                    if (i != null)
                        taintViolation((Taint<T>) i, obj, baseSink, actualSink);
                }
            }
        } else if (obj instanceof Object[]) {
            for (Object o : ((Object[]) obj)) {
                checkTaint(o, baseSink, actualSink);
            }
        } else if (obj instanceof ControlTaintTagStack) {
            ControlTaintTagStack ctrl = (ControlTaintTagStack) obj;
            if (ctrl.taint != null && !ctrl.isEmpty()) {
                taintViolation((Taint<T>) ctrl.taint, obj, baseSink, actualSink);
            }
        } else if (obj instanceof TaintedPrimitiveWithObjTag) {
            if (((TaintedPrimitiveWithObjTag) obj).taint != null) {
                taintViolation(((TaintedPrimitiveWithObjTag) obj).taint, ((TaintedPrimitiveWithObjTag) obj).getValue(), baseSink, actualSink);
            }
        } else if (obj instanceof TaintedPrimitiveWithIntTag) {
            checkTaint(((TaintedPrimitiveWithIntTag) obj).taint, actualSink);
        }
    }

    public void taintViolation(Taint<T> tag, Object obj, String baseSink, String actualSink) {
        throw new TaintSinkError(tag, obj);
    }

    public void checkTaint(int tag, String actualSink) {
        if (tag != 0) {
            throw new IllegalAccessError("Argument carries taint " + tag + " at " + actualSink);
        }
    }

    public void checkTaints(int[] tags, String actualSink) {
        if (tags != null) {
            for (int tag : tags) {
                checkTaint(tag, actualSink);
            }
        }
    }

    /* Called just before a sink method returns. */
    public void exitingSink(String baseSink, String actualSink) {
        return;
    }

    /* Called after a sink method makes its calls to checkTaint but before the rest of the method body executes. */
    public void enteringSink(String baseSink, String actualSink) {
        return;
    }
}
