/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.mtbs3d.minecrift.api.PluginType;
import com.mtbs3d.minecrift.control.*;

import com.mtbs3d.minecrift.settings.profile.ProfileManager;
import com.mtbs3d.minecrift.settings.profile.ProfileReader;
import com.mtbs3d.minecrift.settings.profile.ProfileWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.src.Reflector;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;

import com.mtbs3d.minecrift.api.BasePlugin;
import com.mtbs3d.minecrift.api.IBodyAimController;
import com.mtbs3d.minecrift.control.GuiScreenNavigator;


public class MCController extends BasePlugin implements IBodyAimController
{
    public static final Logger logger = LogManager.getLogger();
	JoystickAim joyAim;
	boolean hasControllers = false;
	ControlBinding nextBind = null;
	static boolean allowControllerEnvReInit = false;

	HashMap<String,String> bindingSaves = new HashMap<String,String>();
	class BindingMap {
		HashMap<Pair<Integer,Boolean>,ControlBinding> axisBinds = new HashMap<Pair<Integer,Boolean>,ControlBinding>();
		HashMap<Integer,ControlBinding> buttonBinds = new HashMap<Integer,ControlBinding>();
		HashMap<ControlBinding,Pair<Integer,Boolean>> revAxisBinds = new HashMap<ControlBinding,Pair<Integer,Boolean>>();
		HashMap<ControlBinding,Integer> revButtonBinds = new HashMap<ControlBinding,Integer>();
		ControlBinding povXBindPos;
		ControlBinding povXBindNeg;
		ControlBinding povYBindPos;
		ControlBinding povYBindNeg;
		
		void bindAxis( ControlBinding nextBind, int index, boolean posVal, String axisName ) {
			axisName = axisName.replace("axis", "").replace("Axis", "");
			Pair<Integer,Boolean> key= Pair.of(index,posVal);
			if( !nextBind.isBiAxis() ) {
				if( axisBinds.get( key ) == null ) {
					nextBind.bindTo(axisName+(posVal?"+":"-")+" axis");
					axisBinds.put(key , nextBind);
					revAxisBinds.put(nextBind, key);
					nextBind.setValid(true);
				} else {
					nextBind.bindTo("Conflict!");
					nextBind.setValid(false);
				}
			} else {
				Pair<Integer,Boolean> key2= Pair.of(index,!posVal);
				if( axisBinds.get( key  ) == null &&
					axisBinds.get( key2 ) == null ) {
					nextBind.bindTo(axisName+" axis");
					axisBinds.put(key , nextBind);
					axisBinds.put(key2, nextBind);
					revAxisBinds.put(nextBind, key  );
					nextBind.setValid(true);
				} else {
					nextBind.bindTo("Conflict!");
					nextBind.setValid(false);
				}
			}
			if( nextBind.isValid())
				bindingSaves.put(nextBind.key, String.format("a:%d:%s:%s",index,posVal?"+":"-",axisName));
		}
		
		void bindButton( ControlBinding nextBind, int index, String buttonName ) {
			if( buttonBinds.get( index ) == null ) {
				nextBind.bindTo(buttonName+" button");
				buttonBinds.put(index, nextBind);
				revButtonBinds.put(nextBind, index);
				nextBind.setValid(true);
			} else {
				nextBind.bindTo("Conflict!");
				nextBind.setValid(false);
			}
			if( nextBind.isValid())
				bindingSaves.put(nextBind.key, String.format("b:%d:%s",index,buttonName));
		}
		
		void bindPovX( ControlBinding nextBind, boolean posVal ) {
			if( posVal ) {
				if( povXBindPos == null ) {
					nextBind.bindTo("POV X+");
					povXBindPos = nextBind;
					povXBindPos.setValid(true);
				} else {
					nextBind.bindTo("Conflict!");
					povXBindPos.setValid(false);
				}
			} else {
				if( povXBindNeg == null ) {
					nextBind.bindTo("POV X-");
					povXBindNeg = nextBind;
					povXBindNeg.setValid(true);
				} else {
					nextBind.bindTo("Conflict!");
					povXBindNeg.setValid(false);
				}
			}
			if( nextBind.isValid())
				bindingSaves.put(nextBind.key, String.format("px:%s",posVal?"+":"-"));
		}
		
		void bindPovY( ControlBinding nextBind, boolean posVal ) {
			if( posVal ) {
				if( povYBindPos == null ) {
					nextBind.bindTo("POV Y+");
					povYBindPos = nextBind;
					povYBindPos.setValid(true);
				} else {
					nextBind.bindTo("Conflict!");
					povYBindPos.setValid(false);
				}
			} else {
				if( povYBindNeg == null ) {
					nextBind.bindTo("POV Y-");
					povYBindNeg = nextBind;
					povYBindNeg.setValid(true);
				} else {
					nextBind.bindTo("Conflict!");
					povYBindNeg.setValid(false);
				}
			}
			if( nextBind.isValid())
				bindingSaves.put(nextBind.key, String.format("py:%s",posVal?"+":"-"));
		}
		
		boolean bind( ControlBinding nextBind) {
			boolean bound = false;
			Controller cont = Controllers.getEventSource();
			String alreadyBound = "";
			
			
			//Unbind a value if it is being remapped
			int index = Controllers.getEventControlIndex();
			if( revAxisBinds.containsKey(nextBind)) {
				alreadyBound = cont.getAxisName(revAxisBinds.get(nextBind).getLeft())+" axis";
				if( nextBind.isBiAxis() ) {
					int axis_index =  revAxisBinds.get(nextBind).getKey();
					axisBinds.remove( Pair.of(axis_index, true ) );
					axisBinds.remove( Pair.of(axis_index, false) );
				} else {
					axisBinds.remove( revAxisBinds.get(nextBind));
				}
				revAxisBinds.remove( nextBind );
			}
			if( revButtonBinds.containsKey(nextBind)) {
				Integer button =  revButtonBinds.get(nextBind);
				alreadyBound = cont.getButtonName(button)+" button";
				buttonBinds.remove(button);
				revButtonBinds.remove( nextBind );
			}
			if( povXBindPos == nextBind ) {
				povXBindPos = null;
				alreadyBound = "POV X+";
			} else if( povYBindPos == nextBind ) {
				povYBindPos = null;
				alreadyBound = "POV Y+";
			} else if( povXBindNeg == nextBind ) {
				povXBindNeg = null;
				alreadyBound = "POV X-";
			} else if( povYBindNeg == nextBind ) {
				povYBindNeg = null;
				alreadyBound = "POV Y-";
			} 

			if( !alreadyBound.isEmpty() )
				System.out.println( nextBind.getDescription()+" already bound to "+alreadyBound+". Removing.");

			
			//Bind to value
			if( Controllers.isEventAxis() ) {
				float joyVal = cont.getAxisValue(index);
				boolean posVal = joyVal>0;
				if(Math.abs(joyVal)>0.5f) {
					bindAxis( nextBind, index, posVal, cont.getAxisName(index));
					bound = true;
				}
			} else if( Controllers.isEventButton() ) {
				if( cont.isButtonPressed(index)) {
					bindButton( nextBind, index, cont.getButtonName(index) );
					bound = true;
				}
			} else if( Controllers.isEventPovX()) {
				if( cont.getPovX() != 0)
				{
					bindPovX(nextBind, cont.getPovX() > 0);
					bound = true;
				}
			} else if( Controllers.isEventPovY()) {
				if( cont.getPovY() != 0)
				{
					bindPovY(nextBind, cont.getPovY() > 0);
					bound = true;
				}
			}
			return bound;
		}
		
		void activate() {
			Controller cont = Controllers.getEventSource();
			int index = Controllers.getEventControlIndex();
			if( Controllers.isEventAxis() ) {
				float joyVal = cont.getAxisValue(index);
				ControlBinding bindPos = axisBinds.get( Pair.of(index,true ) );
				ControlBinding bindNeg = axisBinds.get( Pair.of(index,false) );
				if( bindPos == bindNeg ) {
					if( bindPos != null) 
						bindPos.setValue( joyVal );
				} else { 
					if( joyVal > 0 ) {
						if( bindNeg != null )
							bindNeg.setValue( 0 );
						if( bindPos != null )
							bindPos.setValue( joyVal );
					} else { 
						if( bindPos != null )
							bindPos.setValue( 0 );
						if( bindNeg != null )
							bindNeg.setValue( -joyVal );
					}
				}
			} else if( Controllers.isEventButton() ) {
				ControlBinding bind = buttonBinds.get(index);
				if( bind != null ) {
					bind.setState( cont.isButtonPressed(index));
				}
			} else if( Controllers.isEventPovX()) {
				if( cont.getPovX() > 0) {
					if( povXBindPos != null)
						povXBindPos.setState(true);
				} else if ( cont.getPovX() < 0 ) {
					if( povXBindNeg != null)
						povXBindNeg.setState(true);
				} else {
					if( povXBindPos != null)
						povXBindPos.setState(false);
					if( povXBindNeg != null)
						povXBindNeg.setState(false);
				}
			} else if( Controllers.isEventPovY()) {
				if( cont.getPovY() > 0) {
					if( povYBindPos != null)
						povYBindPos.setState(true);
				} else if ( cont.getPovY() < 0 ) {
					if( povYBindNeg != null)
						povYBindNeg.setState(true);
				} else {
					if( povYBindPos != null)
						povYBindPos.setState(false);
					if( povYBindNeg != null)
						povYBindNeg.setState(false);
				}
			}
		}
	}
	BindingMap ingame = new BindingMap();
	BindingMap GUI    = new BindingMap();
	JoystickAim[] aimTypes = new JoystickAim[] { new JoystickAim(), new JoystickAimLoose(), new JoystickRecenterAim() };
	private Minecraft mc;
	private GuiScreenNavigator screenNavigator;
	private boolean loaded = false;
    long lastIndex = -1;

	public MCController() {
		super();
		mc = Minecraft.getMinecraft();
		System.out.println("Created Controller plugin");
        pluginID = "controller";
        pluginName = "Controller";
	}

	@Override
	public String getInitializationStatus() {
		return hasControllers ? "Ready." : "No Controllers found.";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public boolean init() {
		try {
			loaded = false;
			System.out.println("[Minecrift] Init MCController");
			resetControllerEnvironment();
			Controllers.create();
			hasControllers = Controllers.getControllerCount()> 0;
			loadBindings();

			System.out.println("[Minecrift] Initialized controllers: "+getInitializationStatus());
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
		return isInitialized();
	}

	// Initial credit to Florian Enner, StackOverflow user for the general methodology!
	public void resetControllerEnvironment()
	{
		if (allowControllerEnvReInit) {
			// Create a new default controller environment - expensive
			Object defaultControllerEnvironment = Reflector.newInstance(Reflector.JInput_DefaultControllerEnv_Constructor, new Object[]{});

			// Set this as the default
			Reflector.setFieldValue(Reflector.JInput_ControllerEnv_defaultEnvironment, defaultControllerEnvironment);

			// Set Controllers created field to false
			Reflector.setFieldValue(Reflector.LWJGL_Controllers_created, false);
		}
	}

	public void allowControllerEnvReInit() {
		allowControllerEnvReInit = true;
	}

	@Override
	public boolean isInitialized() {
		return hasControllers;
	}

	@Override
	public void poll(long frameIndex) throws Exception {
        if (frameIndex <= this.lastIndex)
            return;
        this.lastIndex = frameIndex;

		JoystickAim.selectedJoystickMode = aimTypes[mc.vrSettings.joystickAimType];
		joyAim = JoystickAim.selectedJoystickMode;
		for( int c = 0; c < Controllers.getControllerCount();c++) {
			Controller cont = Controllers.getController(c);
			for( int a = 0; a < cont.getAxisCount(); a++ ) {
				cont.setDeadZone(a, mc.vrSettings.joystickDeadzone);
			}
		}
        if( this.mc.currentScreen != null && (this.screenNavigator == null || this.screenNavigator.screen != this.mc.currentScreen) )
        	this.screenNavigator = new GuiScreenNavigator(this.mc.currentScreen );
		Controllers.poll();
		while (Controllers.next()) {
			if( nextBind != null ) {
				boolean bound = false;
				if( nextBind.isGUI()) 
					bound = GUI.bind(nextBind);
				else 
					bound = ingame.bind(nextBind);
				if( nextBind instanceof InventoryBinding ||
					nextBind instanceof MenuBinding  ) //These are in both
					GUI.bind( nextBind );
				
				if(bound) {
					saveBindings();
					nextBind.doneBinding();
					nextBind = null;
				}
			} else if( mc.currentScreen == null ) {
				ingame.activate();
			} else {
				GUI.activate();
			}
		}
		Controllers.clearEvents();

        // Update the aim / pitch
        if(JoystickAim.selectedJoystickMode != null)
            JoystickAim.selectedJoystickMode.update( 1f );

        // Update gui cursor
        this.screenNavigator.guiCursor();
	}

	private void saveBindings() {
		saveBindings(null); // Use null for the current profile
	}

	private void saveBindings(JSONObject theProfiles) {
		//File bindingsSave = new File( mc.mcDataDir, "options_controller.txt");
		if (isInitialized()) {
			ProfileWriter bindingsWriter = new ProfileWriter(ProfileManager.PROFILE_SET_CONTROLLER_BINDINGS, theProfiles);
			for (Map.Entry<String, String> entry : bindingSaves.entrySet()) {
				bindingsWriter.println(entry.getKey() + ":" + entry.getValue());
			}
			bindingsWriter.close();
			System.out.println("[Minecrift] Saved MCController bindings");
		}
	}
	
	private void loadBinding( ControlBinding binding, BindingMap map, String[] bindingTokens) {
		if( bindingTokens[1].equals("a") && bindingTokens.length >= 5 ) {
			int index = Integer.parseInt(bindingTokens[2]);
			boolean posVal = bindingTokens[3].equals("+");
			map.bindAxis(binding, index, posVal, bindingTokens[4]);
		} else if (bindingTokens[1].equals("b") && bindingTokens.length >= 4) {
			int index = Integer.parseInt(bindingTokens[2]);
			map.bindButton(binding, index, bindingTokens[3]);
		} else if (bindingTokens[1].equals("px") && bindingTokens.length >= 3 ) {
			boolean posVal = bindingTokens[2].equals("+");
			map.bindPovX(binding, posVal);
		} else if (bindingTokens[1].equals("py") && bindingTokens.length >= 3 ) {
			boolean posVal = bindingTokens[2].equals("+");
			map.bindPovY(binding, posVal);
		}

	}

	private void loadBindings() {
		loadBindings(null); // Use null for the current profile
	}

	private void loadBindings(JSONObject theProfiles) {
		try {
			ProfileReader bindingsReader = new ProfileReader(ProfileManager.PROFILE_SET_CONTROLLER_BINDINGS, theProfiles);
			String line;
			while ((line = bindingsReader.readLine()) != null)
			{
				String[] bindingTokens = line.split(":");
				if( bindingTokens.length > 1 )
				{
					String key = bindingTokens[0];
					for( ControlBinding binding : ControlBinding.bindings ) {
						if( binding.key.equals(key) ) {

							if( binding.isGUI())
								loadBinding( binding, GUI, bindingTokens );
							else
								loadBinding( binding, ingame, bindingTokens );

							if( binding instanceof InventoryBinding ||
								binding instanceof MenuBinding  ) //These are in both
								loadBinding( binding, GUI, bindingTokens );
							break;
						}
					}
				}
			}
			bindingsReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		loaded = true;
		System.out.println("[Minecrift] Loaded MCController bindings");
	}

	@Override
	public void destroy() {
		Controllers.destroy();
		allowControllerEnvReInit();
	}

	@Override
	public boolean isCalibrated(PluginType type) {
		return true;
	}

    @Override
    public void beginCalibration(PluginType type) {}

    @Override
    public void updateCalibration(PluginType type) {}

	@Override
	public String getCalibrationStep(PluginType type) {
		return "";
	}

	@Override
	public void eventNotification(int eventId) {
	}

	@Override
	public float getBodyYawDegrees() {
		return JoystickAim.getBodyYaw();
	}

	@Override
	public void setBodyYawDegrees(float yawOffset) {
		JoystickAim.setAimYawOffset(yawOffset);
	}

	@Override
	public float getBodyPitchDegrees() {
    	return JoystickAim.getBodyPitch();
	}

	@Override
	public float getAimYaw() {
		return JoystickAim.getAimYaw();
	}

	@Override
	public float getAimPitch() {
		return JoystickAim.getAimPitch();
	}

	@Override
	public void mapBinding(ControlBinding binding) {
		nextBind = binding;
	}

    public void beginFrame() { beginFrame(0); }
    public void beginFrame(long frameIndex) { }
    public boolean endFrame() { return true; }

    @Override
    public double ratchetingYawTransitionPercent()
    {
        if (joyAim == null)
            return -1d;

        return this.joyAim.getYawTransitionPercent();
    }

    @Override
    public double ratchetingPitchTransitionPercent()
    {
        if (joyAim == null)
            return -1d;

        return this.joyAim.getPitchTransitionPercent();
    }

    @Override
    public boolean initBodyAim() {
		allowControllerEnvReInit();
        return init();
    }

	@Override
	public void saveOptions() {
		if (loaded)
			saveBindings();
	}

	@Override
	public void loadDefaults() {
		ProfileManager.loadControllerDefaults();
		loadBindings();
	}

	@Override
	public void triggerYawTransition(boolean isPositive) {
		this.joyAim.triggerYawChange(isPositive);
	}
}
