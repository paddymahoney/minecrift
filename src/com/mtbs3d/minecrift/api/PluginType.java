package com.mtbs3d.minecrift.api;

public enum PluginType
{
    PLUGIN_UNKNOWN(-1),
    PLUGIN_POSITION(0),
    PLUGIN_ORIENT(1),
    PLUGIN_HMDINFO(2),
    PLUGIN_STEREO(3),
    PLUGIN_LOOKAIM(4);

    private final int pluginTypeEnum;

    private PluginType(int value)
    {
        this.pluginTypeEnum = value;
    }

    public int value()
    {
        return this.pluginTypeEnum;
    }

    public static PluginType fromInteger(int x) {
        switch(x) {
        case -1:
            return PLUGIN_UNKNOWN;
        case 0:
            return PLUGIN_POSITION;
        case 1:
            return PLUGIN_ORIENT;
        case 2:
            return PLUGIN_HMDINFO;
        case 3:
            return PLUGIN_STEREO;
        }
        return PLUGIN_LOOKAIM;
    }

    public static String toString(PluginType type)
    {
        switch(type)
        {
        case PLUGIN_UNKNOWN:
            return "PLUGIN_UNKNOWN";
        case PLUGIN_POSITION:
            return "PLUGIN_POSITION";
        case PLUGIN_ORIENT:
            return "PLUGIN_ORIENT";
        case PLUGIN_HMDINFO:
            return "PLUGIN_HMDINFO";
        case PLUGIN_STEREO:
            return "PLUGIN_STEREO";
        }

        return "PLUGIN_LOOKAIM";
    }
}
