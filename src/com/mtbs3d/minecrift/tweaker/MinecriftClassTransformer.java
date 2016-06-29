package com.mtbs3d.minecrift.tweaker;

import net.minecraft.launchwrapper.IClassTransformer;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;

// With apologies to Optifine. Copyright sp614x, this is built on his work.
// The existing classes are overwritten by all of the classes in the minecrift library. The
// minecrift code implements all of the Forge event handlers via reflection so we are 'Forge
// compatible' for non-core mods. Forge coremods most likely wont play nicely with us however.

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
                    debug("Minecrift ClassTransformer");
                    debug("Minecrift URL: " + url);
                    debug("Minecrift ZIP file: " + zipFile);
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
            debug("*** Can not find the Minecrift JAR in the classpath ***");
            debug("*** Minecrift will not be loaded! ***");
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
    	byte[] minecriftClass = this.getMinecriftClass(name);

    	if (minecriftClass == null) {
    		//debug(String.format("Minecrift: Passthrough '%s' -> '%s'", name, transformedName));
    	}
    	else {
    		// Perform any additional mods using ASM
    		minecriftClass = performAsmModification(minecriftClass, transformedName);
    		if(bytes.length != minecriftClass.length){
    		
//    			FileOutputStream fos;
//				try {
//					fos = new FileOutputStream("e:\\vivecraft\\temp\\" + transformedName + ".class");
//	    			fos.write(bytes);
//	    			fos.close();   			
//				} catch (FileNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}

    			debug(String.format("[Vivecraft tweaker]: Overwrite " + name +" "+ transformedName + " " + bytes.length + " > "  + minecriftClass.length));
    		
    		}

    		//writeToFile("original", transformedName, name, bytes);
    		//writeToFile("transformed", transformedName, name, minecriftClass);
    	}
    	return minecriftClass != null ? minecriftClass : bytes;
    }

    private void writeToFile(String dir, String transformedName, String name, byte[] bytes)
    {
        FileOutputStream stream = null;
        ;
        String filepath = String.format("%s/%s/%s/%s_%s.%s", System.getProperty("user.home"), "minecrift_transformed_classes", dir, transformedName.replace(".", "/"), name, "class");
        File file = new File(filepath);
        debug("Writing to: " + filepath);
        try {
            File dir1 = file.getParentFile();
            dir1.mkdirs();
            file.createNewFile();
            stream = new FileOutputStream(filepath);
            stream.write(bytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
                        debug("Invalid size for " + fullName + ": " + bytes.length + ", should be: " + ze.getSize());
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

    private static void debug(String str)
    {
        System.out.println(str);
    }

    private byte[] performAsmModification(final byte[] origBytecode, String className)
    {
//        if (className.equals("net.minecraft.entity.Entity")) {
//            debug("Further transforming class " + className + " via ASM");
//            ClassReader cr = new ClassReader(origBytecode);
//            ClassWriter cw = new ClassWriter(cr, 0);
//            EntityTransform et = new EntityTransform(cw);
//            cr.accept(et, 0);
//            return cw.toByteArray();
//        }

        return origBytecode;
    }

    private static class EntityTransform
            extends ClassVisitor
    {
        String classname = "net.minecraft.entity.Entity";

        public EntityTransform(ClassVisitor cv)
        {
            super(Opcodes.ASM4, cv);
        }

        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
        {
            this.cv.visit(version, access, name, signature, superName, interfaces);
        }

        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
        {
            if (name.equals("extendedProperties") &&
                    desc.equals("Ljava/util/HashMap;") &&
                    signature.equals("Ljava/util/HashMap<Ljava/lang/String;Ljava/lang/Object;>;"))
            {
                debug("Transforming " + classname + ".extendedProperties");
                return replace_Entity_extendedProperties();
            }
            return this.cv.visitField(access, name, desc, signature, value);
        }

        public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                         String[] exceptions)
        {
            if(name.equals("getExtendedProperties") && desc.equals("(Ljava/lang/String;)Ljava/lang/Object;"))
            {
                debug("Transforming " + classname + ".getExtendedProperties()");
                return replace_Entity_getExtendedProperties();
            }
            else if (name.equals("registerExtendedProperties") && desc.equals("(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;"))
            {
                debug("Transforming " + classname + ".registerExtendedProperties()");
                return replace_Entity_registerExtendedProperties();
            }
            else if (name.equals("initExtendedProperties") && desc.equals("()V"))
            {
                debug("Transforming " + classname + ".initExtendedProperties()");
                return replace_Entity_initExtendedProperties();
            }
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        public FieldVisitor replace_Entity_extendedProperties()
        {
            /**
             * This is an ASMified version of net.minecraft.entity.Entity.extendedProperties field
             * with the HashMap types <String, Object> to <String, net.minecraft.common.IExtendedEntityProperties>
             */
            //FieldVisitor fv = cv.visitField(Opcodes.ACC_PROTECTED, "extendedProperties", "Ljava/util/HashMap;", "Ljava/util/HashMap<Ljava/lang/String;Ljava/lang/Object;>;", null);
            FieldVisitor fv = cv.visitField(Opcodes.ACC_PROTECTED, "extendedProperties", "Ljava/util/HashMap;", "Ljava/util/HashMap<Ljava/lang/String;Lnet/minecraft/common/IExtendedEntityProperties;>;", null);
            fv.visitEnd();

            return fv;
        }

        public MethodVisitor replace_Entity_getExtendedProperties()
        {
            /**
             * This is an ASMified version of net.minecraft.entity.Entity.getExtendedProperties
             * with only the return type changed from Object to net.minecraft.common.IExtendedEntityProperties
             */

            //MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "getExtendedProperties", "(Ljava/lang/String;)Ljava/lang/Object;", null, null);
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "getExtendedProperties", "(Ljava/lang/String;)Lnet/minecraftforge/common/IExtendedEntityProperties;", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(2796, l0);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/src/Reflector", "forgeExists", "()Z", false);
            Label l1 = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, l1);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLineNumber(2798, l2);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, "net/minecraft/entity/Entity", "extendedProperties", "Ljava/util/HashMap;");
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/HashMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitLabel(l1);
            mv.visitLineNumber(2801, l1);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitInsn(Opcodes.ARETURN);
            Label l3 = new Label();
            mv.visitLabel(l3);
            mv.visitLocalVariable("this", "Lnet/minecraft/entity/Entity;", null, l0, l3, 0);
            mv.visitLocalVariable("identifier", "Ljava/lang/String;", null, l0, l3, 1);
            mv.visitMaxs(2, 2);
            mv.visitEnd();

            return mv;
        }

        public MethodVisitor replace_Entity_registerExtendedProperties()
        {
            /**
             * This is an ASMified version of net.minecraft.entity.Entity.registerExtendedProperties
             * with only the params changed from (String, Object) to (String, net.minecraft.common.IExtendedEntityProperties)
             */

            //mv = cv.visitMethod(ACC_PUBLIC, "registerExtendedProperties", "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;", null, null);
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "registerExtendedProperties", "(Ljava/lang/String;Lnet/minecraftforge/common/IExtendedEntityProperties;)Ljava/lang/String;", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(2759, l0);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/src/Reflector", "forgeExists", "()Z", false);
            Label l1 = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, l1);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLineNumber(2760, l2);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            Label l3 = new Label();
            mv.visitJumpInsn(Opcodes.IFNONNULL, l3);
            Label l4 = new Label();
            mv.visitLabel(l4);
            mv.visitLineNumber(2762, l4);
            mv.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraft/src/Reflector", "FMLLog_warning", "Lnet/minecraft/src/ReflectorMethod;");
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
            mv.visitInsn(Opcodes.DUP);
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitLdcInsn("Someone is attempting to register extended properties using a null identifier.  This is not allowed.  Aborting.  This may have caused instability.");
            mv.visitInsn(Opcodes.AASTORE);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/src/Reflector", "callVoid", "(Lnet/minecraft/src/ReflectorMethod;[Ljava/lang/Object;)V", false);
            Label l5 = new Label();
            mv.visitLabel(l5);
            mv.visitLineNumber(2763, l5);
            mv.visitLdcInsn("");
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitLabel(l3);
            mv.visitLineNumber(2765, l3);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            Label l6 = new Label();
            mv.visitJumpInsn(Opcodes.IFNONNULL, l6);
            Label l7 = new Label();
            mv.visitLabel(l7);
            mv.visitLineNumber(2767, l7);
            mv.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraft/src/Reflector", "FMLLog_warning", "Lnet/minecraft/src/ReflectorMethod;");
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
            mv.visitInsn(Opcodes.DUP);
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitLdcInsn("Someone is attempting to register null extended properties.  This is not allowed.  Aborting.  This may have caused instability.");
            mv.visitInsn(Opcodes.AASTORE);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/src/Reflector", "callVoid", "(Lnet/minecraft/src/ReflectorMethod;[Ljava/lang/Object;)V", false);
            Label l8 = new Label();
            mv.visitLabel(l8);
            mv.visitLineNumber(2768, l8);
            mv.visitLdcInsn("");
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitLabel(l6);
            mv.visitLineNumber(2771, l6);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ASTORE, 3);
            Label l9 = new Label();
            mv.visitLabel(l9);
            mv.visitLineNumber(2772, l9);
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitVarInsn(Opcodes.ISTORE, 4);
            Label l10 = new Label();
            mv.visitLabel(l10);
            mv.visitLineNumber(2773, l10);
            mv.visitFrame(Opcodes.F_APPEND,2, new Object[] {"java/lang/String", Opcodes.INTEGER}, 0, null);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, "net/minecraft/entity/Entity", "extendedProperties", "Ljava/util/HashMap;");
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/HashMap", "containsKey", "(Ljava/lang/Object;)Z", false);
            Label l11 = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, l11);
            Label l12 = new Label();
            mv.visitLabel(l12);
            mv.visitLineNumber(2774, l12);
            mv.visitLdcInsn("%s%d");
            mv.visitInsn(Opcodes.ICONST_2);
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
            mv.visitInsn(Opcodes.DUP);
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitInsn(Opcodes.AASTORE);
            mv.visitInsn(Opcodes.DUP);
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitVarInsn(Opcodes.ILOAD, 4);
            mv.visitIincInsn(4, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            mv.visitInsn(Opcodes.AASTORE);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);
            mv.visitVarInsn(Opcodes.ASTORE, 1);
            mv.visitJumpInsn(Opcodes.GOTO, l10);
            mv.visitLabel(l11);
            mv.visitLineNumber(2777, l11);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            Label l13 = new Label();
            mv.visitJumpInsn(Opcodes.IF_ACMPEQ, l13);
            Label l14 = new Label();
            mv.visitLabel(l14);
            mv.visitLineNumber(2779, l14);
            mv.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraft/src/Reflector", "FMLLog_info", "Lnet/minecraft/src/ReflectorMethod;");
            mv.visitInsn(Opcodes.ICONST_3);
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
            mv.visitInsn(Opcodes.DUP);
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitLdcInsn("An attempt was made to register exended properties using an existing key.  The duplicate identifier (%s) has been remapped to %s.");
            mv.visitInsn(Opcodes.AASTORE);
            mv.visitInsn(Opcodes.DUP);
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitInsn(Opcodes.AASTORE);
            mv.visitInsn(Opcodes.DUP);
            mv.visitInsn(Opcodes.ICONST_2);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitInsn(Opcodes.AASTORE);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/src/Reflector", "callVoid", "(Lnet/minecraft/src/ReflectorMethod;[Ljava/lang/Object;)V", false);
            mv.visitLabel(l13);
            mv.visitLineNumber(2782, l13);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, "net/minecraft/entity/Entity", "extendedProperties", "Ljava/util/HashMap;");
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/HashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitInsn(Opcodes.POP);
            Label l15 = new Label();
            mv.visitLabel(l15);
            mv.visitLineNumber(2783, l15);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitLabel(l1);
            mv.visitLineNumber(2786, l1);
            mv.visitFrame(Opcodes.F_CHOP,2, null, 0, null);
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitInsn(Opcodes.ARETURN);
            Label l16 = new Label();
            mv.visitLabel(l16);
            mv.visitLocalVariable("baseIdentifier", "Ljava/lang/String;", null, l9, l1, 3);
            mv.visitLocalVariable("identifierModCount", "I", null, l10, l1, 4);
            mv.visitLocalVariable("this", "Lnet/minecraft/entity/Entity;", null, l0, l16, 0);
            mv.visitLocalVariable("identifier", "Ljava/lang/String;", null, l0, l16, 1);
            mv.visitLocalVariable("properties", "Ljava/lang/Object;", null, l0, l16, 2);
            mv.visitMaxs(5, 5);
            mv.visitEnd();

            return mv;
        }

        public MethodVisitor replace_Entity_initExtendedProperties()
        {
            /**
             * This is an ASMified version of net.minecraft.entity.Entity.initExtendedProperties
             * with only the HashMap types changed from <String, Object> to <String, net.minecraft.common.IExtendedEntityProperties>
             */

            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PRIVATE, "initExtendedProperties", "()V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(307, l0);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.NEW, "java/util/HashMap");
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
            mv.visitFieldInsn(Opcodes.PUTFIELD, "net/minecraft/entity/Entity", "extendedProperties", "Ljava/util/HashMap;");
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLineNumber(309, l1);
            mv.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraft/src/Reflector", "ForgeEntityEvent_EntityConstructing_Constructor", "Lnet/minecraft/src/ReflectorConstructor;");
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
            mv.visitInsn(Opcodes.DUP);
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.AASTORE);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/src/Reflector", "postForgeBusEvent", "(Lnet/minecraft/src/ReflectorConstructor;[Ljava/lang/Object;)Z", false);
            mv.visitInsn(Opcodes.POP);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLineNumber(311, l2);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, "net/minecraft/entity/Entity", "extendedProperties", "Ljava/util/HashMap;");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/HashMap", "values", "()Ljava/util/Collection;", false);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Collection", "iterator", "()Ljava/util/Iterator;", true);
            mv.visitVarInsn(Opcodes.ASTORE, 1);
            Label l3 = new Label();
            mv.visitLabel(l3);
            mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {"java/util/Iterator"}, 0, null);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
            Label l4 = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, l4);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
            // New line below
            mv.visitTypeInsn(Opcodes.CHECKCAST, "net/minecraftforge/common/IExtendedEntityProperties");

            mv.visitVarInsn(Opcodes.ASTORE, 2);
            Label l5 = new Label();
            mv.visitLabel(l5);
            mv.visitLineNumber(313, l5);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraft/src/Reflector", "ForgeIExtendedEntityProperties_init", "Lnet/minecraft/src/ReflectorMethod;");
            mv.visitInsn(Opcodes.ICONST_2);
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
            mv.visitInsn(Opcodes.DUP);
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.AASTORE);
            mv.visitInsn(Opcodes.DUP);
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, "net/minecraft/entity/Entity", "worldObj", "Lnet/minecraft/world/World;");
            mv.visitInsn(Opcodes.AASTORE);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/src/Reflector", "callVoid", "(Ljava/lang/Object;Lnet/minecraft/src/ReflectorMethod;[Ljava/lang/Object;)V", false);
            Label l6 = new Label();
            mv.visitLabel(l6);
            mv.visitLineNumber(314, l6);
            mv.visitJumpInsn(Opcodes.GOTO, l3);
            mv.visitLabel(l4);
            mv.visitLineNumber(315, l4);
            mv.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
            mv.visitInsn(Opcodes.RETURN);
            Label l7 = new Label();
            mv.visitLabel(l7);
            //mv.visitLocalVariable("props", "Ljava/lang/Object;", null, l5, l6, 2);
            mv.visitLocalVariable("props", "Lnet/minecraftforge/common/IExtendedEntityProperties;", null, l5, l6, 2);
            mv.visitLocalVariable("i$", "Ljava/util/Iterator;", null, l3, l4, 1);
            mv.visitLocalVariable("this", "Lnet/minecraft/entity/Entity;", null, l0, l7, 0);
            mv.visitMaxs(6, 3);
            mv.visitEnd();

            return mv;
        }
    }
}
