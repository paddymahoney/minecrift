/**
 * Copyright 2013 Mark Browning
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.api;

import com.mtbs3d.minecrift.control.ControlBinding;

/**
 * @author Mark Browning
 *
 */
public interface IBodyAimController extends IBasePlugin {
	
	/**
	 * Gets the body yaw. This is the direction the player will face. Head yaw is added to this to get camera yaw.
	 * @return Player Facing Yaw, in absolute game world relative degrees
	 */
	public float getBodyYawDegrees();
	/**
	 * Sets the body yaw. Used in conjunction with origin setting.
	 */
	public void setBodyYawDegrees(float yawOffset);
	/**
	 * Gets the body pitch. This is the direction the player's body will face. Head pitch is added to this to get camera pitch.
	 * @return Player Facing Pitch, in degrees
	 */
	public float getBodyPitchDegrees();

	/**
	 * Gets the "aim" yaw. This is the direction the player will "mine" or attack.
	 * @return Aiming Yaw, in absolute game world relative degrees
	 */
	public float getAimYaw();
	/**
	 * Gets the "aim" pitch. This is the direction the player will "mine" or attack.
	 * @return Player Aiming Pitch, in degrees
	 */
	public float getAimPitch();
	
	public void mapBinding(ControlBinding binding);

	public boolean initBodyAim();

    /**
     * Get the percentage through a transition of the ratcheting control
     * @return Percentage through the ratcheting transition; or -1 if not
     * transitioning
     */
	public double ratchetingYawTransitionPercent();
    public double ratchetingPitchTransitionPercent();
}
