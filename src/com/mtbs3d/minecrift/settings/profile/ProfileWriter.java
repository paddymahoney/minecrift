package com.mtbs3d.minecrift.settings.profile;

import org.json.JSONObject;

import java.util.*;

/**
 * Created by StellaArtois on 13/04/15.
 */
public class ProfileWriter
{
    private String profile;
    private String set;
    private Map<String, String> data = new HashMap<String, String>();
    private JSONObject theProfiles = null;

    public ProfileWriter(String set)
    {
        this.profile = ProfileManager.currentProfileName;
        this.set = set;

        // Add a new empty profile set
        data = new HashMap<String, String>();
    }

    public ProfileWriter(String set, JSONObject theProfiles)
    {
        this.profile = ProfileManager.currentProfileName;
        this.set = set;
        this.theProfiles = theProfiles;

        // Add a new empty profile set
        data = new HashMap<String, String>();
    }

    public void println(String s)
    {
        String[] array = s.split(":");
        String setting = array[0];
        String value = "";
        if (array.length > 1) {
            value = array[1];
        }
        data.put(setting, value);
    }

    public void close()
    {
        if (this.theProfiles == null) {
            ProfileManager.setProfileSet(this.profile, this.set, this.data);
            ProfileManager.save();
        }
        else {
            ProfileManager.setProfileSet(this.theProfiles, this.set, this.data);
        }
    }
}
