package edu.columbia.cs.psl.phosphor.instrumenter;

import java.io.*;
import java.util.*;

import edu.columbia.cs.psl.phosphor.Configuration;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.*;

public class MyDemoClassVisitor extends ClassVisitor implements Opcodes
{

	public MyDemoClassVisitor(final ClassVisitor cv)
	{
		super(Configuration.ASM_VERSION, cv);
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions)
	{
		MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
		return mv == null ? null : new MethodAdapter(access, desc, mv);
	}
}

class MethodAdapter extends MethodVisitor implements Opcodes
{
	public MethodAdapter(int access, String desc, MethodVisitor mv)
	{
		super(Configuration.ASM_VERSION, mv);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf)
	{
		//if (desc.equals("()Ljava/lang/Object;") && name.equals("nextElement")) {

			// System.out.println("[+] Demo CV Instrumenting " + name);

			mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitLdcInsn("[+] " + name + " | " + desc);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

		//}
		/* do call */
		mv.visitMethodInsn(opcode, owner, name, desc, itf);
	}
}
