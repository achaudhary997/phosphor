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

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MyDemoClassVisitorFuncDup extends ClassVisitor {
	ClassVisitor next;

	public MyDemoClassVisitorFuncDup(ClassVisitor cv) {
		super(Configuration.ASM_VERSION, new ClassNode());
		next = cv;
	}

	@Override
	public void visitEnd() {
		ClassNode cn = (ClassNode) cv;
		// Do shizz here
		List<MethodNode> methods=(List<MethodNode>)cn.methods;
		List<MethodNode> dupMethods = new ArrayList<MethodNode>();
		dupMethods.addAll(methods);
		// dupMethods.addAll(methods);

		for (MethodNode method: methods) {
			if (method.name.equals("<init>") || method.name.equals("<clinit>") || method.name.equals("main")) {
				continue;
			}
			// System.out.println("name="+method.name+" desc="+method.desc);
			dupMethods.add(method);
		}

		cn.methods = dupMethods;

		cn.accept(next);
	}
}
