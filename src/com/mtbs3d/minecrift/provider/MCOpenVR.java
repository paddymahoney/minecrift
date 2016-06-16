package com.mtbs3d.minecrift.provider;

import com.mtbs3d.minecrift.api.*;
import com.mtbs3d.minecrift.control.ViveButtons;
import com.mtbs3d.minecrift.render.QuaternionHelper;
import com.mtbs3d.minecrift.settings.VRSettings;
import com.mtbs3d.minecrift.utils.KeyboardSimulator;
import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import de.fruitfly.ovr.UserProfileData;
import de.fruitfly.ovr.enums.*;
import de.fruitfly.ovr.structs.*;
import de.fruitfly.ovr.structs.Matrix4f;
import de.fruitfly.ovr.structs.Vector2f;
import de.fruitfly.ovr.structs.Vector3f;
import de.fruitfly.ovr.util.BufferUtil;
import net.minecraft.client.Minecraft;
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
import net.minecraft.util.Vec3;
import optifine.Utils;

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
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class MCOpenVR implements IEyePositionProvider, IOrientationProvider, IBasePlugin, IHMDInfo, IStereoProvider,
IEventNotifier, IEventListener, IBodyAimController
{
	private static String initStatus;
	private boolean initialized;
	private Minecraft mc;

	private static VR_IVRSystem_FnTable vrsystem;
	private static VR_IVRCompositor_FnTable vrCompositor;
	private static VR_IVROverlay_FnTable vrOverlay;
	private static VR_IVRSettings_FnTable vrSettings;

	private static IntBuffer hmdErrorStore;
	private static TrackedDevicePose_t.ByReference hmdTrackedDevicePoseReference;
	private static TrackedDevicePose_t[] hmdTrackedDevicePoses;
	private static TrackedDevicePose_t.ByReference hmdGamePoseReference;
	private static TrackedDevicePose_t[] hmdGamePoses;

	private static Matrix4f[] poseMatrices;
	private static Vec3[] deviceVelocity;

	private LongByReference oHandle = new LongByReference();

	// position/orientation of headset and eye offsets
	private static final Matrix4f hmdPose = new Matrix4f();
	private static Matrix4f hmdProjectionLeftEye;
	private static Matrix4f hmdProjectionRightEye;
	private static Matrix4f hmdPoseLeftEye = new Matrix4f();
	private static Matrix4f hmdPoseRightEye = new Matrix4f();
	private static boolean initSuccess = false, flipEyes = false;

	private static IntBuffer hmdDisplayFrequency;

	private static FloatBuffer tlastVsync;
	public static LongBuffer _tframeCount;

	private static float vsyncToPhotons;
	private static double timePerFrame, frameCountRun;
	private static long frameCount;

	// TextureIDs of framebuffers for each eye
	private int LeftEyeTextureId;

	private final VRTextureBounds_t texBounds = new VRTextureBounds_t();
	private final Texture_t texType = new Texture_t();

	// aiming
	private float bodyYaw = 0;
	private float aimYaw = 0;
	private float aimPitch = 0;

	private float laimPitch = 0;
	private float laimYaw = 0;

	private Vec3[] aimSource = new Vec3[2];

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

	private static Vector3f guiPos = new Vector3f();
	private static Matrix4f guiRotationPose = new Matrix4f();
	public static float guiScale = 1.0f;
	public double startedOpeningInventory = 0;

	// For mouse menu emulation
	private float controllerMouseX = -1.0f;
	private float controllerMouseY = -1.0f;
	public boolean controllerMouseValid;
	public int controllerMouseTicks;

	//keyboard
	public static boolean keyboardShowing = false;
	byte[] lastTyped = new byte[256];
	byte[] typed = new byte[256];
	static int pollsSinceLastChange = 0;

	// Touchpad samples
	private Vector2f[][] touchpadSamples = new Vector2f[2][5];
	private int[] touchpadSampleCount = new int[2];

	private float[] inventory_swipe = new float[2];
	
	private boolean headIsTracking;
	
	private HiddenAreaMesh_t[] hiddenMeshes = new HiddenAreaMesh_t[2];
	public float[][] hiddenMesheVertecies = new float[2][];
	
	private int moveModeSwitchcount = 0;

	@Override
	public String getName() {
		return "OpenVR";
	}

	@Override
	public String getID() {
		return "openvr";
	}

	@Override
	public String getInitializationStatus() { return initStatus; }

	@Override
	public boolean isInitialized() { return initialized; }

	@Override
	public String getVersion() { return "Version TODO"; }

	public MCOpenVR()
	{
		super();
		PluginManager.register(this);

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

	private boolean tried;

	@Override
	public boolean init()  throws Exception
	{

		if ( this.initialized )
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
			System.out.println( "VR Headset not detected.");
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



		this.initialized = true;
		return true;
	}

//	KeyBinding rotateLeft = new KeyBinding("Rotate Left", 203, "Movement");
//	KeyBinding rotateRight = new KeyBinding("Rotate Right", 205, "Movement");
	final int rotationIncrement = 0;

	private void initializeJOpenVR() throws Exception { 
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

			//            // init controllers for the first time
			//            VRInput._updateConnectedControllers();
			//            
			//            // init bounds & chaperone info
			//            VRBounds.init();
			//            
			initSuccess = true;
		}
	}

	private Pointer ptrFomrString(String in){
		Pointer p = new Memory(in.length()+1);
		p.setString(0, in);
		return p;

	}


	// needed for in-game keyboard
	public void initOpenVROverlay() throws Exception
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


	public void initOpenVROSettings() throws Exception
	{
		vrSettings =   new VR_IVRSettings_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRSettings_Version, hmdErrorStore));
		if (vrSettings != null &&  hmdErrorStore.get(0) == 0) {     		
			vrSettings.setAutoSynch(false);
			vrSettings.read();					
			System.out.println("OpenVR Settings initialized OK");
		} else {
			throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));		
		}
	}
	
	public void initOpenVRCompositor(boolean set) throws Exception
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
		texType.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
		texType.eType = JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL;
		texType.setAutoSynch(false);
		texType.setAutoRead(false);
		texType.setAutoWrite(false);
		texType.handle = -1;
		texType.write();

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

	@Override
	public void poll(long frameIndex)
	{
		mc.mcProfiler.startSection("poll");


		pollInputEvents();

		updateControllerButtonState();
		updateTouchpadSampleBuffer();
		updateSmoothedVelocity();

		processControllerButtons();
		processTouchpadSampleBuffer();

		// GUI controls
		if( mc.currentScreen != null )
		{
			processGui();
		}

		updatePose();

		mc.mcProfiler.endSection();
	}

	GuiTextField keyboardGui;

	public boolean setKeyboardOverlayShowing(boolean showingState, GuiTextField gui) {
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

		} else {
			try {
				if (keyboardShowing) {
					vrOverlay.HideKeyboard.apply();				
				}
			} catch (Error e) {
				// TODO: handle exception
			}
			keyboardShowing = false;
		}

		return keyboardShowing;
	}

	//sets mouse position for currentscreen
	private void processGui() {
		Vector3f controllerPos = OpenVRUtil.convertMatrix4ftoTranslationVector(controllerPose[0]);

		Vector3f forward = new Vector3f(0,0,1);
		Vector3f controllerDirection = controllerRotation[0].transform(forward);

		Vector3f guiNormal = guiRotationPose.transform(forward);
		Vector3f guiRight = guiRotationPose.transform(new Vector3f(1,0,0));
		Vector3f guiUp = guiRotationPose.transform(new Vector3f(0,1,0));

		float guiWidth = 1.0f;		
		float guiHalfWidth = guiWidth * 0.5f;		
		float guiHeight = 1.0f;	
		float guiHalfHeight = guiHeight * 0.5f;

		Vector3f guiTopLeft = guiPos.subtract(guiUp.divide(1.0f / guiHalfHeight)).subtract(guiRight.divide(1.0f/guiHalfWidth));
		Vector3f guiTopRight = guiPos.subtract(guiUp.divide(1.0f / guiHalfHeight)).add(guiRight.divide(1.0f / guiHalfWidth));

		//Vector3f guiBottomLeft = guiPos.add(guiUp.divide(1.0f / guiHalfHeight)).subtract(guiRight.divide(1.0f/guiHalfWidth));
		//Vector3f guiBottomRight = guiPos.add(guiUp.divide(1.0f / guiHalfHeight)).add(guiRight.divide(1.0f/guiHalfWidth));

		float guiNormalDotControllerDirection = guiNormal.dot(controllerDirection);
		if (Math.abs(guiNormalDotControllerDirection) > 0.00001f)
		{//pointed normal to the GUI
			float intersectDist = -guiNormal.dot(controllerPos.subtract(guiTopLeft)) / guiNormalDotControllerDirection;
			Vector3f pointOnPlane = controllerPos.add(controllerDirection.divide(1.0f/intersectDist));

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
			this.mc.guiU = u;
			this.mc.guiV = v;
			this.mc.intersectDist = intersectDist;
			this.mc.pointOnPlaneX = pointOnPlane.x;
			this.mc.pointOnPlaneY = pointOnPlane.y;
			this.mc.pointOnPlaneZ = pointOnPlane.z;
			this.mc.guiTopLeftX = guiTopLeft.x;
			this.mc.guiTopLeftY = guiTopLeft.y;
			this.mc.guiTopLeftZ = guiTopLeft.z;
			this.mc.guiTopRightX = guiTopRight.x;
			this.mc.guiTopRightY = guiTopRight.y;
			this.mc.guiTopRightZ = guiTopRight.z;
			this.mc.controllerPosX = controllerPos.x;
			this.mc.controllerPosY = controllerPos.y;
			this.mc.controllerPosZ = controllerPos.z;
		}

		mc.currentScreen.mouseOffsetX = -1;
		mc.currentScreen.mouseOffsetY = -1;

		if (controllerMouseX >= 0 && controllerMouseX < mc.displayWidth
				&& controllerMouseY >=0 && controllerMouseY < mc.displayHeight)
		{
			// clamp to screen
			int mouseX = Math.min(Math.max((int) controllerMouseX, 0), mc.displayWidth);
			int mouseY = Math.min(Math.max((int) controllerMouseY, 0), mc.displayHeight);

			if (controllerDeviceIndex[RIGHT_CONTROLLER] != -1)
			{
				Mouse.setCursorPosition(mouseX, mouseY);
				controllerMouseValid = true;
				//LMB
				if (controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold && 
						lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x <= triggerThreshold 
						)
				{
					//click left mouse button
					mc.currentScreen.mouseDown(mouseX, mouseY, 0);
				}	

				if (controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold) {					
					mc.currentScreen.mouseDrag(mouseX, mouseY);//Signals mouse move

				}


				if (controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x <= triggerThreshold && 
						lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold 
						)
				{
					//click left mouse button
					mc.currentScreen.mouseUp(mouseX, mouseY, 0);
				}	

				//RMB
				if (
						(controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
						(lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) == 0 
						)				
				{
					//click left mouse button
					mc.currentScreen.mouseDown(mouseX, mouseY, 1);
				}	

				if (controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold)
				{
					mc.currentScreen.mouseDrag(mouseX, mouseY);//Signals mouse move
				}


				if(		(controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) == 0 &&
						(lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 
						)
				{
					//click left mouse button
					mc.currentScreen.mouseUp(mouseX, mouseY, 1);
				}	
				//end RMB



			} else // right controller not found
			{

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


	@Override
	public void destroy()
	{
		if (this.initialized)
		{
			JOpenVRLibrary.VR_ShutdownInternal();
			this.initialized = false;
		}
	}

	@Override
	public boolean isCalibrated(PluginType type) { return true; }

	@Override
	public void beginCalibration(PluginType type) { }

	@Override
	public void updateCalibration(PluginType type) { }

	@Override
	public String getCalibrationStep(PluginType type) { return "No calibration required"; }

	@Override
	public void beginFrame()
	{
		beginFrame(0);
	}

	@Override
	public void beginFrame(long frameIndex)
	{

	}

	@Override
	public boolean endFrame(EyeType eye)
	{
		mc.mcProfiler.startSection("submit");

		GL11.glFinish();
		int ret = 0;
		if(eye == EyeType.ovrEye_Left){
			ret = vrCompositor.Submit.apply(
				JOpenVRLibrary.EVREye.EVREye_Eye_Left,
				texType, texBounds,
				JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);
		}else{
			ret = vrCompositor.Submit.apply(
				JOpenVRLibrary.EVREye.EVREye_Eye_Right,
				texType, texBounds,
				JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);
		}

		mc.mcProfiler.endSection();

		//System.out.println("vsync="+JOpenVRLibrary.VR_IVRCompositor_GetVSync(vrCompositor));
		return true;
	}

	@Override
	public HmdParameters getHMDInfo()
	{
		HmdParameters hmd = new HmdParameters();
		if ( isInitialized() )
		{
			IntBuffer rtx = IntBuffer.allocate(1);
			IntBuffer rty = IntBuffer.allocate(1);
			vrsystem.GetRecommendedRenderTargetSize.apply(rtx, rty);

			hmd.Type = HmdType.ovrHmd_Other;
			hmd.ProductName = "OpenVR";
			hmd.Manufacturer = "Unknown";
			hmd.AvailableHmdCaps = 0;
			hmd.DefaultHmdCaps = 0;
			hmd.AvailableTrackingCaps = HmdParameters.ovrTrackingCap_Orientation | HmdParameters.ovrTrackingCap_Position;
			hmd.DefaultTrackingCaps = HmdParameters.ovrTrackingCap_Orientation | HmdParameters.ovrTrackingCap_Position;
			hmd.Resolution = new Sizei( rtx.get(0) * 2, rty.get(0) );

			float topFOV = vrsystem.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewTopDegrees_Float, hmdErrorStore);
			float bottomFOV = vrsystem.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewBottomDegrees_Float, hmdErrorStore);
			float leftFOV = vrsystem.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewLeftDegrees_Float, hmdErrorStore);
			float rightFOV = vrsystem.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewRightDegrees_Float, hmdErrorStore);

			hmd.DefaultEyeFov[0] = new FovPort((float)Math.tan(topFOV),(float)Math.tan(bottomFOV),(float)Math.tan(leftFOV),(float)Math.tan(rightFOV));
			hmd.DefaultEyeFov[1] = new FovPort((float)Math.tan(topFOV),(float)Math.tan(bottomFOV),(float)Math.tan(leftFOV),(float)Math.tan(rightFOV));
			hmd.MaxEyeFov[0] = new FovPort((float)Math.tan(topFOV),(float)Math.tan(bottomFOV),(float)Math.tan(leftFOV),(float)Math.tan(rightFOV));
			hmd.MaxEyeFov[1] = new FovPort((float)Math.tan(topFOV),(float)Math.tan(bottomFOV),(float)Math.tan(leftFOV),(float)Math.tan(rightFOV));
			hmd.DisplayRefreshRate = 90.0f;
		}

		return hmd;
	}


	/* Gets the current user profile data */
	@Override
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
	@Override
	public void update(float ipd, float yawHeadDegrees, float pitchHeadDegrees, float rollHeadDegrees,
			float worldYawOffsetDegrees, float worldPitchOffsetDegrees, float worldRollOffsetDegrees)
	{

	}

	private void findControllerDevices()
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

	private void updateControllerButtonState()
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
				lastControllerState[c].ulButtonPressed = 0;

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
	private void processControllerButtons()
	{
		if (mc.theWorld == null)
			return;

		boolean gui = (mc.currentScreen != null);
		boolean sleeping = (mc.thePlayer != null && mc.thePlayer.isPlayerSleeping());

		// right controller
		//last
		boolean lastpressedRGrip = (lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonGrip) > 0;		
		boolean lastpressedRtouchpadBottomLeft = (lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].y <= 0 ) &&
				(lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].x <= 0 ) ;	
		boolean lastpressedRtouchpadBottomRight = (lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].y <= 0 ) &&
				(lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].x > 0 ) ;		
		boolean lastpressedRtouchpadTopLeft = (lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].y > 0 ) &&
				(lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].x <= 0 ) ;	
		boolean lastpressedRtouchpadTopRight = (lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].y > 0 ) &&
				(lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].x > 0 ) ;		
		boolean lastpressedRAppMenu = (lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonAppMenu) > 0;
		boolean lastpressedRTrigger = lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold;		
		boolean lastpressedRTriggerClick =( lastControllerState[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTrigger )>0;
		//current
		boolean pressedRGrip = (controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonGrip) > 0;
		boolean pressedRtouchpadBottomLeft = (controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].y <= 0 ) &&
				(controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].x <= 0 ) ;	
		boolean pressedRtouchpadBottomRight = (controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].y <= 0 ) &&
				(controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].x > 0 ) ;		
		boolean pressedRtouchpadTopLeft = (controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].y > 0 ) &&
				(controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].x <= 0 ) ;	
		boolean pressedRtouchpadTopRight = (controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].y > 0 ) &&
				(controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_TouchPad].x > 0 ) ;	
		boolean pressedRAppMenu = (controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonAppMenu) > 0;
		boolean pressedRTrigger = controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold;
		boolean pressedRTriggerClick =( controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed & k_buttonTrigger )>0;


		//R GRIP
		if (pressedRGrip && !lastpressedRGrip) {
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_GRIP.ordinal()].press();
		}	
		if(!pressedRGrip && lastpressedRGrip) {
			mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_GRIP.ordinal()].unpress();
		}

		//R TOUCHPAD	

		if(!gui){ //this are the mouse buttons.
		
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
				//keyBindR_Trigger.pressKey();
			}	
			if(!pressedRTrigger && lastpressedRTrigger) {
				mc.vrSettings.buttonMappings[ViveButtons.BUTTON_RIGHT_TRIGGER.ordinal()].unpress();
				//	keyBindR_Trigger.unpressKey();
			}
		}

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
				(lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].y <= 0 ) &&
				(lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].x <= 0 ) ;	
		boolean lastpressedLtouchpadBottomRight = (lastControllerState[LEFT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].y <= 0 ) &&
				(lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].x > 0 ) ;		
		boolean lastpressedLtouchpadTopLeft = (lastControllerState[LEFT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].y > 0 ) &&
				(lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].x <= 0 ) ;	
		boolean lastpressedLtouchpadTopRight = (lastControllerState[LEFT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].y > 0 ) &&
				(lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].x > 0 ) ;		
		boolean lastpressedLAppMenu = (lastControllerState[LEFT_CONTROLLER].ulButtonPressed & k_buttonAppMenu) > 0;
		boolean lastpressedLTrigger = lastControllerState[LEFT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold;		
		boolean lastpressedLTriggerClick =( lastControllerState[LEFT_CONTROLLER].ulButtonPressed & k_buttonTrigger )>0;
		//current
		boolean pressedLGrip = (controllerStateReference[LEFT_CONTROLLER].ulButtonPressed & k_buttonGrip) > 0;
		boolean pressedLtouchpadBottomLeft = (controllerStateReference[LEFT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(controllerStateReference[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].y <= 0 ) &&
				(controllerStateReference[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].x <= 0 ) ;	
		boolean pressedLtouchpadBottomRight = (controllerStateReference[LEFT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(controllerStateReference[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].y <= 0 ) &&
				(controllerStateReference[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].x > 0 ) ;		
		boolean pressedLtouchpadTopLeft = (controllerStateReference[LEFT_CONTROLLER].ulButtonPressed & k_buttonTouchpad) > 0 &&
				(controllerStateReference[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].y > 0 ) &&
				(controllerStateReference[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].x <= 0 ) ;	
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


		
		//VIVE SPECIFIC FUNCTIONALITY
		//TODO: Find a better home for these in Minecraft.java		

		//no jump key if cant.Not a good place for this check.
		if(mc.gameSettings.keyBindJump.isPressed()) {
			if(!mc.vrPlayer.getFreeMoveMode() && !mc.vrSettings.simulateFalling) {
				mc.gameSettings.keyBindJump.unpressKey();
			}
		}

//		if(rotateLeft.isPressed()){
//			this.bodyYaw-=rotationIncrement;
//		}
//		
//		if(rotateRight.isPressed()){
//			this.bodyYaw+=rotationIncrement;
//		}
		
		// if you start teleporting, close any UI
		if (gui && !sleeping && mc.gameSettings.keyBindForward.getIsKeyPressed() && !(mc.currentScreen instanceof GuiWinGame))
		{
			mc.thePlayer.closeScreen();
		}

		//handle movementtoggle
		if (mc.gameSettings.keyBindPickBlock.getIsKeyPressed()) {
			if(mc.vrSettings.vrAllowLocoModeSwotch){
				moveModeSwitchcount++;
				if (moveModeSwitchcount >= 20 * 4) {
					moveModeSwitchcount = 0;
					if(mc.vrPlayer.noTeleportClient && mc.vrPlayer.getFreeMoveMode()){
						mc.printChatMessage("Cannot change move mode: This server does not allow teleporting.");
					}else {
					mc.vrPlayer.setFreeMoveMode(!mc.vrPlayer.getFreeMoveMode());
					mc.printChatMessage("Movement mode set to: " + (mc.vrPlayer.getFreeMoveMode() ? "Free Move" : "Teleport"));
					}
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
				
			if(mc.gameSettings.keyBindSneak.getIsKeyPressed()){				
				setKeyboardOverlayShowing(!keyboardShowing, null);			
			} else{
				if(gui || keyboardShowing){

					if(mc.currentScreen instanceof GuiWinGame){ //from 'esc' key on guiwingame since we cant push it.
						this.mc.thePlayer.sendQueue.addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.PERFORM_RESPAWN));
						this.mc.displayGuiScreen((GuiScreen)null);		
					}else {
						mc.thePlayer.closeScreen();
						setKeyboardOverlayShowing(false, null);
					}
				}else
					mc.displayInGameMenu();				
			}
		}
	}

	//jrbuda:: oh hello there you are.
	private void pollInputEvents()
	{
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

	private void updateTouchpadSampleBuffer()
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

	private void clearTouchpadSampleBuffer(int controller)
	{
		for (int sample=0;sample<5;sample++)
		{
			touchpadSamples[controller][sample].x = 0;
			touchpadSamples[controller][sample].y = 0;
		}
		touchpadSampleCount[controller] = 0;
		inventory_swipe[controller] = 0;
	}

	private void processTouchpadSampleBuffer()
	{
		if (mc.thePlayer == null)
			return;

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
		} else 	{
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
					mc.thePlayer.inventory.changeCurrentItem(-1);
					short duration = 250;
					vrsystem.TriggerHapticPulse.apply(controllerDeviceIndex[c], 0, duration);

					inventory_swipe[c] -= swipeDistancePerInventorySlot;
				} else if (inventory_swipe[c] < -swipeDistancePerInventorySlot)
				{
					mc.thePlayer.inventory.changeCurrentItem(1);

					short duration = 250;
					vrsystem.TriggerHapticPulse.apply(controllerDeviceIndex[c], 0, duration);
					inventory_swipe[c] += swipeDistancePerInventorySlot;
				}
			}
		}
			
			

		//TODO: R touchpad up/down scrolls containers

	}

	public void updatePose()
	{
		if ( vrsystem == null || vrCompositor == null )
			return;

		mc.mcProfiler.startSection("updatePose");

		vrCompositor.WaitGetPoses.apply(hmdTrackedDevicePoseReference, JOpenVRLibrary.k_unMaxTrackedDeviceCount, null, 0);

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
		}

		findControllerDevices();

		for (int c=0;c<2;c++)
		{
			if (controllerDeviceIndex[c] != -1)
			{
				OpenVRUtil.Matrix4fCopy(poseMatrices[controllerDeviceIndex[c]], controllerPose[c]);
			}
			else
			{
				OpenVRUtil.Matrix4fSetIdentity(controllerPose[c]);
			}
		}

		updateAim();

		mc.mcProfiler.endSection();
	}

	/**
	 * @return The coordinate of the 'center' eye position relative to the head yaw plane
	 */
	@Override
	public Vec3 getCenterEyePosition() {
		Vector3f pos = OpenVRUtil.convertMatrix4ftoTranslationVector(hmdPose);
		// not sure why the negative y is required here
		return Vec3.createVectorHelper(pos.x, -pos.y, pos.z);
	}

	/**
	 * @return The coordinate of the left or right eye position relative to the head yaw plane
	 */
	@Override
	public Vec3 getEyePosition(EyeType eye)
	{
		Matrix4f hmdToEye = hmdPoseRightEye;
		if ( eye == EyeType.ovrEye_Left )
		{
			hmdToEye = hmdPoseLeftEye;
		}

		Matrix4f pose = Matrix4f.multiply( hmdPose, hmdToEye );
		Vector3f pos = OpenVRUtil.convertMatrix4ftoTranslationVector(pose);

		// not sure why the negative y is required here
		return Vec3.createVectorHelper(pos.x, -pos.y, pos.z);
	}

	/**
	 * Resets the current origin position
	 */
	@Override
	public void resetOrigin()
	{
		// not needed with Lighthouse
	}

	/**
	 * Resets the current origin rotation
	 */
	@Override
	public void resetOriginRotation()
	{
		// not needed with Lighthouse
	}

	/**
	 * Enables prediction/filtering
	 */
	@Override
	public void setPrediction(float delta, boolean enable)
	{
		// n/a
	}

	/**
	 * Gets the Yaw(Y) from YXZ Euler angle representation of orientation
	 *
	 * @return The Head Yaw, in degrees
	 */
	@Override
	public float getHeadYawDegrees(EyeType eye)
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
	@Override
	public float getHeadPitchDegrees(EyeType eye)
	{
		Quatf quat = OpenVRUtil.convertMatrix4ftoRotationQuat(hmdPose);

		EulerOrient euler = OpenVRUtil.getEulerAnglesDegYXZ(quat);

		return euler.pitch;
	}

	/**
	 * Gets the Roll(Z) from YXZ Euler angle representation of orientation
	 *
	 * @return The Head Roll, in degrees
	 */
	@Override
	public float getHeadRollDegrees(EyeType eye)
	{
		Quatf quat = OpenVRUtil.convertMatrix4ftoRotationQuat(hmdPose);

		EulerOrient euler = OpenVRUtil.getEulerAnglesDegYXZ(quat);

		return euler.roll;
	}

	/**
	 * Gets the orientation quaternion
	 *
	 * @return quaternion w, x, y & z components
	 */
	@Override
	public Quaternion getOrientationQuaternion(EyeType eye)
	{
		Quatf orient = OpenVRUtil.convertMatrix4ftoRotationQuat(hmdPose);
		return new Quaternion(orient.x, orient.y, orient.z, orient.w);
	}

	@Override
	public RenderTextureInfo getRenderTextureSizes(FovPort leftFov,
			FovPort rightFov,
			float renderScaleFactor)
	{
		IntBuffer rtx = IntBuffer.allocate(1);
		IntBuffer rty = IntBuffer.allocate(1);
		vrsystem.GetRecommendedRenderTargetSize.apply(rtx, rty);

		RenderTextureInfo info = new RenderTextureInfo();
		info.HmdNativeResolution.w = rtx.get(0);
		info.HmdNativeResolution.h = rty.get(0);
		info.LeftFovTextureResolution.w = (int) (rtx.get(0) );
		info.LeftFovTextureResolution.h = (int) (rty.get(0) );
		info.RightFovTextureResolution.w = (int) (rtx.get(0) );
		info.RightFovTextureResolution.h = (int) (rty.get(0) );
		
		if ( info.LeftFovTextureResolution.w % 2 != 0) info.LeftFovTextureResolution.w++;
		if ( info.LeftFovTextureResolution.h % 2 != 0) info.LeftFovTextureResolution.w++;
		if ( info.RightFovTextureResolution.w % 2 != 0) info.LeftFovTextureResolution.w++;
		if ( info.RightFovTextureResolution.h % 2 != 0) info.LeftFovTextureResolution.w++;
		
		info.CombinedTextureResolution.w = info.LeftFovTextureResolution.w + info.RightFovTextureResolution.w;
		info.CombinedTextureResolution.h = info.LeftFovTextureResolution.h;
				
		
		for (int i = 0; i < 2; i++) {
			hiddenMeshes[i] = vrsystem.GetHiddenAreaMesh.apply(i);
			hiddenMeshes[i].read();

			hiddenMesheVertecies[i] = new float[hiddenMeshes[i].unTriangleCount * 3 * 2];
			Pointer arrptr = new Memory(hiddenMeshes[i].unTriangleCount * 3 * 2);
			hiddenMeshes[i].pVertexData.getPointer().read(0, hiddenMesheVertecies[i], 0, hiddenMesheVertecies[i].length);
			
			for (int ix = 0;ix < hiddenMesheVertecies[i].length;ix+=2) {
				hiddenMesheVertecies[i][ix] = hiddenMesheVertecies[i][ix] * info.LeftFovTextureResolution.w * renderScaleFactor;
				hiddenMesheVertecies[i][ix + 1] = hiddenMesheVertecies[i][ix +1] * info.LeftFovTextureResolution.h * renderScaleFactor;
			}
		}
		
		
//		Pointer pointers = new Memory(k_pch_SteamVR_Section.length()+1);
//		pointers.setString(0, k_pch_SteamVR_Section);
//		Pointer pointerk = new Memory(k_pch_SteamVR_RenderTargetMultiplier_Float.length()+1);
//		pointerk.setString(0, k_pch_SteamVR_RenderTargetMultiplier_Float);
//		IntByReference err = new IntByReference();
//		float test = vrSettings.GetFloat.apply(pointers, pointerk, 99.9f, err);
//		vrSettings.SetFloat.apply(pointers, pointerk, renderScaleFactor, err);
//		float test2 = vrSettings.GetFloat.apply(pointers, pointerk, 99.9f, err);

		return info;
	}
	final String k_pch_SteamVR_Section = "steamvr";
	final String k_pch_SteamVR_RenderTargetMultiplier_Float = "renderTargetMultiplier";
	
	@Override
	public EyeType eyeRenderOrder(int index)
	{
		return ( index == 1 ) ? EyeType.ovrEye_Right : EyeType.ovrEye_Left;
	}

	@Override
	public boolean usesDistortion()
	{
		return true;
	}

	@Override
	public boolean isStereo()
	{
		return true;
	}


	@Override
	public double getFrameTiming() {
		return getCurrentTimeSecs();
	}

	public void onGuiScreenChanged(GuiScreen previousScreen, GuiScreen newScreen)
	{
		if (previousScreen==null && newScreen != null
				|| (newScreen != null && newScreen instanceof GuiContainerCreative) || newScreen instanceof GuiChat) {			

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


			//set default position for inventory and chat screens.
			guiPos = OpenVRUtil.convertMatrix4ftoTranslationVector(hmdPose);

			if(!appearOverBlock){
				controllerOrientationQuat = OpenVRUtil.convertMatrix4ftoRotationQuat(hmdPose);
				EulerOrient controllerOrientationEuler = OpenVRUtil.getEulerAnglesDegYXZ(controllerOrientationQuat);
				guiRotationPose = Matrix4f.rotationY((float)-Math.toRadians(controllerOrientationEuler.yaw));
				Matrix4f tilt = OpenVRUtil.rotationXMatrix((float)-Math.toRadians(controllerOrientationEuler.pitch));	
				guiRotationPose = Matrix4f.multiply(guiRotationPose,tilt);					
			}				else{
				controllerOrientationQuat = OpenVRUtil.convertMatrix4ftoRotationQuat(controllerPose[0]);
				EulerOrient controllerOrientationEuler = OpenVRUtil.getEulerAnglesDegYXZ(controllerOrientationQuat);
				guiRotationPose = Matrix4f.rotationY((float)-Math.toRadians(controllerOrientationEuler.yaw));

			}

			guiScale = 1.0f;

			if (newScreen instanceof GuiChat){
				Vector3f forward = new Vector3f(-0.3f,-.4f,1f);
				Vector3f controllerForward = hmdPose.transform(forward).subtract(guiPos);
				guiPos = guiPos.subtract(controllerForward.divide(0.8f));
			} else if (newScreen instanceof GuiScreenBook || newScreen instanceof GuiEditSign) {
				Vector3f forward = new Vector3f(0,-.4f,1f);
				Vector3f controllerForward = hmdPose.transform(forward).subtract(guiPos);
				guiPos = guiPos.subtract(controllerForward.divide(1f));
			} else {
				Vector3f forward = new Vector3f(0,0,1f).normalised();
				Vector3f controllerForward = hmdPose.transform(forward).subtract(guiPos);
				guiPos = guiPos.subtract(controllerForward.divide(1f));
			}

			if (appearOverBlock && this.mc.objectMouseOver != null) {
				Vec3 blockTop = Vec3.createVectorHelper(
						(double) this.mc.objectMouseOver.blockX + 0.5,
						(double) this.mc.objectMouseOver.blockY + 1.7,
						(double) this.mc.objectMouseOver.blockZ + 0.5);

				// translate controller position by player position, giving a final world coordinate
				Entity player = this.mc.renderViewEntity;
				if (player!=null)
				{
					//Vec3 playerPos = player.getPositionVector();
					Minecraft mc = Minecraft.getMinecraft();
					Vec3 teleportSlide = mc.vrPlayer.getTeleportSlide();
					Vec3 playerPos = mc.vrPlayer.getRoomOrigin().addVector(teleportSlide.xCoord, teleportSlide.yCoord, teleportSlide.zCoord);
					blockTop.xCoord -= playerPos.xCoord;
					blockTop.yCoord -= playerPos.yCoord;
					blockTop.zCoord -= playerPos.zCoord;
				}

				guiPos.x = (float) -blockTop.xCoord;
				guiPos.y = (float) blockTop.yCoord;
				guiPos.z = (float) -blockTop.zCoord;

				guiScale = 2.0f;
			}


			if(mc.theWorld == null) guiScale = 2.0f;

		}
	}

	@Override
	public Matrix4f getProjectionMatrix(FovPort fov,
			EyeType eyeType,
			float nearClip,
			float farClip)
	{
		if ( eyeType == EyeType.ovrEye_Left )
		{
			HmdMatrix44_t mat = vrsystem.GetProjectionMatrix.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left, nearClip, farClip, JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL);
			hmdProjectionLeftEye = new Matrix4f();
			return OpenVRUtil.convertSteamVRMatrix4ToMatrix4f(mat, hmdProjectionLeftEye);
		}else{
			HmdMatrix44_t mat = vrsystem.GetProjectionMatrix.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right, nearClip, farClip, JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL);
			hmdProjectionRightEye = new Matrix4f();
			return OpenVRUtil.convertSteamVRMatrix4ToMatrix4f(mat, hmdProjectionRightEye);
		}
	}


	@Override
	public double getCurrentTimeSecs()
	{
		return System.nanoTime() / 1000000000d;
	}

	//-------------------------------------------------------
	// IBodyAimController

	@Override
	public float getBodyYawDegrees() {
		return bodyYaw;
	}
	@Override
	public void setBodyYawDegrees(float yawOffset) {
		bodyYaw = yawOffset;
	}
	@Override
	public float getBodyPitchDegrees() {
		return 0; //Always return 0 for body pitch
	}
	@Override
	public float getAimYaw() {
		return aimYaw + bodyYaw;
	}
	@Override
	public float getAimPitch() {
		return aimPitch;
	}
    Vector3f forward = new Vector3f(0,0,1);
	@Override
	public Vec3 getAimVector( int controller ) {
		Matrix4f aimRotation = controller == 0 ? controllerRotation[0]: controllerRotation[1];
        Vector3f controllerDirection = aimRotation.transform(forward);
		Vec3 out = Vec3.createVectorHelper(controllerDirection.x, controllerDirection.y,controllerDirection.z);
		out.rotateAroundY((float) Math.toRadians(-bodyYaw));
		return out;

	}
	
	@Override
	public Matrix4f getAimRotation( int controller ) {
		return controller == 0 ? controllerRotation[0]: controllerRotation[1];
	}
	
	@Override
	public boolean initBodyAim() throws Exception
	{
		return init();
	}

	private void updateSmoothedVelocity()
	{
		mc.mcProfiler.startSection("updateSmoothedVelocity");
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
		mc.mcProfiler.endSection();
	}

	@Override
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
	@Override
	public Vec3 getAimSource( int controller ) {
		return Vec3.createVectorHelper(aimSource[controller].xCoord, aimSource[controller].yCoord, aimSource[controller].zCoord);
	}
	@Override
	public void triggerHapticPulse(int controller, int strength) {
		if (controllerDeviceIndex[controller]==-1)
			return;
		vrsystem.TriggerHapticPulse.apply(controllerDeviceIndex[controller], 0, (short)strength);
	}

	private Vector3f headDirection;
	
	private void updateAim() {
		if (this.mc==null)
			return;

		mc.mcProfiler.startSection("updateAim");

		// grab controller position in tracker space, scaled to minecraft units
		Vector3f controllerPos = OpenVRUtil.convertMatrix4ftoTranslationVector(controllerPose[0]);
		aimSource[0].rotateAroundY((float)Math.toRadians(-bodyYaw));
		aimSource[0].xCoord = -controllerPos.x;
		aimSource[0].yCoord = controllerPos.y;
		aimSource[0].zCoord = -controllerPos.z;

		// translate controller position by player position, giving a final world coordinate
		Entity player = this.mc.renderViewEntity;
		if (player!=null)
		{
			//Vec3 playerPos = player.getPositionVector();
			Minecraft mc = Minecraft.getMinecraft();
			Vec3 teleportSlide = mc.vrPlayer.getTeleportSlide();
			Vec3 playerPos = mc.vrPlayer.getRoomOrigin().addVector(teleportSlide.xCoord, teleportSlide.yCoord, teleportSlide.zCoord);
			aimSource[0].xCoord += playerPos.xCoord;
			aimSource[0].yCoord += playerPos.yCoord;
			aimSource[0].zCoord += playerPos.zCoord;
		}

		// build matrix describing controller rotation
		Vector3f forward = new Vector3f(0,0,1);
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

		// Calculate aim angles from controller orientation
		// Minecraft entities don't have a roll, so just base it on a direction
		Vector3f controllerDirection = controllerRotation[0].transform(forward);
		aimPitch = (float)Math.toDegrees(Math.asin(controllerDirection.y/controllerDirection.length()));
		aimYaw = -(float)Math.toDegrees(Math.atan2(controllerDirection.x, controllerDirection.z));

		Vector3f lcontrollerDirection = controllerRotation[1].transform(forward);
		laimPitch = (float)Math.toDegrees(Math.asin(lcontrollerDirection.y/lcontrollerDirection.length()));
		laimYaw = -(float)Math.toDegrees(Math.atan2(lcontrollerDirection.x, lcontrollerDirection.z));

		headDirection = hmdPose.transform(forward);
		
		// update off hand aim
		Vector3f leftControllerPos = OpenVRUtil.convertMatrix4ftoTranslationVector(controllerPose[1]);
		aimSource[1].rotateAroundY((float)Math.toRadians(-bodyYaw));
		aimSource[1].xCoord = -leftControllerPos.x;
		aimSource[1].yCoord = leftControllerPos.y;
		aimSource[1].zCoord = -leftControllerPos.z;

		// translate controller position by player position, giving a final world coordinate
		if (player!=null)
		{
			Minecraft mc = Minecraft.getMinecraft();
			Vec3 teleportSlide = mc.vrPlayer.getTeleportSlide();
			Vec3 playerPos = mc.vrPlayer.getRoomOrigin().addVector(teleportSlide.xCoord, teleportSlide.yCoord, teleportSlide.zCoord);
			aimSource[1].xCoord += playerPos.xCoord;
			aimSource[1].yCoord += playerPos.yCoord;
			aimSource[1].zCoord += playerPos.zCoord;
		}

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

		mc.mcProfiler.endSection();
	}


	public boolean applyGUIModelView(EyeType eyeType)
	{
   		mc.mcProfiler.startSection("applyGUIModelView");

		float scale = guiScale; 

		// main menu view
		if (this.mc.theWorld==null || mc.currentScreen instanceof GuiWinGame) {
			guiPos.x = 0;
			guiPos.y = 1.3f;
			guiPos.z = -1.3f;
			guiRotationPose.M[0][0] = guiRotationPose.M[1][1] = guiRotationPose.M[2][2] = guiRotationPose.M[3][3] = 1.0F;
			guiRotationPose.M[0][1] = guiRotationPose.M[1][0] = guiRotationPose.M[2][3] = guiRotationPose.M[3][1] = 0.0F;
			guiRotationPose.M[0][2] = guiRotationPose.M[1][2] = guiRotationPose.M[2][0] = guiRotationPose.M[3][2] = 0.0F;
			guiRotationPose.M[0][3] = guiRotationPose.M[1][3] = guiRotationPose.M[2][1] = guiRotationPose.M[3][0] = 0.0F;
		}

		// i am dead view
		if (this.mc.thePlayer!=null && !this.mc.thePlayer.isEntityAlive())
		{
			Vector3f headPos = OpenVRUtil.convertMatrix4ftoTranslationVector(hmdPose);

			Vector3f forward = new Vector3f(0,0,1);
			Vector3f headDirection = hmdPose.transform(forward);
			headDirection = headDirection.subtract(headPos);

			OpenVRUtil.Matrix4fCopy(hmdPose, guiRotationPose);
			guiRotationPose.M[0][3] = 0.0F;
			guiRotationPose.M[1][3] = 0.0F;
			guiRotationPose.M[2][3] = 0.0F;
			guiRotationPose.M[3][0] = 0.0f;
			guiRotationPose.M[3][1] = 0.0f;
			guiRotationPose.M[3][2] = 0.0f;
			guiRotationPose.M[3][3] = 1.0f;

			guiPos = headPos.subtract(headDirection);
		}

		// HUD view - attach to head or controller
		else if (this.mc.theWorld!=null && (this.mc.currentScreen==null || mc.vrSettings.floatInventory == false))
		{
			if (mc.vrSettings.hudLockToHead)
			{
  				guiPos = OpenVRUtil.convertMatrix4ftoTranslationVector(hmdPose);

				Quatf orientationQuat = OpenVRUtil.convertMatrix4ftoRotationQuat(hmdPose);
				guiRotationPose = new Matrix4f(orientationQuat);

				//float pitchOffset = (float) Math.toRadians( -mc.vrSettings.hudPitchOffset );
				//float yawOffset = (float) Math.toRadians( -mc.vrSettings.hudYawOffset );
				//guiRotationPose = Matrix4f.multiply(guiRotationPose, OpenVRUtil.rotationXMatrix(yawOffset));
				//guiRotationPose = Matrix4f.multiply(guiRotationPose, Matrix4f.rotationY(pitchOffset));
				guiRotationPose.M[3][3] = 1.0f;

				Vector3f forward = new Vector3f(-mc.vrSettings.hudYawOffset / 45.0f, -mc.vrSettings.hudPitchOffset / 45.0f, mc.vrSettings.hudDistance);
				Vector3f hmdForward = hmdPose.transform(forward).subtract(guiPos);
				guiPos = guiPos.subtract(hmdForward);
			}
			else
			{
				guiPos = OpenVRUtil.convertMatrix4ftoTranslationVector(controllerPose[1]);
				Quatf controllerOrientationQuat = OpenVRUtil.convertMatrix4ftoRotationQuat(controllerPose[1]);
				guiRotationPose = new Matrix4f(controllerOrientationQuat);
				guiRotationPose = Matrix4f.multiply(guiRotationPose, OpenVRUtil.rotationXMatrix((float) Math.PI * -0.2F));
				guiRotationPose = Matrix4f.multiply(guiRotationPose, Matrix4f.rotationY((float) Math.PI * 0.1F));
				guiRotationPose.M[3][3] = 1.7f;

				Vector3f forward = new Vector3f(0, -0.5f, 0.5f);
				Vector3f controllerForward = controllerPose[1].transform(forward).subtract(guiPos);
				guiPos = guiPos.subtract(controllerForward.divide(1.0f / 0.5f));
			}
		} 

		// otherwise, looking at inventory screen. use pose calculated when screen was opened
		//where is this set up... should be here....

		if(this.mc.currentScreen instanceof GuiChat){


		}


		// Show inventory at the guiRotationPose
		FloatBuffer guiRotationBuf = guiRotationPose.transposed().toFloatBuffer();


		// counter head rotation
		Quaternion q = getOrientationQuaternion(eyeType);
		org.lwjgl.util.vector.Matrix4f head = QuaternionHelper.quatToMatrix4f(q);
		FloatBuffer buf = BufferUtil.createFloatBuffer(16);
		head.storeTranspose(buf);
		buf.flip();
		GL11.glMultMatrix(buf);

		// offset from eye to gui pos
		Vec3 eye = getEyePosition(eyeType);
		GL11.glTranslatef((float) (guiPos.x - eye.xCoord), (float) (guiPos.y + eye.yCoord), (float) (guiPos.z - eye.zCoord));

		GL11.glPushMatrix();
			GL11.glMultMatrix(guiRotationBuf);
	
			Quaternion tiltBack = new Quaternion();
			float tiltAngle = 0.0f; //15.0f;
			tiltBack.setFromAxisAngle(new Vector4f(1.0f, 0.0f, 0.0f,  tiltAngle * PIOVER180));
			org.lwjgl.util.vector.Matrix4f tiltBackMatrix = QuaternionHelper.quatToMatrix4f(tiltBack);
			FloatBuffer tiltBackBuf = BufferUtil.createFloatBuffer(16);
			tiltBackMatrix.storeTranspose(tiltBackBuf);
			tiltBackBuf.flip();
			GL11.glMultMatrix(tiltBackBuf);
	
			double timeOpen = getCurrentTimeSecs() - startedOpeningInventory;
	
			if (this.mc.theWorld == null || this.mc.currentScreen instanceof GuiWinGame || (this.mc.currentScreen!=null && this.mc.currentScreen instanceof GuiContainer
					&& !(this.mc.currentScreen instanceof GuiInventory || this.mc.currentScreen instanceof GuiContainerCreative)))
			{
				guiScale = 2.0f; 		
			}else guiScale = 1.0f;//mc.vrSettings.hudScale;
	
			//		if (timeOpen < 1.5) {
			//			scale = (float)(Math.sin(Math.PI*0.5*timeOpen/1.5));
			//		}
	
			mc.mcProfiler.endSection();
	
			return true;
	} //note returns with matrix pushed

	//-------------------------------------------------------
	// EventNotifier/IEventListener

	@Override
	public synchronized void registerListener(IEventListener listener)
	{
		listeners.add(listener);
	}

	@Override
	public synchronized void notifyListeners(int eventId)
	{
		for (IEventListener listener : listeners)
		{
			if (listener != null)
				listener.eventNotification(eventId);
		}
	}

	@Override
	public void eventNotification(int eventId)
	{

	}

	@Override
	public boolean providesMirrorTexture() { return false; }

	@Override
	public int createMirrorTexture(int width, int height) { return -1; }

	@Override
	public void deleteMirrorTexture() { }

	@Override
	public boolean providesRenderTextures() { return true; }

	@Override
	public RenderTextureSet createRenderTexture(int lwidth, int lheight)
	{	
		// generate left eye texture
		LeftEyeTextureId = GL11.glGenTextures();
		int boundTextureId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, LeftEyeTextureId);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, lwidth, lheight, 0, GL11.GL_RGBA, GL11.GL_INT, (java.nio.ByteBuffer) null);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundTextureId);
	
		texType.handle = LeftEyeTextureId;
		texType.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
		texType.eType = JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL;
		texType.write();
				
		RenderTextureSet textureSet = new RenderTextureSet();
		textureSet.leftEyeTextureIds.add(LeftEyeTextureId);
		return textureSet;
	}

	@Override
	public void deleteRenderTextures() {
		
	if (LeftEyeTextureId > 0)	GL11.glDeleteTextures(LeftEyeTextureId);
	
	}

	@Override
	public String getLastError() { return ""; }

	@Override
	public boolean setCurrentRenderTextureInfo(int index, int textureIdx, int depthId, int depthWidth, int depthHeight)
	{
		return true;
	}

	@Override
	public void triggerYawTransition(boolean isPositive) { }

	@Override
	public void saveOptions() {

	}

	@Override
	public void loadDefaults() {

	}

	@Override
	public void configureRenderer(GLConfig cfg) {

	}

	@Override
	public boolean endFrame() {
		if(vrCompositor !=null) vrCompositor.PostPresentHandoff.apply();
		return true;
	}

	@Override
	public float getOffhandAimYaw() {
		return laimYaw;
	}

	@Override
	public float getOffhandAimPitch() {
		return laimPitch;
	}

	@Override
	public boolean isHMDTracking() {
		return headIsTracking;
	}

	@Override
	public Vec3 getHeadVector() {
		return Vec3.createVectorHelper(headDirection.x, headDirection.y, headDirection.z);
	}
}
