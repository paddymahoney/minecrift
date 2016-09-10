package com.mtbs3d.minecrift.provider;

import com.mtbs3d.minecrift.api.*;
import com.mtbs3d.minecrift.control.VRControllerButtonMapping;
import com.mtbs3d.minecrift.control.ViveButtons;
import com.mtbs3d.minecrift.render.QuaternionHelper;
import com.mtbs3d.minecrift.settings.VRHotkeys;
import com.mtbs3d.minecrift.settings.VRSettings;
import com.mtbs3d.minecrift.utils.KeyboardSimulator;
import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import de.fruitfly.ovr.UserProfileData;
import de.fruitfly.ovr.structs.*;
import de.fruitfly.ovr.structs.Matrix4f;
import de.fruitfly.ovr.structs.Vector2f;
import de.fruitfly.ovr.structs.Vector3f;
import de.fruitfly.ovr.util.BufferUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Minecraft.renderPass;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiHopper;
import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraft.client.gui.GuiRepair;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiWinGame;
import net.minecraft.client.gui.inventory.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.src.VertexData;
import net.minecraft.util.Vec3;
import optifine.Utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.util.ByteArrayBuffer;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.*;

import jopenvr.*;
import jopenvr.JOpenVRLibrary.EGraphicsAPIConvention;
import jopenvr.JOpenVRLibrary.EVREventType;

import java.awt.AWTException;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


public class MCOpenVR 
{
	static String initStatus;
	private static boolean initialized;
	private static Minecraft mc;

	static VR_IVRSystem_FnTable vrsystem;
	static VR_IVRCompositor_FnTable vrCompositor;
	static VR_IVROverlay_FnTable vrOverlay;
	static VR_IVRSettings_FnTable vrSettings;

	private static IntBuffer hmdErrorStore;
	private static TrackedDevicePose_t.ByReference hmdTrackedDevicePoseReference;
	private static TrackedDevicePose_t[] hmdTrackedDevicePoses;
	private static TrackedDevicePose_t.ByReference hmdGamePoseReference;
	private static TrackedDevicePose_t[] hmdGamePoses;

	private static Matrix4f[] poseMatrices;
	private static Vec3[] deviceVelocity;

	private LongByReference oHandle = new LongByReference();

	// position/orientation of headset and eye offsets
	static final Matrix4f hmdPose = new Matrix4f();
	public static final Matrix4f hmdRotation = new Matrix4f();
	static Matrix4f hmdProjectionLeftEye;
	static Matrix4f hmdProjectionRightEye;
	static Matrix4f hmdPoseLeftEye = new Matrix4f();
	static Matrix4f hmdPoseRightEye = new Matrix4f();
	static boolean initSuccess = false, flipEyes = false;

	private static IntBuffer hmdDisplayFrequency;

	private static FloatBuffer tlastVsync;
	private static LongBuffer _tframeCount;

	private static float vsyncToPhotons;
	private static double timePerFrame, frameCountRun;
	private static long frameCount;

	// TextureIDs of framebuffers for each eye
	private int LeftEyeTextureId;

	final static VRTextureBounds_t texBounds = new VRTextureBounds_t();
	final static Texture_t texType0 = new Texture_t();
	final static Texture_t texType1 = new Texture_t();

	// aiming
	static float aimYaw = 0;
	static float aimPitch = 0;

	static float laimPitch = 0;
	static float laimYaw = 0;

	static Vec3[] aimSource = new Vec3[2];

	static Vector3f headDirection = new Vector3f();
	static Vector3f controllerDirection = new Vector3f();
	static Vector3f lcontrollerDirection = new Vector3f();
	
	static boolean[] controllerTracking = new boolean[2];
	
	// Controllers
	private static int RIGHT_CONTROLLER = 0;
	private static int LEFT_CONTROLLER = 1;
	private static Matrix4f[] controllerPose = new Matrix4f[2];
	private static Matrix4f[] controllerRotation = new Matrix4f[2];
	private static int[] controllerDeviceIndex = new int[2];
	private static VRControllerState_t.ByReference[] inputStateRefernceArray = new VRControllerState_t.ByReference[2];
	private static VRControllerState_t[] lastControllerState = new VRControllerState_t[2];
	private static VRControllerState_t[] controllerStateReference = new VRControllerState_t[2];
	private static final int maxControllerVelocitySamples = 5;
	private static Vec3[][] controllerVelocitySamples = new Vec3[2][maxControllerVelocitySamples];
	private static int[] controllerVelocitySampleCount = new int[2];

	// Vive axes
	private static int k_EAxis_Trigger = 1;
	private static int k_EAxis_TouchPad = 0;

	// Controls

	private static long k_buttonTouchpad = (1L << JOpenVRLibrary.EVRButtonId.EVRButtonId_k_EButton_SteamVR_Touchpad);
	private static long k_buttonTrigger = (1L << JOpenVRLibrary.EVRButtonId.EVRButtonId_k_EButton_SteamVR_Trigger);
	private static long k_buttonAppMenu = (1L << JOpenVRLibrary.EVRButtonId.EVRButtonId_k_EButton_ApplicationMenu);
	private static long k_buttonGrip =  (1L << JOpenVRLibrary.EVRButtonId.EVRButtonId_k_EButton_Grip);

	private static float triggerThreshold = .25f;

	public static Vector3f guiPos_World = new Vector3f();
	public static Matrix4f guiRotationPose = new Matrix4f();
	public static  float guiScale = 1.0f;
	public static double startedOpeningInventory = 0;
	public static boolean hudPopup = true;

	// For mouse menu emulation
	public static float controllerMouseX = -1.0f;
	public static float controllerMouseY = -1.0f;
	public static boolean controllerMouseValid;
	public static int controllerMouseTicks;

	//keyboard
	public static boolean keyboardShowing = false;
	byte[] lastTyped = new byte[256];
	byte[] typed = new byte[256];
	static int pollsSinceLastChange = 0;

	// Touchpad samples
	private static Vector2f[][] touchpadSamples = new Vector2f[2][5];
	private static int[] touchpadSampleCount = new int[2];

	private static float[] inventory_swipe = new float[2];
	
	static boolean headIsTracking;
	
	private static int moveModeSwitchcount = 0;

	public static boolean isWalkingAbout;
	private static boolean isFreeRotate;
	private static float walkaboutYawStart;
	
	private static float hmdForwardYaw;
	
	public static float rtbX, rtbY;
	
	public String getName() {
		return "OpenVR";
	}

	
	public String getID() {
		return "openvr";
	}

		
	public boolean isInitialized() { return initialized; }

	
	public String getVersion() { return "Version TODO"; }

	static KeyBinding hotbarNext = new KeyBinding("Hotbar Next", 201, "key.categories.gameplay");
	static KeyBinding hotbarPrev = new KeyBinding("Hotbar Prev", 209, "key.categories.gameplay");
	static KeyBinding rotateLeft = new KeyBinding("Rotate Left", 203, "key.categories.movement");
	static KeyBinding rotateRight = new KeyBinding("Rotate Right", 205, "key.categories.movement");
	static KeyBinding walkabout = new KeyBinding("Walkabout", 207, "key.categories.movement");
	static KeyBinding rotateFree = new KeyBinding("Rotate Free", 199, "key.categories.movement");
	static KeyBinding quickTorch = new KeyBinding("Quick Torch", 210, "key.categories.gameplay");

	public MCOpenVR()
	{
		super();

		for (int c=0;c<2;c++)
		{
			aimSource[c] = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
			for (int sample = 0; sample < 5; sample++)
			{
				touchpadSamples[c][sample] = new Vector2f(0, 0);
			}
			touchpadSampleCount[c] = 0;
			controllerPose[c] = new Matrix4f();
			controllerRotation[c] = new Matrix4f();
			controllerDeviceIndex[c] = -1;

			lastControllerState[c] = new VRControllerState_t();
			controllerStateReference[c] = new VRControllerState_t();
			inputStateRefernceArray[c] = new VRControllerState_t.ByReference();

			inputStateRefernceArray[c].setAutoRead(false);
			inputStateRefernceArray[c].setAutoWrite(false);
			inputStateRefernceArray[c].setAutoSynch(false);
			for (int i = 0; i < 5; i++)
			{
				lastControllerState[c].rAxis[i] = new VRControllerAxis_t();
			}

			//controllerVelocitySamples[c] = new Vec3[2][maxControllerVelocitySamples];
			controllerVelocitySampleCount[c] = 0;
			for (int i=0;i<maxControllerVelocitySamples;i++)
			{
				controllerVelocitySamples[c][i] = Vec3.createVectorHelper(0, 0, 0);
			}
		}
	}

	private static boolean tried;

	
	public static boolean init()  throws Exception
	{

		if ( initialized )
			return true;

		if ( tried )
			return initialized;


		tried = true;

		mc = Minecraft.getMinecraft();
		// look in .minecraft first for openvr_api.dll
		File minecraftDir = Utils.getWorkingDirectory();
		String osFolder = "win32";
		if (System.getProperty("os.arch").contains("64"))
		{
			osFolder = "win64";
		}
		File openVRDir = new File( minecraftDir, osFolder );
		String openVRPath = openVRDir.getPath();
		System.out.println( "Adding OpenVR search path: "+openVRPath);

		NativeLibrary.addSearchPath("openvr_api", openVRPath);		

		if(jopenvr.JOpenVRLibrary.VR_IsHmdPresent() == 0){
			initStatus =  "VR Headset not detected.";
			return false;
		}

		try {
			initializeJOpenVR();
			initOpenVRCompositor(true) ;
			initOpenVROverlay() ;	
			initOpenVROSettings();
		} catch (Exception e) {
			initSuccess = false;
			initStatus = e.getLocalizedMessage();
			return false;
		}

		System.out.println( "OpenVR initialized & VR connected." );

		deviceVelocity = new Vec3[JOpenVRLibrary.k_unMaxTrackedDeviceCount];

		for(int i=0;i<poseMatrices.length;i++)
		{
			poseMatrices[i] = new Matrix4f();
			deviceVelocity[i] = Vec3.createVectorHelper(0,0,0);
		}

		HmdMatrix34_t matL = vrsystem.GetEyeToHeadTransform.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left);
		OpenVRUtil.convertSteamVRMatrix3ToMatrix4f(matL, hmdPoseLeftEye);

		HmdMatrix34_t matR = vrsystem.GetEyeToHeadTransform.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right);
		OpenVRUtil.convertSteamVRMatrix3ToMatrix4f(matR, hmdPoseRightEye);

	    mc.gameSettings.keyBindings = (KeyBinding[])((KeyBinding[])ArrayUtils.add(mc.gameSettings.keyBindings, rotateLeft));
	    mc.gameSettings.keyBindings = (KeyBinding[])((KeyBinding[])ArrayUtils.add(mc.gameSettings.keyBindings, rotateRight));
	    mc.gameSettings.keyBindings = (KeyBinding[])((KeyBinding[])ArrayUtils.add(mc.gameSettings.keyBindings, rotateFree));	
	    mc.gameSettings.keyBindings = (KeyBinding[])((KeyBinding[])ArrayUtils.add(mc.gameSettings.keyBindings, walkabout));	
	    mc.gameSettings.keyBindings = (KeyBinding[])((KeyBinding[])ArrayUtils.add(mc.gameSettings.keyBindings, quickTorch));	
	    mc.gameSettings.keyBindings = (KeyBinding[])((KeyBinding[])ArrayUtils.add(mc.gameSettings.keyBindings, hotbarNext));	
	    mc.gameSettings.keyBindings = (KeyBinding[])((KeyBinding[])ArrayUtils.add(mc.gameSettings.keyBindings, hotbarPrev));	
	
		initialized = true;
		return true;
	}

	private static void initializeJOpenVR() throws Exception { 
		hmdErrorStore = IntBuffer.allocate(1);
		vrsystem = null;
		JOpenVRLibrary.VR_InitInternal(hmdErrorStore, JOpenVRLibrary.EVRApplicationType.EVRApplicationType_VRApplication_Scene);
		if( hmdErrorStore.get(0) == 0 ) {
			// ok, try and get the vrsystem pointer..
			vrsystem = new VR_IVRSystem_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRSystem_Version, hmdErrorStore));
		}
		if( vrsystem == null || hmdErrorStore.get(0) != 0 ) {
			throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));		
		} else {
			
			vrsystem.setAutoSynch(false);
			vrsystem.read();
			
			System.out.println("OpenVR initialized & VR connected.");
			
			tlastVsync = FloatBuffer.allocate(1);
			_tframeCount = LongBuffer.allocate(1);

			hmdDisplayFrequency = IntBuffer.allocate(1);
			hmdDisplayFrequency.put( (int) JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayFrequency_Float);
			hmdTrackedDevicePoseReference = new TrackedDevicePose_t.ByReference();
			hmdTrackedDevicePoses = (TrackedDevicePose_t[])hmdTrackedDevicePoseReference.toArray(JOpenVRLibrary.k_unMaxTrackedDeviceCount);
			poseMatrices = new Matrix4f[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
			for(int i=0;i<poseMatrices.length;i++) poseMatrices[i] = new Matrix4f();

			timePerFrame = 1.0 / hmdDisplayFrequency.get(0);

			
			// disable all this stuff which kills performance
			hmdTrackedDevicePoseReference.setAutoRead(false);
			hmdTrackedDevicePoseReference.setAutoWrite(false);
			hmdTrackedDevicePoseReference.setAutoSynch(false);
			for(int i=0;i<JOpenVRLibrary.k_unMaxTrackedDeviceCount;i++) {
				hmdTrackedDevicePoses[i].setAutoRead(false);
				hmdTrackedDevicePoses[i].setAutoWrite(false);
				hmdTrackedDevicePoses[i].setAutoSynch(false);
			}

			initSuccess = true;
		}
	}

	private static Pointer ptrFomrString(String in){
		Pointer p = new Memory(in.length()+1);
		p.setString(0, in);
		return p;

	}


	// needed for in-game keyboard
	public static void initOpenVROverlay() throws Exception
	{
		vrOverlay =   new VR_IVROverlay_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVROverlay_Version, hmdErrorStore));
		if (vrOverlay != null &&  hmdErrorStore.get(0) == 0) {     		
			vrOverlay.setAutoSynch(false);
			vrOverlay.read();					
			System.out.println("OpenVR Overlay initialized OK");
		} else {
			throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));		
		}
	}


	public static void initOpenVROSettings() throws Exception
	{
		vrSettings =   new VR_IVRSettings_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRSettings_Version, hmdErrorStore));
		if (vrSettings != null &&  hmdErrorStore.get(0) == 0) {     		
			vrSettings.setAutoSynch(false);
			vrSettings.read();					
			System.out.println("OpenVR Settings initialized OK");
			
			IntByReference e = new IntByReference();
		
			float ret =	vrSettings.GetFloat.apply(ptrFomrString("steamvr"), ptrFomrString("renderTargetMultiplier"), -1f, e);

			int a = 9;
			
		} else {
			throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));		
		}
	}
	
	public static void initOpenVRCompositor(boolean set) throws Exception
	{
		if( set && vrsystem != null ) {
			vrCompositor = new VR_IVRCompositor_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRCompositor_Version, hmdErrorStore));
			if(vrCompositor != null && hmdErrorStore.get(0) == 0){                
				System.out.println("OpenVR Compositor initialized OK.");
				vrCompositor.setAutoSynch(false);
				vrCompositor.read();
				vrCompositor.SetTrackingSpace.apply(JOpenVRLibrary.ETrackingUniverseOrigin.ETrackingUniverseOrigin_TrackingUniverseStanding);
			} else {
				throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));			 
			}
		}
		if( vrCompositor == null ) {
			System.out.println("Skipping VR Compositor...");
			if( vrsystem != null ) {
				vsyncToPhotons = vrsystem.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_SecondsFromVsyncToPhotons_Float, hmdErrorStore);
			} else {
				vsyncToPhotons = 0f;
			}
		}

		// left eye
		texBounds.uMax = 1f;
		texBounds.uMin = 0f;
		texBounds.vMax = 1f;
		texBounds.vMin = 0f;
		texBounds.setAutoSynch(false);
		texBounds.setAutoRead(false);
		texBounds.setAutoWrite(false);
		texBounds.write();


		// texture type
		texType0.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
		texType0.eType = JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL;
		texType0.setAutoSynch(false);
		texType0.setAutoRead(false);
		texType0.setAutoWrite(false);
		texType0.handle = -1;
		texType0.write();

		
		// texture type
		texType1.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
		texType1.eType = JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL;
		texType1.setAutoSynch(false);
		texType1.setAutoRead(false);
		texType1.setAutoWrite(false);
		texType1.handle = -1;
		texType1.write();
		
		System.out.println("OpenVR Compositor initialized OK.");

	}

	public boolean initOpenVRControlPanel()
	{
		return true;
		//		vrControlPanel = new VR_IVRSettings_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRControlPanel_Version, hmdErrorStore));
		//		if(vrControlPanel != null && hmdErrorStore.get(0) == 0){
		//			System.out.println("OpenVR Control Panel initialized OK.");
		//			return true;
		//		} else {
		//			initStatus = "OpenVR Control Panel error: " + JOpenVRLibrary.VR_GetStringForHmdError(hmdErrorStore.get(0)).getString(0);
		//			return false;
		//		}
	}

	private String lasttyped = "";

	
	public static void poll(long frameIndex)
	{
		Minecraft.getMinecraft().mcProfiler.startSection("input");
		
	if(!mc.vrSettings.seated){
		pollInputEvents();

		updateControllerButtonState();
		updateTouchpadSampleBuffer();
		updateSmoothedVelocity();

		processControllerButtons();
		processTouchpadSampleBuffer();

		// GUI controls
		Minecraft.getMinecraft().mcProfiler.endStartSection("gui");
		if( mc.currentScreen != null )
		{
			processGui();
		}

		if(mc.currentScreen == null && mc.vrSettings.vrTouchHotbar && mc.vrSettings.vrHudLockMode != mc.vrSettings.HUD_LOCK_HEAD && hudPopup){
			processHotbar();
		}
	}
	
		Minecraft.getMinecraft().mcProfiler.endStartSection("updatePose");
		updatePose();
		Minecraft.getMinecraft().mcProfiler.endSection();
	}


	private static Vec3 vecFromVector(Vector3f in){
		return Vec3.createVectorHelper(in.x, in.y, in.z);
	}
	
	private static void processHotbar() {
	
		if(mc.thePlayer == null) return;
		if(mc.thePlayer.inventory == null) return;
		
		Vec3 main = getAimSource(0);
		Vec3 off = getAimSource(1);
		
		Vec3 barStartos = null,barEndos = null;
		
		int i = 1;
		if(mc.vrSettings.vrReverseHands) i = -1;
		
		if (mc.vrSettings.vrHudLockMode == mc.vrSettings.HUD_LOCK_WRIST){
			 barStartos = vecFromVector( getAimRotation(1).transform(new Vector3f(i*0.04f,-0.05f,0.24f)));
			 barEndos = vecFromVector( getAimRotation(1).transform(new Vector3f(i*0.2f,-0.05f,-0.05f)));
		} else if (mc.vrSettings.vrHudLockMode == mc.vrSettings.HUD_LOCK_HAND){
			 barStartos = vecFromVector( getAimRotation(1).transform(new Vector3f(-.18f,0.08f,-0.01f)));
			 barEndos = vecFromVector( getAimRotation(1).transform(new Vector3f(0.19f,0.04f,-0.08f)));
		} else return; //how did u get here
		
	
		Vec3 barStart = off.addVector(barStartos.xCoord, barStartos.yCoord, barStartos.zCoord);	
		Vec3 barEnd = off.addVector(barEndos.xCoord, barEndos.yCoord, barEndos.zCoord);

		Vec3 u = barEnd.subtract(barStart);
		Vec3 pq = main.subtract(barStart);
		float dist = (float) (pq.crossProduct(u).lengthVector() / u.lengthVector());

		if(dist > 0.06) return;
		
		float fact = (float) (pq.dotProduct(u) / (u.xCoord*u.xCoord + u.yCoord*u.yCoord + u.zCoord*u.zCoord));
	
		if(fact < 0) return;
		
		Vec3 w2 = pq.subtract(Vec3.createVectorHelper(u.xCoord*fact, u.yCoord*fact, u.zCoord*fact));
	
		Vec3 point = w2.subtract(main);
		float linelen = (float) barStart.subtract(barEnd).lengthVector();
		float ilen = (float) barStart.subtract(point).lengthVector();

		float pos = ilen / linelen * 9; 
		
		int box = (int) Math.floor(pos);
		if(pos - Math.floor(pos) < 0.1) return;
		
		if(box > 8) return;
		if(box < 0) return;
		//all that maths for this.
		if(box != mc.thePlayer.inventory.currentItem){
		mc.thePlayer.inventory.currentItem = box;	
		vrsystem.TriggerHapticPulse.apply(controllerDeviceIndex[0], 0, (short) 750);
		}
	}

	static GuiTextField keyboardGui;
	private static int quickTorchPreviousSlot;

	public static boolean setKeyboardOverlayShowing(boolean showingState, GuiTextField gui) {
		try {
			if(mc.vrSettings.seated) showingState = false;
			keyboardGui = gui;
			int ret = 1;
			if (showingState) {
				pollsSinceLastChange = 0; // User deliberately tried to show keyboard, shouldn't have chance of immediately resetting   
				Pointer pointer = new Memory(3);
				pointer.setString(0, "mc");
				Pointer empty = new Memory(1);
				empty.setString(0, "");

				ret = vrOverlay.ShowKeyboard.apply(0, 0, pointer, 256, empty, (byte)1, 0)	;	

				keyboardShowing = 0 == ret; //0 = no error, > 0 see EVROverlayError	


				if (ret != 0) {
					System.out.println("VR Overlay Error: " + vrOverlay.GetOverlayErrorNameFromEnum.apply(ret).getString(0));
				}

				if (!Display.isActive()) mc.printChatMessage("Warning: Game window is not in focus, VR keyboard will not function.");

			} else {
				try {
						vrOverlay.HideKeyboard.apply();				
				} catch (Error e) {
					// TODO: handle exception
				}
				keyboardShowing = false;
			}


		} catch (Exception e) {
			// TODO: handle exception
		}

		return keyboardShowing;
	}


	
	//TODO: to hell with all these conversions.
	//sets mouse position for currentscreen
	private static void processGui() {
		
		if(guiRotationPose == null) return;
		
		Vector3f controllerPos = new Vector3f();
		//OpenVRUtil.convertMatrix4ftoTranslationVector(controllerPose[0]);
		Vec3 con = mc.vrPlayer.getControllerPos_World(0);
		controllerPos.x	= (float) con.xCoord;
		controllerPos.y	= (float) con.yCoord;
		controllerPos.z	= (float) con.zCoord;
			
		Vec3 controllerdir = mc.vrPlayer.getControllerMainDir_World();
		Vector3f cdir = new Vector3f((float)controllerdir.xCoord,(float) controllerdir.yCoord,(float) controllerdir.zCoord);
		Vector3f forward = new Vector3f(0,0,1);

		Vector3f guiNormal = guiRotationPose.transform(forward);
		Vector3f guiRight = guiRotationPose.transform(new Vector3f(1,0,0));
		Vector3f guiUp = guiRotationPose.transform(new Vector3f(0,1,0));

		float guiWidth = 1.0f;		
		float guiHalfWidth = guiWidth * 0.5f;		
		float guiHeight = 1.0f;	
		float guiHalfHeight = guiHeight * 0.5f;

		Vector3f gp = new Vector3f();
		
		gp.x = (float) (guiPos_World.x + mc.entityRenderer.interPolatedRoomOrigin.xCoord ) ;
		gp.y = (float) (guiPos_World.y + mc.entityRenderer.interPolatedRoomOrigin.yCoord ) ;
		gp.z = (float) (guiPos_World.z + mc.entityRenderer.interPolatedRoomOrigin.zCoord ) ;
					
		Vector3f guiTopLeft = gp.subtract(guiUp.divide(1.0f / guiHalfHeight)).subtract(guiRight.divide(1.0f/guiHalfWidth));
		Vector3f guiTopRight = gp.subtract(guiUp.divide(1.0f / guiHalfHeight)).add(guiRight.divide(1.0f / guiHalfWidth));

		//Vector3f guiBottomLeft = guiPos.add(guiUp.divide(1.0f / guiHalfHeight)).subtract(guiRight.divide(1.0f/guiHalfWidth));
		//Vector3f guiBottomRight = guiPos.add(guiUp.divide(1.0f / guiHalfHeight)).add(guiRight.divide(1.0f/guiHalfWidth));

		float guiNormalDotControllerDirection = guiNormal.dot(cdir);
		if (Math.abs(guiNormalDotControllerDirection) > 0.00001f)
		{//pointed normal to the GUI
			float intersectDist = -guiNormal.dot(controllerPos.subtract(guiTopLeft)) / guiNormalDotControllerDirection;
			Vector3f pointOnPlane = controllerPos.add(cdir.divide(1.0f/intersectDist));

			Vector3f relativePoint = pointOnPlane.subtract(guiTopLeft);
			float u = relativePoint.dot(guiRight.divide(1.0f/guiWidth));
			float v = relativePoint.dot(guiUp.divide(1.0f/guiWidth));

					
			// adjust vertical for aspect ratio
			v = ( (v - 0.5f) * ((float)mc.displayWidth / (float)mc.displayHeight) ) + 0.5f;

			// TODO: Figure out where this magic 0.68f comes from. Probably related to Minecraft window size.
			//JRBUDDA: It's probbably 1/defaulthudscale (1.5)

			u = ( u - 0.5f ) * 0.68f / guiScale + 0.5f;
			v = ( v - 0.5f ) * 0.68f / guiScale + 0.5f;

			if (u<0 || v<0 || u>1 || v>1)
			{
				// offscreen
				controllerMouseX = -1.0f;
				controllerMouseY = -1.0f;
			}
			else if (controllerMouseX == -1.0f)
			{
				controllerMouseX = (int) (u * mc.displayWidth);
				controllerMouseY = (int) (v * mc.displayHeight);
			}
			else
			{
				// apply some smoothing between mouse positions
				float newX = (int) (u * mc.displayWidth);
				float newY = (int) (v * mc.displayHeight);
				controllerMouseX = controllerMouseX * 0.7f + newX * 0.3f;
				controllerMouseY = controllerMouseY * 0.7f + newY * 0.3f;
			}

			// copy to mc for debugging
			mc.guiU = u;
			mc.guiV = v;
			mc.intersectDist = intersectDist;
			mc.pointOnPlaneX = pointOnPlane.x;
			mc.pointOnPlaneY = pointOnPlane.y;
			mc.pointOnPlaneZ = pointOnPlane.z;
			mc.guiTopLeftX = guiTopLeft.x;
			mc.guiTopLeftY = guiTopLeft.y;
			mc.guiTopLeftZ = guiTopLeft.z;
			mc.guiTopRightX = guiTopRight.x;
			mc.guiTopRightY = guiTopRight.y;
			mc.guiTopRightZ = guiTopRight.z;
			mc.controllerPosX = controllerPos.x;
			mc.controllerPosY = controllerPos.y;
			mc.controllerPosZ = controllerPos.z;
		}

		mc.currentScreen.mouseOffsetX = -1;
		mc.currentScreen.mouseOffsetY = -1;

		if (controllerDeviceIndex[LEFT_CONTROLLER] != -1) {
			//Shift
			if (mc.currentScreen != null &&
					(controllerStateReference[LEFT_CONTROLLER].ulButtonPressed & k_buttonGrip) > 0 &&
					(lastControllerState[LEFT_CONTROLLER].ulButtonPressed & k_buttonGrip) == 0 
					)				
			{
				//press Shift
				mc.currentScreen.pressShiftFake = true;
				if (Display.isActive()) KeyboardSimulator.robot.keyPress(KeyEvent.VK_SHIFT);
			}


			if(mc.currentScreen != null &&
					(
					controllerStateReference[LEFT_CONTROLLER].ulButtonPressed & k_buttonGrip) == 0 &&
					(lastControllerState[LEFT_CONTROLLER].ulButtonPressed & k_buttonGrip) > 0 
					)
			{
				//release Shift
				mc.currentScreen.pressShiftFake = false;
				if (Display.isActive()) KeyboardSimulator.robot.keyRelease(KeyEvent.VK_SHIFT);
			}	
			//end Shift
		}
		
		if (controllerMouseX >= 0 && controllerMouseX < mc.displayWidth
				&& controllerMouseY >=0 && controllerMouseY < mc.displayHeight)
		{
			// clamp to screen
		int mouseX =(int) (controllerMouseX = Math.min(Math.max((int) controllerMouseX, 0), mc.displayWidth));
		int mouseY =(int) (controllerMouseY = Math.min(Math.max((int) controllerMouseY, 0), mc.displayHeight));

			if (controllerDeviceIndex[RIGHT_CONTROLLER] != -1)
			{
				
				Mouse.setCursorPosition((int)controllerMouseX, (int)controllerMouseY);
				controllerMouseValid = true;
				//LMB
				
				if (mc.currentScreen != null &&
						controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold && 
						lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x <= triggerThreshold 
						)
				{
					//press left mouse button
					if (Display.isActive()) 
						KeyboardSimulator.robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
					else mc.currentScreen.mouseDown(mouseX, mouseY, 0);
				}	

				if (mc.currentScreen != null &&
						controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold) {					
					mc.currentScreen.mouseDrag(mouseX, mouseY);//Signals mouse move

				}


				if (mc.currentScreen != null &&
						controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x <= triggerThreshold && 
						lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold 
						)
				{
					//release left mouse button
					if (Display.isActive()) KeyboardSimulator.robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					else mc.currentScreen.mouseUp(mouseX, mouseY, 0);
				}
				//end LMB

				//RMB
				if (mc.currentScreen != null &&
						(controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
						(lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) == 0 
						)				
				{
					//press right mouse button
					if (Display.isActive()) KeyboardSimulator.robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
					else mc.currentScreen.mouseDown(mouseX, mouseY, 1);
				}	

				if (mc.currentScreen != null &&
						(controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0)
				{
					mc.currentScreen.mouseDrag(mouseX, mouseY);//Signals mouse move
				}


				if(mc.currentScreen != null &&
						(
						controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) == 0 &&
						(lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 
						)
				{
					//release right mouse button
					if (Display.isActive()) KeyboardSimulator.robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
					else mc.currentScreen.mouseUp(mouseX, mouseY, 1);
				}	
				//end RMB	

				//MMB
				if (mc.currentScreen != null &&
						(controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonGrip) > 0 &&
						(lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonGrip) == 0 
						)				
				{
					//press middle mouse button
					if (Display.isActive()) KeyboardSimulator.robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
					else mc.currentScreen.mouseDown(mouseX, mouseY, 2);
				}	

				if (mc.currentScreen != null &&
						(controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonGrip) > 0)
				{
					mc.currentScreen.mouseDrag(mouseX, mouseY);//Signals mouse move
				}


				if(mc.currentScreen != null &&
						(
						controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonGrip) == 0 &&
						(lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonGrip) > 0 
						)
				{
					//release middle mouse button
					if (Display.isActive()) KeyboardSimulator.robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
					else mc.currentScreen.mouseUp(mouseX, mouseY, 2);
				}	
				//end MMB

			}
		} else {
			if(controllerMouseTicks == 0)
				controllerMouseValid = false;

			if(controllerMouseTicks>0)controllerMouseTicks--;

			if (mc.thePlayer != null && !(mc.currentScreen instanceof GuiWinGame))
			{
				boolean pressedRMB = ((controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0)
						&& ((lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) == 0);

				boolean pressedLMB = (controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold)
						&& (lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x <= triggerThreshold);

				if (pressedLMB || pressedRMB)
				{
					{mc.thePlayer.closeScreen();}			

				}
			}
		}
	}


	
	public void destroy()
	{
		if (this.initialized)
		{
			JOpenVRLibrary.VR_ShutdownInternal();
			this.initialized = false;
		}
	}

	
	public void beginFrame()
	{
		beginFrame(0);
	}

	
	public void beginFrame(long frameIndex)
	{

	}

	
	
//	public HmdParameters getHMDInfo()
//	{
//		HmdParameters hmd = new HmdParameters();
//		if ( isInitialized() )
//		{
//			IntBuffer rtx = IntBuffer.allocate(1);
//			IntBuffer rty = IntBuffer.allocate(1);
//			vrsystem.GetRecommendedRenderTargetSize.apply(rtx, rty);
//
//			hmd.Type = HmdType.ovrHmd_Other;
//			hmd.ProductName = "OpenVR";
//			hmd.Manufacturer = "Unknown";
//			hmd.AvailableHmdCaps = 0;
//			hmd.DefaultHmdCaps = 0;
//			hmd.AvailableTrackingCaps = HmdParameters.ovrTrackingCap_Orientation | HmdParameters.ovrTrackingCap_Position;
//			hmd.DefaultTrackingCaps = HmdParameters.ovrTrackingCap_Orientation | HmdParameters.ovrTrackingCap_Position;
//			hmd.Resolution = new Sizei( rtx.get(0) * 2, rty.get(0) );
//
//			float topFOV = vrsystem.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewTopDegrees_Float, hmdErrorStore);
//			float bottomFOV = vrsystem.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewBottomDegrees_Float, hmdErrorStore);
//			float leftFOV = vrsystem.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewLeftDegrees_Float, hmdErrorStore);
//			float rightFOV = vrsystem.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewRightDegrees_Float, hmdErrorStore);
//
//			hmd.DefaultEyeFov[0] = new FovPort((float)Math.tan(topFOV),(float)Math.tan(bottomFOV),(float)Math.tan(leftFOV),(float)Math.tan(rightFOV));
//			hmd.DefaultEyeFov[1] = new FovPort((float)Math.tan(topFOV),(float)Math.tan(bottomFOV),(float)Math.tan(leftFOV),(float)Math.tan(rightFOV));
//			hmd.MaxEyeFov[0] = new FovPort((float)Math.tan(topFOV),(float)Math.tan(bottomFOV),(float)Math.tan(leftFOV),(float)Math.tan(rightFOV));
//			hmd.MaxEyeFov[1] = new FovPort((float)Math.tan(topFOV),(float)Math.tan(bottomFOV),(float)Math.tan(leftFOV),(float)Math.tan(rightFOV));
//			hmd.DisplayRefreshRate = 90.0f;
//		}
//
//		return hmd;
//	}


	/* Gets the current user profile data */
	
	public UserProfileData getProfileData()
	{
		UserProfileData userProfile = new UserProfileData();
		if ( isInitialized() )
		{
			userProfile._gender = UserProfileData.GenderType.Gender_Unspecified;    // n/a
			userProfile._playerHeight = 0;                                          // n/a
			userProfile._eyeHeight = 0;                                             // n/a
			userProfile._ipd = vrsystem.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_UserIpdMeters_Float, hmdErrorStore);
			userProfile._name = "Someone";                                          // n/a
			userProfile._isDefault = true;                                          // n/a
		}
		return userProfile;
	}


	/**
	 * Updates the model with the current head orientation
	 * @param ipd hmd ipd
	 * @param yawHeadDegrees Yaw of head only
	 * @param pitchHeadDegrees Pitch of head only
	 * @param rollHeadDegrees Roll of head only
	 * @param worldYawOffsetDegrees Additional yaw input (e.g. mouse)
	 * @param worldPitchOffsetDegrees Additional pitch input (e.g. mouse)
	 * @param worldRollOffsetDegrees Additional roll input
	 */
	
	public void update(float ipd, float yawHeadDegrees, float pitchHeadDegrees, float rollHeadDegrees,
			float worldYawOffsetDegrees, float worldPitchOffsetDegrees, float worldRollOffsetDegrees)
	{

	}

	private static void findControllerDevices()
	{
		controllerDeviceIndex[RIGHT_CONTROLLER] = -1;
		controllerDeviceIndex[LEFT_CONTROLLER] = -1;
		
			if(mc.vrSettings.vrReverseHands){
				controllerDeviceIndex[RIGHT_CONTROLLER]  = vrsystem.GetTrackedDeviceIndexForControllerRole.apply(JOpenVRLibrary.ETrackedControllerRole.ETrackedControllerRole_TrackedControllerRole_LeftHand);
				controllerDeviceIndex[LEFT_CONTROLLER] = vrsystem.GetTrackedDeviceIndexForControllerRole.apply(JOpenVRLibrary.ETrackedControllerRole.ETrackedControllerRole_TrackedControllerRole_RightHand);
			}else {
				controllerDeviceIndex[LEFT_CONTROLLER]  = vrsystem.GetTrackedDeviceIndexForControllerRole.apply(JOpenVRLibrary.ETrackedControllerRole.ETrackedControllerRole_TrackedControllerRole_LeftHand);
				controllerDeviceIndex[RIGHT_CONTROLLER] = vrsystem.GetTrackedDeviceIndexForControllerRole.apply(JOpenVRLibrary.ETrackedControllerRole.ETrackedControllerRole_TrackedControllerRole_RightHand);
			}
	}

	private static void updateControllerButtonState()
	{
		for (int c = 0; c < 2; c++) //each controller
		{
			// store previous state
			lastControllerState[c].unPacketNum = controllerStateReference[c].unPacketNum;
			lastControllerState[c].ulButtonPressed = controllerStateReference[c].ulButtonPressed;
			lastControllerState[c].ulButtonTouched = controllerStateReference[c].ulButtonTouched;

			for (int i = 0; i < 5; i++) //5 axes but only [0] and [1] is anything, trigger and touchpad
			{
				if (controllerStateReference[c].rAxis[i] != null)
				{
					lastControllerState[c].rAxis[i].x = controllerStateReference[c].rAxis[i].x;
					lastControllerState[c].rAxis[i].y = controllerStateReference[c].rAxis[i].y;
				}
			}

			// read new state
			if (controllerDeviceIndex[c] != -1)
			{			
				vrsystem.GetControllerState.apply(controllerDeviceIndex[c], inputStateRefernceArray[c]);
				inputStateRefernceArray[c].read();
				controllerStateReference[c] = inputStateRefernceArray[c];			
			} else
			{
				// controller not connected, clear state
				lastControllerState[c].ulButtonPressed = 0;
				lastControllerState[c].ulButtonTouched = 0;

				for (int i = 0; i < 5; i++)
				{
					if (controllerStateReference[c].rAxis[i] != null)
					{
						lastControllerState[c].rAxis[i].x = 0.0f;
						lastControllerState[c].rAxis[i].y = 0.0f;
					}
				}
				try{
					controllerStateReference[c] = lastControllerState[c];					
				} catch (Throwable e){

				}
			}
		}
	}


	//OK the fundamental problem with this is Minecraft uses a LWJGL event buffer for keyboard and mouse inputs. It polls those devices faster
	//and presents the game with a nice queue of things that happened. With OpenVR we're polling the controllers directly on the -game- (edit render?) loop.
	//This means we should only set keys as pressed when they change state, or they will repeat.
	//And we should still unpress the key when released.
	//TODO: make a new class that polls more quickly and provides Minecraft.java with a HTCController.next() event queue. (unless openVR has one?)
	private static void processControllerButtons()
	{
		if (mc.theWorld == null)
			return;

		boolean sleeping = (mc.thePlayer != null && mc.thePlayer.isPlayerSleeping());

		// right controller
		//last
		boolean lastpressedRGrip = (lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonGrip) > 0;		
		boolean lastpressedRtouchpadBottomLeft = (lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].y < 0 ) &&
				(lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].x < 0 ) ;	
		boolean lastpressedRtouchpadBottomRight = (lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].y < 0 ) &&
				(lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].x > 0 ) ;		
		boolean lastpressedRtouchpadTopLeft = (lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].y > 0 ) &&
				(lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].x < 0 ) ;	
		boolean lastpressedRtouchpadTopRight = (lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].y > 0 ) &&
				(lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].x > 0 ) ;		
		boolean lastpressedRAppMenu = (lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonAppMenu) > 0;
		boolean lastpressedRTrigger = lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold;		
		boolean lastpressedRTriggerClick =( lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTrigger )>0;
		//current
		boolean pressedRGrip = (controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonGrip) > 0;
		boolean pressedRtouchpadBottomLeft = (controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].y < 0 ) &&
				(controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].x < 0 ) ;	
		boolean pressedRtouchpadBottomRight = (controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].y < 0 ) &&
				(controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].x > 0 ) ;		
		boolean pressedRtouchpadTopLeft = (controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].y > 0 ) &&
				(controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].x < 0 ) ;	
		boolean pressedRtouchpadTopRight = (controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].y > 0 ) &&
				(controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].x > 0 ) ;	
		boolean pressedRAppMenu = (controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonAppMenu) > 0;
		boolean pressedRTrigger = controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold;
		boolean pressedRTriggerClick =( controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTrigger )>0;

		rtbX = controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].x;
		rtbY = controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].y;

		//R GRIP
		if (pressedRGrip && !lastpressedRGrip) {
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_GRIP.ordinal()].press();
		}	
		if(!pressedRGrip && lastpressedRGrip) {
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_GRIP.ordinal()].unpress();
		}

		//R TOUCHPAD	

		//if(!gui){ //this are the mouse buttons. ummm do I need this? it causes the key to stick down.
		
		if (pressedRtouchpadBottomLeft && !lastpressedRtouchpadBottomLeft){
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_TOUCHPAD_BL.ordinal()].press();
		}			
		if (!pressedRtouchpadBottomLeft && lastpressedRtouchpadBottomLeft){
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_TOUCHPAD_BL.ordinal()].unpress();
		}		
		if (pressedRtouchpadBottomRight && !lastpressedRtouchpadBottomRight){
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_TOUCHPAD_BR.ordinal()].press();
		}			
		if (!pressedRtouchpadBottomRight && lastpressedRtouchpadBottomRight){
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_TOUCHPAD_BR.ordinal()].unpress();
		}	
		if (pressedRtouchpadTopLeft && !lastpressedRtouchpadTopLeft){
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_TOUCHPAD_UL.ordinal()].press();		}			
		if (!pressedRtouchpadTopLeft && lastpressedRtouchpadTopLeft){
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_TOUCHPAD_UL.ordinal()].unpress();
		}	
		if (pressedRtouchpadTopRight && !lastpressedRtouchpadTopRight ){
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_TOUCHPAD_UR.ordinal()].press();		}			
		if (!pressedRtouchpadTopRight  && lastpressedRtouchpadTopRight ){
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_TOUCHPAD_UR.ordinal()].unpress();
		}	

			//R TRIGGER
			if (pressedRTrigger && !lastpressedRTrigger) {
				mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_TRIGGER.ordinal()].press();
			}	
			if(!pressedRTrigger && lastpressedRTrigger) {
				mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_TRIGGER.ordinal()].unpress();
			}
	//	}

		//R AppMenu
		if (pressedRAppMenu && !lastpressedRAppMenu) {
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_APPMENU.ordinal()].press();
		}	
		if(!pressedRAppMenu && lastpressedRAppMenu) {
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_APPMENU.ordinal()].unpress();
		}

		//R triggerclick
		if (pressedRTriggerClick && !lastpressedRTriggerClick) {
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_TRIGGER_FULLCLICK.ordinal()].press();
		}	
		if(!pressedRTriggerClick && lastpressedRTriggerClick) {
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_TRIGGER_FULLCLICK.ordinal()].unpress();
		}			



		// left controller
		//last
		boolean lastpressedLGrip = (lastControllerState[LEFT_CONTROLLER].ulButtonPressed & k_buttonGrip) > 0;		
		boolean lastpressedLtouchpadBottomLeft = (lastControllerState[LEFT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].y < 0 ) &&
				(lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].x < 0 ) ;	
		boolean lastpressedLtouchpadBottomRight = (lastControllerState[LEFT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].y < 0 ) &&
				(lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].x > 0 ) ;		
		boolean lastpressedLtouchpadTopLeft = (lastControllerState[LEFT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].y > 0 ) &&
				(lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].x < 0 ) ;	
		boolean lastpressedLtouchpadTopRight = (lastControllerState[LEFT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].y > 0 ) &&
				(lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].x > 0 ) ;		
		boolean lastpressedLAppMenu = (lastControllerState[LEFT_CONTROLLER].ulButtonPressed & k_buttonAppMenu) > 0;
		boolean lastpressedLTrigger = lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold;		
		boolean lastpressedLTriggerClick =( lastControllerState[LEFT_CONTROLLER].ulButtonPressed & k_buttonTrigger )>0;
		//current
		boolean pressedLGrip = (controllerStateReference[LEFT_CONTROLLER].ulButtonPressed & k_buttonGrip) > 0;
		boolean pressedLtouchpadBottomLeft = (controllerStateReference[LEFT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(controllerStateReference[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].y < 0 ) &&
				(controllerStateReference[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].x < 0 ) ;	
		boolean pressedLtouchpadBottomRight = (controllerStateReference[LEFT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(controllerStateReference[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].y < 0 ) &&
				(controllerStateReference[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].x > 0 ) ;		
		boolean pressedLtouchpadTopLeft = (controllerStateReference[LEFT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(controllerStateReference[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].y > 0 ) &&
				(controllerStateReference[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].x < 0 ) ;	
		boolean pressedLtouchpadTopRight = (controllerStateReference[LEFT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(controllerStateReference[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].y > 0 ) &&
				(controllerStateReference[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].x > 0 ) ;	
		boolean pressedLAppMenu = (controllerStateReference[LEFT_CONTROLLER].ulButtonPressed & k_buttonAppMenu) > 0;
		boolean pressedLTrigger = controllerStateReference[LEFT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold;
		boolean pressedLTriggerClick =( controllerStateReference[LEFT_CONTROLLER].ulButtonPressed & k_buttonTrigger )>0;

		//l GRIP
		if (pressedLGrip && !lastpressedLGrip) {
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_LEFT_GRIP.ordinal()].press();
		}	
		if(!pressedLGrip && lastpressedLGrip) {
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_LEFT_GRIP.ordinal()].unpress();
		}

		//l TOUCHPAD	

		
		if (pressedLtouchpadBottomLeft && !lastpressedLtouchpadBottomLeft){
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_LEFT_TOUCHPAD_BL.ordinal()].press();
		}			
		if (!pressedLtouchpadBottomLeft && lastpressedLtouchpadBottomLeft){
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_LEFT_TOUCHPAD_BL.ordinal()].unpress();
		}		
		if (pressedLtouchpadBottomRight && !lastpressedLtouchpadBottomRight){
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_LEFT_TOUCHPAD_BR.ordinal()].press();
		}			
		if (!pressedLtouchpadBottomRight && lastpressedLtouchpadBottomRight){
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_LEFT_TOUCHPAD_BR.ordinal()].unpress();
		}	
		if (pressedLtouchpadTopLeft && !lastpressedLtouchpadTopLeft){
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_LEFT_TOUCHPAD_UL.ordinal()].press();		}			
		if (!pressedLtouchpadTopLeft && lastpressedLtouchpadTopLeft){
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_LEFT_TOUCHPAD_UL.ordinal()].unpress();
		}	
		if (pressedLtouchpadTopRight && !lastpressedLtouchpadTopRight ){
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_LEFT_TOUCHPAD_UR.ordinal()].press();		}			
		if (!pressedLtouchpadTopRight  && lastpressedLtouchpadTopRight ){
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_LEFT_TOUCHPAD_UR.ordinal()].unpress();
		}	

		//L TRIGGER
		if (pressedLTrigger && !lastpressedLTrigger) {
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_LEFT_TRIGGER.ordinal()].press();
		}	
		if(!pressedLTrigger && lastpressedLTrigger) {
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_LEFT_TRIGGER.ordinal()].unpress();
		}

		//L AppMenu
		if (pressedLAppMenu && !lastpressedLAppMenu) {
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_LEFT_APPMENU.ordinal()].press();
		}	
		if(!pressedLAppMenu && lastpressedLAppMenu) {
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_LEFT_APPMENU.ordinal()].unpress();
		}

		//L triggerclick
		if (pressedLTriggerClick && !lastpressedLTriggerClick) {
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_LEFT_TRIGGER_FULLCLICK.ordinal()].press();
		}	
		if(!pressedLTriggerClick && lastpressedLTriggerClick) {
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_LEFT_TRIGGER_FULLCLICK.ordinal()].unpress();
		}			

		boolean gui = (mc.currentScreen != null);
	
		//VIVE SPECIFIC FUNCTIONALITY
		//TODO: Find a better home for these in Minecraft.java		

		if(	Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) return;
		
		//no jump key if cant.Not a good place for this check.
		if(mc.gameSettings.keyBindJump.isPressed()) {
			if(!mc.vrPlayer.getFreeMoveMode() && !mc.vrSettings.simulateFalling) {
				mc.gameSettings.keyBindJump.unpressKey();
			}
		}

		if(rotateLeft.isPressed()){
			mc.vrSettings.vrWorldRotation+=mc.vrSettings.vrWorldRotationIncrement;
			mc.vrSettings.vrWorldRotation = mc.vrSettings.vrWorldRotation % 360;
		}
		
		if(rotateRight.isPressed()){
			mc.vrSettings.vrWorldRotation-=mc.vrSettings.vrWorldRotationIncrement;		
			mc.vrSettings.vrWorldRotation = mc.vrSettings.vrWorldRotation % 360;
		}
		
		if(!gui){
			if(walkabout.getIsKeyPressed()){
				float yaw = aimYaw;
				
				//oh this is ugly. TODO: cache which hand when binding button.
				for (VRControllerButtonMapping vb : mc.vrSettings.buttonMappings) {
					if (vb.key == walkabout) {
						if(vb.Button.name().contains("_LEFT")){
							yaw = laimYaw;
							break;
						}
					}
				}
				
				if (!isWalkingAbout){
					isWalkingAbout = true;
					walkaboutYawStart = mc.vrSettings.vrWorldRotation + yaw;  
				}
				else {
					mc.vrSettings.vrWorldRotation = walkaboutYawStart - yaw;
					mc.vrSettings.vrWorldRotation %= 360; // Prevent stupidly large values (can they even happen here?)
					mc.vrPlayer.checkandUpdateRotateScale(true);
				}
			} else {
				isWalkingAbout = false;
			}
		}
		
		if(!gui){
			if(rotateFree.getIsKeyPressed()){
				float yaw = aimYaw;
				
				//oh this is ugly. TODO: cache which hand when binding button.
				for (VRControllerButtonMapping vb : mc.vrSettings.buttonMappings) {
					if (vb.key == rotateFree) {
						if(vb.Button.name().contains("_LEFT")){
							yaw = laimYaw;
							break;
						}
					}
				}
				
				if (!isFreeRotate){
					isFreeRotate = true;
					walkaboutYawStart = mc.vrSettings.vrWorldRotation - yaw;  
				}
				else {
					mc.vrSettings.vrWorldRotation = walkaboutYawStart + yaw;
					mc.vrPlayer.checkandUpdateRotateScale(true);
				}
			} else {
				isFreeRotate = false;
			}
		}
		
		
		if(hotbarNext.isPressed()) changeHotbar(-1);
		
		if(hotbarPrev.isPressed()) changeHotbar(1);
		
		if(quickTorch.isPressed() && mc.thePlayer != null){
		    for (int slot=0;slot<9;slot++)
            {  
		    	ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(slot);
                if (itemStack!=null && itemStack.getUnlocalizedName().equals("tile.torch") && mc.currentScreen == null)
                {
                    quickTorchPreviousSlot = mc.thePlayer.inventory.currentItem;
                    mc.thePlayer.inventory.currentItem = slot;
                    mc.rightClickMouse();
                    // switch back immediately
                    mc.thePlayer.inventory.currentItem = quickTorchPreviousSlot;
                    quickTorchPreviousSlot = -1;
                    break;
                }
            }
        }
		
		// if you start teleporting, close any UI
		if (gui && !sleeping && mc.gameSettings.keyBindForward.getIsKeyPressed() && !(mc.currentScreen instanceof GuiWinGame))
		{
			mc.thePlayer.closeScreen();
		}

		//handle movementtoggle
		if (pressedRGrip) {
			if(mc.vrSettings.vrAllowLocoModeSwotch){
				moveModeSwitchcount++;
				if (moveModeSwitchcount >= 20 * 4) {
					moveModeSwitchcount = 0;
					if(mc.vrPlayer.noTeleportClient && mc.vrPlayer.getFreeMoveMode()){
						mc.printChatMessage("Warning: This server may not allow teleporting.");
					}
					mc.vrPlayer.setFreeMoveMode(!mc.vrPlayer.getFreeMoveMode());
					mc.printChatMessage("Movement mode set to: " + (mc.vrPlayer.getFreeMoveMode() ? "Free Move" : "Teleport"));
				}				
			}
		} else {
			moveModeSwitchcount = 0;
		}

		if(!mc.gameSettings.keyBindInventory.getIsKeyPressed()){
			startedOpeningInventory = 0;
		}

		//GuiContainer.java only listens directly to the keyboard to close.
		if(gui && !(mc.currentScreen instanceof GuiWinGame) && mc.gameSettings.keyBindInventory.getIsKeyPressed()){ //inventory will repeat open/close while button is held down. TODO: fix.
			if((getCurrentTimeSecs() - startedOpeningInventory) > 0.5) mc.thePlayer.closeScreen();
			mc.gameSettings.keyBindInventory.unpressKey(); //minecraft.java will open a new window otherwise.
		}

		if(pressedLAppMenu  && !lastpressedLAppMenu) { //handle menu directly
				
			if(pressedLGrip){				
				setKeyboardOverlayShowing(!keyboardShowing, null);			
			} else{
				if(gui || keyboardShowing){

					if(mc.currentScreen instanceof GuiWinGame){ //from 'esc' key on guiwingame since we cant push it.
						mc.thePlayer.sendQueue.addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.PERFORM_RESPAWN));
						mc.displayGuiScreen((GuiScreen)null);		
					}else {
						mc.thePlayer.closeScreen();
						setKeyboardOverlayShowing(false, null);
					}
				}else
					mc.displayInGameMenu();				
			}
		}
		
		if(pressedRAppMenu  && !lastpressedRAppMenu) { //handle menu directly
			if(pressedRGrip && mc.vrSettings.displayMirrorMode == mc.vrSettings.MIRROR_MIXED_REALITY){				
				VRHotkeys.snapMRCam(mc);	
			}
		}
	}


	
	//jrbuda:: oh hello there you are.
	private static void pollInputEvents()
	{
		if(vrsystem == null) return;
		
		//TODO: use this for everything, maybe.
		jopenvr.VREvent_t event = new jopenvr.VREvent_t();

		while (vrsystem.PollNextEvent.apply(event, event.size() ) > 0)
		{

			switch (event.eventType) {
			case EVREventType.EVREventType_VREvent_KeyboardClosed:
				//'huzzah'
				keyboardShowing = false;
				break;
			case EVREventType.EVREventType_VREvent_KeyboardCharInput:
				byte[] inbytes = event.data.getPointer().getByteArray(0, 8);	
				int len = 0;			
				for (byte b : inbytes) {
					if(b>0)len++;
				}
				String str = new String(inbytes,0,len, StandardCharsets.UTF_8);
				KeyboardSimulator.type(str); //holy shit it works.
				break;
			default:
				break;
			}
		}
	}

	private static void updateTouchpadSampleBuffer()
	{
		for (int c=0;c<2;c++)
		{
			if (controllerStateReference[c].rAxis[k_EAxis_TouchPad]!=null &&
					(controllerStateReference[c].ulButtonTouched & k_buttonTouchpad) > 0)
			{
				int sample = touchpadSampleCount[c] % 5;
				touchpadSamples[c][sample].x = controllerStateReference[c].rAxis[k_EAxis_TouchPad].x;
				touchpadSamples[c][sample].y = controllerStateReference[c].rAxis[k_EAxis_TouchPad].y;
				touchpadSampleCount[c]++;
			} else
			{
				clearTouchpadSampleBuffer(c);
			}
		}
	}

	private static void clearTouchpadSampleBuffer(int controller)
	{
		for (int sample=0;sample<5;sample++)
		{
			touchpadSamples[controller][sample].x = 0;
			touchpadSamples[controller][sample].y = 0;
		}
		touchpadSampleCount[controller] = 0;
		inventory_swipe[controller] = 0;
	}

	private static void processTouchpadSampleBuffer()
	{
		if (mc.currentScreen != null){
		// right touchpad controls mousewheel
			int c =0;
			boolean touchpadPressed = (controllerStateReference[c].ulButtonPressed & k_buttonTouchpad) > 0;

			if (touchpadSampleCount[c] > 3 && !touchpadPressed)
			{
				int sample = touchpadSampleCount[c] - 5;
				if (sample < 0)
					sample = 0;
				sample = sample % 5;
				int nextSample = (sample + 1) % 5;

				float deltaY = touchpadSamples[c][nextSample].y - touchpadSamples[c][sample].y;
				inventory_swipe[c] += deltaY;

				float swipeDistancePerInventorySlot = 0.4f;
				if (inventory_swipe[c] > swipeDistancePerInventorySlot)
				{
					short duration = 225;
					vrsystem.TriggerHapticPulse.apply(controllerDeviceIndex[c], 0, duration);
					KeyboardSimulator.robot.mouseWheel(-25);
					inventory_swipe[c] -= swipeDistancePerInventorySlot;
				} else if (inventory_swipe[c] < -swipeDistancePerInventorySlot)
				{
					KeyboardSimulator.robot.mouseWheel(25);
					short duration = 225;
					vrsystem.TriggerHapticPulse.apply(controllerDeviceIndex[c], 0, duration);
					inventory_swipe[c] += swipeDistancePerInventorySlot;
				}
			}
		} else if (mc.thePlayer != null)	{
		// left touchpad controls inventory
			int c =1;
			boolean touchpadPressed = (controllerStateReference[c].ulButtonPressed & k_buttonTouchpad) > 0;

			if (touchpadSampleCount[c] > 3 && !touchpadPressed)
			{
				int sample = touchpadSampleCount[c] - 5;
				if (sample < 0)
					sample = 0;
				sample = sample % 5;
				int nextSample = (sample + 1) % 5;

				float deltaX = touchpadSamples[c][nextSample].x - touchpadSamples[c][sample].x;
				inventory_swipe[c] += deltaX;

				float swipeDistancePerInventorySlot = 0.4f;
				if (inventory_swipe[c] > swipeDistancePerInventorySlot)
				{
					if (Display.isActive()) {
						KeyboardSimulator.robot.mouseWheel(25);
						short duration = 250;
						vrsystem.TriggerHapticPulse.apply(controllerDeviceIndex[1], 0, duration);
					}
					else hotbarNext.pressKey();
					inventory_swipe[c] -= swipeDistancePerInventorySlot;
				} else if (inventory_swipe[c] < -swipeDistancePerInventorySlot)
				{
					if (Display.isActive()) {
						KeyboardSimulator.robot.mouseWheel(-25);
						short duration = 250;
						vrsystem.TriggerHapticPulse.apply(controllerDeviceIndex[1], 0, duration);
					}
					else hotbarPrev.pressKey();
					inventory_swipe[c] += swipeDistancePerInventorySlot;
				}
			}
		}
	}

	private static void changeHotbar(int dir){
		mc.thePlayer.inventory.changeCurrentItem(dir);
		short duration = 250;
		vrsystem.TriggerHapticPulse.apply(controllerDeviceIndex[1], 0, duration);
	}
	
	private static void updatePose()
	{
		if ( vrsystem == null || vrCompositor == null || vrCompositor.WaitGetPoses == null)
			return;

		Minecraft.getMinecraft().mcProfiler.startSection("waitForPoses");
		vrCompositor.WaitGetPoses.apply(hmdTrackedDevicePoseReference, JOpenVRLibrary.k_unMaxTrackedDeviceCount, null, 0);
		//vrsystem.GetDeviceToAbsoluteTrackingPose.apply(jopenvr.JOpenVRLibrary.ETrackingUniverseOrigin.ETrackingUniverseOrigin_TrackingUniverseStanding,0,hmdTrackedDevicePoseReference,JOpenVRLibrary.k_unMaxTrackedDeviceCount);
		Minecraft.getMinecraft().mcProfiler.endSection();

		for (int nDevice = 0; nDevice < JOpenVRLibrary.k_unMaxTrackedDeviceCount; ++nDevice )
		{
			hmdTrackedDevicePoses[nDevice].read();
			if ( hmdTrackedDevicePoses[nDevice].bPoseIsValid != 0 )
			{
				jopenvr.OpenVRUtil.convertSteamVRMatrix3ToMatrix4f(hmdTrackedDevicePoses[nDevice].mDeviceToAbsoluteTracking, poseMatrices[nDevice]);
				deviceVelocity[nDevice].xCoord = hmdTrackedDevicePoses[nDevice].vVelocity.v[0];
				deviceVelocity[nDevice].yCoord = hmdTrackedDevicePoses[nDevice].vVelocity.v[1];
				deviceVelocity[nDevice].zCoord = hmdTrackedDevicePoses[nDevice].vVelocity.v[2];
			}
		}

		if ( hmdTrackedDevicePoses[JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd].bPoseIsValid != 0 )
		{
			OpenVRUtil.Matrix4fCopy(poseMatrices[JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd], hmdPose);
			headIsTracking = true;
		}
		else
		{
			headIsTracking = false;
			OpenVRUtil.Matrix4fSetIdentity(hmdPose);
			hmdPose.M[1][3] = 1.62f;
		}

		findControllerDevices();

		for (int c=0;c<2;c++)
		{
			if (controllerDeviceIndex[c] != -1)
			{
				controllerTracking[c] = true;
				OpenVRUtil.Matrix4fCopy(poseMatrices[controllerDeviceIndex[c]], controllerPose[c]);
			}
			else
			{
				controllerTracking[c] = false;
				OpenVRUtil.Matrix4fSetIdentity(controllerPose[c]);
			}
		}

		updateAim();
	}

	/**
	 * @return The coordinate of the 'center' eye position relative to the head yaw plane
	 */
	
	static Vec3 getCenterEyePosition() {
		Vector3f pos = OpenVRUtil.convertMatrix4ftoTranslationVector(hmdPose);
		// not sure why the negative y is required here - no more!
		return Vec3.createVectorHelper(pos.x, pos.y, pos.z);
	}

	/**
	 * @return The coordinate of the left or right eye position relative to the head yaw plane
	 */
	
	static Vec3 getEyePosition(renderPass eye)
	{
		Matrix4f hmdToEye = hmdPoseRightEye;
		if ( eye == renderPass.Left )
		{
			hmdToEye = hmdPoseLeftEye;
		} else if ( eye == renderPass.Right)
		{
			hmdToEye = hmdPoseRightEye;
		} else {
			hmdToEye = null;
		}
			
		if(hmdToEye == null){
			Matrix4f pose = hmdPose;
			Vector3f pos = OpenVRUtil.convertMatrix4ftoTranslationVector(pose);
			return Vec3.createVectorHelper(pos.x, pos.y, pos.z);
		} else {
			Matrix4f pose = Matrix4f.multiply( hmdPose, hmdToEye );
			Vector3f pos = OpenVRUtil.convertMatrix4ftoTranslationVector(pose);
			return Vec3.createVectorHelper(pos.x, pos.y, pos.z);
		}
	}

	/**
	 * Resets the current origin position
	 */
	
	public void resetOrigin()
	{
		// not needed with Lighthouse
	}

	/**
	 * Resets the current origin rotation
	 */
	
	public void resetOriginRotation()
	{
		// not needed with Lighthouse
	}

	/**
	 * Enables prediction/filtering
	 */
	
	public void setPrediction(float delta, boolean enable)
	{
		// n/a
	}

	/**
	 * Gets the Yaw(Y) from YXZ Euler angle representation of orientation
	 *
	 * @return The Head Yaw, in degrees
	 */
	
	static float getHeadYawDegrees()
	{
		Quatf quat = OpenVRUtil.convertMatrix4ftoRotationQuat(hmdPose);

		EulerOrient euler = OpenVRUtil.getEulerAnglesDegYXZ(quat);

		return euler.yaw;
	}

	/**
	 * Gets the Pitch(X) from YXZ Euler angle representation of orientation
	 *
	 * @return The Head Pitch, in degrees
	 */
	
	static float getHeadPitchDegrees()
	{
		Quatf quat = OpenVRUtil.convertMatrix4ftoRotationQuat(hmdPose);

		EulerOrient euler = OpenVRUtil.getEulerAnglesDegYXZ(quat);

		return euler.pitch;
	}

	/**
	 * Gets the orientation quaternion
	 *
	 * @return quaternion w, x, y & z components
	 */
	
		
	final String k_pch_SteamVR_Section = "steamvr";
	final String k_pch_SteamVR_RenderTargetMultiplier_Float = "renderTargetMultiplier";
	
	static void onGuiScreenChanged(GuiScreen previousScreen, GuiScreen newScreen)
	{
		KeyBinding.unPressAllKeys();
		KeyboardSimulator.robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		KeyboardSimulator.robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
		KeyboardSimulator.robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
		KeyboardSimulator.robot.keyRelease(KeyEvent.VK_SHIFT);
		
		if(newScreen == null 	|| (mc.thePlayer!=null && !mc.thePlayer.isEntityAlive())){
			guiPos_World = null;
			guiRotationPose = null;
		}
		
		// main menu/win game/
		if (mc.theWorld==null || mc.currentScreen instanceof GuiWinGame ) {
			//TODO reset scale things
			MCOpenVR.guiScale = 2.0f;
			mc.vrPlayer.worldScale = 1;
			mc.vrPlayer.worldRotationRadians = (float) Math.toRadians( mc.vrSettings.vrWorldRotation);
			guiPos_World = new Vector3f(
					(float) (0 + mc.vrPlayer.getRoomOriginPos_World().xCoord),
					(float) (1.3f + mc.vrPlayer.getRoomOriginPos_World().yCoord),
					(float) (-1.3f + mc.vrPlayer.getRoomOriginPos_World().zCoord));
			
			
			guiRotationPose = new Matrix4f();
			guiRotationPose.M[0][0] = guiRotationPose.M[1][1] = guiRotationPose.M[2][2] = guiRotationPose.M[3][3] = 1.0F;
			guiRotationPose.M[0][1] = guiRotationPose.M[1][0] = guiRotationPose.M[2][3] = guiRotationPose.M[3][1] = 0.0F;
			guiRotationPose.M[0][2] = guiRotationPose.M[1][2] = guiRotationPose.M[2][0] = guiRotationPose.M[3][2] = 0.0F;
			guiRotationPose.M[0][3] = guiRotationPose.M[1][3] = guiRotationPose.M[2][1] = guiRotationPose.M[3][0] = 0.0F;
			
			return;
		} else { //these dont update when screen open.

		}		
		
		// i am dead view / now uses main menu room
		if (mc.thePlayer!=null && !mc.thePlayer.isEntityAlive())
		{
			Matrix4f rot = Matrix4f.rotationY((float) Math.toRadians(mc.vrSettings.vrWorldRotation));
			Matrix4f max = Matrix4f.multiply(rot, MCOpenVR.hmdRotation);
			MCOpenVR.guiScale = 1.0f*mc.vrSettings.vrWorldScale;
			Vec3 v = mc.entityRenderer.getEyeRenderPos(renderPass.Center);
			Vec3 d = mc.roomScale.getHMDDir_World();
			guiPos_World = new Vector3f(
					(float) (v.xCoord + d.xCoord*mc.vrSettings.vrWorldScale),
					(float) (v.yCoord + d.yCoord*mc.vrSettings.vrWorldScale),
					(float) (v.zCoord + d.zCoord*mc.vrSettings.vrWorldScale));

			Quatf orientationQuat = OpenVRUtil.convertMatrix4ftoRotationQuat(max);

			guiRotationPose = new Matrix4f(orientationQuat);

			guiRotationPose.M[3][3] = 1.0f;
			
			guiPos_World = guiPos_World.subtract(new Vector3f((float)mc.entityRenderer.interPolatedRoomOrigin.xCoord,
					(float) mc.entityRenderer.interPolatedRoomOrigin.yCoord, (float) mc.entityRenderer.interPolatedRoomOrigin.zCoord));
		
		}  else if ( previousScreen==null && newScreen != null	|| 
				newScreen instanceof GuiContainerCreative 
				|| newScreen instanceof GuiChat) {			

			Quatf controllerOrientationQuat;
			boolean appearOverBlock = (newScreen instanceof GuiCrafting)
					|| (newScreen instanceof GuiChest)
					|| (newScreen instanceof GuiHopper)
					|| (newScreen instanceof GuiFurnace)
					|| (newScreen instanceof GuiBrewingStand)
					|| (newScreen instanceof GuiBeacon)
					|| (newScreen instanceof GuiDispenser)
					|| (newScreen instanceof GuiEnchantment)
					|| (newScreen instanceof GuiRepair)
					;

			if(appearOverBlock && mc.objectMouseOver !=null){	

				guiScale =(float) (Math.sqrt(mc.vrSettings.vrWorldScale) * 2);
				guiPos_World =new Vector3f((float) mc.objectMouseOver.blockX + 0.5f,
						(float) mc.objectMouseOver.blockY + 1.7f,
						(float) mc.objectMouseOver.blockZ + 0.5f);

				Vec3 pos = mc.roomScale.getHMDPos_World();
				Vector3f look = new Vector3f();
				look.x = (float) (guiPos_World.x - pos.xCoord);
				look.y = (float) (guiPos_World.y - pos.yCoord);
				look.z = (float) (guiPos_World.z - pos.zCoord);

				float pitch = (float) Math.asin(look.y/look.length());
				float yaw = (float) ((float) Math.PI + Math.atan2(look.x, look.z));    
				guiRotationPose = Matrix4f.rotationY((float) yaw);
				Matrix4f tilt = OpenVRUtil.rotationXMatrix(pitch);	
				guiRotationPose = Matrix4f.multiply(guiRotationPose,tilt);	
				
				guiPos_World = guiPos_World.subtract(new Vector3f((float)mc.entityRenderer.interPolatedRoomOrigin.xCoord,
						(float) mc.entityRenderer.interPolatedRoomOrigin.yCoord, (float) mc.entityRenderer.interPolatedRoomOrigin.zCoord));
			
			}				
			else{
				Vec3 adj = Vec3.createVectorHelper(0,0,-2);
				if (newScreen instanceof GuiChat){
					 adj = Vec3.createVectorHelper(0.3,1,-2);
				} else if (newScreen instanceof GuiScreenBook || newScreen instanceof GuiEditSign) {
					 adj = Vec3.createVectorHelper(0,1,-2);
				}
				
				Vec3 v = mc.vrPlayer.getHMDPos_World();
				Vec3 e = mc.roomScale.getCustomHMDVector(adj);

				guiPos_World = new Vector3f(
						(float) (e.xCoord * mc.vrSettings.vrWorldScale / 2 + v.xCoord),
						(float) (e.yCoord* mc.vrSettings.vrWorldScale / 2 + v.yCoord),
						(float) (e.zCoord* mc.vrSettings.vrWorldScale / 2 + v.zCoord));
				Matrix4f hmd = hmdRotation;
				Matrix4f cont = controllerRotation[0];
				Matrix4f rot = Matrix4f.rotationY((float) Math.toRadians(mc.vrSettings.vrWorldRotation));
				hmd = Matrix4f.multiply(hmd, rot);
				cont = Matrix4f.multiply(cont, rot);

				guiScale = mc.vrSettings.vrWorldScale;
				if(mc.theWorld == null) guiScale = 2.0f;
							
				guiRotationPose = Matrix4f.rotationY((float) Math.toRadians( getHeadYawDegrees() + mc.vrSettings.vrWorldRotation));
				Matrix4f tilt = OpenVRUtil.rotationXMatrix((float)Math.toRadians(mc.roomScale.getHMDPitch_World()));	
				guiRotationPose = Matrix4f.multiply(guiRotationPose,tilt);		

				guiPos_World = guiPos_World.subtract(new Vector3f((float)mc.entityRenderer.interPolatedRoomOrigin.xCoord,
						(float) mc.entityRenderer.interPolatedRoomOrigin.yCoord, (float) mc.entityRenderer.interPolatedRoomOrigin.zCoord));
			
			}
		}
		



	}

	//-------------------------------------------------------
	// IBodyAimController

	float getBodyPitchDegrees() {
		return 0; //Always return 0 for body pitch
	}
	
	float getAimYaw() {
		return aimYaw;
	}
	
	float getAimPitch() {
		return aimPitch;
	}
	
    Vector3f forward = new Vector3f(0,0,-1);
	
	Vec3 getAimVector( int controller ) {
		Matrix4f aimRotation = controller == 0 ? controllerRotation[0]: controllerRotation[1];
        Vector3f controllerDirection = aimRotation.transform(forward);
		Vec3 out = Vec3.createVectorHelper(controllerDirection.x, controllerDirection.y,controllerDirection.z);
		return out;

	}
	
	
	public static Matrix4f getAimRotation( int controller ) {
		return controller == 0 ? controllerRotation[0]: controllerRotation[1];
	}
	
	
	public boolean initBodyAim() throws Exception
	{
		return init();
	}

	private static void updateSmoothedVelocity()
	{
		int maxSamples = 1000000;
		for (int c=0;c<1;c++)
		{
			int device = controllerDeviceIndex[c];
			if (device == -1)
			{
				controllerVelocitySampleCount[c]=0;
			}
			else
			{
				int sample = controllerVelocitySampleCount[c] % maxControllerVelocitySamples;
				controllerVelocitySamples[c][sample].xCoord = deviceVelocity[device].xCoord;
				controllerVelocitySamples[c][sample].yCoord = deviceVelocity[device].yCoord;
				controllerVelocitySamples[c][sample].zCoord = deviceVelocity[device].zCoord;
				controllerVelocitySampleCount[c]++;
				if (controllerVelocitySampleCount[c] > maxSamples)
				{
					controllerVelocitySampleCount[c] -= maxSamples;
				}
			}
		}
	}

	
	public Vec3 getSmoothedAimVelocity(int controller)
	{
		Vec3 velocity = Vec3.createVectorHelper(0,0,0);

		int samples = Math.min( maxControllerVelocitySamples, controllerVelocitySampleCount[controller] );
		samples = Math.min( samples, 3 );

		for (int i=0;i<samples;i++)
		{
			int sample = ( ( controllerVelocitySampleCount[controller] - 1 ) - i ) % maxControllerVelocitySamples;
			velocity.xCoord += controllerVelocitySamples[controller][sample].xCoord;
			velocity.yCoord += controllerVelocitySamples[controller][sample].yCoord;
			velocity.zCoord += controllerVelocitySamples[controller][sample].zCoord;
		}
		if (samples>0)
		{
			velocity.xCoord /= (float) samples;
			velocity.yCoord /= (float) samples;
			velocity.zCoord /= (float) samples;
		}

		return velocity;
	}
	
	static Vec3 getAimSource( int controller ) {
		return Vec3.createVectorHelper(aimSource[controller].xCoord, aimSource[controller].yCoord, aimSource[controller].zCoord);
	}
	
	static void triggerHapticPulse(int controller, int strength) {
		if (controllerDeviceIndex[controller]==-1)
			return;
		vrsystem.TriggerHapticPulse.apply(controllerDeviceIndex[controller], 0, (short)strength);
	}

	

	
	private static void updateAim() {
		if (mc==null)
			return;
		Vector3f forward = new Vector3f(0,0,-1);
		
		hmdRotation.M[0][0] = hmdPose.M[0][0];
		hmdRotation.M[0][1] = hmdPose.M[0][1];
		hmdRotation.M[0][2] = hmdPose.M[0][2];
		hmdRotation.M[0][3] = 0.0F;
		hmdRotation.M[1][0] = hmdPose.M[1][0];
		hmdRotation.M[1][1] = hmdPose.M[1][1];
		hmdRotation.M[1][2] = hmdPose.M[1][2];
		hmdRotation.M[1][3] = 0.0F;
		hmdRotation.M[2][0] = hmdPose.M[2][0];
		hmdRotation.M[2][1] = hmdPose.M[2][1];
		hmdRotation.M[2][2] = hmdPose.M[2][2];
		hmdRotation.M[2][3] = 0.0F;
		hmdRotation.M[3][0] = 0.0F;
		hmdRotation.M[3][1] = 0.0F;
		hmdRotation.M[3][2] = 0.0F;
		hmdRotation.M[3][3] = 1.0F;
		
		headDirection = hmdRotation.transform(forward);
		
		if(mc.vrSettings.seated){
			controllerPose[0] = hmdPose.inverted().inverted();
			controllerPose[1] = hmdPose.inverted().inverted();
		}
		
		// grab controller position in tracker space, scaled to minecraft units
		Vector3f controllerPos = OpenVRUtil.convertMatrix4ftoTranslationVector(controllerPose[0]);
		aimSource[0].xCoord = controllerPos.x;
		aimSource[0].yCoord = controllerPos.y;
		aimSource[0].zCoord = controllerPos.z;

		// build matrix describing controller rotation
		controllerRotation[0].M[0][0] = controllerPose[0].M[0][0];
		controllerRotation[0].M[0][1] = controllerPose[0].M[0][1];
		controllerRotation[0].M[0][2] = controllerPose[0].M[0][2];
		controllerRotation[0].M[0][3] = 0.0F;
		controllerRotation[0].M[1][0] = controllerPose[0].M[1][0];
		controllerRotation[0].M[1][1] = controllerPose[0].M[1][1];
		controllerRotation[0].M[1][2] = controllerPose[0].M[1][2];
		controllerRotation[0].M[1][3] = 0.0F;
		controllerRotation[0].M[2][0] = controllerPose[0].M[2][0];
		controllerRotation[0].M[2][1] = controllerPose[0].M[2][1];
		controllerRotation[0].M[2][2] = controllerPose[0].M[2][2];
		controllerRotation[0].M[2][3] = 0.0F;
		controllerRotation[0].M[3][0] = 0.0F;
		controllerRotation[0].M[3][1] = 0.0F;
		controllerRotation[0].M[3][2] = 0.0F;
		controllerRotation[0].M[3][3] = 1.0F;

		if(mc.vrSettings.seated && mc.currentScreen == null){
			org.lwjgl.util.vector.Matrix4f temp = new org.lwjgl.util.vector.Matrix4f();
			
			float hRange = 110;
			float vRange = 180;
			double h = Mouse.getX() / (double) mc.displayWidth * hRange - (hRange / 2);
			double v = Mouse.getY() / (double) mc.displayHeight * vRange - (vRange / 2);
			
			if(Display.isActive()){
				float rotStart = 30;
				float rotSpeed = 180; // Degrees per second
				float rotMul = ((float)Math.abs(h) - rotStart) / ((hRange / 2) - rotStart); // Scaled 0...1 from rotStart to FOV edge
				if(h < -rotStart){
					mc.vrSettings.vrWorldRotation += rotSpeed * rotMul * mc.getFrameDelta();
					mc.vrSettings.vrWorldRotation %= 360; // Prevent stupidly large values
					hmdForwardYaw = (float)Math.toDegrees(Math.atan2(headDirection.x, headDirection.z));    
					mc.vrPlayer.checkandUpdateRotateScale(true);
				}
				if(h > rotStart){
					mc.vrSettings.vrWorldRotation -= rotSpeed * rotMul * mc.getFrameDelta();
					mc.vrSettings.vrWorldRotation %= 360; // Prevent stupidly large values
					hmdForwardYaw = (float)Math.toDegrees(Math.atan2(headDirection.x, headDirection.z));    
					mc.vrPlayer.checkandUpdateRotateScale(true);
				}
			}
			temp.rotate((float) Math.toRadians(-v), new org.lwjgl.util.vector.Vector3f(1,0,0));
			
			temp.rotate((float) Math.toRadians(-180 + h - hmdForwardYaw), new org.lwjgl.util.vector.Vector3f(0,1,0));
			
			controllerRotation[0].M[0][0] = temp.m00;
			controllerRotation[0].M[0][1] = temp.m01;
			controllerRotation[0].M[0][2] = temp.m02;
			
			controllerRotation[0].M[1][0] = temp.m10;
			controllerRotation[0].M[1][1] = temp.m11;
			controllerRotation[0].M[1][2] = temp.m12;
			
			controllerRotation[0].M[2][0] = temp.m20;
			controllerRotation[0].M[2][1] = temp.m21;
			controllerRotation[0].M[2][2] = temp.m22;
		}
		
		// Calculate aim angles from controller orientation
		// Minecraft entities don't have a roll, so just base it on a direction
		controllerDirection = controllerRotation[0].transform(forward);
		aimPitch = (float)Math.toDegrees(Math.asin(controllerDirection.y/controllerDirection.length()));
		aimYaw = (float)Math.toDegrees(Math.atan2(controllerDirection.x, controllerDirection.z));

	
		// update off hand aim
		Vector3f leftControllerPos = OpenVRUtil.convertMatrix4ftoTranslationVector(controllerPose[1]);
		aimSource[1].xCoord = leftControllerPos.x;
		aimSource[1].yCoord = leftControllerPos.y;
		aimSource[1].zCoord = leftControllerPos.z;

		// build matrix describing controller rotation
		controllerRotation[1].M[0][0] = controllerPose[1].M[0][0];
		controllerRotation[1].M[0][1] = controllerPose[1].M[0][1];
		controllerRotation[1].M[0][2] = controllerPose[1].M[0][2];
		controllerRotation[1].M[0][3] = 0.0F;
		controllerRotation[1].M[1][0] = controllerPose[1].M[1][0];
		controllerRotation[1].M[1][1] = controllerPose[1].M[1][1];
		controllerRotation[1].M[1][2] = controllerPose[1].M[1][2];
		controllerRotation[1].M[1][3] = 0.0F;
		controllerRotation[1].M[2][0] = controllerPose[1].M[2][0];
		controllerRotation[1].M[2][1] = controllerPose[1].M[2][1];
		controllerRotation[1].M[2][2] = controllerPose[1].M[2][2];
		controllerRotation[1].M[2][3] = 0.0F;
		controllerRotation[1].M[3][0] = 0.0F;
		controllerRotation[1].M[3][1] = 0.0F;
		controllerRotation[1].M[3][2] = 0.0F;
		controllerRotation[1].M[3][3] = 1.0F;

		if(mc.vrSettings.seated){
			aimSource[1] = getCenterEyePosition();
			aimSource[0] = getCenterEyePosition();
		}
		
		lcontrollerDirection = controllerRotation[1].transform(forward);
		laimPitch = (float)Math.toDegrees(Math.asin(lcontrollerDirection.y/lcontrollerDirection.length()));
		laimYaw = (float)Math.toDegrees(Math.atan2(lcontrollerDirection.x, lcontrollerDirection.z));

	}

	public static double getCurrentTimeSecs()
	{
		return System.nanoTime() / 1000000000d;
	}
//	public static void renderFade(float dur, float r, float g, float b, float a){
//		vrCompositor.FadeToColor.apply(dur, r, g, b, a, (byte)0);
//	}
//	
//	public static void renderGrid(Color4f color){
//		vrCompositor.FadeGrid.apply(0.5f, (byte)0);
//	}
}
