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
		super(binding.getKeyDescription(),binding.getKeyDescription());
		key = binding;
		mc = Minecraft.getMinecraft();
	}

	@Override
	public void setValue(float value) {
		if( Math.abs(value) > 0.1 )
		{
			if(!key.getIsKeyPressed() )
				setState( true );
		} else {
			setState(false);
		}
	}

	@Override
	public void setState(boolean state) {
		if( state ) {
			key.setKeyBindState(key, state);
//			if( mc.currentScreen != null && mc.gameSettings.keyBindInventory == key ) {
//				key.unpressKey();
//				mc.displayGuiScreen(null);
//			}
//		} else {key.unpressKey();
	}}
}
