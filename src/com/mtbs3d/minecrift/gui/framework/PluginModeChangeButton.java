package com.mtbs3d.minecrift.gui.framework;

import java.util.Iterator;
import java.util.List;

import com.mtbs3d.minecrift.api.IBasePlugin;
import com.mtbs3d.minecrift.gui.framework.GuiSmallButtonEx;
import net.minecraft.client.Minecraft;

public class PluginModeChangeButton extends GuiSmallButtonEx {

	List<IBasePlugin> pluginList;
	Iterator<IBasePlugin> iterPlugin;
	IBasePlugin currentPlugin;
    public PluginModeChangeButton(int par1, int par2, int par3, List<IBasePlugin> _pluginList, String pluginID) {
		super(par1, par2, par3, "Mode: " );
		pluginList = _pluginList;
		iterPlugin = pluginList.iterator();
		while( iterPlugin.hasNext() )
		{
			currentPlugin = iterPlugin.next();
			if( currentPlugin.getID().equals(pluginID) )
				break;
		}
		displayString = "Mode: "+ currentPlugin.getName();
	}
    
    public String getSelectedID()
    {
    	return currentPlugin.getID();
    }

	public boolean mousePressed(Minecraft par1Minecraft, int par2, int par3)
    {
        boolean result = super.mousePressed(par1Minecraft, par2, par3);

        if( result )
        {
            setNextPlugin();
        }

        return result;
    }

    public void setNextPlugin()
    {
        //Cycle iterator
        if( !iterPlugin.hasNext())
            iterPlugin = pluginList.iterator();
        currentPlugin = iterPlugin.next();
        displayString = "Mode: "+ currentPlugin.getName();
    }
}
