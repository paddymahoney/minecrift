/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.provider;


import com.mtbs3d.minecrift.api.*;

import com.mtbs3d.minecrift.settings.VRSettings;
import de.fruitfly.ovr.EyeRenderParams;
import de.fruitfly.ovr.IOculusRift;
import de.fruitfly.ovr.OculusRift;
import de.fruitfly.ovr.UserProfileData;
import de.fruitfly.ovr.enums.Axis;
import de.fruitfly.ovr.enums.EyeType;
import de.fruitfly.ovr.enums.HandedSystem;
import de.fruitfly.ovr.enums.RotateDirection;
import de.fruitfly.ovr.structs.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Quaternion;

public class MCOculus extends OculusRift //OculusRift does most of the heavy lifting
	implements IEyePositionProvider, IOrientationProvider, IBasePlugin, IHMDInfo, IStereoProvider, IEventNotifier, IEventListener {

    public static final int NOT_CALIBRATING = 0;
    public static final int CALIBRATE_AWAITING_FIRST_ORIGIN = 1;
    public static final int CALIBRATE_AT_FIRST_ORIGIN = 2;
    public static final int CALIBRATE_COOLDOWN = 7;

    public static final long COOLDOWNTIME_MS = 1500L;

    private boolean isCalibrated = false;
    private long coolDownStart = 0L;
    private int calibrationStep = NOT_CALIBRATING;
    private int MagCalSampleCount = 0;
    private boolean forceMagCalibration = false; // Don't force mag cal initially
    private FrameTiming frameTiming = new FrameTiming();
    private float yawOffsetRad = 0f;
    private float pitchOffsetRad = 0f;
    private float rollHeadRad = 0f;
    private float pitchHeadRad = 0f;
    private float yawHeadRad = 0f;
    private boolean polledThisFrame = false;
    private TrackerState ts = new TrackerState();
    private Posef[] eyePose = new Posef[2];
    private EulerOrient[] eulerOrient = new EulerOrient[2];
    private EyeType lastEyePolled = EyeType.ovrEye_Left;

    public MCOculus()
    {
        super();
        eyePose[0] = new Posef();
        eyePose[1] = new Posef();
        eulerOrient[0] = new EulerOrient();
        eulerOrient[1] = new EulerOrient();
    }

    @Override
    public EyeType eyeRenderOrder(int index)
    {
        HmdDesc desc = getHMDInfo();
        if (index < 0 || index >= desc.EyeRenderOrder.length)
            return EyeType.ovrEye_Left;

        return desc.EyeRenderOrder[index];
    }

    @Override
    public String getVersion()
    {
        return OculusRift.getVersionString();
    }

    @Override
    public boolean usesDistortion() {
        return true;
    }

    @Override
    public boolean isStereo() {
        return true;
    }

    @Override
    public boolean isGuiOrtho()
    {
        return false;
    }

    public FrameTiming getFrameTiming() { return frameTiming; };

    public static UserProfileData theProfileData = null;

    public void beginFrame()
    {
        polledThisFrame = false;
        frameTiming = super.beginFrameGetTiming();
    }

    public Posef getEyePose(EyeType eye)
    {
        this.eyePose[eye.value()] = super.getEyePose(eye);
        this.eulerOrient[eye.value()] = OculusRift.getEulerAnglesDeg(this.eyePose[eye.value()].Orientation,
                                                                     1.0f,
                                                                     Axis.Axis_Y,
                                                                     Axis.Axis_X,
                                                                     Axis.Axis_Z,
                                                                     HandedSystem.Handed_L,
                                                                     RotateDirection.Rotate_CCW);

        this.lastEyePolled = eye;
        return this.eyePose[eye.value()];
    }

    public Matrix4f getMatrix4fProjection(FovPort fov,
                                          float nearClip,
                                          float farClip)
    {
         return super.getMatrix4fProjection(fov, nearClip, farClip);
    }

    public void endFrame()
    {
        GL11.glDisable(GL11.GL_CULL_FACE);  // Oculus wants CW orientations, avoid the problem by turning off culling...
        GL11.glDisable(GL11.GL_DEPTH_TEST); // Nothing is drawn with depth test on...
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0); // Unbind GL_ARRAY_BUFFER for my own vertex arrays to work...
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        // End the frame
        super.endFrame();

        GL11.glFrontFace(GL11.GL_CCW);   // Needed for OVR SDK 0.4.0
        GL11.glEnable(GL11.GL_CULL_FACE); // Turn back on...
        GL11.glEnable(GL11.GL_DEPTH_TEST); // Turn back on...
        GL11.glClearDepth(1); // Oculus set this to 0 (the near plane), return to normal...
        ARBShaderObjects.glUseProgramObjectARB(0); // Oculus shader is still active, turn it off...

        Display.processMessages();
    }

    @Override
    public HmdDesc getHMDInfo()
    {
        HmdDesc hmdDesc = new HmdDesc();
        if (isInitialized())
            hmdDesc = getHmdDesc();

        return hmdDesc;
    }

	@Override
	public String getName() {
		return "Oculus Rift";
	}

	@Override
	public String getID() {
		return "oculus";
	}

    @Override
    public void update(float ipd,
                       float yawHeadDegrees,
                       float pitchHeadDegrees,
                       float rollHeadDegrees,
                       float worldYawOffsetDegrees,
                       float worldPitchOffsetDegrees,
                       float worldRollOffsetDegrees)
    {
        if (!polledThisFrame)
        {
            ts = poll(frameTiming.ScanoutMidpointSeconds);
            polledThisFrame = true;
        }
        rollHeadRad = (float)Math.toRadians(rollHeadDegrees);
        pitchHeadRad = (float)Math.toRadians(pitchHeadDegrees);
        yawHeadRad =  (float)Math.toRadians(yawHeadDegrees);
        yawOffsetRad = (float)Math.toRadians(worldYawOffsetDegrees);
        pitchOffsetRad = (float)Math.toRadians(worldPitchOffsetDegrees);
    }

    @Override
    public Vec3 getCenterEyePosition()
    {
        VRSettings vr = Minecraft.getMinecraft().vrSettings;
        Vec3 eyePosition = new Vec3(0, 0, 0);
        if (Minecraft.getMinecraft().vrSettings.usePositionTracking)
        {
            eyePosition = new Vec3(ts.HeadPose.ThePose.Position.x * vr.posTrackDistanceScale * vr.worldScale,
                                  -ts.HeadPose.ThePose.Position.y * vr.posTrackDistanceScale * vr.worldScale,
                                   ts.HeadPose.ThePose.Position.z * vr.posTrackDistanceScale * vr.worldScale);
        }

        return eyePosition;
    }

    @Override
    public Vec3 getEyePosition(EyeType eye)
    {
        VRSettings vr = Minecraft.getMinecraft().vrSettings;
        Vec3 eyePosition = new Vec3(0, 0, 0);
        if (Minecraft.getMinecraft().vrSettings.usePositionTracking)
        {
            Vector3f eyePos = super.getEyePos(eye);
            eyePosition = new Vec3(eyePos.x * vr.posTrackDistanceScale * vr.worldScale,
                                  -eyePos.y * vr.posTrackDistanceScale * vr.worldScale,
                                   eyePos.z * vr.posTrackDistanceScale * vr.worldScale);
        }

        return eyePosition;
    }

    @Override
	public void resetOrigin() {
        super.resetTracking();
    }

    @Override
    public void resetOriginRotation() {
        // TODO:
    }

    @Override
    public void setPrediction(float delta, boolean enable) {
        // Now ignored
    }

    @Override
    public void beginCalibration(PluginType type)
    {
        if (isInitialized())
            processCalibration();
    }

    @Override
    public void updateCalibration(PluginType type)
    {
        if (isInitialized())
            processCalibration();
    }

    @Override
    public boolean isCalibrated(PluginType type) {
        if (!isInitialized())
            return true;  // Return true if not initialised

        if (!getHMDInfo().IsReal)
            return true;  // Return true if debug (fake) Rift...

        if (type != PluginType.PLUGIN_POSITION)   // Only position provider needs calibrating
            return true;

        return isCalibrated;
    }

	@Override
	public String getCalibrationStep(PluginType type)
    {
        String step = "";
        String newline = "\n";

        switch (calibrationStep)
        {
            case CALIBRATE_AWAITING_FIRST_ORIGIN:
            {
                StringBuilder sb = new StringBuilder();
                sb.append("HEALTH AND SAFETY WARNING").append(newline).append(newline)
                        .append("Read and follow all warnings and instructions").append(newline)
                        .append("included with the Headset before use. Headset").append(newline)
                        .append("should be calibrated for each user. Not for use by").append(newline)
                        .append("children under 13. Stop use if you experience any").append(newline)
                        .append("discomfort or health reactions.").append(newline).append(newline)
                        .append("More: www.oculus.com/warnings").append(newline).append(newline)
                        .append("Look ahead and press SPACEBAR to acknowledge").append(newline)
                        .append("and reset origin.");
                step = sb.toString();
                break;
            }
            case CALIBRATE_AT_FIRST_ORIGIN:
            case CALIBRATE_COOLDOWN:
            {
                step = "Done!";
                break;
            }
        }

        return step;
	}

    @Override
    public void eventNotification(int eventId)
    {
        switch (eventId)
        {
            case IBasePlugin.EVENT_CALIBRATION_SET_ORIGIN:
            {
                if (calibrationStep == CALIBRATE_AWAITING_FIRST_ORIGIN)
                {
                    calibrationStep = CALIBRATE_AT_FIRST_ORIGIN;
                    processCalibration();
                }
                break;
            }
            case IBasePlugin.EVENT_SET_ORIGIN:
            {
                resetOrigin();
            }
        }
    }

    @Override
    public synchronized void registerListener(IEventListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public synchronized void notifyListeners(int eventId)
    {
        for (IEventListener listener : listeners)
        {
            if (listener != null)
                listener.eventNotification(eventId);
        }
    }

    private void processCalibration()
    {
        switch (calibrationStep)
        {
            case NOT_CALIBRATING:
            {
                calibrationStep = CALIBRATE_AWAITING_FIRST_ORIGIN;
                isCalibrated = false;
                break;
            }
            case CALIBRATE_AT_FIRST_ORIGIN:
            {
                //_reset();

                // Calibration of Mag cal is now handled solely by the Oculus config utility.

                MagCalSampleCount = 0;
                coolDownStart = System.currentTimeMillis();
                calibrationStep = CALIBRATE_COOLDOWN;
                resetOrigin();
                notifyListeners(IBasePlugin.EVENT_SET_ORIGIN);

                break;
            }
            case CALIBRATE_COOLDOWN:
            {
                if ((System.currentTimeMillis() - coolDownStart) > COOLDOWNTIME_MS)
                {
                    coolDownStart = 0;
                    calibrationStep = NOT_CALIBRATING;
                    isCalibrated = true;
                }
                break;
            }
        }
    }

    @Override
    public void poll(/*EyeType eyeHint, */float delta)
    {
        // Do nothing
    }


	@Override
	public float getHeadYawDegrees()
    {
        return this.eulerOrient[lastEyePolled.value()].yaw;
	}

	@Override
	public float getHeadPitchDegrees()
    {
        return this.eulerOrient[lastEyePolled.value()].pitch;
	}

	@Override
	public float getHeadRollDegrees()
    {
        return this.eulerOrient[lastEyePolled.value()].roll;
	}

    @Override
    public Quaternion getOrientationQuaternion()
    {
        Quatf orient = this.eyePose[lastEyePolled.value()].Orientation;
        return new Quaternion(orient.x, orient.y, orient.z, orient.w);
    }

    @Override
    public UserProfileData getProfileData()
    {
        UserProfileData userProfile = null;

        if (isInitialized())
        {
            userProfile = _getUserProfileData();
        }
        else
        {
            userProfile = new UserProfileData();
        }

        return userProfile;
    }

    @Override
    public double getCurrentTimeSecs()
    {
        return getCurrentTimeSeconds();
    }
}
