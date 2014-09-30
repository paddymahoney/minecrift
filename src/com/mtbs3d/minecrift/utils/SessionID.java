package com.mtbs3d.minecrift.utils;

import net.minecraft.util.Session;
import java.io.*;
import java.net.*;

import org.json.JSONObject; // JAR available at http://mvnrepository.com/artifact/org.json/json/20140107

public class SessionID
{
    public static final Session GetSSID(String username, String password)
    {
        byte[] b = null;
        String jsonEncoded =
                "{\"agent\":{\"name\":\"Minecraft\",\"version\":1},\"username\":\""
                        + username
                        + "\",\"password\":\""
                        + password + "\"}";
        String response = executePost("https://authserver.mojang.com/authenticate", jsonEncoded);
        if (response == null || response.isEmpty())
            return null;

        // **** JSON parsing courtesy of ssewell ****
        // Create a parsable JSON object from our response string
        JSONObject jsonRepsonse = new JSONObject(response);

        // Obtain our current profile (which contains the user ID and name
        JSONObject jsonSelectedProfile = jsonRepsonse.getJSONObject("selectedProfile");

        // Session ID = "token:<accessToken>:<profile ID>"
        // Username will probably *not be an email address
        String accessToken = jsonRepsonse.getString("accessToken");
        String id = jsonSelectedProfile.getString("id");
        String sessionID = "token:" + jsonRepsonse.getString("accessToken") + ":" + jsonSelectedProfile.getString("id");
        String userName = jsonSelectedProfile.getString("name");

        Session session = new Session(userName, id, accessToken, "legacy");
        return session;
    }

    public static String executePost(String targetURL, String urlParameters)
    {
        URL url;
        HttpURLConnection connection = null;
        try {
            //Create connection
            url = new URL(targetURL);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/json");

            connection.setRequestProperty("Content-Length", "" +
                    Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches (false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream ());
            wr.writeBytes (urlParameters);
            wr.flush ();
            wr.close ();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();

        } catch (Exception e) {

            e.printStackTrace();
            return null;

        } finally {

            if(connection != null) {
                connection.disconnect();
            }
        }
    }
}