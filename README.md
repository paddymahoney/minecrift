Minecrift Mod for Minecraft
===========================

Current Versions
================

The latest maintained code can be found in the branches:

Minecrift 1.7 (with Forge support): 'port_to_1_7_10'
Minecrift 1.8:                      'port_to_1_8_1'

Getting the source
==================

A typical set of commands to retrieve the source correctly (including all required
submodules) is shown below (you'll require a newish version of git installed and setup for
commandline operation) e.g. for branch 1.7.10:

For OSX / Linux:

> git clone -b port_to_1_7_10 https://github.com/mabrowning/minecrift.git ~/minecrift-public-1710
> cd ~/minecrift-public-1710
> git submodule update --init --recursive

For Windows:

>git clone -b port_to_1_7_10 https://github.com/mabrowning/minecrift.git c:\minecrift-public-1710
>cd /D c:\minecrift-public-1710
>git submodule update --init --recursive

Setting up
==========

Install build prerequisites
---------------------------

- Java JDK 1.6, 1.7 or 1.8 (the Java JRE will NOT work, it MUST be the JDK)
- JAVA_HOME should be set to the JDK directory
- ${JAVA_HOME}\bin or %JAVA_HOME%/bin must be added to your path
- Python 2.7.x (NOT 3.x)
- Scala is NOT required (and currently for Windows should NOT be present on your path to avoid
build issues)

Installing
----------

The build process has been tested on Windows 8.1, OSX 10.10, and Ubuntu 14.10. It utilises the
MCP (Minecraft Coders Pack). To install and build a clean checkout of this repo,
you should run the following from the root of the repo directory:

For OSX / Linux:

> ./install.sh
> ./build.sh

For Windows:

> install.bat
> build.bat

NOTE: Build errors will be seen in the console during the install process (the initial MCP rebuild will
fail). This is normal - the code is later patched to compile correctly.

These scripts will generate deobfuscated Minecrift source in mcpxxx/src/minecraft (with the 'unaltered'
source in mcpxxx/src/minecraft_orig). Required libs and natives will be in lib/<mcversion>. A versioned
installer will also be created.

Setting up a build / debug environment
--------------------------------------

This is currently a manual process (if anyone has maven / gradle experience and is willing to help create an
automated project setup process let us know). NOTE: Assumes the project working & current directory
is the root of this repo.

Add the following to your Eclipse / Idea / whatever project:

Non-Forge
+++++++++

Java Source (in order):

- Add ./JRift/JRift/src
- Add ./JMumbleLib/JMumble/src
- Add ./Sixense-Java/SixenseJava/src
- Add ./mcpxxx/src/minecraft

Libraries:

- Add all libraries in ./lib/<minecraft_version>

Run Configuration:

Main class: Start
JVM args:
Linux:
-Djava.library.path=./JRift/JRiftLibrary/natives/linux;./Sixense-Java/SixenseJavaLibrary/natives/linux;./JMumbleLink/JMumbleLibrary/natives/linux;./lib/<minecraft_version>/natives/linux
OSX:
-Djava.library.path=./JRift/JRiftLibrary/natives/osx:./Sixense-Java/SixenseJavaLibrary/natives/osx:./JMumbleLink/JMumbleLibrary/natives/osx:./lib/<minecraft_version>/natives/osx
Windows:
-Djava.library.path=.\JRift\JRiftLibrary\natives\windows;.\Sixense-Java\SixenseJavaLibrary\natives\windows;.\JMumbleLink\JMumbleLibrary\natives\windows;.\lib\<minecraft_version>\natives\windows

Program args (these are optional; but allow you to test on Minecraft multiplayer servers):
--username <minecraft_account_mailaddress_or_username> --password <minecraft_account_password>

TBC: How to setup the minecraft assets for the debugger.

Forge
+++++

This is somewhat more complicated! TBD.

Testing changes, and generating patches
---------------------------------------

- Surround any code changes with /** MINECRIFT **/ and /** END MINECRIFT **/
- Keep changes to the original source to a minimum, add new functions /classes ideally so that minimal changes
occur to the existing source. Do not refactor existing source, this will make future ports to new Minecraft
versions very tricky. Larger changes to mtbs package classes are less problematic however.
- Test your changes in the debugger.
- Run build.sh (or build.bat) to create a release installer. Run the release installer against
a real Minecraft install to test the reobfuscated changes via the Minecraft launcher.
- Run getchanges.sh (or getchanges.bat) to create patches between your modified ./mcpxxx/src/minecraft files
and the original ./mcpxxx/src/minecraft_orig files. The patches are copied to ./patches and new files copied
into the ./src directory. Check-in your changes and create a pull request for them.

License
-------

See [The License File](LICENSE.md) for more information.

StellaArtois, mabrowning 2013, 2014, 2015


*********************************************
Detailed Information
*********************************************

The Build Process
=================

It consists of a number of stages (and associated build scripts):

- Install
This is used to install the Minecrift source from a clean checkout of this repo, to a deobfuscated
source environment.
MCP is extracted, and patched where necessary. Optifine is merged into the Minecraft jar, and then
the MCP decompile / build process is run. This initial build will fail due to Optifine induced
build errors. We patch those errors (the first stage patch), then rebuild and generate the
client MD5s that MCP will use to determine which files are modified. Clean Minecraft + Optifine source
(with build erros corrected) will be present in mcpxxx/src/minecraft_orig.
Finally we apply the actual Minecrift patches (the second stage patch). Minecrift deobfuscated source
will now be present in mcpxxx/src/minecraft.

- Build
This builds the obfuscated Minecrift jar, and builds the versioned installer.
The scripts update the Minecrift version numbers in the source, as read from minecriftversion.py. Then
MCP recompiles the Minecrift source (checking for build errors), reobfuscates any changed files
(as compared to the source in mcpxxx/src/minecraft_orig) and then these files are added to a minecrift.jar.
The installer is build, versioned and minecrift.jar embedded within.

More to come...