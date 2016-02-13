/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.control;

import net.minecraft.client.settings.GameSettings;

import java.util.ArrayList;


public abstract class ControlBinding {
	public interface ControlBindCallback {
		public void doneBinding();
	}
	
	boolean valid = true;
	
	public void setValid( boolean val ) { valid = val; }
	public boolean isValid() { return valid; }
	
	public static ArrayList<ControlBinding> bindings = new ArrayList<ControlBinding>();
	
	public static void CreateBindingList( GameSettings settings )
	{
		bindings.clear();

        // In game
		bindings.add( new WalkForwardBinding());
		bindings.add( new WalkBackwardBinding());
		bindings.add( new WalkLeftBinding());
		bindings.add( new WalkRightBinding());
		bindings.add( new JoystickAim.JoyAimYawBinding() );
		bindings.add( new JoystickAim.JoyAimPitchBinding() );
		bindings.add( new JoystickAim.JoyAimCenterBinding() );
		bindings.add( new KeyControlBinding( settings.keyBindAttack ));
		bindings.add( new KeyControlBinding( settings.keyBindUseItem ));
		bindings.add( new KeyControlBinding( settings.keyBindJump ));
		bindings.add( new ItemLeftControlBinding() );
		bindings.add( new ItemRightControlBinding() );
		bindings.add( new KeyControlBinding( settings.keyBindDrop ));
		bindings.add( new KeyControlBinding( settings.keyBindChat ));
		bindings.add( new KeyControlBinding( settings.keyBindSneak ));
		bindings.add( new KeyControlBinding( settings.keyBindPickBlock ));
		bindings.add( new InventoryBinding( settings.keyBindInventory ));
		bindings.add( new MenuBinding() );
		bindings.add( new KeyControlBinding( settings.keyBindPlayerList ));
		//TODO: read from settings.keyBindings, instead, which is populated by Forge.

        // Gui navigation related
        bindings.add( new GuiScreenNavigator.GuiCursorAimPitchBinding() );
        bindings.add( new GuiScreenNavigator.GuiCursorAimYawBinding() );
		bindings.add( new GuiScreenNavigator.GuiUpBinding() );
		bindings.add( new GuiScreenNavigator.GuiDownBinding() );
		bindings.add( new GuiScreenNavigator.GuiLeftBinding() );
		bindings.add( new GuiScreenNavigator.GuiRightBinding() );
		bindings.add( new GuiScreenNavigator.GuiSelectBinding() );
		bindings.add( new GuiScreenNavigator.GuiAltSelectBinding() );
		bindings.add( new GuiScreenNavigator.GuiBackBinding() );
		bindings.add( new GuiScreenNavigator.GuiShiftBinding() );
	}
	
	/**
	 * @return True if this binding is for both positive and negative axis
	 */
	public boolean isBiAxis() { return false; }
	/**
	 * @return True if this binding is used only in the GUI (not ingame)
	 */
	public boolean isGUI() { return false; }
	
	ControlBindCallback callback = null;
	
	public void setDoneBindingCallback( ControlBindCallback callback ) {
		this.callback = callback;
	}
	
	public void doneBinding() {
		if( callback != null )
			callback.doneBinding();
	}
	
	public ControlBinding( String desc, String key ) {
		this.description = desc;
		this.key = key;
	}
	public String key;

	String description;
	public abstract void setValue( float value );
	public abstract void setState( boolean state );
	public String getDescription() {
		return description;
	}
	
	String bound = "None";
	public String boundTo() {
		return bound;
	}
	public void bindTo(String name) {
		System.out.println("[Minecrift]"+getDescription()+" bound to "+name+"!");
		bound = name;
	}
}
