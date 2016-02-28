package com.mtbs3d.minecrift.gui.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.mtbs3d.minecrift.api.IBasePlugin;

public class PluginModeChangeButton extends GuiSmallButtonEx {

	List<IBasePlugin> pluginList;
	Iterator<IBasePlugin> iterPlugin;
	IBasePlugin currentPlugin;

    public PluginModeChangeButton(int par1, int par2, int par3, List<IBasePlugin> pluginList, String pluginID)
    {
		super(par1, par2, par3, "Mode: Unknown" );
		this.pluginList = pluginList;
        setPluginByID(pluginID);
		displayString = "Mode: "+ currentPlugin.getName();
	}

    public boolean setPluginByID(String pluginID)
    {
        boolean success = false;
        iterPlugin = this.pluginList.iterator();
        while( iterPlugin.hasNext() )
        {
            currentPlugin = iterPlugin.next();
            if( currentPlugin.getID().equals(pluginID) ) {
                success = true;
                break;
            }
        }
        if (!success) {
            currentPlugin = null;
        }
        return success;
    }

    public boolean setPluginByName(String pluginName)
    {
        boolean success = false;
        iterPlugin = this.pluginList.iterator();
        while( iterPlugin.hasNext() )
        {
            currentPlugin = iterPlugin.next();
            if( currentPlugin.getName().equals(pluginName) ) {
                success = true;
                break;
            }
        }
        if (!success) {
            currentPlugin = null;
        }
        return success;
    }
    
    public String getSelectedID()
    {
    	return currentPlugin.getID();
    }

    public String getSelectedName()
    {
        return currentPlugin.getName();
    }

    public String[] getPluginIDs()
    {
        List<String> lst = new ArrayList<String>();

        Iterator<IBasePlugin> it = this.pluginList.iterator();
        while( it.hasNext() )
        {
            IBasePlugin plugin = it.next();
            lst.add(plugin.getID());
        }

        Collections.sort(lst);
        String[] array = new String[lst.size()];
        int index = 0;
        for (String value : lst) {
            array[index] = value;
            index++;
        }

        return array;
    }

    public String[] getPluginNames()
    {
        List<String> lst = new ArrayList<String>();

        Iterator<IBasePlugin> it = this.pluginList.iterator();
        while( it.hasNext() )
        {
            IBasePlugin plugin = it.next();
            lst.add(plugin.getName());
        }

        Collections.sort(lst);
        String[] array = new String[lst.size()];
        int index = 0;
        for (String value : lst) {
            array[index] = value;
            index++;
        }

        return array;
    }
}
