package com.mtbs3d.minecrift.provider;

import com.mtbs3d.minecrift.api.BasePlugin;
import com.mtbs3d.minecrift.api.IStereoProvider;
import com.mtbs3d.minecrift.api.PluginType;
import de.fruitfly.ovr.EyeRenderParams;
import de.fruitfly.ovr.structs.FullPoseState;
import de.fruitfly.ovr.enums.EyeType;
import de.fruitfly.ovr.structs.*;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.io.File;

/**
 * Created by StellaArtois on 26/6/2014.
 */
public class NullStereoRenderer extends BasePlugin implements IStereoProvider
{
    FrameTiming frameTiming = new FrameTiming();

    @Override
    public String getID() {
        return "mono";
    }

    @Override
    public String getName() {
        return "Mono";
    }

    @Override
    public void eventNotification(int eventId) {

    }

    @Override
    public FovTextureInfo getFovTextureSize(FovPort LeftFov,
            FovPort RightFov,
            float renderScaleFactor)
    {
        return null;
    }

    @Override
    public EyeRenderParams configureRendering(Sizei InTextureSize, Sizei OutTextureSize, GLConfig glConfig, FovPort LeftFov,
            FovPort RightFov, float worldScale)
    {
        return null;
    }

    @Override
    public EyeRenderParams configureRenderingDualTexture(Sizei InTexture1Size, Sizei InTexture2Size, Sizei OutDisplaySize, GLConfig glConfig, FovPort LeftFov,
            FovPort RightFov, float worldScale)
    {
        return null;
    }

    @Override
    public void resetRenderConfig() {

    }

    @Override
    public EyeType eyeRenderOrder(int index)
    {
        //return EyeType.ovrEye_Center;
        return EyeType.ovrEye_Left; // Hack for now
    }

    @Override
    public boolean usesDistortion() {
        return false;
    }

    @Override
    public boolean isStereo() {
        return false;
    }

    @Override
    public boolean isGuiOrtho()
    {
        return true;
    }

    @Override
    public FrameTiming getFrameTiming()
    {
        return frameTiming;
    }

    @Override
    public Posef getEyePose(EyeType eye) {
        return null;
    }

    @Override
    public FullPoseState getEyePoses(int frameIndex) {
        return new FullPoseState();
    }

    @Override
    public Matrix4f getMatrix4fProjection(FovPort fov, float nearClip, float farClip) {
        return null;
    }

    @Override
    public String getInitializationStatus() {
        return null;
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean init(File nativeDir) {
        return false;
    }

    @Override
    public boolean init() {
        return false;
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void poll(int index) {

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
        return null;
    }

    @Override
    public void beginFrame()
    {
        beginFrame(0);
    }

    @Override
    public void beginFrame(int frameIndex)
    {
        frameTiming = new FrameTiming();
        frameTiming.ScanoutMidpointSeconds = getCurrentTimeSecs();  // Hack to current for now - doesn't really matter
    }

    @Override
    public void endFrame() {
        GL11.glFlush();
        Display.update();
    }

    @Override
    public double getCurrentTimeSecs()
    {
        return System.nanoTime() / 1000000000d;
    }
}
