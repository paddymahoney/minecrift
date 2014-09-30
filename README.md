Minecrift Mod for Minecraft
===========================

Current Version: Minecrift 1.7.10, MCP 908 

---

Building
========

The installation process has been tested on Windows 7, 8, 8.1, OSX 10.8, and Ubuntu
Linux, but was written with cross-platform support in mind, so should be usable
"everywhere". It requires the MCP (Minecraft Coders Pack).

Install build prerequisites
---------------------------

- Be sure to pull in the required submodules linked to the minecrift git project:
 JRift, Sixense, repo, JMumble

- Java JDK 1.6 or 1.7 (Java JRE, or 1.8 JDK or 1.8 JRE will NOT work)
- JAVA_HOME should be set to the JDK directory
- %JAVA_HOME%/bin must be added to your path
- Python 2.7 latest version
- Scala is NOT required (and should NOT be present on your path to avoid build issues)
- MCP908 from http://mcp.ocean-labs.de/news.php?extend.14.1
- Copy the contents of the unzipped mcp908 directory into the minecrift/mcp908 directory.

Building / updating the patches / pushing changes
-------------------------------------------------

- Run install.sh (or install.bat) to download minecraft, Optifine, and other
libraries; deobfuscate the base system, and apply the patches and new files.

NOTE: The initial build will fail; applychanges is then called and a few patches will
fail. This is normal for now. 

- Use the MCP environment in /mcpxxx to modify, test, and recompile.  If you use
the built-in eclipse workspace, you'll need to add the JRift, SixenseJava and
JMumble jars, located in the jars/libraries/ directory. JRift is in de/fruitfly/ovr
and SixenseJava is in com/sixense. 

To run Minecrift in your dev env you will need to:

  - Delete META-INF dir within minecrift/mcp908/jars/versions/1.7.10/1.7.10.jar otherwise
  you get a SecurityException on Minecrift startup
  - Merge in the assets directory from .minecraft into the minecrift/assets dir.
  - To allow testing on Multiplayer servers, launch via Start.main, and add your Mojang account
    program arguments:
       --username <username> --password <password>

- Run build.sh (or build.bat) to create a release installer. Run the release installer to
  test the reobfuscated changes in the Minecraft launcher.

- Run getchanges.sh (or getchanges.bat) to diff the modified /mcpxxx/src files into
version controlled /patches and copy the new classes into the /src/
directory. Create a pull request for your changes.


License
-------

See [The License File](LICENSE.md) for more information.

StellaArtois, mabrowning 2013, 2014

With thanks to:

- Palmer Luckey and his team for creating the Oculus Rift. The future is
  finally here (well for some people anyway; mine hasn't arrived yet).
- Markus "Notch" Persson for creating Minecraft. What has it grown into?
- The team behind the MCP coders' pack, and the Minecraft community - why
  Mojang bother obfuscating the source when you guys have done such a fantastic
  job of de-obfuscating it is beyond me!
- Powback for his initial work on the Java JNI wrapper to the Oculus SDK. Seeing 
  this inspired me to get off my arse and get modding. See
  [this Reddit thread](http://www.reddit.com/r/oculus/comments/1c1vh0/java_wrapper_for_devs/)
- shakesoda and Ben (and others?) at MTBS for creating the GLSL version of the
  Oculus distortion shader.
- The guys at Valve for giving some good advice on updating a game for VR.
- @PyramidHead76 for building the MacOS libs, and toiling to produce the
  installation guide!!
- Brad Larson and his GPUImage library, for the Lanczos GLSL shader
  implementation for the FSAA.
- All the feedback and support of the folks in the MTBS3D forums!

What is Minecrift?
------------------

The cheesy name apart, Minecrift attempts to update Minecraft to support the
Oculus Rift. Initially this means allowing head-tracking input and using the
correct stereo rendering parameters for the Rift. We also are in the progress
of supporting different control schemes and positional head tracking. Minecraft
for various control schemes. If and when Minecraft officially supports the
Rift, Minecrift development might cease, but probably not.


---
Where to get it?
----------------

Check [our website](http://minecraft-vr.com) for latest official releases!
We also make regular updates to the MTBS3D forum thread when a release is 
ready for general use. 

However, if you can't wait that long, we do have a continuous integration
service generously provided by 
[CloudBees](http://www.cloudbees.com). 

Click the button below to go to our Jenkins page where you can download the
latest build hot-off-the-presses.

[![Powered By CloudBees](http://www.cloudbees.com/sites/default/files/Button-Powered-by-CB.png)
](https://minecraftvr.ci.cloudbees.com/job/minecrift-1.6.4/lastSuccessfulBuild/artifact/)

---

Installation
------------

Download and run the latest minecrift-1.6.4-b###-installer.exe/jar.
*WINDOWS USERS: IMPORTANT* (but only required once). Install the Microsoft
VS2010 C++ redists (both x86 and x64) from
[here](http://www.microsoft.com/en-us/download/details.aspx?id=5555)

----
Controllers/JoyPads
-------------------
Version 1.2 supports many different controllers. Change VR->Move/Aim Mode: to 
'Controller' and then remap the keys that best fit your controller in 'Remap 
Controls'.

Razer Hydra
-----------
Version 1.0 is the first to include full Razer Hydra support.
 - OrientationTracker: If you don't have an Oculus Rift, you can use the left
   controler for head orientation (direction).
 - PositionTracker: This makes a huge immersion difference. The Hydra can feed
   position data the game engine to allow you to look up, down, around corners,
   squat, and generally move around. Use one or two controllers attached to
   your head and adjust the offsets from your eye center on the VR Options.
 - Controller: Use the right controller to turn the view left/right, move
   forward and backwards, place blocks, mine, select item, jump, sneak, access
   your inventory and navigate menus. The controls are currently hardcoded:
 - In game:
   - Joystick X: Move left/right
   - Joystick Y: Move forward/back
   - 1: Drop item
   - 2: Jump
   - 3: Select next left item (mousewheel up)
   - 4: Select next right item (mousewheel down)
   - JOYSTICK: Sneak
   - Bumper: Place block/use item/interact (right mouse)
   - Trigger: Mine block (left mouse)
   - Start: Access Inventory
   - Keyhole aiming: turning too far left or right will turn the character.

 -  In Menus/Inventories:
   - Joystick: mouse up/down/left/right (don't use the actual mouse at
			   the same time: known issue)
   - Trigger: Left Click
   - Bumper: Right Click
   - JOYSTICK: "Shift"

 - You should be able to take advantage of the new 1.5 inventory management
	 controls with this joystick mapping.

Joystick sensitivity can be set in VR Options.

Controls/Usage
--------

Here are some other hotkeys that allow quick access to changing VR settings.

- All Minecrift settings are present in the Options->VR Settings screen, but
  keyboard shortcuts are also available for convenience
- Make sure to read the tool-tips on each setting in VR Options to get an
  understanding for what it is adjusting. VR is best when you tune the experience
  to your setup.
- Pressing space-bar while in a menu will reset the orientation of the head tracker 
  to make the current direction "forward"

- F1 to bring up the game HUD / overlay if it isn't already up. 
- Ctrl and - / = for IPD adjustment. Hold ALT as well for fine adjustment. The
  IPD setting should be saved between sessions.
- Ctrl O to attempt to reinitialise the Rift (including head tracking).
- Ctrl P while not in a menu to turn distortion on / off. Sometimes useful if
  the offset mouse pointer is a pain in the menus. Ctrl-Alt P to toggle
  chromatic aberration correction.
- Ctrl L toggles head-tracking ON/OFF. Ctrl-Alt L toggles tracking prediction
  ON/OFF. It is OFF by default.
- Ctrl U changes the HUD distance. Ctrl-Alt U changes the HUD scale. Ctrl-Alt Y
  toggles opacity on the HUD.
- Ctrl-M toggles rendering of the player's mask ON/OFF.
- FOV adjustment within Minecraft will have no effect - I use the FOV as
  calculated from within the Oculus SDK.
- Allow user to use mouse pitch input as well as yaw. Use Ctrl-N to toggle.
- Large or Auto GUI size recommended.
- Use Ctrl-B to turn Full Scene Anti-aliasing (FSAA) on/off. Use Ctrl-Alt B to
  cycle the FSAA renderscale. Be warned; this feature is a resource hog!! If
  you cannot get 60fps at your desired FSAA level, cycle it to a lower scale
  factor. Anyone with a nVidia GTX Titan please let me know what average FPS
  you get at scale factor 4.0!
- Ctrl , or . decreases or increases the FOV scale factor. This can be used to
  fine tune FOV if it doesn't look quite right to you.
- Ctrl-Alt , or . decreases or increases various sizes of distortion border.
  This can be used to improve rendering speed, at a potential loss of FOV.
- Ctrl V cycles through head track sensitivity multipliers. Try this at your
  own risk!

Known Issues
------------

There are quite a small bugs that can be seen on the [GitHub issue
tracker](https://github.com/mabrowning/minecrift/issues)

Feedback, bug reporting
-----------------------

Please post feedback, bug reports etc. to the [GitHub issue
tracker](https://github.com/mabrowning/minecrift/issues). Please search before
posting to see if the issue has already been reported

There is also discussion happening at this [forum thread at
MTBS](http://www.mtbs3d.com/phpbb/viewtopic.php?f=140&t=17146)

Roadmap
-------

- Investigate gamepad support.
- Make controls remappable.
- Add more natural VR interfaces.
- Fix bugs.

Release Notes
=============

The change-list can be seen [here](CHANGES.md)

