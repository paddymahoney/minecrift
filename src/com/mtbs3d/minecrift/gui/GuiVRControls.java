/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.gui;

import com.mtbs3d.minecrift.control.ControlBinding;
import com.mtbs3d.minecrift.control.ControlBinding.ControlBindCallback;
import com.mtbs3d.minecrift.settings.VRSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.EnumChatFormatting;

public class GuiVRControls extends BaseGuiSettings {

	ControlSlot slots;
	class ControlSlot extends GuiSlot implements ControlBindCallback {

		int currentlyBinding;
		int selected;
		public ControlSlot(GuiVRControls parent) {
			super(parent.mc, parent.width, parent.height,32,parent.height - 64, 12 );
			currentlyBinding = -1;
			selected = -1;
		}

		@Override
		protected int getSize() {
			return ControlBinding.bindings.size();
		}

		@Override
		protected void elementClicked(int controlIndex, boolean var2, int mousex, int mousey) {
			if( currentlyBinding > -1 && currentlyBinding != controlIndex ) {
				ControlBinding.bindings.get(currentlyBinding).doneBinding();
                Minecraft.getMinecraft().lookaimController.mapBinding(null);
			}
			selected = controlIndex;
			if( var2 ) {
				currentlyBinding = controlIndex;
				ControlBinding.bindings.get(currentlyBinding).setDoneBindingCallback(this);
                Minecraft.getMinecraft().lookaimController.mapBinding(ControlBinding.bindings.get(currentlyBinding));
			}
			
		}

		@Override
		protected boolean isSelected(int var1) {
			return selected == var1;
		}

		@Override
		protected void drawBackground() {
			drawDefaultBackground();
		}

		@Override
		protected void drawSlot(int index, int xPos, int yPos, int height, Tessellator var5, int mousex, int mousey) {
			String display = "";
			if( index == currentlyBinding ) {
                ControlBinding binding = ControlBinding.bindings.get(index);
				display = binding.getDescription()+": " + EnumChatFormatting.WHITE + "> " + EnumChatFormatting.YELLOW + "??? " + EnumChatFormatting.WHITE + "<";
			} else { 
				ControlBinding binding = ControlBinding.bindings.get(index);
				display = (binding.isValid() ? "" : (""+ EnumChatFormatting.RED )) + binding.getDescription()+": "+binding.boundTo();
			}
			drawString(fontRendererObj,display, xPos, yPos, 8421504);
		}

		@Override
		public void doneBinding() {
			currentlyBinding = -1;
		}
		
	}
	
	public GuiVRControls(GuiScreen par1GuiScreen, VRSettings par2vrSettings) {
		super(par1GuiScreen, par2vrSettings);
        screenTitle = "Control Remapping";
	}

    /**
     * Fired when a key is typed. This is the equivalent of KeyListener.keyTyped(KeyEvent e).
     */
    protected void keyTyped(char par1, int par2)
    {
        if ( slots.currentlyBinding >= 0) {
        	slots.doneBinding();
        } else {
            super.keyTyped(par1, par2);
        }
    }


    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    public void initGui() {
        this.buttonList.clear();
        //this.buttonList.add(new GuiButtonEx(202, this.width / 2 - 100, this.height / 6 + 148, "Reset To Defaults"));
        this.buttonList.add(new GuiButtonEx(200, this.width / 2 - 100, this.height / 6 + 168, "Done"));
        
        slots = new ControlSlot(this);
        slots.func_148134_d(201, 202);
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int par1, int par2, float par3) {
        if (reinit) {
            initGui();
            reinit = false;
        }
        this.slots.func_148128_a(par1,par2,par3);
        super.drawScreen(par1,par2,par3,false);
    }

    /**
     * Fired when a control is clicked. This is the equivalent of ActionListener.actionPerformed(ActionEvent e).
     */
    protected void actionPerformed(GuiButton par1GuiButton) {
    	if (par1GuiButton.id == 200) {
            this.guivrSettings.saveOptions();
            this.mc.displayGuiScreen(this.parentGuiScreen);
        }
    }
}
