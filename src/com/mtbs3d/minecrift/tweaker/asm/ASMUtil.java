package com.mtbs3d.minecrift.tweaker.asm;

import java.util.Iterator;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class ASMUtil {
	private ASMUtil() {
	}
	
	public static MethodNode findMethod(ClassNode node, MethodTuple tuple) {
		for (Iterator<MethodNode> methods = node.methods.iterator(); methods.hasNext(); ) {
			MethodNode method = methods.next();
			if ((method.name.equals(tuple.methodName) && method.desc.equals(tuple.methodDesc)) || (method.name.equals(tuple.methodNameObf) && method.desc.equals(tuple.methodDescObf))) {
				return method;
			}
		}
		return null;
	}
	
	public static boolean deleteMethod(ClassNode node, MethodTuple tuple) {
		for (Iterator<MethodNode> methods = node.methods.iterator(); methods.hasNext(); ) {
			MethodNode method = methods.next();
			if ((method.name.equals(tuple.methodName) && method.desc.equals(tuple.methodDesc)) || (method.name.equals(tuple.methodNameObf) && method.desc.equals(tuple.methodDescObf))) {
				methods.remove();
				return true;
			}
		}
		return false;
	}
}
