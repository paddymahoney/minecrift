package com.mtbs3d.minecrift.gui;

import com.mtbs3d.minecrift.settings.VRSettings;

import de.fruitfly.ovr.enums.EyeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class GuiLocomotionSettings extends BaseGuiSettings implements GuiEventEx
{
    static VRSettings.VrOptions[] locomotionSettings = new VRSettings.VrOptions[]
    {
            VRSettings.VrOptions.ALLOW_FORWARD_PLUS_STRAFE,
            VRSettings.VrOptions.WALK_UP_BLOCKS,
            VRSettings.VrOptions.MOVEMENT_MULTIPLIER,
            VRSettings.VrOptions.DUMMY,
            VRSettings.VrOptions.DUMMY,
            VRSettings.VrOptions.DUMMY,
            VRSettings.VrOptions.USE_VR_COMFORT,
            VRSettings.VrOptions.VR_COMFORT_TRANSITION_LINEAR,
            VRSettings.VrOptions.VR_COMFORT_TRANSITION_ANGLE_DEGS,
            VRSettings.VrOptions.VR_COMFORT_TRANSITION_TIME_SECS,
            VRSettings.VrOptions.VR_COMFORT_TRANSITION_BLANKING_MODE,
    };

    public GuiLocomotionSettings(GuiScreen guiScreen, VRSettings guivrSettings) {
        super( guiScreen, guivrSettings );
        screenTitle = "Locomotion Settings";
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    public void initGui()
    {
        this.buttonList.clear();
        this.buttonList.add(new GuiButtonEx(ID_GENERIC_DEFAULTS, this.width / 2 - 100, this.height / 6 + 148, "Reset To Defaults"));
        this.buttonList.add(new GuiButtonEx(ID_GENERIC_DONE, this.width / 2 - 100, this.height / 6 + 168, "Done"));
        VRSettings.VrOptions[] buttons = locomotionSettings;

        for (int var12 = 2; var12 < buttons.length + 2; ++var12)
        {
            VRSettings.VrOptions var8 = buttons[var12 - 2];
            int width = this.width / 2 - 155 + var12 % 2 * 160;
            int height = this.height / 6 + 21 * (var12 / 2) - 10;

            if (var8 == VRSettings.VrOptions.DUMMY)
                continue;

            if (var8.getEnumFloat())
            {
                float minValue = 0.0f;
                float maxValue = 1.0f;
                float increment = 0.01f;

                if (var8 == VRSettings.VrOptions.MOVEMENT_MULTIPLIER)
                {
                    minValue = 0.15f;
                    maxValue = 1.0f;
                    increment = 0.01f;
                }
                else if ( var8 == VRSettings.VrOptions.VR_COMFORT_TRANSITION_ANGLE_DEGS)
                {
                    minValue = 15f;
                    maxValue = 45f;
                    increment = 15f;
                }
                else if ( var8 == VRSettings.VrOptions.VR_COMFORT_TRANSITION_TIME_SECS)
                {
                    minValue = 0f;
                    maxValue = 0.75f;
                    increment = 0.005f;
                }

                GuiSliderEx slider = new GuiSliderEx(var8.returnEnumOrdinal(), width, height, var8, this.guivrSettings.getKeyBinding(var8), minValue, maxValue, increment, this.guivrSettings.getOptionFloatValue(var8));
                slider.setEventHandler(this);
                slider.enabled = getEnabledState(var8);
                this.buttonList.add(slider);
            }
            else
            {
                GuiSmallButtonEx smallButton = new GuiSmallButtonEx(var8.returnEnumOrdinal(), width, height, var8, this.guivrSettings.getKeyBinding(var8));
                smallButton.setEventHandler(this);
                smallButton.enabled = getEnabledState(var8);
                this.buttonList.add(smallButton);
            }
        }
    }

    private boolean getEnabledState(VRSettings.VrOptions var8)
    {
        String s = var8.getEnumString();

        if (this.guivrSettings.useVrComfort == this.guivrSettings.VR_COMFORT_OFF &&
               (s == VRSettings.VrOptions.VR_COMFORT_TRANSITION_ANGLE_DEGS.getEnumString() ||
                s == VRSettings.VrOptions.VR_COMFORT_TRANSITION_TIME_SECS.getEnumString() ||
                s == VRSettings.VrOptions.VR_COMFORT_TRANSITION_BLANKING_MODE.getEnumString() ||
                s == VRSettings.VrOptions.VR_COMFORT_TRANSITION_LINEAR.getEnumString()))
        {
            return false;
        }

        return true;
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int par1, int par2, float par3)
    {
        if (reinit)
        {
            initGui();
            reinit = false;
        }
        super.drawScreen(par1,par2,par3);
    }

    /**
     * Fired when a control is clicked. This is the equivalent of ActionListener.actionPerformed(ActionEvent e).
     */
    protected void actionPerformed(GuiButton par1GuiButton)
    {
        VRSettings vr = mc.vrSettings;

        if (par1GuiButton.enabled)
        {
            if (par1GuiButton.id == ID_GENERIC_DONE)
            {
                Minecraft.getMinecraft().vrSettings.saveOptions();
                this.mc.displayGuiScreen(this.parentGuiScreen);
            }
            else if (par1GuiButton.id == ID_GENERIC_DEFAULTS)
            {
                vr.useVrComfort = VRSettings.VR_COMFORT_OFF;
                vr.allowForwardPlusStrafe = true;
                vr.vrComfortTransitionLinear = false;
                vr.movementAccelerationScaleFactor = 1f;
                vr.vrComfortTransitionTimeSecs = 0.150f;
                vr.vrComfortTransitionAngleDegs = 30f;
                vr.vrComfortTransitionBlankingMode = VRSettings.VR_COMFORT_TRANS_BLANKING_MODE_OFF;
                vr.movementQuantisation = 0;
                Minecraft.getMinecraft().vrSettings.saveOptions();
                this.reinit = true;
            }
            else if (par1GuiButton instanceof GuiSmallButtonEx)
            {
                VRSettings.VrOptions num = VRSettings.VrOptions.getEnumOptions(par1GuiButton.id);
                this.guivrSettings.setOptionValue(((GuiSmallButtonEx)par1GuiButton).returnVrEnumOptions(), 1);
                par1GuiButton.displayString = this.guivrSettings.getKeyBinding(VRSettings.VrOptions.getEnumOptions(par1GuiButton.id));
            }
        }
    }

    @Override
    public void event(int id, VRSettings.VrOptions enumm)
    {
        if (enumm == VRSettings.VrOptions.USE_VR_COMFORT)
        {
            this.reinit = true;
        }
    }

    @Override
    protected String[] getTooltipLines(String displayString, int buttonId)
    {
        VRSettings.VrOptions e = VRSettings.VrOptions.getEnumOptions(buttonId);
        if( e != null )
            switch(e)
            {
                case MOVEMENT_MULTIPLIER:
                    return new String[] {
                            "Sets a movement multiplier, allowing slower movement",
                            "than default. This may help reduce locomotion induced",
                            "simulator sickness.",
                            "WARNING: May trigger anti-cheat warnings if on a",
                            "Multiplayer server!!",
                            "Defaults to 1.0 (no movement adjustment, standard",
                            "Minecraft movement speed)."
                    } ;
                case WALK_UP_BLOCKS:
                    return new String[] {
                            "Allows you to set the ability to walk up blocks without",
                            "having to jump. HOTKEY - RCtrl-B",
                            "WARNING: May trigger anti-cheat warnings if on a",
                            "Multiplayer server!!",
                            "  OFF: (Default) You will have to jump up blocks.",
                            "  ON:  You can walk up single blocks. May reduce",
                            "       locomotion induced simulator sickness for some."
                    } ;
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
                case VR_COMFORT_TRANSITION_LINEAR:
                    return new String[] {
                            "Determines how the view transitions from one ratchet",
                            "angle to the next.",
                            "  Sinusoidal: (default) The view movement accelerates",
                            "  and then decelerates to the required position. Can",
                            "  feel more natural to some.",
                            "  Linear: The view transitions to the next angle at a",
                            "  constant velocity."
                    } ;
                case VR_COMFORT_TRANSITION_BLANKING_MODE:
                    return new String[] {
                            "Determines if the view is blanked as the view",
                            "transitions from one ratchet angle to the next. This can",
                            "relieve locomotion induced motion sickness for some.",
                            "  None: (Default) No view blanking is applied.",
                            "  Black: The view is completely black during transition.",
                            "  Blink: A simulated blink. The view fades to black, and",
                            "  and then fades in again over the transition period."
                    } ;
                case VR_COMFORT_TRANSITION_TIME_SECS:
                    return new String[] {
                            "Determines how long a ratchet transition takes, in ms.",
                            "  0ms: Instant transition.",
                            "  200-400ms: Human blink speed."
                    } ;
                case VR_COMFORT_TRANSITION_ANGLE_DEGS:
                    return new String[]{
                            "Determines how many degrees a ratchet transition",
                            "rotates."
                    };
                case ALLOW_FORWARD_PLUS_STRAFE:
                    return new String[] {
                            "Determines if strafing (or sideways movement) is",
                            "allowed while moving forward or backwards.",
                            "  Allowed: (Default) Forwards and strafe movement is",
                            "  allowed at the same time. May cause motion sickness",
                            "  for some.",
                            "  Disallowed: Anything more than a small forward",
                            "  movement will cause any strafe input to be zeroed.",
                            "  This can help make movement more 'natural'."
                    } ;
                default:
                    return null;
            }
        else
            switch(buttonId)
            {
//                case 201:
//                    return new String[] {
//                            "Open this configuration screen to adjust the Head",
//                            "  Tracker orientation (direction) settings. ",
//                            "  Ex: Head Tracking Selection (Hydra/Oculus), Prediction"
//                    };
                default:
                    return null;
            }
    }
}
