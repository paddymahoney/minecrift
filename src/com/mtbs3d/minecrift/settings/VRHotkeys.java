/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.settings;

import com.mtbs3d.minecrift.api.IBasePlugin;
import com.mtbs3d.minecrift.api.PluginManager;
import com.mtbs3d.minecrift.settings.VRSettings;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class VRHotkeys {

	public static void handleKeyboardInputs(Minecraft mc)
	{                                
		// Capture Minecrift key events

	    //  Reinitialise head tracking
	    if (Keyboard.getEventKey() == Keyboard.KEY_BACK && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
	    {
            PluginManager.destroyAll();
            mc.printChatMessage("Re-initialising all plugins (RCTRL+BACK): done");
	    }

        // Reset positional track origin
        if ((Keyboard.getEventKey() == Keyboard.KEY_RETURN && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) || Keyboard.isKeyDown(Keyboard.KEY_F12))
        {
            mc.vrSettings.posTrackResetPosition = true;
            mc.printChatMessage("Reset origin (RCTRL+RET or F12): done");
        }

        // Debug aim
        if (Keyboard.getEventKey() == Keyboard.KEY_RSHIFT && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
        {
            mc.vrSettings.storeDebugAim = true;
            mc.printChatMessage("Show aim (RCTRL+RSHIFT): done");
        }

        // Walk up blocks
        if (Keyboard.getEventKey() == Keyboard.KEY_B && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
        {
            mc.vrSettings.walkUpBlocks = !mc.vrSettings.walkUpBlocks;
            mc.vrSettings.saveOptions();
            mc.printChatMessage("Walk up blocks (RCTRL+B): " + (mc.vrSettings.walkUpBlocks ? "YES" : "NO"));
        }

        // Render full player model or just an disembodied hand...
        if (Keyboard.getEventKey() == Keyboard.KEY_H && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
        {
            mc.vrSettings.renderFullFirstPersonModelMode++;
            if (mc.vrSettings.renderFullFirstPersonModelMode > VRSettings.RENDER_FIRST_PERSON_NONE)
                mc.vrSettings.renderFullFirstPersonModelMode = VRSettings.RENDER_FIRST_PERSON_FULL;

            mc.vrSettings.saveOptions();
            switch (mc.vrSettings.renderFullFirstPersonModelMode)
            {
            case VRSettings.RENDER_FIRST_PERSON_FULL:
                mc.printChatMessage("First person model (RCTRL-H): Full");
                break;
            case VRSettings.RENDER_FIRST_PERSON_HAND:
                mc.printChatMessage("First person model (RCTRL-H): Hand");
                break;
            case VRSettings.RENDER_FIRST_PERSON_NONE:
                mc.printChatMessage("First person model (RCTRL-H): None");
                break;
            }
        }

        // If an orientation plugin is performing calibration, space also sets the origin
        if (Keyboard.isKeyDown(Keyboard.KEY_SPACE))
        {
            PluginManager.notifyAll(IBasePlugin.EVENT_CALIBRATION_SET_ORIGIN);
        }
	}
}
