package com.mtbs3d.minecrift.settings;

import java.lang.reflect.Field;
import com.mtbs3d.minecrift.api.IBasePlugin;
import net.minecraft.client.Minecraft;

public class PluginValue extends StringValue {

	public PluginValue(String name, String label, String defValue, String pluginFieldName ) {
		super(name, label, defValue);
		try {
			pluginField = Minecraft.class.getDeclaredField(pluginFieldName);
		} catch (Exception e) { e.printStackTrace(); }
	}
	Field pluginField;
	IBasePlugin getPlugin() {
		try {
			return (IBasePlugin)pluginField.get(Minecraft.getMinecraft());
		} catch (Exception e) { e.printStackTrace(); return null; }
	}
	@Override
	public String getDisplayString() {
		IBasePlugin plugin = getPlugin();
		if( plugin != null )
			return plugin.getName();
		return "None";
	}

	public void setValue(String value) {
		try {
			settingField.set(VRSettings.inst,value);
		} catch (Exception e) { e.printStackTrace(); }
	}

}
