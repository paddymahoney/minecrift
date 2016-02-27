/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.api;

import java.util.*;
import com.mtbs3d.minecrift.api.IBasePlugin;
import com.mtbs3d.minecrift.api.PluginType;
import net.minecraft.client.Minecraft;

public class CalibrationHelper {
    public Map<PluginType, IBasePlugin> pluginsToCalibrate = new HashMap<PluginType, IBasePlugin>();
	
	public IBasePlugin currentPlugin = null;
	public Iterator<PluginType> iterator = null;
	public String calibrationStep = "";
    PluginType type = PluginType.PLUGIN_UNKNOWN;

	public CalibrationHelper(Minecraft mc)
	{
        // Never calibrate HMDInfo; not required currently
        pluginsToCalibrate.put(PluginType.PLUGIN_LOOKAIM, mc.lookaimController);
        pluginsToCalibrate.put(PluginType.PLUGIN_POSITION, mc.positionTracker);
		pluginsToCalibrate.put(PluginType.PLUGIN_ORIENT, mc.headTracker);

        iterator = pluginsToCalibrate.keySet().iterator();
		currentPlugin = null;
	}
	
	public boolean allPluginsCalibrated()
	{
        do
		{
			if( currentPlugin == null)
			{
                type = iterator.next();
				currentPlugin = pluginsToCalibrate.get(type);
				if (currentPlugin != null) {
					currentPlugin.beginCalibration(type);
				}
			}

            if( currentPlugin != null )
            {
                currentPlugin.updateCalibration(type);
            }

			if( currentPlugin != null && currentPlugin.isCalibrated(type) )
			{
				currentPlugin = null;
			}
		} while( currentPlugin == null && iterator.hasNext() );
		
		if( currentPlugin != null )
		{
			calibrationStep = currentPlugin.getCalibrationStep(type);
			return false; //Not done yet
		}
		//Otherwise, all are calibrated
		return true;
	}
}
