package com.mtbs3d.minecrift.tweaker;

import net.minecraft.launchwrapper.IClassTransformer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MinecriftClassTransformer implements IClassTransformer
{
    private ZipFile mcZipFile = null;

    public MinecriftClassTransformer()
    {
        try
        {
            URLClassLoader e = (URLClassLoader)MinecriftClassTransformer.class.getClassLoader();
            URL[] urls = e.getURLs();

            for (int i = 0; i < urls.length; ++i)
            {
                URL url = urls[i];
                ZipFile zipFile = getMinecriftZipFile(url);

                if (zipFile != null)
                {
                    this.mcZipFile = zipFile;
                    dbg("Minecrift ClassTransformer");
                    dbg("Minecrift URL: " + url);
                    dbg("Minecrift ZIP file: " + zipFile);
                    break;
                }
            }
        }
        catch (Exception var6)
        {
            var6.printStackTrace();
        }

        if (this.mcZipFile == null)
        {
            dbg("*** Can not find the Minecrift JAR in the classpath ***");
            dbg("*** Minecrift will not be loaded! ***");
        }
    }

    private static ZipFile getMinecriftZipFile(URL url)
    {
        try
        {
            URI uri = url.toURI();
            File file = new File(uri);
            ZipFile zipFile = new ZipFile(file);

            if (zipFile.getEntry("com/mtbs3d/minecrift/provider/MCOculus.class") == null)
            {
                zipFile.close();
                return null;
            }
            else
            {
                return zipFile;
            }
        }
        catch (Exception var4)
        {
            return null;
        }
    }

    public byte[] transform(String name, String transformedName, byte[] bytes)
    {
        byte[] ofBytes = this.getMinecriftClass(name);
        return ofBytes != null ? ofBytes : bytes;
    }

    private byte[] getMinecriftClass(String name)
    {
        if (this.mcZipFile == null)
        {
            return null;
        }
        else
        {
            String fullName = name + ".class";
            ZipEntry ze = this.mcZipFile.getEntry(fullName);

            if (ze == null)
            {
                return null;
            }
            else
            {
                try
                {
                    InputStream e = this.mcZipFile.getInputStream(ze);
                    byte[] bytes = readAll(e);

                    if ((long)bytes.length != ze.getSize())
                    {
                        dbg("Invalid size for " + fullName + ": " + bytes.length + ", should be: " + ze.getSize());
                        return null;
                    }
                    else
                    {
                        return bytes;
                    }
                }
                catch (IOException var6)
                {
                    var6.printStackTrace();
                    return null;
                }
            }
        }
    }

    public static byte[] readAll(InputStream is) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];

        while (true)
        {
            int bytes = is.read(buf);

            if (bytes < 0)
            {
                is.close();
                byte[] bytes1 = baos.toByteArray();
                return bytes1;
            }

            baos.write(buf, 0, bytes);
        }
    }

    private static void dbg(String str)
    {
        System.out.println(str);
    }
}
