package com.mtbs3d.minecrift.tweaker;

import java.util.ArrayList;
import java.util.List;

import com.mtbs3d.minecrift.tweaker.asm.ASMClassHandler;
import com.mtbs3d.minecrift.tweaker.asm.ClassTuple;
import com.mtbs3d.minecrift.tweaker.asm.handler.*;

import net.minecraft.entity.EntityLiving;
import net.minecraft.launchwrapper.IClassTransformer;

public class MinecriftForgeLateClassTransformer implements IClassTransformer {
	private static final List<ASMClassHandler> asmHandlers = new ArrayList<ASMClassHandler>();
	static {
		asmHandlers.add(new ASMHandlerItemRendererFix());
	}
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		for (ASMClassHandler handler : asmHandlers) {
			if (!handler.shouldPatchClass()) continue;
			ClassTuple tuple = handler.getDesiredClass();
			if (transformedName.equals(tuple.className)) {
				System.out.println("Patching class: " + transformedName);
				bytes = handler.patchClass(bytes, false);
			}
		}
		return bytes;
	}
}
