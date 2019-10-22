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
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import static org.objectweb.asm.Opcodes.*;


public class Tree {
	public static void main(String[] args) throws Exception {
		FileInputStream is = new FileInputStream(args[0]);
		byte[] b;

		ClassNode classNode=new ClassNode();
		ClassReader cr = new ClassReader(is);
		cr.accept(classNode, 0);
		new AddTimerTransformer(null).transform(classNode);
		
		ClassWriter cw = new ClassWriter(0);
		classNode.accept(cw);
		b = cw.toByteArray();

		new NewTransformer(null).transform()

		FileOutputStream fos = new FileOutputStream(args[1]);
		fos.write(b);
		fos.close();
	}
}

class ClassTransformer {
	protected ClassTransformer ct;
	
	public ClassTransformer(ClassTransformer ct) {
		this.ct = ct;
	}
	public void transform(ClassNode cn) {
		if (ct != null) {
		ct.transform(cn);
		}
	}
}

class AddTimerTransformer extends ClassTransformer {
	public AddTimerTransformer(ClassTransformer ct) {
		super(ct);
	}

	@Override public void transform(ClassNode cn) {
		for (MethodNode mn : (List<MethodNode>) cn.methods) {
			if ("<init>".equals(mn.name) || "<clinit>".equals(mn.name)) {
				continue;
			}
			InsnList insns = mn.instructions;
			if (insns.size() == 0) {
				continue;
			}
			Iterator<AbstractInsnNode> j = insns.iterator();
			while (j.hasNext()) {
				AbstractInsnNode in = j.next();
				int op = in.getOpcode();
				if ((op >= IRETURN && op <= RETURN) || op == ATHROW) {
					InsnList il = new InsnList();
					il.add(new FieldInsnNode(GETSTATIC, cn.name, "timer", "J"));
					il.add(new MethodInsnNode(INVOKESTATIC, "java/lang/System",
					"currentTimeMillis", "()J"));
					il.add(new InsnNode(LADD));
					il.add(new FieldInsnNode(PUTSTATIC, cn.name, "timer", "J"));
					insns.insert(in.getPrevious(), il);
				}
			}
			InsnList il = new InsnList();
			il.add(new FieldInsnNode(GETSTATIC, cn.name, "timer", "J"));
			il.add(new MethodInsnNode(INVOKESTATIC, "java/lang/System",
			"currentTimeMillis", "()J"));
			il.add(new InsnNode(LSUB));
			il.add(new FieldInsnNode(PUTSTATIC, cn.name, "timer", "J"));
			insns.insert(il);
			mn.maxStack += 4;
		}
		int acc = ACC_PUBLIC + ACC_STATIC;
		cn.fields.add(new FieldNode(acc, "timer", "J", null, null));
		super.transform(cn);
	}
}