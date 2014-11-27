package com.mtbs3d.minecrift.settings;

public enum VROption {
    //General
    USE_VR( new BooleanValue("useVRRenderer","VR Mode",false)),
    HUD_SCALE( new FloatValue("hudScale", "HUD Scale", 1.0f, "%0.2f", 0.5f, 3.0f, 0.01f )),
    HUD_DISTANCE( new FloatValue("hudDistance","HUD Distance", 1.0f, "%0.2f", 0.5f, 3.0f, 0.01f )),
    HUD_OPACITY( new BooleanValue("hudOpacity","HUD Opacity", false)),
    HUD_OCCLUSION( new BooleanValue("hudOpacity","HUD Occlusion", false)),
    HEAD_TRACKING( new BooleanValue("useHeadTracking","Head Trackisng", false)),
    DUMMY( null ),
    VR_HEAD_ORIENTATION( new PluginValue("headTrackerPluginID","Head Orientation","oculus","headTracker")),
    VR_HEAD_POSITION( new PluginValue("headPositionPluginID","Head Position","oculus","positionTracker")),
    HMD_CHOIES( new PluginValue("hmdPluginID","HMD Plugin","oculus","hmdInfo")),
    STEREO_PROVIDER_CHOICES( new PluginValue("stereoProviderPluginID","Stereo Provider","oculus","stereoProvider")),
    VR_CONTROLLER( new PluginValue("controllerPluginID","Controller","mouse","lookaimController")),
    CROSSHAIR_SCALE( new FloatValue("crosshairScale", "Crosshair Size", 1.0f, "%0.2f", 0.25f, 2.5f, 0.01f )),
    CROSSHAIR_ALWAYS_SHOW( new BooleanValue("alwaysRenderInGameCrosshair", "Show Crosshair", false, "Always","With HUD")),
    CROSSHAIR_ROLL(new BooleanValue("crosshairRollsWithHead", "Roll Crosshair", false, "With Head","With HUD")),
    BLOCK_OUTLINE_ALWAYS_SHOW(new BooleanValue("alwaysRenderBlockOutline","Show Block Outline", false, "Always","With HUD")),
    // Player

    EYE_HEIGHT( new FloatValue( "playerEyeHeight","Eye Height",1.74f,"%.2fm",1.62f,1.85f,0.01f)),
    EYE_PROTRUSION( new FloatValue( "eyeProtrusion","Eye Protrusion",0.185f,"%.3fm",0f,0.25f,0.001f)),
    NECK_LENGTH( new FloatValue("neckBaseToEyeHeight","Neck Length",0.225f,"%.3fm",0.0f,0.25f,0.001f)),

    /*
    RENDER_OWN_HEADWEAR("Render Own Headwear", false, true),
    RENDER_FULL_FIRST_PERSON_MODEL("First Person Model", false, true),
    RENDER_PLAYER_OFFSET("View Body Offset", true, false),
    IPD("IPD", true, false),
    OCULUS_PROFILE("Use Oculus Profile", false, false),
    OCULUS_PROFILE_NAME("Name", false, true),
    OCULUS_PROFILE_GENDER("Gender", false, true),

    //HMD/render
    USE_DISTORTION("Distortion", false, true),
    CHROM_AB_CORRECTION("Chrom. Ab. Correction", false, true),
    FOV_CHANGE("FOV Scale", true, false),
    DISTORTION_FIT_POINT("Distortion Border", true, false),
    FSAA("FSAA", false, true),
    FSAA_SCALEFACTOR("FSAA Render Scale", true, false),

    //Head orientation tracking
    HEAD_TRACK_PREDICTION("Head Track Prediction", false, true),
    HEAD_TRACK_SENSITIVITY("Head Track Sensitivity", true, false),
    HEAD_TRACK_PREDICTION_TIME("Prediction time", true, false),

    //eye center position tracking
    POS_TRACK_HYDRALOC("Position", false, false),
    POS_TRACK_HYDRA_OFFSET_X("Hydra X Offset", true, false),
    POS_TRACK_HYDRA_OFFSET_Y("Hydra Y Offset", true, false),
    POS_TRACK_HYDRA_OFFSET_Z("Hydra Z Offset", true, false),
    POS_TRACK_OFFSET_SET_DEFAULT("Default Offsets", false, true),
    POS_TRACK_WORLD_SCALE("Dist. Scale", true, false),
    POS_TRACK_HYDRA_USE_CONTROLLER_ONE("Controller", false, true),
    POS_TRACK_HYDRA_AT_BACKOFHEAD_IS_POINTING_LEFT("Hydra Direction", false, true),
    HYDRA_USE_FILTER("Filter", false, true),
    POS_TRACK_Y_AXIS_DISTANCE_SKEW("Distance Skew Angle", true, false),

    //Movement/aiming controls
    DECOUPLE_LOOK_MOVE("Decouple Look/Move", false, true),
    MOVEMENT_MULTIPLIER("Move. Speed Multiplier", true, false),
    PITCH_AFFECTS_CAMERA("Pitch Affects Camera", false, true),
    JOYSTICK_SENSITIVITY("Joystick Sensitivity",true,false),
    DECOUPLE_LOOK_AIM_PITCH("Free Aim - Vertical",false,true),
    KEYHOLE_WIDTH("Keyhole Width",true,false),
    KEYHOLE_HEAD_RELATIVE("Keyhole Moves With Head",false,true),
    MOVEAIM_HYDRA_USE_CONTROLLER_ONE("Controller", false, true),

    // Calibration
    CALIBRATION_STRATEGY("Initial Calibration", false, false);
    */
    ;

	public OptionValue value;
	public String serName;
	
	static void loadDefaults(){
		for( VROption opt : VROption.values() )
			opt.value.setDefaultValue();
	}

	VROption( OptionValue val )
	{
		value = val;
		this.serName = val.fieldName;
	}
	VROption( String serializeName, OptionValue val )
	{
		value = val;
		this.serName = serializeName;
	}

}
