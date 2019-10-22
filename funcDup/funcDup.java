import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

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

	public static void main(String[] args) throws Exception {

		//read in, build classNode
		FileInputStream is = new FileInputStream(args[0]);
		ClassNode classNode=new ClassNode();
		ClassReader cr=new ClassReader(is);
		cr.accept(classNode, 0);

		//peek at classNode and modifier
		List<MethodNode> methods=(List<MethodNode>)classNode.methods;
		for(MethodNode method: methods) {
			System.out.println("name="+method.name+" desc="+method.desc);
			InsnList insnList=method.instructions;
			Iterator ite=insnList.iterator();
			while(ite.hasNext()) {
				AbstractInsnNode insn=(AbstractInsnNode)ite.next();
				int opcode=insn.getOpcode();
				//add before return: System.out.println("Returning ... ")
				if (opcode==RETURN) {
					System.out.println("HIIIIHI");
					InsnList tempList=new InsnList();
					tempList.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
					tempList.add(new LdcInsnNode("Returning ... "));
					tempList.add(new MethodInsnNode(INVOKEVIRTUAL,"java/io/PrintStream","println", "(Ljava/lang/String;)V"));
					insnList.insert(insn.getPrevious(), tempList);
					method.maxStack +=2;
				}
			}
		}
		
		
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

	// public static void output(String filename, byte[] data) throws IOException {
	// 	FileOutputStream out=new FileOutputStream(filename);
	// 	out.write(data);
	// 	out.close();
	// }
}