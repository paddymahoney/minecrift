package com.mtbs3d.minecrift.main;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mtbs3d.minecrift.tweaker.MinecriftClassTransformer;
import com.mtbs3d.minecrift.tweaker.MinecriftClassTransformer.Stage;
import com.mtbs3d.minecrift.tweaker.MinecriftForgeClassTransformer;
import com.mtbs3d.minecrift.tweaker.MinecriftForgeLateClassTransformer;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.src.Reflector;

public class VivecraftMain
{
	private final static String smcName = "shadersmodcore.transform.SMCClassTransformer";
	
	public static void main(String[] p_main_0_)
    {

		LaunchClassLoader load = (LaunchClassLoader) Thread.currentThread().getContextClassLoader();
		
		try {
			Field f = load.getClass().getDeclaredField("transformers");
			f.setAccessible(true);
			
			
			List<IClassTransformer> transformers = (List<IClassTransformer>) f.get(load);
			List<IClassTransformer> encapsulate = new ArrayList<IClassTransformer>();
							
			for (final Iterator it = transformers.iterator(); it.hasNext(); ) {
				IClassTransformer t = (IClassTransformer) it.next();
				
				System.out.println(t.getClass().getName());
			
				
			    if (t.getClass().getName().equalsIgnoreCase(smcName)) {
			    	encapsulate.add(t);
			    	it.remove();
			    }
			    
//			    if (t.getClass().getName().startsWith("Reika.DragonAPI")) {
//			    	encapsulate.add(t);
//			    	it.remove();
//			    }
			}
				
			transformers.add(2,new MinecriftClassTransformer(Stage.main));
			try {
				Class.forName("cpw.mods.fml.common.API"); // don't ask
				transformers.add(3, new MinecriftForgeClassTransformer());
				transformers.add(transformers.size() - 1, new MinecriftForgeLateClassTransformer());
			} catch (ClassNotFoundException e) {}
	
			if(encapsulate.size() > 0){ //Dirty Harry Potter.
				transformers.add(new MinecriftClassTransformer(Stage.cache));
					transformers.addAll(encapsulate);
				transformers.add(new MinecriftClassTransformer(Stage.replace));
			}

	    	System.out.println("************** Vivecraft classloader filter ***************");
	    	
	    	
			for (final Iterator it = transformers.iterator(); it.hasNext(); ) {
				IClassTransformer t = (IClassTransformer) it.next();
				System.out.println(t.getClass().getName());
			}
			
		} catch (Exception e) {
			System.out.println("Vivecraft Filter Error");
			e.printStackTrace();
		}

		try {
        	
       	 final String launchTarget = "net.minecraft.client.main.Main";
         final Class<?> clazz = Class.forName(launchTarget, false, load);
         final Method mainMethod = clazz.getMethod("main", new Class[]{String[].class});

		mainMethod.invoke(null, (Object) p_main_0_);
			
		} catch (Exception e) {
	    	System.out.println("************** Vivecraft critical error ***************");
			e.printStackTrace();
			System.exit(1);
		}
    	
    }

}
