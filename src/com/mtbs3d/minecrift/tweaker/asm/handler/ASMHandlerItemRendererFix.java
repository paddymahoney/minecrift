package com.mtbs3d.minecrift.tweaker.asm.handler;

import java.util.Iterator;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.mtbs3d.minecrift.tweaker.asm.ASMClassHandler;
import com.mtbs3d.minecrift.tweaker.asm.ASMMethodHandler;
import com.mtbs3d.minecrift.tweaker.asm.ASMUtil;
import com.mtbs3d.minecrift.tweaker.asm.ClassTuple;
import com.mtbs3d.minecrift.tweaker.asm.MethodTuple;

import static org.objectweb.asm.Opcodes.*;

public class ASMHandlerItemRendererFix extends ASMClassHandler {
	@Override
	public ClassTuple getDesiredClass() {
		return new ClassTuple("net.minecraft.client.renderer.ItemRenderer", "bly");
	}

	@Override
	public ASMMethodHandler[] getMethodHandlers() {
		return new ASMMethodHandler[]{};
	}

	@Override
	public boolean getComputeFrames() {
		return true;
	}

	@Override
	protected void patchClass(ClassNode node, boolean obfuscated) {
		{
			MethodVisitor mv = node.visitMethod(ACC_PUBLIC, "renderItem", obfuscated ? "(Lsv;Ladd;ILnet/minecraftforge/client/IItemRenderer$ItemRenderType;)V" : "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;ILnet/minecraftforge/client/IItemRenderer$ItemRenderType;)V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitLineNumber(new java.util.Random().nextInt(), l0); // lol
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitVarInsn(ILOAD, 3);
			mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/client/renderer/ItemRenderer", obfuscated ? "a" : "func_78443_a", obfuscated ? "(Lsv;Ladd;I)V" : "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;I)V", false);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLineNumber(new java.util.Random().nextInt(), l1);
			mv.visitInsn(RETURN);
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitLocalVariable("this", obfuscated ? "Lbly;" : "Lnet/minecraft/client/renderer/ItemRenderer;", null, l0, l2, 0);
			mv.visitLocalVariable("p_78443_1_", obfuscated ? "Lsv;" : "Lnet/minecraft/entity/EntityLivingBase;", null, l0, l2, 1);
			mv.visitLocalVariable("p_78443_2_", obfuscated ? "Ladd;" : "Lnet/minecraft/item/ItemStack;", null, l0, l2, 2);
			mv.visitLocalVariable("p_78443_3_", "I", null, l0, l2, 3);
			mv.visitMaxs(5, 4);
			mv.visitEnd();
			System.out.println("Injected stupid Forge renderItem");
		}
	}

	/*public static class MethodHandler implements ASMMethodHandler {
		@Override
		public MethodTuple getDesiredMethod() {
			return new MethodTuple("renderItem", "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;I)V", "a", "(Lsv;Ladd;I)V");
		}

		@Override
		public void patchMethod(MethodNode methodNode, ClassNode classNode, boolean obfuscated) {
			for (int i = 0; i < methodNode.instructions.size(); i++) {
				AbstractInsnNode insn = methodNode.instructions.get(i);
				if (insn instanceof MethodInsnNode) {
					MethodInsnNode insn2 = (MethodInsnNode)insn;
				}
			}
		}
	}*/
}
