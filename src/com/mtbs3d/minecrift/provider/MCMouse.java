/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.provider;

import java.io.File;


import com.mtbs3d.minecrift.api.PluginType;
import com.mtbs3d.minecrift.control.Aim;
import com.mtbs3d.minecrift.control.DiscreteAngle;
import com.mtbs3d.minecrift.settings.VRSettings;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;

import com.mtbs3d.minecrift.api.BasePlugin;
import com.mtbs3d.minecrift.api.IBodyAimController;
import com.mtbs3d.minecrift.control.ControlBinding;

public class MCMouse extends BasePlugin implements IBodyAimController {

    private Aim aim = new Aim();
    long lastIndex = -1;

	private Minecraft mc;

	@Override
	public String getName() {
		return "Mouse";
	}

	@Override
	public String getID() {
		return "mouse";
	}
	@Override
	public String getInitializationStatus() {
		return "";
	}

	@Override
	public String getVersion() {
		return "0.28";
	}

	@Override
	public boolean init(File nativeDir) {
		return init();
	}

	@Override
	public boolean init() {
		mc = Minecraft.getMinecraft();
		return isInitialized();
	}

	@Override
	public boolean isInitialized() {
		return mc != null;
	}

	@Override
	public void poll(long frameIndex)
    {
        if (frameIndex <= this.lastIndex)
            return;

        this.lastIndex = frameIndex;
        if(this.mc.currentScreen == null && Display.isActive())
        {
            this.mc.mouseHelper.mouseXYChange();
            float mouseSensitivityMultiplier1 = this.mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
            float mouseSensitivityMultiplier2 = mouseSensitivityMultiplier1 * mouseSensitivityMultiplier1 * mouseSensitivityMultiplier1 * 8.0F;
            float deltaYaw = (float) this.mc.mouseHelper.deltaX * mouseSensitivityMultiplier2 * 0.15f;
            float deltaPitch = (float) this.mc.mouseHelper.deltaY * mouseSensitivityMultiplier2 * 0.15f * (mc.gameSettings.invertMouse ? 1 : -1);

            aim.updateAim(this.mc.vrSettings.mouseKeyholeTight ? VRSettings.AIM_TYPE_TIGHT : VRSettings.AIM_TYPE_LOOSE, deltaPitch, deltaYaw, 0f, 0f);
        }
	}

	@Override
	public void destroy() {/*no-op*/ }

	@Override
	public float getBodyYawDegrees() {
		return aim.getBodyYaw();
	}

	@Override
	public void setBodyYawDegrees( float yawOffset ) {
		aim.setAimYawOffset(yawOffset);
	}

	@Override
	public float getBodyPitchDegrees() {
		return aim.getBodyPitch();
	}

	@Override
	public float getAimYaw() {
		return aim.getAimYaw();
	}

	@Override
	public float getAimPitch() {
		return aim.getAimPitch();
	}

	@Override
	public boolean isCalibrated(PluginType type) {
		return true;
	}

    @Override
    public void beginCalibration(PluginType type) {}

    @Override
    public void updateCalibration(PluginType type) {}

	@Override
	public String getCalibrationStep(PluginType type) {
		return "";
	}

    @Override
    public void eventNotification(int eventId) {
    }

	@Override
	public void mapBinding(ControlBinding binding) {
	}
    public void beginFrame() { beginFrame(0); }
    public void beginFrame(long frameIndex) { }
    public void endFrame() { }

    @Override
    public double ratchetingYawTransitionPercent()
    {
        return this.aim.getYawTransitionPercent();
    }

    @Override
    public double ratchetingPitchTransitionPercent()
    {
        return this.aim.getPitchTransitionPercent();
    }

    @Override
    public boolean initBodyAim()
    {
        return init();
    }
}
