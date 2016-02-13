/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.control;

import com.mtbs3d.minecrift.settings.VRSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;

public class WalkLeftBinding extends ControlBinding {
	public WalkLeftBinding() {
		super("key.left","key.left");
	}

	@Override
	public void setValue(float value) {
        EntityClientPlayerMP thePlayer = Minecraft.getMinecraft().thePlayer;
        if( thePlayer != null )
        	thePlayer.movementInput.baseMoveStrafe = Math.abs(value);
	}

	@Override
	public void setState(boolean state) {
		VRSettings settings = Minecraft.getMinecraft().vrSettings;
		setValue( state ? settings.movementSpeedMultiplier * settings.strafeSpeedMultiplier : 0.0f );
	}
}
