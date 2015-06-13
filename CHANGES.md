Minecrift 1.7.10 R1
===================

New
------

- Added support for the Oculus 0.5.0.1 SDK (tested on Win 8.1, OSX 10.10, Ubuntu 14.10). 
- [With thanks to Zach Jaggi for work towards the linux port, and Jherico for his Oculus SDK repo]
- Now use Oculus best practice for sensor polling to reduce judder in some scenarios
- Added initial *experimental* support for Forge 1.7.10 10.13.4.1448. This is a WIP.
- Ported to Optifine 1.7.10 HD U B7
- Added settings profiles in-game. Different settings configurations may be created, duplicated or deleted. You can switch between profiles in-game via the VR options GUI (VR Options, profile button)
- Added support for optionally adjusting player movement inertia. 
- Added support for optionally allowing player pitch input to affect up/down direction while flying.
- Streamlined the installer. 
- Can (optionally) add / update Minecrift launcher profiles. 
- Downloads Windows redists automatically if necessary.
- Added support for FOV changes in mono mode


Bug fixes
-------------

- Fixed crosshair pitch issues with arrows [with thanks to Zach Jaggi for the fix]
- FSAA now working correctly in mono mode
- Positional tracking now *generally* works in mono mode, some issues remain
- JMumbleLib rebuilt for Linux in an attempt to avoid librt issues on startup


Known Issues
--------------------

- Hydra *still* not working, disabled in installer for now
- Some rendering issues - some fancy water / lightmap effects seem to not take account of player head orientation / position
- The Forge build will most likely not play nicely with other Forge coremods
- Controller button map is not yet stored in the settings profile
- No default button map for a controller on first install


Roadmap
-------------

General
- Fix known issues
- Add support for Oculus SDK 0.6 on Windows (will continue to support 0.5 on OSX and Linux)
- Add initial support for SteamVR / Vive (if a Vive dev kit is forthcoming!)
- Add support for selected Forge coremods - kayronix shaders mod, FTB?
- Add rudimentary IK to player avatar animation so that body follows head position (within reason!)
- Add debug console as secondary UI screen element
- Investigate OSVR support
- Investigate room-space movement
- Investigate pos-track of arm/hand position with tracked controllers
- Fix crosshair weirdness at extreme angles (due to current Euler implementation)

1.8
- Port 1.7.10 features / fixes to Minecrift 1.8.1
- Port to 1.8.7 when MCP release allows
- Port to Forge 1.8.x