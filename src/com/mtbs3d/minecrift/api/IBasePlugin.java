/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public interface IBasePlugin {

	float PIOVER180 = (float)(Math.PI/180);

    public static final int EVENT_CALIBRATION_SET_ORIGIN = 0;
    public static final int EVENT_SET_ORIGIN = 1;

	/**
	 * Plugin ID: should be fixed per plugin! Used in optionsvr.txt
	 */
	public String getID();

	/**
	 * Printable name
	 */
	public String getName();

	public String getInitializationStatus();

	public String getVersion();

	public boolean init(File nativeDir);

	public boolean init();

	public boolean isInitialized();

	public void poll(float delta);

	public void destroy();

	public boolean isCalibrated(PluginType type);

    void beginCalibration(PluginType type);

    void updateCalibration(PluginType type);
	
	public String getCalibrationStep(PluginType type);

    public void beginFrame();

    public void endFrame();
}