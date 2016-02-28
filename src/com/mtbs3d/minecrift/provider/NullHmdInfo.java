package com.mtbs3d.minecrift.provider;

import com.mtbs3d.minecrift.api.*;
import de.fruitfly.ovr.UserProfileData;
import de.fruitfly.ovr.structs.HmdParameters;

/**
 * Created by StellaArtois on 2/27/2016.
 */
public class NullHmdInfo extends BasePlugin implements IBasePlugin, IHMDInfo
{
    @Override
    public HmdParameters getHMDInfo() {
        return null;
    }

    @Override
    public UserProfileData getProfileData() {
        return null;
    }

    @Override
    public String getID() {
        return "null";
    }

    @Override
    public String getName() {
        return "Null HMD";
    }

    @Override
    public String getVersion() {
        return null;
    }
}
