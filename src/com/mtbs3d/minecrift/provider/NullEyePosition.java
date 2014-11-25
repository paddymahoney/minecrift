/**
 * Copyright 2013 Mark Browning
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.provider;

import java.io.File;

import com.mtbs3d.minecrift.api.IEyePositionProvider;
import com.mtbs3d.minecrift.api.PluginType;
import de.fruitfly.ovr.enums.EyeType;
import com.mtbs3d.minecrift.api.BasePlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Vec3;

/**
 * "None" head position plugin
 * @author mabrowning
 *
 */
public class NullEyePosition extends BasePlugin implements IEyePositionProvider {

	private Vec3 headPos;
    private Vec3 leftEyePos;
    private Vec3 rightEyePos;

	@Override
	public String getID() {
		return "null-pos";
	}

	@Override
	public String getName() {
		return "Neck Model";
	}

	@Override
	public String getInitializationStatus() {
		return "";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public boolean init(File nativeDir) {
		return true;
	}

	@Override
	public boolean init() {
		return true;
	}

	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public void poll(float delta) {
	}

	@Override
	public void destroy() {
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

	//Basic neck model:
	@Override
	public void update(float ipd, float yawHeadDegrees, float pitchHeadDegrees, float rollHeadDegrees,
                       float worldYawOffsetDegrees, float worldPitchOffsetDegrees, float worldRollOffsetDegrees)
    {
        float cameraYaw = (worldYawOffsetDegrees + yawHeadDegrees ) % 360;
        headPos = new Vec3(0, Minecraft.getMinecraft().vrSettings.neckBaseToEyeHeight, -Minecraft.getMinecraft().vrSettings.eyeProtrusion);
		headPos.rotateAroundZ( rollHeadDegrees  * PIOVER180 );
		headPos.rotateAroundX( pitchHeadDegrees * PIOVER180 );
        headPos.rotateAroundY( -cameraYaw * PIOVER180 );
        leftEyePos = new Vec3(headPos.xCoord, headPos.yCoord, headPos.zCoord);
        leftEyePos.xCoord -= (ipd / 2f);
        rightEyePos = new Vec3(headPos.xCoord, headPos.yCoord, headPos.zCoord);
        rightEyePos.xCoord += (ipd / 2f);
	}

	@Override
	public Vec3 getCenterEyePosition() {
		return headPos;
	}

    @Override
    public Vec3 getEyePosition(EyeType eye)
    {
        if (eye == EyeType.ovrEye_Left)
            return leftEyePos;
        else
            return rightEyePos;
    }

	@Override
	public void resetOrigin() {
	}

	@Override
	public void setPrediction(float delta, boolean enable) {
	}

	@Override
	public void resetOriginRotation() {
	}

    @Override
    public void eventNotification(int eventId) {
    }

    public void beginFrame() { polledThisFrame = false; }
    public void endFrame() { }
}
