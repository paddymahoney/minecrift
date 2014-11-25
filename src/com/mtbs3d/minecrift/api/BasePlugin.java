/**
 * Copyright 2013 Mark Browning
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.api;

/**
 * A base API for plugins. Plugins can be loaded, initialized, queried, and polled.
 * 
 * @author Mark Browning
 *
 */
public abstract class BasePlugin implements IBasePlugin, IEventListener
{
	@Override
	public String getID() { return pluginID; };
	public String pluginID = "BasePlugin";

	@Override
	public String getName(){ return pluginName; };
	public String pluginName = "BasePlugin - Not Named!";

    public boolean getPolledThisFrame() { return polledThisFrame; }
    public boolean polledThisFrame = false;
	
	/**
	 * Constructs, initializes, and registers plugin
	 */
	public BasePlugin()
	{
		PluginManager.register(this);
	}
}
