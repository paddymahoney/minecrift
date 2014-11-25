/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.control;

import com.mtbs3d.minecrift.settings.VRSettings;

public class JoystickAimLoose extends JoystickAim {

    public JoystickAimLoose()
    {
        super();
        aimType = VRSettings.AIM_TYPE_LOOSE;
    }

	public void update( float partialTicks ) {

        getInput(partialTicks);
        super.updateAim(aimType, aimPitchAdd, aimYawAdd, aimPitchRate, aimYawRate);
	}
}
