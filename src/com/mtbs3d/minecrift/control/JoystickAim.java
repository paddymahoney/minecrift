/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.control;

import com.mtbs3d.minecrift.settings.VRSettings;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

public class JoystickAim extends Aim {

    static public float aimPitchRate = 0.0f;
    static public float aimYawRate = 0.0f;
    static public float aimPitchAdd = 0f;
    static public float aimYawAdd = 0f;

    protected int aimType = VRSettings.AIM_TYPE_TIGHT;

	public static class JoyAimPitchBinding extends ControlBinding {
		
		@Override
		public boolean isBiAxis() { return true; }

		public JoyAimPitchBinding() {
			super("Aim Up/Down","axis.updown");
		}

		@Override
		public void setValue(float value) {
			aimPitchRate = value;
		}

		@Override
		public void setState(boolean state) {
		}
	}

	public static class JoyAimYawBinding extends ControlBinding {

		@Override
		public boolean isBiAxis() { return true; }

		public JoyAimYawBinding() {
			super("Aim Left/Right","axis.leftright");
		}

		@Override
		public void setValue(float value) {
			aimYawRate = value;
		}

		@Override
		public void setState(boolean state) {
		}
		
	}

	public static class JoyAimCenterBinding extends ControlBinding {
		public JoyAimCenterBinding() {
			super("Center Aim (hold)","key.aimcenter");
		}

		@Override
		public void setValue(float value) {
			setState( Math.abs(value)> 0.1 );
		}

		@Override
		public void setState(boolean state) {
			setHoldCenter(state);
		}
	}
	
	public static JoystickAim selectedJoystickMode;

	public void update( float partialTicks ) {
        getInput(partialTicks);
        super.updateAim(aimType, aimPitchAdd, aimYawAdd, aimPitchRate, aimYawRate);
    }

    public void updateTick()
    {
        update(1.0f);
    }

    protected void getInput(float partialTicks)
    {
        aimYawAdd = 2 * aimYawRate * VRSettings.inst.joystickSensitivity * partialTicks;
        aimPitchAdd = 2 * aimPitchRate * VRSettings.inst.joystickSensitivity * partialTicks;
    }
}
