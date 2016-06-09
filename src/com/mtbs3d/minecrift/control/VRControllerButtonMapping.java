package com.mtbs3d.minecrift.control;

import com.mtbs3d.minecrift.utils.KeyboardSimulator;

import net.minecraft.client.settings.KeyBinding;

public class VRControllerButtonMapping {

	public ViveButtons Button;
	public String FunctionDesc = "none";
	public char FunctionExt = 0;
	public KeyBinding key;
	
	public VRControllerButtonMapping(ViveButtons button, String function) {
		this.Button = button;
		this.FunctionDesc = function;		
	}
	
	@Override
	public String toString() {
		return Button.toString() + ":" + FunctionDesc + ( FunctionExt!=0  ? "_" + FunctionExt:"");
	};

	public void press(){	
		if(this.FunctionDesc.equals("none")) return;
		if(key!=null) key.pressKey();
		if(FunctionExt!=0){
			KeyboardSimulator.type(FunctionExt);
		}	
	}
	
	public void unpress(){
		if(this.FunctionDesc.equals("none")) return;
		if(key!=null) key.unpressKey();		
	}
}
