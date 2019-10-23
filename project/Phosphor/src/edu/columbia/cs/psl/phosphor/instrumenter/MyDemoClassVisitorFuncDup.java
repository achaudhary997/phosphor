package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import edu.columbia.cs.psl.phosphor.Configuration;

public class MyDemoClassVisitorFuncDup extends ClassVisitor {
	ClassVisitor next;
	final String FASTSUFFIX = "_$$FAST";

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

		for (MethodNode method: methods) {
			if (method.name.equals("<init>") || method.name.equals("<clinit>") || method.name.equals("main")) {
				continue;
			}
			// System.out.println("name="+method.name+" desc="+method.desc);
			dupMethods.add(method);
		}
		cn.methods = dupMethods;

		ClassWriter cw = new ClassWriter(0);
		cn.accept(cw);
		
		byte[] b = cw.toByteArray();
		
		ClassNode cnDup = new ClassNode();
		ClassReader crDup = new ClassReader(b);
		crDup.accept(cnDup, 0);		
		List<MethodNode> methodsDup = (List<MethodNode>) cnDup.methods;
		Set<String> hasBeenSeen = new HashSet<String>();
		
		for (MethodNode method : methodsDup) {
			String check = method.name + ":" + method.desc;
			if (hasBeenSeen.contains(check)) {
				System.out.println("name="+method.name+" desc="+method.desc);
				method.name = method.name + FASTSUFFIX;
			} else {
				hasBeenSeen.add(check);
			}
		}
		 		


		cnDup.accept(next);
	}
}
