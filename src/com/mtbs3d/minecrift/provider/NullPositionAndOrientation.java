package com.mtbs3d.minecrift.provider;

import com.mtbs3d.minecrift.api.BasePlugin;
import com.mtbs3d.minecrift.api.IEyePositionProvider;
import com.mtbs3d.minecrift.api.IOrientationProvider;
import de.fruitfly.ovr.enums.EyeType;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Quaternion;

/**
 * Created by StellaArtois on 2/27/2016.
 */
public class NullPositionAndOrientation extends BasePlugin implements IOrientationProvider, IEyePositionProvider
{
    private final Quaternion IDENTITY = new Quaternion();
    private final Vec3 POS = Vec3.createVectorHelper(0,0,0);

    @Override
    public String getID() {
        return "null";
    }

    @Override
    public String getName() {
        return "None";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public float getHeadYawDegrees(EyeType eye) {
        return 0f;
    }

    @Override
    public float getHeadPitchDegrees(EyeType eye) {
        return 0f;
    }

    @Override
    public float getHeadRollDegrees(EyeType eye) {
        return 0f;
    }

    @Override
    public Quaternion getOrientationQuaternion(EyeType eye) {
        return IDENTITY;
    }

    @Override
    public void resetOrigin() {

    }

    @Override
    public void update(float ipd, float yawHeadDegrees, float pitchHeadDegrees, float rollHeadDegrees,
                       float worldYawOffsetDegrees, float worldPitchOffsetDegrees, float worldRollOffsetDegrees) {

    }

    @Override
    public Vec3 getCenterEyePosition() {
        return POS;
    }

    @Override
    public Vec3 getEyePosition(EyeType eye) {
        return POS;
    }

    @Override
    public void resetOriginRotation() {

    }

    @Override
    public void setPrediction(float delta, boolean enable) {

    }

	@Override
	public boolean endFrame(EyeType eye) {
		// TODO Auto-generated method stub
		return false;
	}
}
