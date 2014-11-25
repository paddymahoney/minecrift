/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.control;

import net.minecraft.client.settings.KeyBinding;

public class InventoryBinding extends KeyControlBinding {

	public InventoryBinding(KeyBinding inventoryBinding) {
		super(inventoryBinding);
	}

	@Override
	public void setState(boolean state) {
		if( state && mc.currentScreen != null ) {
            this.mc.thePlayer.closeScreen();
		} else {
			super.setState(state);
		}
	}
}
