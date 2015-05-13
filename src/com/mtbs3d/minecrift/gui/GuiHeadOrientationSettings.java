/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.gui;

import java.util.List;

import com.mtbs3d.minecrift.gui.framework.*;
import com.mtbs3d.minecrift.provider.MCHydra;
import com.mtbs3d.minecrift.provider.MCOculus;
import com.mtbs3d.minecrift.api.IBasePlugin;
import com.mtbs3d.minecrift.api.PluginManager;
import com.mtbs3d.minecrift.settings.VRSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class GuiHeadOrientationSettings  extends BaseGuiSettings implements GuiEventEx
{
    /** An array of all of EnumOption's video options. */
    static VRSettings.VrOptions[] oculusHeadOrientationOptions = new VRSettings.VrOptions[] {
            VRSettings.VrOptions.HEAD_TRACKING,
            //VRSettings.VrOptions.HEAD_TRACK_SENSITIVITY,
    };
    static VRSettings.VrOptions[] hydraHeadOrientationOptions = new VRSettings.VrOptions[] {
            VRSettings.VrOptions.HEAD_TRACKING,
            VRSettings.VrOptions.HEAD_TRACK_SENSITIVITY,
    };
	private PluginModeChangeButton pluginModeChangeutton;
    public GuiHeadOrientationSettings(GuiScreen par1GuiScreen,
                                       VRSettings par2vrSettings)
    {
    	super( par1GuiScreen, par2vrSettings );
        screenTitle = "Head Tracking Settings";
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    public void initGui()
    {
        this.buttonList.clear();
        this.buttonList.add(new GuiButtonEx(200, this.width / 2 - 100, this.height / 6 + 168, "Done"));
        this.buttonList.add(new GuiButtonEx(201, this.width / 2 - 100, this.height / 6 + 148, "Reset To Defaults"));
        if( Minecraft.getMinecraft().headTracker instanceof MCHydra )
            this.buttonList.add(new GuiButtonEx(202, this.width / 2 - 100, this.height / 6 + 128, "Recalibrate..."));
        pluginModeChangeutton = new PluginModeChangeButton(203, this.width / 2 - 78, this.height / 6 - 14, (List<IBasePlugin>)(List<?>) PluginManager.thePluginManager.orientPlugins, this.guivrSettings.headTrackerPluginID );
        this.buttonList.add(pluginModeChangeutton);
        VRSettings.VrOptions[] var10 = null;
        if( Minecraft.getMinecraft().headTracker instanceof MCHydra )
            var10 = hydraHeadOrientationOptions;
        else
            var10 = oculusHeadOrientationOptions;

        int var11 = var10.length;

        for (int var12 = 2; var12 < var11 + 2; ++var12)
        {
            VRSettings.VrOptions var8 = var10[var12-2];
            int width = this.width / 2 - 155 + var12 % 2 * 160;
            int height = this.height / 6 + 21 * (var12 / 2) - 10;

            if (var8.getEnumFloat())
            {
                float minValue = 0.0f;
                float maxValue = 1.0f;
                float increment = 0.001f;

                if (var8 == VRSettings.VrOptions.HEAD_TRACK_SENSITIVITY)
                {
                    minValue = 0.5f;
                    maxValue = 3.0f;
                    increment = 0.01f;
                }

                GuiSliderEx slider = new GuiSliderEx(var8.returnEnumOrdinal(), width, height, var8, this.guivrSettings.getKeyBinding(var8), minValue, maxValue, increment, this.guivrSettings.getOptionFloatValue(var8));
                slider.setEventHandler(this);
                slider.enabled = getEnabledState(var8);
                this.buttonList.add(slider);
            }
            else
            {
                this.buttonList.add(new GuiSmallButtonEx(var8.returnEnumOrdinal(), width, height, var8, this.guivrSettings.getKeyBinding(var8)));
            }
        }
    }

    /**
     * Fired when a control is clicked. This is the equivalent of ActionListener.actionPerformed(ActionEvent e).
     */
    protected void actionPerformed(GuiButton par1GuiButton)
    {
        VRSettings.VrOptions num = VRSettings.VrOptions.getEnumOptions(par1GuiButton.id);

        if (par1GuiButton.enabled)
        {
            if (par1GuiButton.id < 200 && par1GuiButton instanceof GuiSmallButtonEx)
            {
                this.guivrSettings.setOptionValue(((GuiSmallButtonEx)par1GuiButton).returnVrEnumOptions(), 1);
                par1GuiButton.displayString = this.guivrSettings.getKeyBinding(VRSettings.VrOptions.getEnumOptions(par1GuiButton.id));

                if (num == VRSettings.VrOptions.HEAD_TRACK_PREDICTION)
                {
                    // TODO: Prediction
                   // mc.headTracker.setPrediction(this.mc.vrSettings.headTrackPredictionTimeSecs, this.mc.vrSettings.useHeadTrackPrediction);
                }
            }
            else if (par1GuiButton.id == 200)
            {
                Minecraft.getMinecraft().vrSettings.saveOptions();
                this.mc.displayGuiScreen(this.parentGuiScreen);
            }
            else if (par1GuiButton.id == 201)
            {
                Minecraft.getMinecraft().vrSettings.useHeadTracking = true;
                if(Minecraft.getMinecraft().headTracker instanceof MCOculus)
                {
                    Minecraft.getMinecraft().vrSettings.useHeadTrackPrediction = true;
                    Minecraft.getMinecraft().vrSettings.headTrackPredictionTimeSecs = 0f;
                    // TODO: Prediction
                    //Minecraft.getMinecraft().headTracker.setPrediction(this.mc.vrSettings.headTrackPredictionTimeSecs, this.mc.vrSettings.useHeadTrackPrediction);
                }
                Minecraft.getMinecraft().vrSettings.setHeadTrackSensitivity(1.0f);
                this.reinit = true;
            }
            else if (par1GuiButton.id == 202)
            {
            	this.mc.entityRenderer.startCalibration();
            }
            else if (par1GuiButton.id == 203) // Mode Change
            {
                Minecraft.getMinecraft().vrSettings.headTrackerPluginID = pluginModeChangeutton.getSelectedID();
                Minecraft.getMinecraft().vrSettings.saveOptions();
                Minecraft.getMinecraft().headTracker = PluginManager.configureOrientation(Minecraft.getMinecraft().vrSettings.headTrackerPluginID);
                this.reinit = true;
            }
        }
    }

    @Override
    public boolean event(int id, VRSettings.VrOptions enumm)
    {
        if (enumm == VRSettings.VrOptions.HEAD_TRACK_PREDICTION_TIME)
        {
            // TODO: Prediction
            //mc.headTracker.setPrediction(this.mc.vrSettings.headTrackPredictionTimeSecs, this.mc.vrSettings.useHeadTrackPrediction);
        }

        return true;
    }

    @Override
    public boolean event(int id, String s) {
        return true;
    }

    private boolean getEnabledState(VRSettings.VrOptions var8)
    {
        String s = var8.getEnumString();

        if (var8 == VRSettings.VrOptions.HEAD_TRACK_SENSITIVITY)
        {
            return false;
//            else     // TODO: If head track sensitivity is ever working....if
//                return true;
        }

        return true;
    }

    @Override
    protected String[] getTooltipLines(String displayString, int buttonId)
    {
        VRSettings.VrOptions e = VRSettings.VrOptions.getEnumOptions(buttonId);
    	if( e != null )
    	switch(e)
    	{
    	case HEAD_TRACKING:
    		return new String[] {
    				"If head tracking should be enabled or not", 
    				"  OFF: No head tracking",
    				"  ON: Head tracking enabled",
    				"  Recommended: ON"} ;
    	case HEAD_TRACK_PREDICTION:
    		return new String[] {
    				"For the Oculus Rift, enable Prediction?", 
    				" OFF: Prediction disabled",
    				" ON:  Prediction enabled",
    				" Recommended value: ON to reduce latency" } ;
    	case HEAD_TRACK_SENSITIVITY:
    		return new String[] {
    				"In-game camera orientation multiplied by this value.", 
    				"  Recommended value: 1.0",
                    "NOTE: Will be locked at 1.0 if the Orientation render",
                    "mode is set to 'Quaternion'."} ;
    	case HEAD_TRACK_PREDICTION_TIME:
    		return new String[] {
    				"Number of seconds to predict motion. Higher values will",
    				"enhance the perceived precision of slow movements, but ",
    				"cause issues with sudden movements. Auto attempts to",
                    "dynamically set the value based on previous frame time.",
    				"  Recommended value: AUTO (set to 0)"};
    	default:
    		return null;
    	}
    	else
    	switch(buttonId)
    	{
	    	case 201:
	    		return new String[] {
	    			"Resets all values on this screen to their defaults"
	    		};
	    	case 202:
	    		return new String[] {
	    			"Starts calibration of the Oculus Rift headset",
	    			"  Press this button while facing forward.",
	    			"  Then, look to the left.",
	    			"  Then, look to the right.",
	    			"  Then, look up."
	    		};
    		default:
    			return null;
    	}
    }
}
