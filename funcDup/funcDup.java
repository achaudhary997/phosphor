import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.*;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.*;

public class funcDup {

	public static String FASTSUFFIX = "_$$FAST";

	public static void main(String[] args) throws Exception {

		//read in, build classNode
		FileInputStream is = new FileInputStream(args[0]);

		// Read once
		ClassNode cn=new ClassNode();
		ClassReader cr = new ClassReader(is);
		cr.accept(cn, 0);

		// Read again
		// ClassNode classNodeDup = new ClassNode();
		// ClassReader crDup = new ClassReader(isDup);
		// crDup.accept(classNodeDup, 0);

		//peek at classNode and modifier
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
		 		
		
		
		//write classNode
		ClassWriter cwfinal = new ClassWriter(0);
		cnDup.accept(cwfinal);
		FileOutputStream fos = new FileOutputStream(args[1]);
		byte[] bfinal;
		bfinal = cwfinal.toByteArray();
		fos.write(bfinal);
		fos.close();

		// output("/tmp/sample/Hello.class",  out.toByteArray());
	}

	// private static MethodNode getCloneMethod(MethodNode method) {
	// 	MethodNode clonedMethod = new MethodNode(method.access, method.name, method.desc, method.signature, method.exceptions);

	// }
}