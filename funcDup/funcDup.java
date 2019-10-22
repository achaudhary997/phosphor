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
import static org.objectweb.asm.Opcodes.*;

public class funcDup {

	public static String FASTSUFFIX = "_$$FAST";

	public static void main(String[] args) throws Exception {

		//read in, build classNode
		FileInputStream is = new FileInputStream(args[0]);
		FileInputStream isDup = new FileInputStream(args[0]);

		// Read once
		ClassNode classNode=new ClassNode();
		ClassReader cr = new ClassReader(is);
		cr.accept(classNode, 0);

		// Read again
		// ClassNode classNodeDup = new ClassNode();
		ClassNode classNodeDup = (ClassNode) ((Object) classNode).clone();
		// ClassReader crDup = new ClassReader(isDup);
		// crDup.accept(classNodeDup, 0);

		//peek at classNode and modifier
		List<MethodNode> methods=(List<MethodNode>)classNode.methods;
		List<MethodNode> dupMethods = new ArrayList<MethodNode>();
		dupMethods.addAll(methods);

		List<MethodNode> methodsDup = (List<MethodNode>)classNodeDup.methods;
		// dupMethods.addAll(methods);

		for (MethodNode method: methodsDup) {
			System.out.println("name="+method.name+" desc="+method.desc);
			if (method.name.equals("<init>") || method.name.equals("<clinit>") || method.name.equals("main")) {
				continue;
			}
			method.name = method.name + FASTSUFFIX;
			dupMethods.add(method);
		}
		classNode.methods = dupMethods;

		// for(MethodNode method: methods) {
		// 	System.out.println("name="+method.name+" desc="+method.desc);
		// 	InsnList insnList=method.instructions;
		// 	Iterator ite=insnList.iterator();
		// 	while(ite.hasNext()) {
		// 		AbstractInsnNode insn=(AbstractInsnNode)ite.next();
		// 		int opcode=insn.getOpcode();
		// 		//add before return: System.out.println("Returning ... ")
		// 		if (opcode==RETURN) {
		// 			System.out.println("HIIIIHI");
		// 			InsnList tempList=new InsnList();
		// 			tempList.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
		// 			tempList.add(new LdcInsnNode("Returning ... "));
		// 			tempList.add(new MethodInsnNode(INVOKEVIRTUAL,"java/io/PrintStream","println", "(Ljava/lang/String;)V"));
		// 			insnList.insert(insn.getPrevious(), tempList);
		// 			method.maxStack +=2;
		// 		}
		// 	}
		// }
		
		
		//write classNode
		ClassWriter cw = new ClassWriter(0);
		classNode.accept(cw);
		FileOutputStream fos = new FileOutputStream(args[1]);
		byte[] b;
		b = cw.toByteArray();
		fos.write(b);
		fos.close();

		// output("/tmp/sample/Hello.class",  out.toByteArray());
	}

	// private static MethodNode getCloneMethod(MethodNode method) {
	// 	MethodNode clonedMethod = new MethodNode(method.access, method.name, method.desc, method.signature, method.exceptions);

	// }
}