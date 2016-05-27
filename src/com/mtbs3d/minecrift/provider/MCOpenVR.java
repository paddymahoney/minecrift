package com.mtbs3d.minecrift.provider;

import com.mtbs3d.minecrift.api.*;
import com.mtbs3d.minecrift.control.ControlBinding;
import com.mtbs3d.minecrift.control.GuiScreenNavigator;
import com.mtbs3d.minecrift.render.QuaternionHelper;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import de.fruitfly.ovr.UserProfileData;
import de.fruitfly.ovr.enums.*;
import de.fruitfly.ovr.structs.*;
import de.fruitfly.ovr.structs.Matrix4f;
import de.fruitfly.ovr.structs.Vector2f;
import de.fruitfly.ovr.structs.Vector3f;
import de.fruitfly.ovr.util.BufferUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiHopper;
import net.minecraft.client.gui.GuiRepair;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import optifine.Utils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.*;
import jopenvr.*;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class MCOpenVR implements IEyePositionProvider, IOrientationProvider, IBasePlugin, IHMDInfo, IStereoProvider,
IEventNotifier, IEventListener, IBodyAimController
{
	public static String initStatus;
	public boolean initialized;
	private Minecraft mc;

	private static Pointer vrsystem;
	private static Pointer vrCompositor;
	private static Pointer vrControlPanel;
	private static IntBuffer hmdErrorStore;

	private static TrackedDevicePose_t.ByReference hmdTrackedDevicePoseReference;
	private static TrackedDevicePose_t[] hmdTrackedDevicePoses;
	private static TrackedDevicePose_t.ByReference hmdGamePoseReference;
	private static TrackedDevicePose_t[] hmdGamePoses;

	private static Matrix4f[] poseMatrices;
	private static Vec3[] deviceVelocity;

	// position/orientation of headset and eye offsets
	private static final Matrix4f hmdPose = new Matrix4f();
	private static Matrix4f hmdProjectionLeftEye;
	private static Matrix4f hmdProjectionRightEye;
	private static Matrix4f hmdPoseLeftEye = new Matrix4f();
	private static Matrix4f hmdPoseRightEye = new Matrix4f();

	// TextureIDs of framebuffers for each eye
	private int LeftEyeTextureId;
	private int RightEyeTextureId;

	private final VRTextureBounds_t texBoundsLeft = new VRTextureBounds_t(), texBoundsRight = new VRTextureBounds_t();
	private final Texture_t texTypeLeft = new Texture_t(), texTypeRight = new Texture_t();

	// aiming
	private float bodyYaw = 0;
	private float aimYaw = 0;
	private float aimPitch = 0;
	
	public float laimPitch = 0;
	public float laimYaw = 0;
	
	private Vec3[] aimSource = new Vec3[2];

	// Controllers
	public static int RIGHT_CONTROLLER = 0;
	public static int LEFT_CONTROLLER = 1;
	private static Matrix4f[] controllerPose = new Matrix4f[2];
	private static Matrix4f[] controllerRotation = new Matrix4f[2];
	private static int[] controllerDeviceIndex = new int[2];
	private static VRControllerState_t.ByReference[] controllerStateReference = new VRControllerState_t.ByReference[2];
	private static VRControllerState_t[] lastControllerState = new VRControllerState_t[2];
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
	private static float guiScale = 1.0f;
	private double startedOpeningInventory = 0;

	// For mouse menu emulation
	private float controllerMouseX = -1.0f;
	private float controllerMouseY = -1.0f;
	private Field keyDownField;
	private Field buttonDownField;

	// Touchpad samples
	public Vector2f[][] touchpadSamples = new Vector2f[2][5];
	public int[] touchpadSampleCount = new int[2];

	public float[] inventory_swipe = new float[2];

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

			controllerStateReference[c] = new VRControllerState_t.ByReference();
			controllerStateReference[c].setAutoRead(false);
			controllerStateReference[c].setAutoWrite(false);
			controllerStateReference[c].setAutoSynch(false);

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

	@Override
	public boolean init()  throws Exception
	{
		if ( this.initialized )
		return true;

		mc = Minecraft.getMinecraft();
		hmdErrorStore = IntBuffer.allocate(1);
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
		JOpenVRLibrary.VR_InitInternal(hmdErrorStore, JOpenVRLibrary.EVRApplicationType.EVRApplicationType_VRApplication_Scene);
		if ( hmdErrorStore.get(0) == 0 )
		{
			vrsystem = JOpenVRLibrary.VR_GetGenericInterface( JOpenVRLibrary.IVRSystem_Version, hmdErrorStore );
		}

		if( vrsystem == null || hmdErrorStore.get(0) != 0 )
		{
			Pointer errstr = JOpenVRLibrary.VR_GetStringForHmdError( hmdErrorStore.get(0) );
			initStatus = "OpenVR Initialize Result: " + errstr.getString(0);
			return false;
		}

		System.out.println( "OpenVR initialized & VR connected." );

		hmdTrackedDevicePoseReference = new TrackedDevicePose_t.ByReference();
		hmdTrackedDevicePoses = (TrackedDevicePose_t[])hmdTrackedDevicePoseReference.toArray(JOpenVRLibrary.k_unMaxTrackedDeviceCount);
		hmdGamePoseReference = new TrackedDevicePose_t.ByReference();
		hmdGamePoses = (TrackedDevicePose_t[])hmdTrackedDevicePoseReference.toArray(1);
		poseMatrices = new Matrix4f[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
		deviceVelocity = new Vec3[JOpenVRLibrary.k_unMaxTrackedDeviceCount];

		for(int i=0;i<poseMatrices.length;i++)
		{
			poseMatrices[i] = new Matrix4f();
			deviceVelocity[i] = Vec3.createVectorHelper(0,0,0);
		}

		// disable all this stuff which kills performance
		hmdTrackedDevicePoseReference.setAutoRead(false);
		hmdTrackedDevicePoseReference.setAutoWrite(false);
		hmdTrackedDevicePoseReference.setAutoSynch(false);

		if ( !initOpenVRCompositor() )
		return false;

		if ( !initOpenVRControlPanel() )
		return false;

		try {
			keyDownField = Keyboard.class.getDeclaredField("keyDownBuffer");
			keyDownField.setAccessible(true);
			buttonDownField = Mouse.class.getDeclaredField("buttons");
			buttonDownField.setAccessible(true);
		} catch (SecurityException e) {
		} catch (NoSuchFieldException e) {
		}

		HmdMatrix34_t matL = JOpenVRLibrary.VR_IVRSystem_GetEyeToHeadTransform(vrsystem, JOpenVRLibrary.EVREye.EVREye_Eye_Left);
		OpenVRUtil.convertSteamVRMatrix3ToMatrix4f(matL, hmdPoseLeftEye);

		HmdMatrix34_t matR = JOpenVRLibrary.VR_IVRSystem_GetEyeToHeadTransform(vrsystem, JOpenVRLibrary.EVREye.EVREye_Eye_Right);
		OpenVRUtil.convertSteamVRMatrix3ToMatrix4f(matR, hmdPoseRightEye);

		this.initialized = true;
		return true;
	}

	public boolean initOpenVRCompositor()
	{
		vrCompositor = JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRCompositor_Version, hmdErrorStore);
		if(vrCompositor == null || hmdErrorStore.get(0) != 0)
		{
			initStatus = "OpenVR Compositor error: " + JOpenVRLibrary.VR_GetStringForHmdError(hmdErrorStore.get(0)).getString(0);
			return false;
		}
		// left eye
		texBoundsLeft.uMax = 1f;
		texBoundsLeft.uMin = 0f;
		texBoundsLeft.vMax = 1f;
		texBoundsLeft.vMin = 0f;
		texBoundsLeft.setAutoSynch(false);
		texBoundsLeft.setAutoRead(false);
		texBoundsLeft.setAutoWrite(false);
		texBoundsLeft.write();
		// right eye
		texBoundsRight.uMax = 1f;
		texBoundsRight.uMin = 0f;
		texBoundsRight.vMax = 1f;
		texBoundsRight.vMin = 0f;
		texBoundsRight.setAutoSynch(false);
		texBoundsRight.setAutoRead(false);
		texBoundsRight.setAutoWrite(false);
		texBoundsRight.write();
		// texture type
		texTypeLeft.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
		texTypeLeft.eType = JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL;
		texTypeLeft.setAutoSynch(false);
		texTypeLeft.setAutoRead(false);
		texTypeLeft.setAutoWrite(false);
		texTypeLeft.handle = -1;
		texTypeRight.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
		texTypeRight.eType = JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL;
		texTypeRight.setAutoSynch(false);
		texTypeRight.setAutoRead(false);
		texTypeRight.setAutoWrite(false);
		texTypeRight.handle = -1;

		System.out.println("OpenVR Compositor initialized OK.");
		return true;
	}

	public boolean initOpenVRControlPanel()
	{
		vrControlPanel = JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRCompositor_Version, hmdErrorStore);
		if(vrControlPanel != null && hmdErrorStore.get(0) == 0){
			System.out.println("OpenVR Control Panel initialized OK.");
			return true;
		} else {
			initStatus = "OpenVR Control Panel error: " + JOpenVRLibrary.VR_GetStringForHmdError(hmdErrorStore.get(0)).getString(0);
			return false;
		}
	}

	@Override
	public void poll(long frameIndex)
	{
		mc.mcProfiler.startSection("poll");

		updateControllerButtonState();
		updateTouchpadSampleBuffer();
		updateSmoothedVelocity();

		processControllerButtons();
		processTouchpadSampleBuffer();

		pollInputEvents();

		// GUI controls
		if( mc.currentScreen != null )
		{
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
			{
				float intersectDist = -guiNormal.dot(controllerPos.subtract(guiTopLeft)) / guiNormalDotControllerDirection;
				Vector3f pointOnPlane = controllerPos.add(controllerDirection.divide(1.0f/intersectDist));

				Vector3f relativePoint = pointOnPlane.subtract(guiTopLeft);
				float u = relativePoint.dot(guiRight.divide(1.0f/guiWidth));
				float v = relativePoint.dot(guiUp.divide(1.0f/guiWidth));

				// adjust vertical for aspect ratio
				v = ( (v - 0.5f) * ((float)mc.displayWidth / (float)mc.displayHeight) ) + 0.5f;

				// TODO: Figure out where this magic 0.68f comes from. Probably related to Minecraft window size.
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

			if (controllerMouseX>=0 && controllerMouseX<mc.displayWidth
					&& controllerMouseY>=0 && controllerMouseY<mc.displayHeight)
			{
				// clamp to screen
				int mouseX = Math.min(Math.max((int) controllerMouseX, 0), mc.displayWidth);
				int mouseY = Math.min(Math.max((int) controllerMouseY, 0), mc.displayHeight);

				if (controllerDeviceIndex[RIGHT_CONTROLLER] != -1)
				{
					
					Mouse.setCursorPosition(mouseX, mouseY);
					
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
						if (buttonDownField != null)
						{
							try
							{
								((ByteBuffer) buttonDownField.get(null)).put(0, (byte) 1);
							} catch (IllegalArgumentException e)
							{
							} catch (IllegalAccessException e)
							{
							}
						}
					}
				
				
					if (controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x <= triggerThreshold && 
							lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold 
							)
					{
						//click left mouse button
						mc.currentScreen.mouseUp(mouseX, mouseY, 0);
					}	
				
					// hack for scrollbars
					GuiScreenNavigator.selectDepressed = (controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold);

					
				//end LMB
					
					
					//RMB
					if (
					(controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed.longValue() & k_buttonTouchpad) > 0 &&
					(lastControllerState[RIGHT_CONTROLLER].ulButtonPressed.longValue() & k_buttonTouchpad) == 0 
					)				
					{
						//click left mouse button
						mc.currentScreen.mouseDown(mouseX, mouseY, 1);
					}	
				
					if ((controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed.longValue() & k_buttonTouchpad) > 0 )
					{
						mc.currentScreen.mouseDrag(mouseX, mouseY);//Signals mouse move
					}
				
				
			if(		(controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed.longValue() & k_buttonTouchpad) == 0 &&
					(lastControllerState[RIGHT_CONTROLLER].ulButtonPressed.longValue() & k_buttonTouchpad) > 0 
							)
					{
						//click left mouse button
						mc.currentScreen.mouseUp(mouseX, mouseY, 1);
					}	
					//end RMB



					// clicking off screen?
					if (controllerMouseX < 0 && mc.thePlayer != null)
					{
						boolean pressedPlaceBlock = ((controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed.longValue() & k_buttonTouchpad) > 0)
						&& ((lastControllerState[RIGHT_CONTROLLER].ulButtonPressed.longValue() & k_buttonTouchpad) == 0);
						
						boolean pressedAttack = (controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold)
						&& (lastControllerState[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x <= triggerThreshold);
						
						if (pressedAttack || pressedPlaceBlock)
						{
					 if (getCurrentTimeSecs() - startedOpeningInventory > 0.2f)	{mc.thePlayer.closeScreen();}
						}
					}

				} else
				{
					// no controller connected, do mouse ups
					if (mc.gameSettings.keyBindAttack.getIsKeyPressed())
					{
						mc.currentScreen.mouseUp(mouseX, mouseY, 0);
						mc.gameSettings.keyBindAttack.unpressKey();
					}
					// TODO: rmb?
				}
			}
		}

	
		updatePose();

		mc.mcProfiler.endSection();
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
	public boolean endFrame()
	{
		mc.mcProfiler.startSection("submit");

		//GL11.glFlush();
		GL11.glFinish();

		JOpenVRLibrary.VR_IVRCompositor_Submit(
		vrCompositor,
		JOpenVRLibrary.EVREye.EVREye_Eye_Left,
		texTypeLeft, texBoundsLeft,
		JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);

		JOpenVRLibrary.VR_IVRCompositor_Submit(
		vrCompositor,
		JOpenVRLibrary.EVREye.EVREye_Eye_Right,
		texTypeRight, texBoundsRight,
		JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);

		Display.update();
		//Display.processMessages();

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
			JOpenVRLibrary.VR_IVRSystem_GetRecommendedRenderTargetSize(vrsystem, rtx, rty);

			hmd.Type = HmdType.ovrHmd_Other;
			hmd.ProductName = "OpenVR";
			hmd.Manufacturer = "Unknown";
			hmd.AvailableHmdCaps = 0;
			hmd.DefaultHmdCaps = 0;
			hmd.AvailableTrackingCaps = HmdParameters.ovrTrackingCap_Orientation | HmdParameters.ovrTrackingCap_Position;
			hmd.DefaultTrackingCaps = HmdParameters.ovrTrackingCap_Orientation | HmdParameters.ovrTrackingCap_Position;
			hmd.Resolution = new Sizei( rtx.get(0) * 2, rty.get(0) );

			float topFOV = JOpenVRLibrary.VR_IVRSystem_GetFloatTrackedDeviceProperty(vrsystem, JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewTopDegrees_Float, hmdErrorStore);
			float bottomFOV = JOpenVRLibrary.VR_IVRSystem_GetFloatTrackedDeviceProperty(vrsystem, JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewBottomDegrees_Float, hmdErrorStore);
			float leftFOV = JOpenVRLibrary.VR_IVRSystem_GetFloatTrackedDeviceProperty(vrsystem, JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewLeftDegrees_Float, hmdErrorStore);
			float rightFOV = JOpenVRLibrary.VR_IVRSystem_GetFloatTrackedDeviceProperty(vrsystem, JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewRightDegrees_Float, hmdErrorStore);

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
			userProfile._ipd = JOpenVRLibrary.VR_IVRSystem_GetFloatTrackedDeviceProperty(vrsystem, JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_UserIpdMeters_Float, hmdErrorStore);
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
		for (int nDevice = 0; nDevice < JOpenVRLibrary.k_unMaxTrackedDeviceCount; ++nDevice ) {
			int deviceClass = JOpenVRLibrary.VR_IVRSystem_GetTrackedDeviceClass(vrsystem,nDevice);
			int connected = JOpenVRLibrary.VR_IVRSystem_IsTrackedDeviceConnected(vrsystem,nDevice);
			if ( deviceClass == JOpenVRLibrary.ETrackedDeviceClass.ETrackedDeviceClass_TrackedDeviceClass_Controller
					&& connected != 0
					&& hmdTrackedDevicePoses[nDevice].bPoseIsValid != 0) {
				if (controllerDeviceIndex[RIGHT_CONTROLLER]==-1)
				{
					controllerDeviceIndex[RIGHT_CONTROLLER] = nDevice;
				}
				else if ( controllerDeviceIndex[LEFT_CONTROLLER]==-1)
				{
					controllerDeviceIndex[LEFT_CONTROLLER] = nDevice;
				}
			}
		}
	}

	private void updateControllerButtonState()
	{
		for (int c = 0; c < 2; c++)
		{
			// store previous state
			lastControllerState[c].unPacketNum = controllerStateReference[c].unPacketNum;
			lastControllerState[c].ulButtonPressed = controllerStateReference[c].ulButtonPressed;
			lastControllerState[c].ulButtonTouched = controllerStateReference[c].ulButtonTouched;

			for (int i = 0; i < 5; i++)
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
				JOpenVRLibrary.VR_IVRSystem_GetControllerState(vrsystem, controllerDeviceIndex[c], controllerStateReference[c]);
				controllerStateReference[c].read();
			} else
			{
				// controller not connected, clear state
				lastControllerState[c].ulButtonPressed.setValue(0L);
				lastControllerState[c].ulButtonTouched.setValue(0L);

				for (int i = 0; i < 5; i++)
				{
					if (controllerStateReference[c].rAxis[i] != null)
					{
						lastControllerState[c].rAxis[i].x = 0.0f;
						lastControllerState[c].rAxis[i].y = 0.0f;
					}
				}
			}
		}
	}

	private void processControllerButtons()
	{
		if (mc.theWorld == null)
		return;

		// map functionality to keybinds
		KeyBinding keyBindAttack = mc.gameSettings.keyBindAttack;
		KeyBinding keyBindUseItem = mc.gameSettings.keyBindUseItem;
		KeyBinding keyBindInventory = mc.gameSettings.keyBindInventory;
		KeyBinding keyBindDrop = mc.gameSettings.keyBindDrop;
		KeyBinding keyBindTeleport = mc.gameSettings.keyBindForward;
		KeyBinding keyBindJump = mc.gameSettings.keyBindJump;
		KeyBinding keyPick = mc.gameSettings.keyBindPickBlock;
		KeyBinding keyBindSneak = mc.gameSettings.keyBindSneak;

		boolean gui = (mc.currentScreen != null);
		boolean sleeping = (mc.thePlayer != null && mc.thePlayer.isPlayerSleeping());

		// right controller
		boolean pressedRGrip = (controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed.longValue() & k_buttonGrip) > 0;
	
		if (pressedRGrip) {
			keyPick.pressKey();
			
			if(mc.vrSettings.vrAllowLocoModeSwotch){
				
				moveModeSwitchcount++;
				if (moveModeSwitchcount >= 20 * 4) {
					moveModeSwitchcount = 0;
					mc.vrPlayer.setFreeMoveMode(!mc.vrPlayer.getFreeMoveMode());
					mc.printChatMessage("Free movement mode set to: " + mc.vrPlayer.getFreeMoveMode());
				}	
				
			}			
		} else {moveModeSwitchcount = 0; keyPick.unpressKey();}

	if (!gui) {
		if ( controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger]!=null)
		{
			if (controllerStateReference[RIGHT_CONTROLLER].rAxis[k_EAxis_Trigger].x > triggerThreshold)
			{
				keyBindAttack.pressKey();
			} else
			{
				keyBindAttack.unpressKey();
			}
		}
	}
	
	if(!gui) {
		if( (controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed.longValue() & k_buttonTouchpad) > 0) {
			keyBindUseItem.pressKey();
		} else {
			keyBindUseItem.unpressKey();
		}
		
		if( (controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed.longValue() & k_buttonAppMenu) > 0) {			
			keyBindDrop.pressKey();
		} else {
			keyBindDrop.unpressKey();
		}
	}
		
//		 if ((controllerStateReference[RIGHT_CONTROLLER].ulButtonPressed.longValue() & k_buttonAppMenu) > 0 &&
//		       (lastControllerState[RIGHT_CONTROLLER].ulButtonPressed.longValue() & k_buttonAppMenu)== 0) {
//				keyBindDrop.pressTime++; 	   		   
//			   }
			   
		// left controller
		//bottom of  l trackpad
		if ((controllerStateReference[LEFT_CONTROLLER].ulButtonPressed.longValue() & k_buttonTouchpad) > 0 &&
		(controllerStateReference[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].y <= 0 )) {
			if(mc.vrPlayer.getFreeMoveMode() || mc.vrSettings.simulateFalling) {
			keyBindJump.pressKey();
			}
			
		} else {
			keyBindJump.unpressKey(); 
		}
		
		// if you start teleporting, close any UI
		if (gui && !sleeping && (controllerStateReference[LEFT_CONTROLLER].ulButtonPressed.longValue() & k_buttonTrigger) > 0)
		{
			mc.thePlayer.closeScreen();
		}
		
	if( !sleeping && (controllerStateReference[LEFT_CONTROLLER].ulButtonPressed.longValue() & k_buttonTrigger) > 0) {
		keyBindTeleport.pressKey();
	} else {
		keyBindTeleport.unpressKey();
	}
		
		if ( 
				(controllerStateReference[LEFT_CONTROLLER].ulButtonPressed.longValue() & k_buttonTouchpad) > 0 &&
				(lastControllerState[LEFT_CONTROLLER].ulButtonPressed.longValue() & k_buttonTouchpad) == 0 &&
				(controllerStateReference[LEFT_CONTROLLER].rAxis[k_EAxis_TouchPad].y > 0) && 
				!keyBindTeleport.getIsKeyPressed()
				)
		{
			if (!gui)
			{
				// if no UI screen is up, show inventory when button is pressed
				startedOpeningInventory = getCurrentTimeSecs();
				keyBindInventory.pressKey();
			}
			else if (getCurrentTimeSecs() - startedOpeningInventory > 0.2f)
			{
				// if inventory button is pressed while a screen is up, dismiss it
				if (mc.thePlayer!=null)
				{
					mc.thePlayer.closeScreen();
				}
				//keyBindInventory.pressKey();
			}
		}
		else
		{
			// keyBindInventory.unpressKey(); //let this be consumed always cause the touchpad is funny sometimes
		}
		
		// in restricted mode, update room origin immediately when you release trigger
		if (mc.vrPlayer.getFreeMoveMode() && (controllerStateReference[LEFT_CONTROLLER].ulButtonPressed.longValue() & k_buttonTrigger) == 0 &&
				(lastControllerState[LEFT_CONTROLLER].ulButtonPressed.longValue() & k_buttonTrigger) > 0)
		{
			mc.vrPlayer.lastRoomUpdateTime = 0;
		}
		
		if ((controllerStateReference[LEFT_CONTROLLER].ulButtonPressed.longValue() & k_buttonGrip) > 0) {
			keyBindSneak.pressKey();
		} else {
			keyBindSneak.unpressKey();
		}

		if ( (controllerStateReference[LEFT_CONTROLLER].ulButtonPressed.longValue() & k_buttonAppMenu) > 0 &&
				(lastControllerState[LEFT_CONTROLLER].ulButtonPressed.longValue() & k_buttonAppMenu) == 0)
		{
			if(gui)
		  	mc.thePlayer.closeScreen();
			else
			mc.displayInGameMenu();
		}

		// Tick keybinds
		// NOTE: enabling this for Attack and PlaceBlock causes some actions to happen way too fast...
		//if (keyBindAttack.pressed) KeyBinding.onTick(keyBindAttack.getKeyCode());
		//if (keyBindUseItem.pressed) KeyBinding.onTick(keyBindUseItem.getKeyCode());
		//if (keyBindInventory.pressed) KeyBinding.onTick(keyBindInventory.getKeyCode());
		//if (keyBindDrop.pressed) KeyBinding.onTick(keyBindDrop.getKeyCode());
		//if (keyBindTeleport.pressed) KeyBinding.onTick(keyBindTeleport.getKeyCode());
	}


	private void pollInputEvents()
	{
		// not using these yet
		/*
		JOpenVRLibrary.VREvent_t event = new JOpenVRLibrary.VREvent_t();
		while (VR_IVRSystem_PollNextEvent(vrsystem, event) > 0)
		{
			if (event.trackedDeviceIndex == controllerDeviceIndex[0])
			{

			}
		}
		*/
	}

	private void updateTouchpadSampleBuffer()
	{

		for (int c=0;c<2;c++)
		{
			if (controllerStateReference[c].rAxis[k_EAxis_TouchPad]!=null &&
					(controllerStateReference[c].ulButtonTouched.longValue() & k_buttonTouchpad) > 0)
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

		// left touchpad controls inventory
		for (int c=1;c<2;c++)
		{
			boolean touchpadPressed = (controllerStateReference[c].ulButtonPressed.longValue() & k_buttonTouchpad) > 0;

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
					JOpenVRLibrary.VR_IVRSystem_TriggerHapticPulse(vrsystem, controllerDeviceIndex[c], 0, duration);

					inventory_swipe[c] -= swipeDistancePerInventorySlot;
				} else if (inventory_swipe[c] < -swipeDistancePerInventorySlot)
				{
					mc.thePlayer.inventory.changeCurrentItem(1);

					short duration = 250;
					JOpenVRLibrary.VR_IVRSystem_TriggerHapticPulse(vrsystem, controllerDeviceIndex[c], 0, duration);
					inventory_swipe[c] += swipeDistancePerInventorySlot;
				}
			}
		}
	}

	public void updatePose()
	{
		if ( vrsystem == null || vrCompositor == null )
		return;

		mc.mcProfiler.startSection("updatePose");

		JOpenVRLibrary.VR_IVRCompositor_WaitGetPoses(vrCompositor, hmdTrackedDevicePoseReference, hmdTrackedDevicePoses.length, hmdGamePoseReference, hmdGamePoses.length);

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
		}
		else
		{
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
		JOpenVRLibrary.VR_IVRSystem_GetRecommendedRenderTargetSize(vrsystem, rtx, rty);

		RenderTextureInfo info = new RenderTextureInfo();
		info.HmdNativeResolution.w = rtx.get(0);
		info.HmdNativeResolution.h = rty.get(0);
		info.LeftFovTextureResolution.w = rtx.get(0);
		info.LeftFovTextureResolution.h = rty.get(0);
		info.RightFovTextureResolution.w = rtx.get(0);
		info.RightFovTextureResolution.h = rty.get(0);
		info.CombinedTextureResolution.w = info.LeftFovTextureResolution.w + info.RightFovTextureResolution.w;
		info.CombinedTextureResolution.h = info.LeftFovTextureResolution.h;
		return info;
	}

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
	public boolean isGuiOrtho()
	{
		return false;
	}

	@Override
	public double getFrameTiming() {
		return getCurrentTimeSecs();
	}

	public void onGuiScreenChanged(GuiScreen previousScreen, GuiScreen newScreen)
	{
		if (previousScreen==null && newScreen != null
				|| (newScreen != null && newScreen instanceof GuiContainerCreative)) {

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

			if (appearOverBlock)
			{
				// right controller is used to click a block, so use its orientation
				controllerOrientationQuat = OpenVRUtil.convertMatrix4ftoRotationQuat(controllerPose[0]);
			}
			else
			{
				controllerOrientationQuat = OpenVRUtil.convertMatrix4ftoRotationQuat(controllerPose[1]);
			}

			guiPos = OpenVRUtil.convertMatrix4ftoTranslationVector(controllerPose[1]);
			EulerOrient controllerOrientationEuler = OpenVRUtil.getEulerAnglesDegYXZ(controllerOrientationQuat);
			guiRotationPose = Matrix4f.rotationY((float)-Math.toRadians(controllerOrientationEuler.yaw));
			guiScale = 1.0f;

			Vector3f forward = new Vector3f(0,-0.5f,1).normalised();
			Vector3f controllerForward = controllerPose[1].transform(forward).subtract(guiPos);
			guiPos = guiPos.subtract(controllerForward.divide(1.0f/0.5f));

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
			HmdMatrix44_t mat = JOpenVRLibrary.VR_IVRSystem_GetProjectionMatrix(vrsystem, JOpenVRLibrary.EVREye.EVREye_Eye_Left, nearClip, farClip, JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL);
			hmdProjectionLeftEye = new Matrix4f();
			return OpenVRUtil.convertSteamVRMatrix4ToMatrix4f(mat, hmdProjectionLeftEye);
		}

		HmdMatrix44_t mat = JOpenVRLibrary.VR_IVRSystem_GetProjectionMatrix(vrsystem, JOpenVRLibrary.EVREye.EVREye_Eye_Right, nearClip, farClip, JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL);
		hmdProjectionRightEye = new Matrix4f();
		return OpenVRUtil.convertSteamVRMatrix4ToMatrix4f(mat, hmdProjectionRightEye);
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
	@Override
	public Matrix4f getAimRotation( int controller ) {
		return controller == 0 ? controllerRotation[0] : controllerRotation[1];
	}
	@Override
	public void mapBinding(ControlBinding binding) { }
	@Override
	public double ratchetingYawTransitionPercent()
	{
		return -1d;//this.discreteYaw._percent;
	}
	@Override
	public double ratchetingPitchTransitionPercent()
	{
		return -1d;//this.discretePitch._percent;
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
	public void triggerHapticPulse(int controller, int duration) {
		if (controllerDeviceIndex[controller]==-1)
		return;
		JOpenVRLibrary.VR_IVRSystem_TriggerHapticPulse(vrsystem, controllerDeviceIndex[controller], 0, (short)duration);
	}

	private void updateAim() {
		if (this.mc==null)
		return;

		mc.mcProfiler.startSection("updateAim");

		// grab controller position in tracker space, scaled to minecraft units
		Vector3f controllerPos = OpenVRUtil.convertMatrix4ftoTranslationVector(controllerPose[0]);
		aimSource[0].xCoord = -controllerPos.x;
		aimSource[0].yCoord = controllerPos.y;
		aimSource[0].zCoord = -controllerPos.z;
		aimSource[0].rotateAroundY((float)Math.toRadians(-bodyYaw));

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

		
		// update off hand aim
		Vector3f leftControllerPos = OpenVRUtil.convertMatrix4ftoTranslationVector(controllerPose[1]);
		aimSource[1].xCoord = -leftControllerPos.x;
		aimSource[1].yCoord = leftControllerPos.y;
		aimSource[1].zCoord = -leftControllerPos.z;
		aimSource[1].rotateAroundY((float)Math.toRadians(-bodyYaw));

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

		float scale = 1.0f;

		// main menu view
		if (this.mc.theWorld==null) {
			scale = 2.0f;
			guiPos.x = 0;
			guiPos.y = 1.3f;
			guiPos.z = -1.3f;
			guiRotationPose.M[0][0] = guiRotationPose.M[1][1] = guiRotationPose.M[2][2] = guiRotationPose.M[3][3] = 1.0F;
			guiRotationPose.M[0][1] = guiRotationPose.M[1][0] = guiRotationPose.M[2][3] = guiRotationPose.M[3][1] = 0.0F;
			guiRotationPose.M[0][2] = guiRotationPose.M[1][2] = guiRotationPose.M[2][0] = guiRotationPose.M[3][2] = 0.0F;
			guiRotationPose.M[0][3] = guiRotationPose.M[1][3] = guiRotationPose.M[2][1] = guiRotationPose.M[3][0] = 0.0F;
		}

		// dead view
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
		// HUD view - attach to head/controller
		else if (this.mc.theWorld!=null && this.mc.currentScreen==null)
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

		if (this.mc.currentScreen!=null && this.mc.currentScreen instanceof GuiContainer
				&& !(this.mc.currentScreen instanceof GuiInventory || this.mc.currentScreen instanceof GuiContainerCreative))
		{
			scale = 2.0f;
		}
		if (timeOpen<0.1) {
			scale = (float)(Math.sin(Math.PI*0.5*timeOpen/0.1));
		}
		//GlStateManager.translate(0.0f, 0.3f*scale, -0.2*scale);
		GL11.glScalef(scale, scale, scale);

		mc.mcProfiler.endSection();

		return true;
	}

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
	public RenderTextureSet createRenderTextureSet(int lwidth, int lheight, int rwidth, int rheight)
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
		texTypeLeft.handle = LeftEyeTextureId;
		texTypeLeft.write();

		// generate right eye texture
		RightEyeTextureId = GL11.glGenTextures();
		int boundTextureId2 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, RightEyeTextureId);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, rwidth, rheight, 0, GL11.GL_RGBA, GL11.GL_INT, (java.nio.ByteBuffer) null);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundTextureId2);
		texTypeRight.handle = RightEyeTextureId;
		texTypeRight.write();

		RenderTextureSet textureSet = new RenderTextureSet();
		textureSet.leftEyeTextureIds.add(LeftEyeTextureId);
		textureSet.rightEyeTextureIds.add(RightEyeTextureId);
		return textureSet;
	}

	@Override
	public void deleteRenderTextures() { }

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
}
