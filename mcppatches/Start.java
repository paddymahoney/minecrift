import java.util.ArrayList;
import java.util.Arrays;

import com.mtbs3d.minecrift.utils.SessionID;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.client.main.Main;
import net.minecraft.util.Session;

public class Start
{
    public static void main(String[] args) throws Exception
    {
        // Support --username <username> and --password <password> parameters as args.
        /** LEAVE THE LINE BELOW - IT'S UPDATED BY THE INSTALL SCRIPTS TO THE CORRECT MINECRAFT VERSION */
        args = concat(new String[] {"--version", "mcp", "--accessToken", "0", "--assetsDir", "assets", "--assetIndex", "1.8", "--userProperties", "{}"}, args);

        // Authenticate --username <username> and --password <password> with Mojang.
        // *** Username should most likely be an email address!!! ***
        OptionParser optionParser = new OptionParser();
        optionParser.allowsUnrecognizedOptions();
        ArgumentAcceptingOptionSpec username = optionParser.accepts("username").withRequiredArg();
        ArgumentAcceptingOptionSpec password = optionParser.accepts("password").withRequiredArg();
        OptionSet optionSet = optionParser.parse(args);
        String user = (String)optionSet.valueOf(username);
        String pass = (String)optionSet.valueOf(password);
        if (user != null && pass != null)
        {
            Session session = null;

            try {
                session = SessionID.GetSSID(user, pass);
                if (session == null)
                    throw new Exception("Bad login!");
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                Main.main(args);
            }

            ArrayList<String> newArgs = new ArrayList<String>();

            // Remove old username & session, password fields etc.
            for(int i = 0; i < args.length; i++)
            {
                if (args[i].compareToIgnoreCase("--username") == 0 ||
                        args[i].compareToIgnoreCase("--password") == 0 ||
                        args[i].compareToIgnoreCase("--session") == 0 ||
                        args[i].compareToIgnoreCase("--uuid") == 0 ||
                        args[i].compareToIgnoreCase("--accessToken") == 0)
                {
                    // Skip next index as well...
                    i++;
                }
                else
                {
                    // Add to new arg list
                    newArgs.add(args[i]);
                }
            }

            newArgs.add("--username");
            newArgs.add(session.getUsername());
            newArgs.add("--uuid");
            newArgs.add(session.getPlayerID());
            newArgs.add("--accessToken");
            newArgs.add(session.getToken());

            //dumpArgs(newArgs.toArray(new String[0]));
            Main.main(newArgs.toArray(new String[0]));
        }
        else
        {
            //dumpArgs(args);
            Main.main(args);
        }
    }

    private static void dumpArgs(String[] newArgs)
    {
        StringBuilder sb = new StringBuilder();
        for (String s : newArgs) {
            sb.append(s).append(" ");
        }
        System.out.println("[Minecrift] Calling Main.main with args: " + sb.toString());
    }

    public static <T> T[] concat(T[] first, T[] second)
    {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
