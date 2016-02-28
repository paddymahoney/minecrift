package com.mtbs3d.minecrift.provider;

import com.mtbs3d.minecrift.api.*;
import com.valvesoftware.openvr.OpenVR;
import de.fruitfly.ovr.UserProfileData;
import de.fruitfly.ovr.enums.EyeType;
import de.fruitfly.ovr.structs.*;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Quaternion;

/**
 * Created by StellaArtois on 2/27/2016.
 */
public class MCOpenVR extends OpenVR
    implements IEyePositionProvider, IOrientationProvider, IBasePlugin, IHMDInfo, IStereoProvider, IEventNotifier, IEventListener
{
    public MCOpenVR()
    {
        super();
        PluginManager.register(this);
    }

    @Override
    public String getID() {
        return "openvr";
    }

    @Override
    public String getName() {
        return "OpenVR";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public void eventNotification(int eventId) {

    }

    @Override
    public void registerListener(IEventListener listener) {

    }

    @Override
    public void notifyListeners(int eventId) {

    }

    @Override
    public void update(float ipd, float yawHeadDegrees, float pitchHeadDegrees, float rollHeadDegrees, float worldYawOffsetDegrees, float worldPitchOffsetDegrees, float worldRollOffsetDegrees) {

    }

    @Override
    public Vec3 getCenterEyePosition() {
        return null;
    }

    @Override
    public Vec3 getEyePosition(EyeType eye) {
        return null;
    }

    @Override
    public float getHeadYawDegrees(EyeType eye) {
        return 0;
    }

    @Override
    public float getHeadPitchDegrees(EyeType eye) {
        return 0;
    }

    @Override
    public float getHeadRollDegrees(EyeType eye) {
        return 0;
    }

    @Override
    public Quaternion getOrientationQuaternion(EyeType eye) {
        return null;
    }

    @Override
    public void resetOrigin() {

    }

    @Override
    public void resetOriginRotation() {

    }

    @Override
    public void setPrediction(float delta, boolean enable) {

    }

    @Override
    public HmdParameters getHMDInfo() {
        return null;
    }

    @Override
    public UserProfileData getProfileData() {
        return null;
    }

    @Override
    public RenderTextureInfo getRenderTextureSizes(FovPort LeftFov, FovPort RightFov, float renderScaleFactor) {
        return null;
    }

    @Override
    public boolean providesMirrorTexture() {
        return false;
    }

    @Override
    public int createMirrorTexture(int width, int height) {
        return 0;
    }

    @Override
    public void deleteMirrorTexture() {

    }

    @Override
    public boolean providesRenderTextures() {
        return false;
    }

    @Override
    public RenderTextureSet createRenderTextureSet(int lwidth, int lheight, int rwidth, int rheight) {
        return null;
    }

    @Override
    public void deleteRenderTextures() {

    }

    @Override
    public EyeType eyeRenderOrder(int index) {
        return null;
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
    public boolean isGuiOrtho() {
        return false;
    }

    @Override
    public double getFrameTiming() {
        return 0;
    }

    @Override
    public Matrix4f getProjectionMatrix(FovPort fov, float nearClip, float farClip) {
        return null;
    }

    @Override
    public double getCurrentTimeSecs() {
        return 0;
    }

    @Override
    public boolean setCurrentRenderTextureInfo(int index, int textureIdx, int depthId, int depthWidth, int depthHeight) {
        return false;
    }

    @Override
    public void configureRenderer(GLConfig cfg) {

    }

    @Override
    public void poll(long frameIndex) throws Exception {

    }

    @Override
    public boolean isCalibrated(PluginType type) {
        return false;
    }

    @Override
    public void beginCalibration(PluginType type) {

    }

    @Override
    public void updateCalibration(PluginType type) {

    }

    @Override
    public String getCalibrationStep(PluginType type) {
        return null;
    }

    @Override
    public void beginFrame() {

    }

    @Override
    public void beginFrame(long frameIndex) {

    }

    @Override
    public boolean endFrame() {
        return false;
    }
}
