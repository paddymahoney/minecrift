/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.*;

public class FBOParams
{
    protected enum FBO_SUPPORT
    {
        USE_EXT_UNKNOWN,
        USE_GL30,
        USE_EXT,
    }

    int _textureType;

    protected static FBO_SUPPORT fboSupport = FBO_SUPPORT.USE_EXT_UNKNOWN;

    public FBOParams(String fboName, int textureType, int internalFormat, int baseFormat, int bufferType, int fboWidth, int fboHeight ) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        _textureType = textureType;

        if (fboSupport == FBO_SUPPORT.USE_EXT_UNKNOWN)
        {
            // The framebuffer, which regroups 0, 1, or more textures, and 0 or 1 depth buffer.
            try {
                _frameBufferId = GL30.glGenFramebuffers();
                fboSupport = FBO_SUPPORT.USE_GL30;
            }
            catch (IllegalStateException ex)
            {
                System.out.println("[Minecrift] FBO creation: GL30.glGenFramebuffers not supported. Attempting to use EXTFramebufferObject.glGenFramebuffersEXT");
                fboSupport = FBO_SUPPORT.USE_EXT;

                try {
                    _frameBufferId = EXTFramebufferObject.glGenFramebuffersEXT();
                }
                catch (IllegalStateException ex1)
                {
                    System.out.println("[Minecrift] FBO creation: EXTFramebufferObject.glGenFramebuffersEXT not supported, FBO creation failed.");
                    throw ex1;
                }
            }
        }
        else if (fboSupport == FBO_SUPPORT.USE_GL30)
        {
            _frameBufferId = GL30.glGenFramebuffers();
        }
        else
        {
            _frameBufferId = EXTFramebufferObject.glGenFramebuffersEXT();
        }

        if (fboSupport == FBO_SUPPORT.USE_GL30)
        {
            _colorTextureId = GL11.glGenTextures();
            _depthRenderBufferId = GL30.glGenRenderbuffers();

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, _frameBufferId);
            mc.checkGLError("FBO bind framebuffer");

            GL11.glBindTexture(textureType, _colorTextureId);
            mc.checkGLError("FBO bind texture");

            GL11.glEnable(textureType);
            GL11.glTexParameterf(textureType, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameterf(textureType, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexImage2D(textureType, 0, internalFormat, fboWidth, fboHeight, 0, baseFormat, bufferType, (java.nio.ByteBuffer) null);

            //System.out.println("[Minecrift] FBO '" + fboName + "': w: " + fboWidth + ", h: " + fboHeight);

            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, textureType, _colorTextureId, 0);

            mc.checkGLError("FBO bind texture framebuffer");

            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, _depthRenderBufferId);                // bind the depth renderbuffer
            GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL14.GL_DEPTH_COMPONENT24, fboWidth, fboHeight); // get the data space for it
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, _depthRenderBufferId);
            mc.checkGLError("FBO bind depth framebuffer");
        }
        else
        {
            _colorTextureId = GL11.glGenTextures();
            _depthRenderBufferId = EXTFramebufferObject.glGenRenderbuffersEXT();

            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, _frameBufferId);
            mc.checkGLError("FBO bind framebuffer");

            GL11.glBindTexture(textureType, _colorTextureId);
            mc.checkGLError("FBO bind texture");

            GL11.glEnable(textureType);
            GL11.glTexParameterf(textureType, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameterf(textureType, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexImage2D(textureType, 0, internalFormat, fboWidth, fboHeight, 0, baseFormat, bufferType, (java.nio.ByteBuffer) null);
            //System.out.println("[Minecrift] FBO '" + fboName + "': w: " + fboWidth + ", h: " + fboHeight);

            EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, textureType, _colorTextureId, 0);

            mc.checkGLError("FBO bind texture framebuffer");

            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, _depthRenderBufferId);                // bind the depth renderbuffer
            EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, GL14.GL_DEPTH_COMPONENT24, fboWidth, fboHeight); // get the data space for it
            EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, _depthRenderBufferId);
            mc.checkGLError("FBO bind depth framebuffer");
        }

        if (!checkFramebufferStatus())
        {
            // OK, if we have an error here - then throw an exception
            System.out.println("[Minecrift] FAILED to create framebuffer!!");
            throw new Exception("Failed to create framebuffer");
        }
    }

    public void bindRenderTarget()
    {
        if (fboSupport == FBO_SUPPORT.USE_GL30)
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, _frameBufferId );
        else
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, _frameBufferId);
    }

    public void bindTexture()
    {
        OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(_textureType, _colorTextureId);
    }

    public void bindTexture_Unit1()
    {
        OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(_textureType, _colorTextureId);
    }

    public int getColorTextureId()
    {
        return _colorTextureId;
    }

    public void delete()
    {
        if (_depthRenderBufferId != -1)
        {
            if (fboSupport == FBO_SUPPORT.USE_GL30)
                GL30.glDeleteRenderbuffers(_depthRenderBufferId);
            else
                EXTFramebufferObject.glDeleteRenderbuffersEXT(_depthRenderBufferId);

            _depthRenderBufferId = -1;
        }

        if (_colorTextureId != -1)
        {
            GL11.glDeleteTextures(_colorTextureId);
            _colorTextureId = -1;
        }

        if (_frameBufferId != -1)
        {
            if (fboSupport == FBO_SUPPORT.USE_GL30)
                GL30.glDeleteFramebuffers(_frameBufferId);
            else
                EXTFramebufferObject.glDeleteFramebuffersEXT(_frameBufferId);

            _frameBufferId = -1;
        }
    }

    // check FBO completeness
    public static boolean checkFramebufferStatus()
    {
        // check FBO status
        int status = ARBFramebufferObject.glCheckFramebufferStatus(ARBFramebufferObject.GL_FRAMEBUFFER);
        switch(status)
        {
            case ARBFramebufferObject.GL_FRAMEBUFFER_COMPLETE:
                //System.out.println("[Minecrift] Framebuffer complete.");
                return true;

            case ARBFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                System.out.println("[ERROR] Framebuffer incomplete: Attachment is NOT complete.");
                return false;

            case ARBFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                System.out.println("[ERROR] Framebuffer incomplete: No image is attached to FBO.");
                return false;

//            case GL30.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
//                System.out.println("[ERROR] Framebuffer incomplete: Attached images have different dimensions.");
//                return false;
//
//            case ARBFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
//                System.out.println("[ERROR] Framebuffer incomplete: Color attached images have different internal formats.");
//                return false;

            case ARBFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                System.out.println("[ERROR] Framebuffer incomplete: Draw buffer.");
                return false;

            case ARBFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                System.out.println("[ERROR] Framebuffer incomplete: Read buffer.");
                return false;

            case ARBFramebufferObject.GL_FRAMEBUFFER_UNSUPPORTED:
                System.out.println("[ERROR] Framebuffer incomplete: Unsupported by FBO implementation.");
                return false;

            default:
                System.out.println("[ERROR] Framebuffer incomplete: Unknown error.");
                return false;
        }
    }

    int _frameBufferId = -1;
    int _colorTextureId = -1;
    int _depthRenderBufferId = -1;
}

