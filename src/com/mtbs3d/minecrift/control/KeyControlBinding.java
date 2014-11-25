/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.control;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

public class KeyControlBinding extends ControlBinding {

	KeyBinding key;
	Minecraft mc;
	public KeyControlBinding(KeyBinding binding ) {
		super(binding.keyDescription,binding.keyDescription);
		key = binding;
		mc = Minecraft.getMinecraft();
	}

	@Override
	public void setValue(float value) {
		if( Math.abs(value) > 0.1 )
		{
			if(!key.pressed )
				setState( true );
		} else {
			setState(false);
		}
	}

	@Override
	public void setState(boolean state) {
		key.pressed = state;
		if( state ) {
			if( mc.currentScreen != null && mc.gameSettings.keyBindInventory == key ) {
				key.pressed = false;
				mc.displayGuiScreen(null);
			} else {
				key.pressTime ++;
			}
		}
	}
}
