/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.gui;

import com.mtbs3d.minecrift.api.IStereoProvider;
import com.mtbs3d.minecrift.gui.framework.BaseGuiSettings;
import com.mtbs3d.minecrift.gui.framework.GuiButtonEx;
import com.mtbs3d.minecrift.gui.framework.GuiSmallButtonEx;
import com.mtbs3d.minecrift.gui.framework.VROption;
import com.mtbs3d.minecrift.settings.VRSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;


public class GuiMinecriftSettings extends BaseGuiSettings
{
    public static final int PROFILES_ID = 915;


    static VROption[] vrOnDeviceList = new VROption[]
        {
            // VIVE START - hide options not relevant to teleport/room scale
            new VROption(202,                                      VROption.Position.POS_RIGHT,  2,  VROption.ENABLED, "HUD Settings..."),
            new VROption(206,                                      VROption.Position.POS_LEFT,   1f, VROption.ENABLED, "Stereo Rendering..."),
            new VROption(VRSettings.VrOptions.VR_RENDERER,         VROption.Position.POS_RIGHT,  1f, VROption.DISABLED, null),
            new VROption(209,                                      VROption.Position.POS_LEFT,   2f, VROption.ENABLED, "Locomotion Settings..."),
            new VROption(VRSettings.VrOptions.REVERSE_HANDS,       VROption.Position.POS_LEFT,   3f, VROption.ENABLED, null),
            new VROption(210, 							           VROption.Position.POS_RIGHT,  3f, VROption.ENABLED, "Chat/Crosshair Settings..."),
            new VROption(220, 							           VROption.Position.POS_LEFT,   4f, VROption.ENABLED, "Controller Buttons..."),
            
            
            // VIVE END - hide options not relevant to teleport/room scale
        };

    /** An array of all of EnumOption's video options. */

    GameSettings settings;

    public GuiMinecriftSettings( GuiScreen par1GuiScreen,
                                VRSettings par2vrSettings,
                                GameSettings gameSettings)
    {
    	super( par1GuiScreen, par2vrSettings );
    	screenTitle = "VR Settings";
        settings = gameSettings;
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    public void initGui()
    {
    	this.buttonList.clear();
    	int profileButtonWidth = 240;
    	GuiSmallButtonEx profilesButton = new GuiSmallButtonEx(PROFILES_ID, (this.width / 2 - 155 + 1 * 160 / 2) - ((profileButtonWidth - 150) / 2), this.height / 6 - 14, profileButtonWidth, 20, "Profile: " + VRSettings.getCurrentProfile());
    	this.buttonList.add(profilesButton);
    	this.buttonList.add(new GuiButtonEx(200, this.width / 2 - 100, this.height / 6 + 168, "Done"));
    	VROption[] buttons = null;

    	buttons = vrOnDeviceList;

    	for (VROption var8 : buttons)
    	{
    		int width = var8.getWidth(this.width);
    		int height = var8.getHeight(this.height);

    		{
    			GuiSmallButtonEx button = new GuiSmallButtonEx(var8.getOrdinal(), width, height, var8._e, var8.getButtonText());
    			button.enabled = var8._enabled;
    			this.buttonList.add(button);
    		}
    	}
    }

    /**
     * Fired when a control is clicked. This is the equivalent of ActionListener.actionPerformed(ActionEvent e).
     */
    protected void actionPerformed(GuiButton par1GuiButton)
    {
        if (par1GuiButton.enabled)
        {
            VRSettings vr = Minecraft.getMinecraft().vrSettings;
//            IHMDInfo hmdInfo = Minecraft.getMinecraft().hmdInfo;
            IStereoProvider stereoProvider = Minecraft.getMinecraft().stereoProvider;
//            IOrientationProvider headTracker = Minecraft.getMinecraft().headTracker;
//            IEyePositionProvider positionTracker = Minecraft.getMinecraft().positionTracker;

            if (par1GuiButton.id < 200 && par1GuiButton instanceof GuiSmallButtonEx)
            {
                VRSettings.VrOptions num = VRSettings.VrOptions.getEnumOptions(par1GuiButton.id);
                this.guivrSettings.setOptionValue(((GuiSmallButtonEx)par1GuiButton).returnVrEnumOptions(), 1);
                par1GuiButton.displayString = this.guivrSettings.getKeyBinding(VRSettings.VrOptions.getEnumOptions(par1GuiButton.id));

                if (num == VRSettings.VrOptions.USE_VR)
                {
                    Minecraft.getMinecraft().reinitFramebuffers = true;
                    this.reinit = true;
                }
            }
            else if (par1GuiButton.id == 201)
            {
                Minecraft.getMinecraft().vrSettings.saveOptions();
              //  this.mc.displayGuiScreen(new GuiPlayerPreferenceSettings(this, this.guivrSettings));
            }
            else if (par1GuiButton.id == 202)
            {

                    Minecraft.getMinecraft().vrSettings.saveOptions();
                    this.mc.displayGuiScreen(new GuiHUDSettings(this, this.guivrSettings));

            }
            else if (par1GuiButton.id == 206)
            {

                    Minecraft.getMinecraft().vrSettings.saveOptions();
	                this.mc.displayGuiScreen(new GuiRenderOpticsSettings(this, this.guivrSettings, this.settings));

            } 
            else if (par1GuiButton.id == 200)
            {
                Minecraft.getMinecraft().vrSettings.saveOptions();
                this.mc.displayGuiScreen(this.parentGuiScreen);
            }
            else if (par1GuiButton.id == 209)
            {
                this.guivrSettings.saveOptions();
                this.mc.displayGuiScreen(new GuiLocomotionSettings(this, this.guivrSettings));
            }
            else if (par1GuiButton.id == 210)
            {
                this.guivrSettings.saveOptions();
                this.mc.displayGuiScreen(new GuiOtherHUDSettings(this, this.guivrSettings));
            }
            else if (par1GuiButton.id == 220)
            {
                this.guivrSettings.saveOptions();
                this.mc.displayGuiScreen(new GuiVRControls(this, this.guivrSettings));
            }
            else if (par1GuiButton.id == PROFILES_ID)
            {
                Minecraft.getMinecraft().vrSettings.saveOptions();
                this.mc.displayGuiScreen(new GuiSelectSettingsProfile(this, this.guivrSettings));
            }
        }
    }

    @Override
    protected String[] getTooltipLines(String displayString, int buttonId)
    {
        VRSettings.VrOptions e = VRSettings.VrOptions.getEnumOptions(buttonId);

    	if( e != null )
    	switch(e)
    	{
    	case USE_VR:
    		return new String[] {
				"Whether to enable all the fun new Virtual Reality features",
				"  ON: Yay Fun!",
				"  OFF: Sad vanilla panda: gameplay unchanged"
    		};
     	case REVERSE_HANDS:
    		return new String[] {
				"Swap left/right hands as dominant",
				"  ON: Left dominant, weirdo.",
				"  OFF: Right dominant"
    		};
        case USE_VR_COMFORT:
            return new String[] {
                    "Enables view ratcheting on controller yaw or pitch input.",
                    "For some people this can allow a more comfortable",
                    "viewing experience while moving around. Known as",
                    "'VR Comfort Mode' (with thanks to Cloudhead Games)!",
                    "  OFF: (Default) No view ratcheting is applied.",
                    "  Yaw Only: View ratcheting applied to Yaw only.",
                    "  Pitch Only: View ratcheting applied to Pitch only.",
                    "  Yaw and Pitch: You guessed it...",
            } ;
            default:
    		return null;
    	}
    	else
    	switch(buttonId)
    	{
            case 201:
                return new String[] {
                        "Open this configuration screen to adjust the Player",
                        "  avatar preferences, select Oculus profiles etc.",
                        "  Ex: IPD, Player (Eye) Height"
                };
            case 202:
                return new String[] {
                        "Open this configuration screen to adjust the Head",
                        "Up Display (HUD) overlay properties.",
                        "  Ex: HUD size, HUD distance, Crosshair options"
                };
            case 203:
                return new String[] {
                        "Open this configuration screen to adjust device",
                        "calibration settings.",
                        "  Ex: Initial calibration time"
                };
	    	case 205:
	    		return new String[] {
	    			"Open this configuration screen to adjust the Head",
	    			"  Tracker orientation (direction) settings. ",
	    			"  Ex: Head Tracking Selection (Hydra/Oculus), Prediction"
	    		};
	    	case 206:
	    		return new String[] {
	    			"Open this configuration screen to adjust the Head ",
	    			"  Mounted Display optics or other rendering features.",
	    			"  Ex: FOV, Distortion, FSAA, Chromatic Abberation"
	    		};
	    	case 207:
	    		return new String[] {
	    			"Open this configuration screen to adjust the Head",
	    			"  Tracker position settings. ",
	    			"  Ex: Head Position Selection (Hydra/None), " ,
	    			"       Hydra head placement (left, right, top etc)"
	    		};
	    	case 208:
	    		return new String[] {
	    			"Open this configuration screen to adjust how the ",
	    			"  character is controlled. ",
	    			"  Ex: Look/move/aim decouple, joystick sensitivty, " ,
	    			"     Keyhole width, Mouse-pitch-affects camera" ,
	    		};
            case 209:
                return new String[] {
                        "Configure the locomotion based settings: movement",
                        "attributes, VR comfort mode etc..."
                } ;
            case 211:
                return new String[] {
                        "Resets the origin point to your current head",
                        "position. HOTKEY - F12 or RCtrl-Ret"
                };
            case PROFILES_ID:
                return new String[] {
                        "Open this configuration screen to manage",
                        "configuration profiles."
                };
    		default:
    			return null;
    	}
    }

}
