package com.mtbs3d.minecrift.tweaker;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.util.List;

public class MinecriftForgeTweaker implements ITweaker
{
		
	public MinecriftForgeTweaker() {

	}
	
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile)
    {	
		
    }

    public void injectIntoClassLoader(LaunchClassLoader classLoader)
    {
    	
    }

    public String getLaunchTarget()
    {
        return "com.mtbs3d.minecrift.main.VivecraftMain";
    }

    public String[] getLaunchArguments()
    {

        return new String[0];
    }

    private static void dbg(String str)
    {
        System.out.println(str);
    }
}
