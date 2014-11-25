/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.control;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

public class ItemLeftControlBinding extends ControlBinding {

	public ItemLeftControlBinding() {
		super("Next Item Left","key.itemleft");
	}

	@Override
	public void setValue(float value) {
		if( value > 0.1 ) 
			setState(true);
	}

	@Override
	public void setState(boolean state) {
		if( state ) {
            EntityPlayerSP thePlayer = Minecraft.getMinecraft().thePlayer;
			if( thePlayer != null )
	        	thePlayer.inventory.changeCurrentItem(1);
		}
			
	}
}
