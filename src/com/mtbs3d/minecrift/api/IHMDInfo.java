/**
 * Copyright 2013 Mark Browning
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.api;

import de.fruitfly.ovr.*;
import de.fruitfly.ovr.structs.FovTextureInfo;
import de.fruitfly.ovr.structs.GLConfig;
import de.fruitfly.ovr.structs.HmdDesc;
import de.fruitfly.ovr.structs.Sizei;

/**
 * Implement this class to inform Minecrift of distortion, resolution, and
 * chromatic aberration parameters
 * 
 * @author Mark Browning
 *
 */
public interface IHMDInfo extends IBasePlugin
{
	public HmdDesc getHMDInfo();

    /* Gets the current user profile data */
    public UserProfileData getProfileData();
}