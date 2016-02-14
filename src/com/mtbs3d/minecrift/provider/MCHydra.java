/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.provider;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import com.mtbs3d.minecrift.api.*;
import com.mtbs3d.minecrift.control.ControlBinding;
import com.mtbs3d.minecrift.control.DiscreteAngle;
import com.mtbs3d.minecrift.settings.VRSettings;
import com.sixense.utils.enums.EnumControllerDesc;
import de.fruitfly.ovr.enums.EyeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import com.sixense.ControllerData;
import com.sixense.EnumButton;
import com.sixense.Sixense;
import com.sixense.utils.ControllerManager;
import com.sixense.utils.enums.EnumSetupStep;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector4f;

/**
 * @author Mark Browning
 *
 */
public class MCHydra extends BasePlugin implements IEyePositionProvider, IOrientationProvider, IBodyAimController, IEventListener {

    ControllerManager cm;
    public static ControllerData[] newData = {new ControllerData(), new ControllerData(), new ControllerData(), new ControllerData()};

    Vec3 headPos; //in meters, in world coordinates
    Vec3 leftEyePos;
    Vec3 rightEyePos;
    Vec3 origin = Vec3.createVectorHelper(0, 0, 0);  //in meters, relative to hydra base station

    public final float[] IDENTITY_QUAT = {0.0f, 0.0f, 0.0f, 1.0f};   // x, y, z, w

    float baseStationYawOffset = 0.0f;

    boolean hydraRunning = false;
	private boolean hydraInitialized = false;
	private String initStatus = "";
	private String calibrationStep = "";

	private static boolean libraryLoaded = false;
	
    private boolean resetOrigin = true;
    private boolean resetOriginRotation = true;

    private DiscreteAngle discreteYaw = new DiscreteAngle();
    private DiscreteAngle discretePitch = new DiscreteAngle();

	//latest data
	
	private float cont1PosX = 0;
	private float cont1PosY = 0;
	private float cont1PosZ = 0;
	private float cont2PosX = 0;
	private float cont2PosY = 0;
	private float cont2PosZ = 0;

	private float cont1Yaw = 0;
	private float cont1Pitch = 0;
	private float cont1Roll = 0;
    private float[] cont1OrientationQuat_xyzw = IDENTITY_QUAT;

	private float cont2Yaw = 0;
	private float cont2Pitch = 0;
	private float cont2Roll = 0;
    private float[] cont2OrientationQuat_xyzw = IDENTITY_QUAT;
	
	//For IOrientationProvider implementation; allows setting origin to set yaw offset as well
	private float yawOffset = 0;


    private final float SCALE = 0.001f; //mm -> m
    private float userScale = SCALE;

    // Previously +X 1.0f, -Y -1.0f, +Z 1.0f
    private final float XDIRECTION = 1.0f;    // +X
    private final float YDIRECTION = 1.0f;    // +Y
    private final float ZDIRECTION = 1.0f;    // +Z
    
    //body/Aim implementation
    private float bodyYaw = 0; //floating; not fixed to base station;
    private float aimYaw = 0; //relative to bodyYaw, so that IRL "forward"
    //(that is, baseStationYawOffset) is always the direction of movement

    //aimPitch is always cont2Pitch

    //Input/controller
	int lastcont1Buttons = 0;
	int lastcont2Buttons = 0;
	
	//For mouse menu emulation
	private int hydraMouseX = 0;
	private int hydraMouseY = 0;
	private boolean leftMouseClicked = false;
	private Field keyDownField; //Whee, reflection
	private Field buttonDownField; //Whee, reflection
	private boolean mouseUseJoystick = true;
	long lastIndex = -1;
    boolean isCalibrating = false;

	public MCHydra()
	{
		super();
        pluginID = "hydra";
        pluginName = "Razer Hydra";

	}
	@Override
	public String getInitializationStatus() {
		return initStatus;
	}

	@Override
	public String getVersion() {
		return "0.29";
	}

	@Override
	public boolean init() throws Exception {
		hydraInitialized = libraryLoaded;

		if( !libraryLoaded )
		{
			try {
				File nativeDir = new File("Dummy");
				libraryLoaded = Sixense.LoadLibrary(nativeDir);
			}
			catch( UnsatisfiedLinkError e )
			{
				initStatus = e.getMessage();
				throw e;
			}
		}

		Sixense.init();
		cm = ControllerManager.getInstance();
		cm.setGameType(com.sixense.utils.enums.EnumGameType.ONE_PLAYER_TWO_CONTROLLER);
		Sixense.setActiveBase(0);
		Sixense.setFilterEnabled(Minecraft.getMinecraft().vrSettings.hydraUseFilter);
		
        try {
			keyDownField = Keyboard.class.getDeclaredField("keyDownBuffer");
			keyDownField.setAccessible(true);
			buttonDownField = Mouse.class.getDeclaredField("buttons");
			buttonDownField.setAccessible(true);
		} catch (SecurityException e) {
		} catch (NoSuchFieldException e) {
		}

		hydraInitialized = true;
		return isInitialized(); //no re-init method
	}

	@Override
	public boolean isInitialized() {
		return hydraInitialized;
	}
	
	@Override
	public void poll(long frameIndex)
    {
        if (!isInitialized())
            return;
        if (frameIndex <= this.lastIndex)
            return;

        this.lastIndex = frameIndex;

        Minecraft mc = Minecraft.getMinecraft();

        // Poll hydras; get orientation, and position information in metres
        Sixense.getAllNewestData(newData);

        // Update the controller manager, allowing us to determine which
        // controller is the 'Left' controller, and which is the 'Right'
        // (if calibrated)
        cm.update(newData);
        int leftIndex = cm.getIndex(EnumControllerDesc.P1L);
        int rightIndex = cm.getIndex(EnumControllerDesc.P1R);

        ControllerData cont1, cont2;
        if (leftIndex != -1 && rightIndex != -1)
        {
            if (mc.vrSettings.posTrackHydraUseController1)
            {
                cont1 = newData[leftIndex];
                cont2 = newData[rightIndex];
            }
            else
            {
                cont1 = newData[rightIndex];
                cont2 = newData[leftIndex];
            }
        }
        else
        {
            // Not yet calibrated
            cont1 = newData[0];
            cont2 = newData[1];
        }

        if (!hydraRunning && isCalibrating)
        {
            EnumSetupStep step = cm.getCurrentStep();
            switch ( step )
            {
                case P1C2_IDLE:
                    setCalibrated();
                    break;
                default:
                    hydraRunning = false;
                    if (step != null)
                    {
                        //System.out.println("HYDRA: " + step.toString());
                        calibrationStep = cm.getStepString() + "\n(or ESC to abort)";
                    }
                    break;
            }
        }

        if (hydraRunning)
        {
            cont1Yaw   = cont1.yaw;
            cont1Pitch = cont1.pitch;
            cont1Roll  = cont1.roll;
            cont1OrientationQuat_xyzw = cont1.rot_quat;

            cont2Yaw   = cont2.yaw;
            cont2Pitch = cont2.pitch;
            cont2Roll  = cont2.roll;
            cont2OrientationQuat_xyzw = cont2.rot_quat;
        }
        else
        {
            cont1Yaw   = 0;
            cont1Pitch = 0;
            cont1Roll  = 0;
            cont1OrientationQuat_xyzw = IDENTITY_QUAT;

            cont2Yaw   = 0;
            cont2Pitch = 0;
            cont2Roll  = 0;
            cont2OrientationQuat_xyzw = IDENTITY_QUAT;
        }

        userScale = SCALE * mc.vrSettings.posTrackDistanceScale;

        cont1PosX = userScale * cont1.pos[0] * XDIRECTION;
        cont1PosY = userScale * cont1.pos[1] * YDIRECTION;
        cont1PosZ = userScale * cont1.pos[2] * ZDIRECTION;

        cont2PosX = userScale * cont2.pos[0] * XDIRECTION;
        cont2PosY = userScale * cont2.pos[1] * YDIRECTION;
        cont2PosZ = userScale * cont2.pos[2] * ZDIRECTION;

        if( mc.lookaimController != this) return;
        if( mc.theWorld != null )
        {
	        EntityClientPlayerMP thePlayer = mc.thePlayer;
	        
	        //buttons?
	        //TODO: move this somewhere sane and make configurable
	        GameSettings settings = mc.gameSettings;
	
	        if((cont2.buttons & EnumButton.START.mask())>0 &&
	        	(lastcont2Buttons & EnumButton.START.mask()) == 0)
	        {
	        	if(mc.currentScreen != null)
		        	thePlayer.closeScreen();
	        	else
		        	settings.keyBindInventory.pressTime=1;
	        }

	        if((cont1.buttons & EnumButton.START.mask())>0 &&
	        	(lastcont1Buttons & EnumButton.START.mask()) == 0)
	        {
	        	if(mc.currentScreen != null)
		        	thePlayer.closeScreen();
	        	else
		        	mc.displayInGameMenu();
	        }
	       
	        //In game!
	        if( mc.currentScreen == null )
	        {
	        	hydraMouseX = 0;
	        	hydraMouseY = 0;

		        //aim/body adjustments
	        	float headYaw = 0;
	        	if( mc.vrSettings.keyholeHeadRelative )
		        	headYaw = mc.headTracker.getHeadYawDegrees(EyeType.ovrEye_Center);
	        	
	        	//Adjust keyhole width on controller pitch; otherwise its a very narrow window at the top and bottom
	        	float keyholeYaw = mc.vrSettings.keyholeWidth /2/ MathHelper.cos(cont2Pitch * PIOVER180);
	        	
	        	float bodyYawT = cont2Yaw - baseStationYawOffset; //

	            if( bodyYawT > headYaw + keyholeYaw  ) 
	            {
	            	bodyYawT = Math.min(10,bodyYawT - headYaw - keyholeYaw) * 0.1f*mc.vrSettings.joystickSensitivity; //TODO: add new sensitivity
	            	//Controller pointing too far right, move body to the right
	                aimYaw = headYaw + keyholeYaw;
	            }
	            else if( bodyYawT < headYaw -keyholeYaw )
	            {
	            	//Controller pointing too far left, move body to the left
	            	bodyYawT = Math.max(-10,bodyYawT -headYaw + keyholeYaw) * 0.1f*mc.vrSettings.joystickSensitivity;
	                aimYaw = headYaw - keyholeYaw;
	            }
	            else
	            {
	            	aimYaw = bodyYawT;
	            	bodyYawT = 0;
	            }
            	bodyYaw += bodyYawT;
                bodyYaw %= 360;
		        
		        if( thePlayer != null )
		        {
			        if( Math.abs(cont2.joystick_y)>0.01)
				        thePlayer.movementInput.baseMoveForward = cont2.joystick_y;
			        else
				        thePlayer.movementInput.baseMoveForward = 0.0f;
			        
			        if( Math.abs(cont2.joystick_x)>0.01)
				        thePlayer.movementInput.baseMoveStrafe = -cont2.joystick_x;
			        else
				        thePlayer.movementInput.baseMoveStrafe = 0.0f;
		        }
		
		        //Do buttons
		        if( cont2.trigger > 0.05 )
		        {
		        	if( !leftMouseClicked )
		        		mc.clickMouse();
		        	settings.keyBindAttack.pressed = true;
		        	leftMouseClicked = true;
		        }
		        else
		        {
		        	leftMouseClicked = false;
		        	settings.keyBindAttack.pressed = false;
		        }
		        	
		        if((cont2.buttons & EnumButton.BUMPER.mask()) >0 && 
		        	 (lastcont2Buttons & EnumButton.BUMPER.mask()) == 0)
		        {
		        	mc.rightClickMouse();
		        }

	        	settings.keyBindUseItem.pressed = (cont2.buttons & EnumButton.BUMPER.mask()) >0 ;
		        
		        if((cont2.buttons & EnumButton.JOYSTICK.mask())>0 &&
		        	 (lastcont2Buttons & EnumButton.JOYSTICK.mask()) == 0)
		        {
		        	settings.keyBindSneak.pressed = true;
		        }
		        if(( lastcont2Buttons   & EnumButton.JOYSTICK.mask())>0 &&
		        	(cont2.buttons & EnumButton.JOYSTICK.mask()) == 0)
		        {
		        	settings.keyBindSneak.pressed = false;
		        }
	
		        if((cont2.buttons & EnumButton.BUTTON_1.mask())>0 &&
		        	(lastcont2Buttons & EnumButton.BUTTON_1.mask()) == 0)
		        {
		        	settings.keyBindDrop.pressTime++;
		        }
	
		        if((cont2.buttons & EnumButton.BUTTON_2.mask())>0 &&
		        	(lastcont2Buttons & EnumButton.BUTTON_2.mask()) == 0)
		        {
		        	settings.keyBindJump.pressed = true;
		        }
		        if((lastcont2Buttons & EnumButton.BUTTON_2.mask())>0 &&
		        	(cont2.buttons & EnumButton.BUTTON_2.mask()) == 0)
		        {
		        	settings.keyBindJump.pressed = false;
		        }
		        
		        if((cont2.buttons & EnumButton.BUTTON_3.mask())>0 &&
		        	(lastcont2Buttons & EnumButton.BUTTON_3.mask()) == 0)
		        {
		        	thePlayer.inventory.changeCurrentItem(1);
		        }
		
		        if((cont2.buttons & EnumButton.BUTTON_4.mask())>0 &&
		        	(lastcont2Buttons & EnumButton.BUTTON_4.mask()) == 0)
		        {
		        	thePlayer.inventory.changeCurrentItem(-1);
		        }
	        }
        }
        
        //GUI controls
        if( mc.currentScreen != null && Display.isActive() )
        {
        	mouseUseJoystick = false;
        	if( mouseUseJoystick )
        	{
	        	hydraMouseX += 2*mc.vrSettings.joystickSensitivity*cont2.joystick_x;
	        	hydraMouseY += 2*mc.vrSettings.joystickSensitivity*cont2.joystick_y;
        	}
        	else
        	{
        		hydraMouseX = (int)(mc.displayWidth * (  0.5*cont2Yaw/75f   )); 
        		hydraMouseY = (int)(mc.displayWidth * ( -0.5*cont2Pitch/60f ));
        	}
            float scaleX = mc.displayFBWidth/(float)mc.displayWidth;
            float scaleY = mc.displayFBHeight/(float)mc.displayHeight;
            mc.currentScreen.mouseOffsetX = (int)(hydraMouseX*scaleX);
            mc.currentScreen.mouseOffsetY = (int)(hydraMouseY*scaleY);
	        int mouseX = Mouse.getX() + hydraMouseX;
	        int	mouseY = Mouse.getY() + hydraMouseY;
            //Mouse.setCursorPosition(mouseX, mouseY);
            
            mouseX = (int)(mouseX*scaleX);
            mouseY = (int)(mouseY*scaleY);

            //hack, hack hack... Joystick button is right shift for use in GUI
            //Set the internal keyboard state that right shift is depressed if this button is
	        if( keyDownField != null )
	        {
	        	try {
	        		byte b = (byte)((cont2.buttons & EnumButton.JOYSTICK.mask())>0?1:0);
					((ByteBuffer)keyDownField.get(null)).put(Keyboard.KEY_RSHIFT,b);
				} catch (IllegalArgumentException e) {
				} catch (IllegalAccessException e) {
				}
	        }

	        if( cont2.trigger > 0.05 )
	        {
	        	//click left mouse button
	        	if(!leftMouseClicked)
	        		mc.currentScreen.mouseDown(mouseX,mouseY,0);
	        	else
	        		//Already down
	        		mc.currentScreen.mouseDrag(mouseX, mouseY);//Signals mouse move
	        	leftMouseClicked = true;
		        if( buttonDownField != null )
		        {
		        	try {
						((ByteBuffer)buttonDownField.get(null)).put(0,(byte) 1);
					} catch (IllegalArgumentException e) {
					} catch (IllegalAccessException e) {
					}
		        }
	        }
	        else if( leftMouseClicked )
	        {
        		mc.currentScreen.mouseUp(mouseX,mouseY,0);
	        	leftMouseClicked = false;
	        }

	        if((cont2.buttons & EnumButton.BUMPER.mask())>0 )
	        {
	        	if ((lastcont2Buttons & EnumButton.BUMPER.mask()) == 0)
	        		mc.currentScreen.mouseDown(mouseX,mouseY,1);
	        	else
	        		mc.currentScreen.mouseDrag(mouseX, mouseY);
		        if( buttonDownField != null )
		        {
		        	try {
						((ByteBuffer)buttonDownField.get(null)).put(1,(byte) 1);
					} catch (IllegalArgumentException e) {
					} catch (IllegalAccessException e) {
					}
		        }
	        }
	        else if( ( lastcont2Buttons & EnumButton.BUMPER.mask())>0 )
	        {
        		mc.currentScreen.mouseUp(mouseX,mouseY,1);
	        }
	        
        }

        lastcont1Buttons = cont1.buttons;
        lastcont2Buttons = cont2.buttons;
	}

	@Override
	public void destroy() {
		Sixense.exit();
        hydraRunning = false;
        hydraInitialized = false;
        initStatus = "Not initialised";
        cm = null;
	}

	@Override
	public void update(float ipd, float yawHeadDegrees, float pitchHeadDegrees, float rollHeadDegrees,
                       float worldYawOffsetDegrees, float worldPitchOffsetDegrees, float worldRollOffsetDegrees)
    {
        if (!hydraInitialized)
        {
        	headPos = Vec3.createVectorHelper(0, 0, 0);
            return;
        }

        VRSettings vrSettings = Minecraft.getMinecraft().vrSettings;
        
        if( resetOriginRotation )
        {
        	//TODO: this might be backwards: with only a razer to test the yawHeadDegrees, its always the same!
        	if( Minecraft.getMinecraft().headTracker != this ) //if the positional tracker is the hydra, they are always aligned
        	{
        		baseStationYawOffset = cont1Yaw - yawHeadDegrees; //assume hydra oriented straight with head orientation
	        	if( vrSettings.posTrackHydraLoc == vrSettings.POS_TRACK_HYDRA_LOC_BACK_OF_HEAD)   // TODO: Also needed for HMD top?
                {
                    //assume hydra oriented at 90degrees to head orientation
	        		if (vrSettings.posTrackHydraBIsPointingLeft)
                    {
                        baseStationYawOffset -= 90;
                    }
                    else
                    {
                        baseStationYawOffset += 90;
                    }
                }
        	}
        	resetOriginRotation = false;
        }
        

        // Using a single controller. Select controller. poll() has already switch left and right depending on configuration 
        //cont1 is for head tracking, if head tracking is enabled (TODO: make head tracking optional?)
        float rawX = cont1PosX;
        float rawY = cont1PosY;
        float rawZ = cont1PosZ;

        if (vrSettings.posTrackHydraLoc == vrSettings.POS_TRACK_HYDRA_LOC_HMD_LEFT_AND_RIGHT)
        {
        	//Otherwise, use average of controllers
            rawX = (cont1PosX + cont2PosX) / 2.0f;
            rawY = (cont1PosY + cont2PosY) / 2.0f;
            rawZ = (cont1PosZ + cont2PosZ) / 2.0f;
        }

        // Correct for distance from base y axis skew
        // TODO: figure out why the heck this is required for Stella's Hydras...
        // TODO: rename this gameSetting to "hydraXRotationSkew" or other sensible name
        rawY += rawZ * MathHelper.sin(vrSettings.posTrackHydraYAxisDistanceSkewAngleDeg*PIOVER180 );
        
        //Raw is the absolute coordinate in hydra reference frame of the sample (possible average of two controllers)
        Vec3 raw = Vec3.createVectorHelper(rawX, rawY, rawZ);
        
        //Rel is the relative coordinate in hydra reference frame
        Vec3 rel = origin.subtract(raw); 
        
        //Account for hydra base station / head tracker orientation not aligned
        //After this, rel is in body coordinates (relative to head tracker reference frame, not accounting for mouse-induced world yaw offset)
        rel.rotateAroundY(baseStationYawOffset*PIOVER180);

        //Now, compute the offset from the hydra controller to the camera location. Straight from the settings (although negated -
        //vrSettings stores eye center -> hydra values for user readability. We need hydra -> eye center values here)
        float hydraXOffset = -vrSettings.getPosTrackHydraOffsetX();
        float hydraYOffset = -vrSettings.getPosTrackHydraOffsetY();
        float hydraZOffset = vrSettings.getPosTrackHydraOffsetZ();

        // The configured offset is for a 0,0,0 rotation head. Apply current head orientation to get final offset
        Vec3 correctionToCentreEyePosition = Vec3.createVectorHelper(hydraXOffset, hydraYOffset, hydraZOffset);

        correctionToCentreEyePosition.rotateAroundZ(rollHeadDegrees*PIOVER180);
        correctionToCentreEyePosition.rotateAroundX(pitchHeadDegrees*PIOVER180);
        correctionToCentreEyePosition.rotateAroundY(-yawHeadDegrees*PIOVER180);

        //Add the hydra position (in head tracker reference frame) to the camera offset 
        //to get the camera position in head tracker reference frame
        headPos = vecAdd(rel,correctionToCentreEyePosition);

        if (resetOrigin)
        {
        	//We compute the "ideal" neck model position, in head tracker reference frame
        	Vec3 neckModelToCentreEyePosition = Vec3.createVectorHelper(0, vrSettings.neckBaseToEyeHeight, -vrSettings.eyeProtrusion);
	        neckModelToCentreEyePosition.rotateAroundZ(rollHeadDegrees*PIOVER180);
	        neckModelToCentreEyePosition.rotateAroundX(pitchHeadDegrees*PIOVER180);
	        neckModelToCentreEyePosition.rotateAroundY(-yawHeadDegrees*PIOVER180);

	        //The actual hydra position on the head is offset from the eye center by this amount
	        Vec3 originOffset = correctionToCentreEyePosition.subtract(neckModelToCentreEyePosition);
        
	        //Counteract the base station yaw to get back to absolute razer coordinates
	        originOffset.rotateAroundY(-baseStationYawOffset*PIOVER180);

            // save raw - originOffset as origin. That way, when origin is subtracted in the future, 
	        // positions very close to the current location+orientation will have the eye in the correct spot
	        origin = originOffset.subtract(raw);
            
            resetOrigin = false;
        }

        // Rotate the centre eye position around any world yaw offset (mouse/controller induced rotation)
        headPos.rotateAroundY(-worldYawOffsetDegrees*PIOVER180);

        leftEyePos = Vec3.createVectorHelper(headPos.xCoord, headPos.yCoord, headPos.zCoord);
        leftEyePos.xCoord -= (ipd / 2f);
        rightEyePos = Vec3.createVectorHelper(headPos.xCoord, headPos.yCoord, headPos.zCoord);
        rightEyePos.xCoord += (ipd / 2f);

        //System.out.println(String.format("Positional Track: (l/r)x=%.3fcm, (up/down)y=%.3fcm, (in/out)z=%.3fcm", new Object[] {Float.valueOf((float)headPos.xCoord * 100.0f), Float.valueOf((float)headPos.yCoord * 100.0f), Float.valueOf((float)headPos.zCoord * 100.0f)}));
	}

    @Override
	public Vec3 getCenterEyePosition() {
		return headPos;
	}

    @Override
    public Vec3 getEyePosition(EyeType eye)
    {
        if (eye == EyeType.ovrEye_Left)
            return leftEyePos;
        else
            return rightEyePos;
    }

	@Override
	public void resetOrigin() {
		resetOrigin = true;
	}

    @Override
	public void resetOriginRotation() {
		resetOriginRotation = true;

        Minecraft mc = Minecraft.getMinecraft();
        if( resetOriginRotation && mc.headTracker == this )
        {
        	float prevTotalYaw = mc.lookaimController.getBodyYawDegrees() + getHeadYawDegrees(EyeType.ovrEye_Center);
        	yawOffset = cont1Yaw;
        	if( mc.thePlayer == null )
        		//Reset bodyYaw for main menu
        		mc.lookaimController.setBodyYawDegrees(0);
        	else
        		mc.lookaimController.setBodyYawDegrees(prevTotalYaw);
        }

	}

	@Override
	public void setPrediction(float delta, boolean enable) {
        Sixense.setFilterEnabled(enable);
	}

	@Override
	public float getHeadYawDegrees(EyeType eye) {
		return cont1Yaw - yawOffset;
	}

	@Override
	public float getHeadPitchDegrees(EyeType eye) {
		return cont1Pitch;
	}

	@Override
	public float getHeadRollDegrees(EyeType eye) {
		return cont1Roll;
	}

    @Override
    public Quaternion getOrientationQuaternion(EyeType eye)
    {
        // Needs x, y, z, w
        Quaternion orientation   = new Quaternion(cont1OrientationQuat_xyzw[0],
                cont1OrientationQuat_xyzw[1], cont1OrientationQuat_xyzw[2], cont1OrientationQuat_xyzw[3]);

        // Apply yaw offset
        Quaternion yaw           = new Quaternion();
        Vector4f vecAxisYawAngle = new Vector4f(0f, 1f, 0f, -yawOffset * PIOVER180);
        yaw.setFromAxisAngle(vecAxisYawAngle);
        Quaternion.mul(yaw, orientation, orientation);
        return orientation;
    }

    @Override
    public void beginCalibration(PluginType type) {
        isCalibrating = true;
        resetOriginRotation(); //Reset the head tracker/hydra orientation reference frame
    }

    @Override
    public void updateCalibration(PluginType type) {/*no-op*/ }

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
		return cont2Pitch;
	}

	@Override
	public boolean isCalibrated(PluginType type) {
		return hydraRunning;
	}

	@Override
	public String getCalibrationStep(PluginType type) {
		return calibrationStep;
	}
	
	Vec3 vecAdd(Vec3 a, Vec3 b)
	{
		return a.addVector(b.xCoord, b.yCoord, b.zCoord);
	}

    @Override
    public void eventNotification(int eventId)
    {
        if (eventId == IBasePlugin.EVENT_SET_ORIGIN)
        {
            resetOrigin();
            resetOriginRotation();
        }
        if (eventId == IBasePlugin.EVENT_CALIBRATION_ABORT && !hydraRunning)
        {
            setCalibrated();
        }
    }

    private void setCalibrated()
    {
        hydraRunning = true;
        calibrationStep = "";
        isCalibrating = false;
    }

	@Override
	public void mapBinding(ControlBinding binding) {
		// TODO Auto-generated method stub
		
	}

    public void beginFrame() { beginFrame(0); }
    public void beginFrame(long frameIndex) { }
    public boolean endFrame() { return true; }

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

	@Override
	public void saveOptions() {

	}

	@Override
	public void loadDefaults() {

	}

	@Override
	public void triggerYawTransition(boolean isPositive) {
		this.discreteYaw.triggerChange(isPositive);
	}
}
