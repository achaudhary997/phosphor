package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.TaggedValue;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Remaps old argument index's to some new arguments. Guess: Adds indexes for taint(s) of the corressponding variables.
 */ 
public class MethodArgReindexer extends MethodVisitor {
    int originalLastArgIdx;
    int[] oldArgMappings;
    int[] origArgMappings;
    int newArgOffset;
    boolean isStatic;
    int origNumArgs;
    String name;
    String desc;
    boolean hasTaintSentinalAddedToDesc = false;
    ArrayList<Type> oldArgTypesList;
    Type[] oldArgTypes;
    MethodNode lvStore;
    int nNewArgs = 0;

    Type[] firstFrameLocals;
    int idxOfReturnPrealloc;

    boolean hasPreAllocedReturnAddr;
    Type newReturnType;

    ArrayList<Type> oldTypesDoublesAreOne;
    boolean isLambda;

    int nLongDoubleArgs = 0;
    int nLVTaintsCounted = 0;
    boolean returnLVVisited = false;
    HashMap<String, Integer> parameters = new HashMap<>();
    int line;
    int indexOfControlTagsInLocals;

    /**
     * Remaps old argument index's to some new arguments. Guess: Adds indexes for taint(s) of the corressponding variables.
     * @param mv
     * @param access
     * @param name
     * @param desc
     * @param originalDesc
     * @param lvStore
     * @param isLambda
     */
    public MethodArgReindexer(MethodVisitor mv, int access, String name, String desc, String originalDesc, MethodNode lvStore, boolean isLambda) {
        super(Configuration.ASM_VERSION, mv);
        this.lvStore = lvStore;
        this.isLambda = isLambda;
        lvStore.localVariables = new ArrayList<LocalVariableNode>();
        this.name = name;
        this.desc = desc;
        oldArgTypes = Type.getArgumentTypes(originalDesc);
        origNumArgs = oldArgTypes.length;
        isStatic = (Opcodes.ACC_STATIC & access) != 0;
        for (Type t : oldArgTypes)
            originalLastArgIdx += t.getSize();
        if (!isStatic)
            originalLastArgIdx++;
        if (!isStatic)
            origNumArgs++;
        newArgOffset = 0;
//		System.out.println(name+originalDesc + " -> origLastArg is " + originalLastArgIdx + "orig nargs " + origNumArgs);
        oldArgTypesList = new ArrayList<Type>();
        oldTypesDoublesAreOne = new ArrayList<Type>();
        if (!isStatic) {
            oldArgTypesList.add(Type.getType("Lthis;"));
            oldTypesDoublesAreOne.add(Type.getType("Lthis;"));
        }
        firstFrameLocals = new Type[origNumArgs];
        int ffl = 0;
        if (!isStatic) {
            firstFrameLocals[0] = Type.getObjectType("java/lang/Object");
            ffl++;
        }
        for (Type t : Type.getArgumentTypes(originalDesc)) {
            oldArgTypesList.add(t);
            oldTypesDoublesAreOne.add(t);
            firstFrameLocals[ffl] = t;
            ffl++;
            if (t.getSize() == 2) {
                oldArgTypesList.add(Type.getType("LTOP;"));
                nLongDoubleArgs++;
            }
        }

//		System.out.println("OLd:::" + oldTypesDoublesAreOne);
        boolean hasBeenRemapped = false;
        oldArgMappings = new int[originalLastArgIdx + 1];
        int oldVarCount = (isStatic ? 0 : 1);
        for (int i = 0; i < oldArgTypes.length; i++) {
            if (!isLambda) {
                if (oldArgTypes[i].getSort() == Type.ARRAY) {
                    if (oldArgTypes[i].getElementType().getSort() != Type.OBJECT) {
                        if (oldArgTypes[i].getDimensions() == 1) {
                            newArgOffset++;
                            nNewArgs++;
                        }
                        hasBeenRemapped = true;
                    }
                } else if (oldArgTypes[i].getSort() != Type.OBJECT) {
                    hasBeenRemapped = true;
                    newArgOffset += 1;
                    nNewArgs++;
                }
            }
            oldArgMappings[oldVarCount] = oldVarCount + newArgOffset;
            if (oldArgTypes[i].getSize() == 2) {
                oldArgMappings[oldVarCount + 1] = oldVarCount + newArgOffset + 1;
                oldVarCount++;
            }
            if (TaintUtils.DEBUG_LOCAL)
                System.out.println(">>>>" + oldVarCount + "->" + oldArgMappings[oldVarCount]);
            oldVarCount++;
        }
        if ((Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING) && !name.equals("<clinit>")) {
            hasBeenRemapped = true;
            indexOfControlTagsInLocals = oldArgTypes.length + newArgOffset + (isStatic ? 0 : 1);
            newArgOffset++;
        }
        if (name.equals("<init>") && hasBeenRemapped) {
            hasTaintSentinalAddedToDesc = true;
            newArgOffset++;
            nNewArgs++;
        }
        hasPreAllocedReturnAddr = TaintUtils.isPreAllocReturnType(originalDesc);
        if (hasPreAllocedReturnAddr) {
            newReturnType = Type.getReturnType(desc);
            newArgOffset++;
            nNewArgs++;
            idxOfReturnPrealloc = originalLastArgIdx + newArgOffset - 1;
        }
//		System.out.println(name+desc+"NEWARGOFFSET: "  + newArgOffset);
        origArgMappings = new int[oldArgMappings.length];
        System.arraycopy(oldArgMappings, 0, origArgMappings, 0, oldArgMappings.length);
        if (TaintUtils.DEBUG_FRAMES || TaintUtils.DEBUG_LOCAL)
            System.out.println(name + " origLastArg " + originalLastArgIdx + ", oldvarcount = " + oldVarCount + ", newargoffset " + newArgOffset);
    }

    public int getNewArgOffset() {
        return newArgOffset;
    }

    @Override
    public void visitParameter(String name, int access) {
        super.visitParameter(name, access);
        parameters.put(name, access);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        if (parameters.size() > 0) {
            //Add fake params
            for (int i = 0; i < nNewArgs; i++) {
                super.visitParameter("Phosphor$$Param$$" + i, 0);
            }
        }
    }

    /**
     * Visits the local variable keeping in mind that taints are kept along with them on the stack
     */
    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        if (index < originalLastArgIdx) {
            boolean found = false;
            for (Object _lv : lvStore.localVariables) {
                LocalVariableNode lv = (LocalVariableNode) _lv;
                if (lv != null && lv.name != null && lv.name.equals(name) && lv.index == index)
                    found = true;
            }
            if (!found)
                lvStore.localVariables.add(new LocalVariableNode(name, desc, signature, null, null, index));
        }
        // If "this" variable.
        if (!isStatic && index == 0)
            super.visitLocalVariable(name, desc, signature, start, end, index);
        else if (index < originalLastArgIdx) {
            String shadow = TaintUtils.getShadowTaintType(desc);
            if (shadow != null)
                super.visitLocalVariable(name + TaintUtils.METHOD_SUFFIX, shadow, null, start, end, origArgMappings[index] - 1);
            super.visitLocalVariable(name, desc, signature, start, end, origArgMappings[index]);
            if (index == originalLastArgIdx - 1 && (Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING)) {
                super.visitLocalVariable("Phopshor$$ImplicitTaintTrackingFromParent", Type.getDescriptor(ControlTaintTagStack.class), null, start, end, origArgMappings[index] + 1);

            }
            if (index == originalLastArgIdx - 1 && this.name.equals("<init>") && hasTaintSentinalAddedToDesc) {
                super.visitLocalVariable("Phosphor$$TaintSentinel", "Ljava/lang/Object;", null, start, end, origArgMappings[index] + ((Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING) ? 2 : 1));
            }
            if ((index == originalLastArgIdx - Type.getType(desc).getSize()) && hasPreAllocedReturnAddr) {
                super.visitLocalVariable("Phosphor$$ReturnPreAllocated", newReturnType.getDescriptor(), null, start, end, origArgMappings[index] + ((Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING) ? 2 : 1));
            }
        } else {
            super.visitLocalVariable(name, desc, signature, start, end, index + newArgOffset);
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
//		this.line = line;
        this.debug = line == 182;
    }


    boolean debug = false;

    @Override
    public void visitLdcInsn(Object value) {
        super.visitLdcInsn(value);
//		if (value.equals(604800000L)) {
//			debug = true;
//		}
    }

    /**
     * 
     * @param obj
     * @return the type of object passed in as argument.
     */
    public static Type getTypeForStackTypeTOPAsNull(Object obj) {
        if (obj instanceof TaggedValue)
            obj = ((TaggedValue) obj).v;
        if (obj == Opcodes.INTEGER)
            return Type.INT_TYPE;
        if (obj == Opcodes.FLOAT)
            return Type.FLOAT_TYPE;
        if (obj == Opcodes.DOUBLE)
            return Type.DOUBLE_TYPE;
        if (obj == Opcodes.LONG)
            return Type.LONG_TYPE;
        if (obj == Opcodes.NULL)
            return Type.getType("Ljava/lang/Object;");
        if (obj == Opcodes.TOP)
            return Type.getType("Ljava/lang/Object;");
        if (obj instanceof String)
            if (!(((String) obj).charAt(0) == '[') && ((String) obj).length() > 1)
                return Type.getType("L" + obj + ";");
            else
                return Type.getType((String) obj);
        if (obj instanceof Label || obj == Opcodes.UNINITIALIZED_THIS)
            return Type.getType("Luninitialized;");
        throw new IllegalArgumentException("got " + obj + " zzz" + obj.getClass());
    }

    /**
     * Handles remapped indexing creates shadow variables for primitive types and primitive array.  Creates a new stack also
     * but not sure why as it is never returned/used.
     */
    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        Object[] remappedLocals = new Object[Math.max(local.length, origNumArgs) + newArgOffset + 1 + nLongDoubleArgs]; //was +1, not sure why??
        if (TaintUtils.DEBUG_FRAMES) {
            System.out.println(name + desc + " orig nArgs = " + origNumArgs);
            System.out.println("Pre-reindex Frame: " + Arrays.toString(local) + ";" + nLocal + " ; " + Arrays.toString(stack) + nStack);
        }
        int nLocalsInputFrame = nLocal;
        oldArgMappings = new int[originalLastArgIdx + 1];
        if (type == Opcodes.F_FULL || type == Opcodes.F_NEW) {
            int thisLocalVarNumberInNewFrame = 0; //accounts for long/double
            int thisLocalVarNumberInOldFrame = 0;
            int thisLocalIndexInNewFrame = 0; //does not account for long/double
            int thisLocalIndexInOldFrame = 0;
            //Special cases of no args
            if (origNumArgs == 0) {
                if ((Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING) && !name.equals("<clinit>")) {
                    remappedLocals[thisLocalIndexInNewFrame] = Type.getInternalName(ControlTaintTagStack.class);
                    thisLocalIndexInNewFrame++;
                    nLocal++;
                }
                if (hasPreAllocedReturnAddr) {
                    remappedLocals[thisLocalIndexInNewFrame] = newReturnType.getInternalName();
                    thisLocalIndexInNewFrame++;
                    nLocal++;
                }
            }

            //Iterate over every LV slot. Some LV slots may be high end of 2-word vars.
            for (int i = 0; i < oldArgTypesList.size() && local.length > 0; i++) {
                //for each local in a slot that is mapping to an arg
                Object thisLocalTypeObjNew = null;

                if (thisLocalIndexInOldFrame < local.length)
                    thisLocalTypeObjNew = local[thisLocalIndexInOldFrame];
                else
                    thisLocalTypeObjNew = Opcodes.TOP;
                //check and see what type was here before
                Type thisLocalTypeOld = oldArgTypesList.get(i);
                Type thisLocalTypeNew = getTypeForStackTypeTOPAsNull(thisLocalTypeObjNew);

                if (thisLocalTypeOld.getDescriptor().equals("LTOP;") && thisLocalIndexInOldFrame > 0 &&
                        thisLocalIndexInOldFrame - 1 < local.length &&
                        getTypeForStackTypeTOPAsNull(local[thisLocalIndexInOldFrame - 1]).getSize() == 2) {
                    continue;
                }
                //add taint storage if WAS originally a primitive or primitive array
                if (TaintUtils.isPrimitiveOrPrimitiveArrayType(thisLocalTypeOld)) {
                    if (TaintUtils.isPrimitiveOrPrimitiveArrayType(thisLocalTypeNew)) {
                        //Add the shadow type
                        remappedLocals[thisLocalIndexInNewFrame] = TaintUtils.getShadowTaintTypeForFrame(thisLocalTypeNew);
                        thisLocalIndexInNewFrame++;
                        thisLocalVarNumberInNewFrame++;
                        nLocal++;
                    } else {
                        //add TOP
                        remappedLocals[thisLocalIndexInNewFrame] = Opcodes.TOP;
                        thisLocalIndexInNewFrame++;
                        thisLocalVarNumberInNewFrame++;
                        nLocal++;
                    }
                } else if (TaintUtils.isPrimitiveOrPrimitiveArrayType(thisLocalTypeNew)) {
                    thisLocalTypeObjNew = new TaggedValue(thisLocalTypeObjNew);
                }

                if (thisLocalIndexInNewFrame < remappedLocals.length)
                    remappedLocals[thisLocalIndexInNewFrame] = thisLocalTypeObjNew;

                if (thisLocalTypeNew.getSort() == Type.ARRAY && thisLocalTypeNew.getDimensions() > 1 && thisLocalTypeNew.getElementType().getSort() != Type.OBJECT && thisLocalIndexInNewFrame < remappedLocals.length) {
                    remappedLocals[thisLocalIndexInNewFrame] = MultiDTaintedArray.getTypeForType(Type.getObjectType((String) local[thisLocalIndexInOldFrame])).getInternalName();
                }


                //Increment counts for this var
                oldArgMappings[thisLocalVarNumberInOldFrame] = thisLocalVarNumberInNewFrame;
                if (thisLocalTypeNew.getSize() == 2)
                    oldArgMappings[thisLocalVarNumberInOldFrame + 1] = thisLocalVarNumberInNewFrame + 1;
                thisLocalIndexInNewFrame++;
                thisLocalIndexInOldFrame++;
                thisLocalVarNumberInNewFrame += thisLocalTypeNew.getSize();
                thisLocalVarNumberInOldFrame += thisLocalTypeNew.getSize();


            }
            if (origNumArgs != 0 && (Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING)) {
                while (thisLocalIndexInNewFrame < indexOfControlTagsInLocals) //There are no locals in this frame, BUT there were args on the method - make sure metadata goes to the right spot
                {
                    remappedLocals[thisLocalIndexInNewFrame] = Opcodes.TOP;
                    thisLocalIndexInNewFrame++;
                    nLocal++;
                }
                remappedLocals[thisLocalIndexInNewFrame] = Type.getInternalName(ControlTaintTagStack.class);
                thisLocalIndexInNewFrame++;
                thisLocalVarNumberInNewFrame++;
                nLocal++;
            }
            if (origNumArgs != 0 && hasPreAllocedReturnAddr) {

                remappedLocals[thisLocalIndexInNewFrame] = newReturnType.getInternalName();
                thisLocalIndexInNewFrame++;
                thisLocalVarNumberInNewFrame++;
                nLocal++;
            }
            if (hasTaintSentinalAddedToDesc) {
                remappedLocals[thisLocalIndexInNewFrame] = Opcodes.TOP;
                thisLocalIndexInNewFrame++;
                thisLocalVarNumberInNewFrame++;
                nLocal++;
            }
            for (int i = thisLocalIndexInOldFrame; i < nLocalsInputFrame; i++) {
                remappedLocals[thisLocalIndexInNewFrame] = local[i];
                Type t = getTypeForStackTypeTOPAsNull(local[i]);
                if (t.getSort() == Type.ARRAY && t.getDimensions() > 1 && t.getElementType().getSort() != Type.OBJECT) {
                    remappedLocals[thisLocalIndexInNewFrame] = MultiDTaintedArray.getTypeForType(Type.getObjectType((String) local[i])).getInternalName();
                }
                thisLocalIndexInNewFrame++;
                thisLocalVarNumberInNewFrame += t.getSize();
                thisLocalVarNumberInOldFrame += t.getSize();
            }

        } else {
            remappedLocals = local;
        }

        if (nLocal > remappedLocals.length)
            throw new IllegalStateException();
//		if(debug)
//		System.out.println("New locals : " + name + desc + ":\t\t" + Arrays.toString(remappedLocals));
        ArrayList<Object> newStack = new ArrayList<Object>();
        int origNStack = nStack;
        for (int i = 0; i < origNStack; i++) {
            if (stack[i] instanceof TaggedValue) {
                Object o = ((TaggedValue) stack[i]).v;
                if (o instanceof String || o == Opcodes.NULL) {
                    if (o == Opcodes.NULL)
                        newStack.add(Opcodes.NULL);
                    else
                        newStack.add(TaintUtils.getShadowTaintTypeForFrame((String) (o)));
                    nStack++;
                } else {
                    newStack.add(Configuration.TAINT_TAG_STACK_TYPE);
                    nStack++;
                }
                newStack.add(stack[i]);
            } else if (stack[i] != Opcodes.TOP && stack[i] instanceof String && ((String) stack[i]).charAt(1) == '[' && Type.getObjectType((String) stack[i]).getElementType().getSort() != Type.OBJECT) {
                // If primitive array type
                newStack.add(MultiDTaintedArray.getTypeForType(Type.getObjectType((String) stack[i])).getInternalName());
            } else // Else simply add
                newStack.add(stack[i]);
        }
        Object[] stack2 = new Object[newStack.size()];
        stack2 = newStack.toArray();
        if (TaintUtils.DEBUG_FRAMES)
            System.out.println("Post-adjust Frame: " + Arrays.toString(remappedLocals) + ";" + Arrays.toString(stack2));
        super.visitFrame(type, nLocal, remappedLocals, nStack, stack2);
        if (TaintUtils.DEBUG_FRAMES)
            System.out.println("Post-visit Frame: " + Arrays.toString(remappedLocals) + ";" + Arrays.toString(stack2));
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        if (!isStatic)
            parameter++;
        int remappedVar = parameter;
        if (parameter < originalLastArgIdx) {
            remappedVar = oldArgMappings[parameter];
        } else {
            remappedVar += newArgOffset;
        }
        if (!isStatic)
            remappedVar--;
        return super.visitParameterAnnotation(remappedVar, descriptor, visible);
    }

    /**
     * If accessing an arg then get the new mapped index and call super on that.
     */
    @Override
    public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
        if (!isStatic)
            parameterCount++;
        int remappedVar = parameterCount;
        if (parameterCount < originalLastArgIdx) {
            remappedVar = oldArgMappings[parameterCount];
        } else {
            // Not an argument so just add an offset and let it go.
            remappedVar += newArgOffset;
        }
        if (!isStatic)
            remappedVar--;
        super.visitAnnotableParameterCount(remappedVar, visible);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        int origVar = var;
        if (!isStatic && var == 0)
            var = 0;
        else if (var < originalLastArgIdx) {
            //accessing an arg; remap it
            var = oldArgMappings[var];// + (isStatic?0:1);
        } else {
            //not accessing an arg. just add offset.
            var += newArgOffset;
        }
        if (TaintUtils.DEBUG_LOCAL)
            System.out.println("\t\t" + origVar + "->" + var);
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
        super.visitMethodInsn(opcode, owner, name, desc, itfc);
    }

    public void visitVarInsn(int opcode, int var) {
        if (opcode == TaintUtils.BRANCH_END || opcode == TaintUtils.BRANCH_START) {
            super.visitVarInsn(opcode, var);
            return;
        }
        int origVar = var;
        if (!isStatic && var == 0)
            var = 0;
        else if (var < originalLastArgIdx) {
            //accessing an arg; remap it
            var = oldArgMappings[var];// + (isStatic?0:1);
        } else {
            //not accessing an arg. just add offset.
            var += newArgOffset;
        }
        if (TaintUtils.DEBUG_LOCAL)
            System.out.println("MAR\t\t" + origVar + "->" + var + " " + originalLastArgIdx);
        super.visitVarInsn(opcode, var);
    }
}
