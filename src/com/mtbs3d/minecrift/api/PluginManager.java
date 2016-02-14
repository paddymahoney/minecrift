package com.mtbs3d.minecrift.api;

import de.fruitfly.ovr.enums.EyeType;
import de.fruitfly.ovr.structs.Posef;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Quaternion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PluginManager implements IEventListener
{
    static public PluginManager thePluginManager = new PluginManager();

    public List<IBasePlugin> allPlugins = new ArrayList<IBasePlugin>();
    public List<IHMDInfo> hmdInfoPlugins = new ArrayList<IHMDInfo>();
    public List<IOrientationProvider> orientPlugins = new ArrayList<IOrientationProvider>();
    public List<IEyePositionProvider> positionPlugins = new ArrayList<IEyePositionProvider>();
    public List<IBodyAimController> controllerPlugins = new ArrayList<IBodyAimController>();
    public List<IStereoProvider> stereoProviderPlugins = new ArrayList<IStereoProvider>();

    public static void create()
    {
        thePluginManager = new PluginManager();
    }

    public static IHMDInfo configureHMD( String pluginID ) throws Exception {
        IHMDInfo hmdInfo = null;
        for( IHMDInfo hmd : thePluginManager.hmdInfoPlugins )
        {
            if( hmd.getID().equals(pluginID) )
            {
                hmdInfo = hmd;
                break;
            }
        }
        //If we still don't have one
        if( hmdInfo == null && thePluginManager.hmdInfoPlugins.size() > 0 )
        {
            hmdInfo = thePluginManager.hmdInfoPlugins.get(0);
        }

        if( hmdInfo != null  )
        {
            initForMinecrift( hmdInfo );
        }
        return hmdInfo;
    }

    public static IOrientationProvider configureOrientation( String pluginID ) throws Exception {
        IOrientationProvider headTracker = null;
        for( IOrientationProvider tracker: thePluginManager.orientPlugins )
        {
            if( tracker.getID().equals( pluginID ) )
            {
                headTracker = tracker;
                break;
            }
        }
        //If we still don't have one, try to use the first in the list
        if( headTracker == null && thePluginManager.orientPlugins.size() > 0 )
        {
            headTracker = thePluginManager.orientPlugins.get(0);
        }

        if( headTracker != null )
        {
            initForMinecrift( headTracker );
        }
        return headTracker;
    }

    public static IEyePositionProvider configurePosition( String pluginID ) throws Exception {
        IEyePositionProvider positionTracker = null;
        for( IEyePositionProvider posTracker: thePluginManager.positionPlugins )
        {
            if( posTracker.getID().equals( pluginID ) )
            {
                positionTracker = posTracker;
                break;
            }
        }
        //If we still don't have one, try to use the first in the list
        if( positionTracker == null && thePluginManager.positionPlugins.size() > 0 )
        {
            positionTracker = thePluginManager.positionPlugins.get(0);
        }

        if( positionTracker != null )
        {
            initForMinecrift( positionTracker );
        }
        return positionTracker;
    }

    public static IBodyAimController configureController( String pluginID ) throws Exception {

        IBodyAimController lookaimController = null;
        for( IBodyAimController controller: thePluginManager.controllerPlugins )
        {
            if( controller.getID().equals( pluginID ) )
            {
                lookaimController = controller;
                break;
            }
        }
        //If we still don't have one, try to use the first in the list
        if( lookaimController == null && thePluginManager.controllerPlugins.size() > 0 )
        {
            lookaimController = thePluginManager.controllerPlugins.get(0);
        }

        if( lookaimController != null )
        {
            initForMinecrift( lookaimController );
        }
        return lookaimController;
    }

    public static IStereoProvider configureStereoProvider( String pluginID ) throws Exception {

        IStereoProvider stereoProvider = null;
        for( IStereoProvider sp : thePluginManager.stereoProviderPlugins )
        {
            if( sp.getID().equals( pluginID ) )
            {
                stereoProvider = sp;
                break;
            }
        }
        // If we still don't have one, try to use the first in the list
        if( stereoProvider == null && thePluginManager.stereoProviderPlugins.size() > 0 )
        {
            stereoProvider = thePluginManager.stereoProviderPlugins.get(0);
        }

        if( stereoProvider != null )
        {
            initForMinecrift( stereoProvider );
        }
        return stereoProvider;
    }

    private static void initForMinecrift(IBasePlugin plugin) throws Exception
    {
        if( !plugin.isInitialized() )
        {
            System.out.println("[Minecrift] Attempting to initialise plugin: " + plugin.getName() + " (" + PluginManager.getPluginTypes(plugin) + ")");

            if ( !plugin.init() ) {
                String error = "Error! Couldn't load " + plugin.getName() + ": " + plugin.getInitializationStatus();
                System.err.println(error);
                try {
                    throw new Exception(error);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void register( IBasePlugin that )
    {
        if( that instanceof IHMDInfo )
            thePluginManager.hmdInfoPlugins.add((IHMDInfo) that);
        if( that instanceof IOrientationProvider )
            thePluginManager.orientPlugins.add((IOrientationProvider) that);
        if( that instanceof IEyePositionProvider)
            thePluginManager.positionPlugins.add((IEyePositionProvider) that);
        if( that instanceof IBodyAimController )
            thePluginManager.controllerPlugins.add((IBodyAimController) that);
        if( that instanceof IStereoProvider )
            thePluginManager.stereoProviderPlugins.add((IStereoProvider) that);
        if (that instanceof IEventNotifier)
            ((IEventNotifier)that).registerListener(thePluginManager);
        thePluginManager.allPlugins.add(that);
    }

    public static String getPluginTypes( IBasePlugin that )
    {
        StringBuilder sb = new StringBuilder();

        if( that instanceof IHMDInfo )
            sb.append("IHMDInfo ");
        if( that instanceof IOrientationProvider )
            sb.append("IOrientationProvider ");
        if( that instanceof IEyePositionProvider)
            sb.append("IEyePositionProvider ");
        if( that instanceof IBodyAimController )
            sb.append("IBodyAimController ");
        if( that instanceof IStereoProvider )
            sb.append("IStereoProvider ");

        if (sb.length() == 0)
            sb.append("Unknown ");

        return sb.toString();
    }

    public static void pollAll(long frameIndex) throws Exception {
        for( IBasePlugin p : thePluginManager.allPlugins )
        {
            if( p.isInitialized() )
                p.poll(frameIndex);
        }
    }

    public static void beginFrameAll()
    {
        for( IBasePlugin p : thePluginManager.allPlugins )
        {
            if( p.isInitialized() )
                p.beginFrame();
        }
    }

    public static void beginFrameAll(long frameIndex)
    {
        for( IBasePlugin p : thePluginManager.allPlugins )
        {
            if( p.isInitialized() )
                p.beginFrame(frameIndex);
        }
    }

    public static void endFrameAll()
    {
        for( IBasePlugin p : thePluginManager.allPlugins )
        {
            if( p.isInitialized() )
                p.endFrame();
        }
    }

    public static void notifyAll(int eventId)
    {
        for( IBasePlugin p : thePluginManager.allPlugins )
        {
            if( p.isInitialized() && p instanceof IEventListener)
                ((IEventListener)p).eventNotification(eventId);
        }
    }

    public static void destroyAll()
    {
        for( IBasePlugin p : thePluginManager.allPlugins )
        {
            if( p.isInitialized() )
                p.destroy();
        }
    }

    @Override
    public void eventNotification(int eventId)
    {
        switch (eventId)
        {
            case IBasePlugin.EVENT_SET_ORIGIN:
            {
                Minecraft.getMinecraft().vrSettings.posTrackResetPosition = true;
                break;
            }
        }
    }
}
