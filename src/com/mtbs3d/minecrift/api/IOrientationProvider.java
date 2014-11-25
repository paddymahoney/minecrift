/**
 * Copyright 2013 Mark Browning
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.api;

import org.lwjgl.util.vector.Quaternion;

/**
 * The "head-tracking" module. Provides orientation fixed to the real-world reference frame.
 * 
 * @author Mark Browning
 *
 */
public interface IOrientationProvider extends IBasePlugin {
	
    public final float MAXPITCH = (90 * 0.98f);
    public final float MAXROLL = (180 * 0.98f);

    /**
     * Gets the Yaw(Y) from YXZ Euler angle representation of orientation
     * 
     * @return The Head Yaw, in degrees 
     */
    public float getHeadYawDegrees();

    /**
     * Gets the Pitch(X) from YXZ Euler angle representation of orientation
     * 
     * @return The Head Pitch, in degrees 
     */
    public float getHeadPitchDegrees();

    /**
     * Gets the Roll(Z) from YXZ Euler angle representation of orientation
     * 
     * @return The Head Roll, in degrees 
     */
    public float getHeadRollDegrees();

    /**
     * Gets the orientation quaternion
     *
     * @return quaternion w, x, y & z components
     */
    public Quaternion getOrientationQuaternion();

    /**
     * Resets the current position/orientation to the origin
     */
    public void resetOrigin();
}
