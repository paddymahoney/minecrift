package com.mtbs3d.minecrift.provider;

import com.mtbs3d.minecrift.api.BasePlugin;
import com.mtbs3d.minecrift.api.IStereoProvider;
import com.mtbs3d.minecrift.api.PluginType;
import de.fruitfly.ovr.enums.EyeType;
import de.fruitfly.ovr.structs.*;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/**
 * Created by StellaArtois on 26/6/2014.
 */
public class NullStereoRenderer extends BasePlugin implements IStereoProvider
{
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
    public RenderTextureInfo getRenderTextureSizes(FovPort LeftFov,
                                                   FovPort RightFov,
                                                   float renderScaleFactor)
    {
        return null;
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
    public double getFrameTiming() { return (double)System.currentTimeMillis() / 1000d; }
    
    @Override
    public Matrix4f getProjectionMatrix(FovPort fov, EyeType eyeType, float nearClip, float farClip) {
        return null;
    } // VIVE included eyeType

    @Override
    public String getInitializationStatus() {
        return null;
    }

    @Override
    public String getVersion() {
        return "1.0";
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
    public void poll(long frameIndex) {

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
    public void beginFrame(long frameIndex)
    {

    }

    @Override
    public boolean endFrame() {
        return true;
    }

    @Override
    public double getCurrentTimeSecs()
    {
        return System.nanoTime() / 1000000000d;
    }

    @Override
    public boolean providesMirrorTexture() { return false; }

    @Override
    public int createMirrorTexture(int width, int height)
    {
        return -1;
    }

    @Override
    public void deleteMirrorTexture() {}

    @Override
    public boolean providesRenderTextures() { return false; }

    @Override
    public RenderTextureSet createRenderTexture(int lwidth, int lheight)
    { 
        return null;
    }

	@Override
	public boolean setCurrentRenderTextureInfo(int index, int textureIdx, int depthId, int depthWidth, int depthHeight) {
		return true;
	}

    @Override
    public void deleteRenderTextures() {}

    @Override
    public String getLastError() { return "Success"; }

    @Override
    public void configureRenderer(GLConfig cfg) {}

    // VIVE START
    public void onGuiScreenChanged(GuiScreen previousScreen, GuiScreen newScreen) { }
    // VIVE END

	@Override
	public boolean endFrame(EyeType eye) {
		return this.endFrame();
	}
}
