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
	public String pluginID = "BasePlugin";
	public String pluginName = "BasePlugin - Not Named!";

	/**
	 * Constructs, initializes, and registers plugin
	 */
	public BasePlugin()
	{
		PluginManager.register(this);
	}

	public void eventNotification(int eventId) {

	}

	public String getInitializationStatus() {
		return null;
	}

	public boolean init() throws Exception {
		return false;
	}

	public boolean isInitialized() {
		return true;
	}

	public void poll(long frameIndex) throws Exception {

	}

	public void destroy() {

	}

	public void beginFrame()
	{
		beginFrame(0);
	}

	public void beginFrame(long frameIndex)
	{

	}

	public boolean endFrame() {
		return true;
	}


}
