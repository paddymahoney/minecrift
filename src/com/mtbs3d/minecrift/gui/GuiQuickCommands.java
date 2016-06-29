package com.mtbs3d.minecrift.gui;

import com.google.common.base.Enums;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.achievement.GuiAchievements;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;

public class GuiQuickCommands extends GuiScreen
{
    private int field_146445_a;
    private int field_146444_f;
    private static final String __OBFID = "CL_00000703";

    public enum chatcommands {
	  	  help {
		      public String toString() {
		          return "/help";
		      }
		  },
    	  gamemode {
    	      public String toString() {
    	          return "/gamemode survival";
    	      }
    	  },
    	  gamemodec {
    	      public String toString() {
    	          return "/gamemode creative";
    	      }
    	  },
    	  home {
    	      public String toString() {
    	          return "/home";
    	      }
    	  },
    	  sethome {
    	      public String toString() {
    	          return "/sethome";
    	      }
    	  },
    	  respawn {
    	      public String toString() {
    	          return "/spawn";
    	      }
    	  },
    	  hello {
    	      public String toString() {
    	          return "hi!";
    	      }
    	  },
    	  goodbye {
    	      public String toString() {
    	          return "bye!";
    	      }
    	  },
    	  followme {
    	      public String toString() {
    	          return "follow me!";
    	      }
    	  },
    	  takethis {
    	      public String toString() {
    	          return "take this!";
    	      }
    	  },
    	  thankyou {
    	      public String toString() {
    	          return "thank you!";
    	      }
    	  },
    	  lol {
    	      public String toString() {
    	          return "praise the sun!";
    	      }
    	  }
    	}
    
    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    public void initGui()
    {
    	KeyBinding.unPressAllKeys();
        this.field_146445_a = 0;
        this.buttonList.clear();
        byte var1 = -16;
        boolean var2 = true;

        	int w = 0;
        	for (int i = 0; i < chatcommands.values().length; i++) {
        		w = i > 5 ? 1 : 0;
        		chatcommands com  = chatcommands.values()[i];
        		this.buttonList.add(new GuiButton(200 + i, this.width / 2 - 125 + 127 * w, 36 + (i-6*w) * 24, 125, 20, com.toString()));        	
        	}

        	this.buttonList.add(new GuiButton(102, this.width / 2 -50, this.height -30  + var1, 100, 20, "Cancel"));

    }

    protected void actionPerformed(GuiButton button)
    {
        switch (button.id)
        {
            case 102:
            	this.mc.displayGuiScreen(new GuiIngameMenu());
            	break;
            default:
            	if(button.id >= 200) {
                	this.mc.displayGuiScreen(null);
            		Minecraft.getMinecraft().thePlayer.sendChatMessage(button.displayString);
            	}
        }
    }

    /**
     * Called from the main game loop to update the screen.
     */
    public void updateScreen()
    {
        super.updateScreen();
        ++this.field_146444_f;
    }

    /**
     * Draws the screen and all the components in it. Args : mouseX, mouseY, renderPartialTicks
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, "Quick Commands", this.width / 2, 16, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}