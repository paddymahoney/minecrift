package com.mtbs3d.minecrift.gameplay;


import de.fruitfly.ovr.structs.EulerOrient;
import de.fruitfly.ovr.structs.Matrix4f;
import de.fruitfly.ovr.structs.Quatf;
import de.fruitfly.ovr.structs.Vector3f;
import jopenvr.OpenVRUtil;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

// VIVE
public class VRPlayer
{
    public double lastRoomUpdateTime = 0;
    public Vec3 movementTeleportDestination = Vec3.createVectorHelper(0.0,0.0,0.0);
    public int movementTeleportDestinationSideHit;
    public double movementTeleportProgress;
    public double movementTeleportDistance;
    private Vec3 roomOrigin = Vec3.createVectorHelper(0,0,0);
    public Vec3 teleportSlide = Vec3.createVectorHelper(0,0,0);
    public long teleportSlideStartTime = 0;
    public VRMovementStyle vrMovementStyle = new VRMovementStyle();
    public Vec3[] movementTeleportArc = new Vec3[50];
    public int movementTeleportArcSteps = 0;
    private boolean restrictedViveClient = true;        // true when connected to another server that doesn't have this mod
	public boolean useLControllerForRestricedMovement = true;
    public double lastTeleportArcDisplayOffset = 0;
    public double fallTime = 0;

    private float teleportEnergy;
    
    
    
    public static VRPlayer get()
    {
        return Minecraft.getMinecraft().vrPlayer;
    }

    public VRPlayer()
    {
        for (int i=0;i<50;i++)
        {
            movementTeleportArc[i] = Vec3.createVectorHelper(0,0,0);
        }
    }

    public Vec3 getRoomOrigin() { return this.roomOrigin;}
    
    public void setRoomOrigin(double x, double y, double z) { 
    	this.roomOrigin.xCoord = x;
    	this.roomOrigin.yCoord = y;
    	this.roomOrigin.zCoord = z;
        lastRoomUpdateTime = Minecraft.getMinecraft().stereoProvider.getCurrentTimeSecs();
    }
    
    
    public void snapRoomOriginToPlayerEntity(EntityPlayerSP player)
    {
        if (Thread.currentThread().getName().equals("Server thread"))
            return;

        double x = player.posX;
        double y = player.boundingBox.minY;
        double z = player.posZ;
        
        Minecraft mc = Minecraft.getMinecraft();
        boolean bStartingUp = (roomOrigin.xCoord==0 && roomOrigin.yCoord==0 && roomOrigin.zCoord==0);
        boolean bRestricted = !bStartingUp && restrictedViveClient;

        if (mc.positionTracker == null || !mc.positionTracker.isInitialized() || bRestricted)
        { // set room origin exactly to x,y,z since in restricted mode the room origin is always the players feet
            if (lastRoomUpdateTime==0
                    || mc.stereoProvider.getCurrentTimeSecs() - lastRoomUpdateTime >= mc.vrSettings.restrictedCameraUpdateInterval)
            {
            	setRoomOrigin(x, y, z);
            }
        }
        else
        { //set room origin to underneath the headset... which is where the player entity should be already? shouldnt be already anyway? maybe cause collosion?
            teleportSlide.xCoord = 0;
            teleportSlide.yCoord = 0;
            teleportSlide.zCoord = 0;
            Vec3 hmdOffset = mc.positionTracker.getCenterEyePosition();
            double newX = x + hmdOffset.xCoord;
            double newY = y;
            double newZ = z + hmdOffset.zCoord;
            teleportSlide.xCoord = roomOrigin.xCoord - newX;
            teleportSlide.yCoord = roomOrigin.yCoord - newY;
            teleportSlide.zCoord = roomOrigin.zCoord - newZ;
            
            setRoomOrigin(newX, newY, newZ);

            teleportSlideStartTime = System.nanoTime();
        }
    }
    
    public  double topofhead = 1.62;
    
    
    
    public void onLivingUpdate(EntityPlayerSP player, Minecraft mc, Random rand)
    {
	
    	
        updateSwingAttack();
		
       if(mc.vrSettings.vrAllowCrawling){         //experimental
           topofhead = (double) (mc.entityRenderer.getCameraLocation().yCoord + .05) - player.boundingBox.minY;
           
           if(topofhead < .5) {topofhead = 0.5f;}
           if(topofhead > 1.8) {topofhead = 1.8f;}
           
           player.height = (float) topofhead;

           player.boundingBox.maxY = player.boundingBox.minY +  player.height;    	   
       }

      
        // don't do teleport movement if on a server that doesn't have this mod installed
        if (restrictedViveClient) {
			  return; //let mc handle look direction movement
			// controller vs gaze movement is handled in Entity.java > moveFlying
          }
				
        mc.mcProfiler.startSection("VRPlayerOnLivingUpdate");

        if (teleportEnergy < 100) { teleportEnergy++;}
        
        boolean doTeleport = false;
        Vec3 dest = null;

        if (player.movementInput.moveForward != 0) //holding down Ltrigger
        {
            dest = movementTeleportDestination;

            if (vrMovementStyle.teleportOnRelease)
            {
                if (player.movementTeleportTimer==0)
                {
                    String sound = vrMovementStyle.startTeleportingSound;
                    if (sound != null)
                    {
                        player.playSound(sound, vrMovementStyle.startTeleportingSoundVolume,
                                1.0F / (rand.nextFloat() * 0.4F + 1.2F) + 1.0f * 0.5F);
                    }
                }
                player.movementTeleportTimer++;
                if (player.movementTeleportTimer > 0)
                {
                    movementTeleportProgress = (float) player.movementTeleportTimer / 4.0f;
                    if (movementTeleportProgress>=1.0f)
                    {
                        movementTeleportProgress = 1.0f;
                    }

                    if (dest.xCoord != 0 || dest.yCoord != 0 || dest.zCoord != 0)
                    {
                        Vec3 teleportSlide = mc.vrPlayer.getTeleportSlide();
                        Vec3 eyeCenterPos = mc.positionTracker.getCenterEyePosition();

                        eyeCenterPos.xCoord = roomOrigin.xCoord + teleportSlide.xCoord - eyeCenterPos.xCoord;
                        eyeCenterPos.yCoord = roomOrigin.yCoord + teleportSlide.yCoord - eyeCenterPos.yCoord;
                        eyeCenterPos.zCoord = roomOrigin.zCoord + teleportSlide.zCoord - eyeCenterPos.zCoord;

                        // cloud of sparks moving past you
                        Vec3 motionDir = dest.addVector(-eyeCenterPos.xCoord, -eyeCenterPos.yCoord, -eyeCenterPos.zCoord).normalize();
                        Vec3 forward;
						
						forward	= player.getLookVec();

                        Vec3 right = forward.crossProduct(Vec3.createVectorHelper(0, 1, 0));
                        Vec3 up = right.crossProduct(forward);

                        if (vrMovementStyle.airSparkles)
                        {
                            for (int iParticle = 0; iParticle < 3; iParticle++)
                            {
                                double forwardDist = rand.nextDouble() * 1.0 + 3.5;
                                double upDist = rand.nextDouble() * 2.5;
                                double rightDist = rand.nextDouble() * 4.0 - 2.0;

                                Vec3 sparkPos = Vec3.createVectorHelper(eyeCenterPos.xCoord + forward.xCoord * forwardDist,
                                        eyeCenterPos.yCoord + forward.yCoord * forwardDist,
                                        eyeCenterPos.zCoord + forward.zCoord * forwardDist);
                                sparkPos = sparkPos.addVector(right.xCoord * rightDist, right.yCoord * rightDist, right.zCoord * rightDist);
                                sparkPos = sparkPos.addVector(up.xCoord * upDist, up.yCoord * upDist, up.zCoord * upDist);

                                double speed = -0.6;
                                EntityFX particle = new EntityVRTeleportFX(
                                        player.worldObj,
                                        sparkPos.xCoord, sparkPos.yCoord, sparkPos.zCoord,
                                        motionDir.xCoord * speed, motionDir.yCoord * speed, motionDir.zCoord * speed,
                                        1.0f);
                                mc.effectRenderer.addEffect(particle);
                            }
                        }
                    }
                }
            }
            else
            {
                if (player.movementTeleportTimer >= 0 && (dest.xCoord != 0 || dest.yCoord != 0 || dest.zCoord != 0))
                {
                    if (player.movementTeleportTimer == 0)
                    {
                        String sound = vrMovementStyle.startTeleportingSound;
                        if (sound != null)
                        {
                            player.playSound(sound, vrMovementStyle.startTeleportingSoundVolume,
                                    1.0F / (rand.nextFloat() * 0.4F + 1.2F) + 1.0f * 0.5F);
                        }
                    }
                    player.movementTeleportTimer++;

                    Vec3 playerPos = Vec3.createVectorHelper(player.posX, player.posY, player.posZ);
                    double dist = dest.distanceTo(playerPos);
                    double progress = (player.movementTeleportTimer * 1.0) / (dist + 3.0);

                    if (player.movementTeleportTimer > 0)
                    {
                        movementTeleportProgress = progress;

                        // spark at dest point
                        if (vrMovementStyle.destinationSparkles)
                        {
                            player.worldObj.spawnParticle("instantSpell", dest.xCoord, dest.yCoord, dest.zCoord, 0, 1.0, 0);
                        }

                        // cloud of sparks moving past you
                        Vec3 motionDir = dest.addVector(-player.posX, -player.posY, -player.posZ).normalize();
                        Vec3 forward = player.getLookVec();
                        Vec3 right = forward.crossProduct(Vec3.createVectorHelper(0, 1, 0));
                        Vec3 up = right.crossProduct(forward);

                        if (vrMovementStyle.airSparkles)
                        {
                            for (int iParticle = 0; iParticle < 3; iParticle++)
                            {
                                double forwardDist = rand.nextDouble() * 1.0 + 3.5;
                                double upDist = rand.nextDouble() * 2.5;
                                double rightDist = rand.nextDouble() * 4.0 - 2.0;
                                Vec3 sparkPos = Vec3.createVectorHelper(player.posX + forward.xCoord * forwardDist,
                                        player.posY + forward.yCoord * forwardDist,
                                        player.posZ + forward.zCoord * forwardDist);
                                sparkPos = sparkPos.addVector(right.xCoord * rightDist, right.yCoord * rightDist, right.zCoord * rightDist);
                                sparkPos = sparkPos.addVector(up.xCoord * upDist, up.yCoord * upDist, up.zCoord * upDist);

                                double speed = -0.6;
                                EntityFX particle = new EntityVRTeleportFX(
                                        player.worldObj,
                                        sparkPos.xCoord, sparkPos.yCoord, sparkPos.zCoord,
                                        motionDir.xCoord * speed, motionDir.yCoord * speed, motionDir.zCoord * speed,
                                        1.0f);
                                mc.effectRenderer.addEffect(particle);
                            }
                        }
                    } else
                    {
                        movementTeleportProgress = 0;
                    }

                    if (progress >= 1.0)
                    {
                        doTeleport = true;
                    }
                }
            }
        }
        else //not holding down Ltrigger
        {
            if (vrMovementStyle.teleportOnRelease && movementTeleportProgress>=1.0f)
            {
                dest = movementTeleportDestination;
                doTeleport = true;
            }
            player.movementTeleportTimer = 0;
            movementTeleportProgress = 0;
        }

        if (doTeleport && dest!=null && (dest.xCoord != 0 || dest.yCoord !=0 || dest.zCoord != 0)) //execute teleport
        {
            movementTeleportDistance = (float)MathHelper.sqrt_double(dest.squareDistanceTo(player.posX, player.posY, player.posZ));
            boolean playTeleportSound = movementTeleportDistance > 0.0f && vrMovementStyle.endTeleportingSound != null;
            Block block = null;

            if (playTeleportSound)
            {
                String sound = vrMovementStyle.endTeleportingSound;
                if (sound != null)
                {
                    player.playSound(sound, vrMovementStyle.endTeleportingSoundVolume, 1.0F);
                }
            }
            else
            {
                playFootstepSound(mc, dest.xCoord, dest.yCoord, dest.zCoord);
            }

            //execute teleport      
            player.setPositionAndUpdate(dest.xCoord, dest.yCoord, dest.zCoord);         
          
            if(mc.vrSettings.vrLimitedSurvivalTeleport){
              player.addExhaustion((float) (movementTeleportDistance / 16 * 1.2f));    
              
              if (!mc.vrPlayer.getFreeMoveMode() && mc.playerController.isNotCreative() && mc.vrPlayer.vrMovementStyle.arcAiming){
              	teleportEnergy -= movementTeleportDistance * 4;	
              }              
            }
               
            
          //  System.out.println("teleport " + dest.toString());
            player.fallDistance = 0.0F;

            if (playTeleportSound)
            {
                String sound = vrMovementStyle.endTeleportingSound;
                if (sound != null)
                {
                    player.playSound(sound, vrMovementStyle.endTeleportingSoundVolume, 1.0F);
                }
            }
            else
            {
                playFootstepSound(mc, dest.xCoord, dest.yCoord, dest.zCoord);
            }

            player.movementTeleportTimer = -1;
            
        }
        else //standing still
        {
			doPlayerMoveInRoom(player);
        }
        mc.mcProfiler.endSection();
    }

    private boolean wasYMoving;
    
	private void doPlayerMoveInRoom(EntityPlayerSP player){
		// this needs... work...
	
		
		
				if(player.isSneaking()) {return;} //jrbudda : prevent falling off things or walking up blocks while moving in room scale.
		
				Minecraft mc = Minecraft.getMinecraft();
	            float playerHalfWidth = player.width / 2.0F;

                // move player's X/Z coords as the HMD moves around the room

                Vec3 eyePos = mc.positionTracker.getCenterEyePosition();

                double x = roomOrigin.xCoord - eyePos.xCoord;
                double y = player.posY;
                double z = roomOrigin.zCoord - eyePos.zCoord;
			
                // create bounding box at dest position
                AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(
                        x - (double) playerHalfWidth,
                        y - (double) player.yOffset + (double) player.yOffset2,
                        z - (double) playerHalfWidth,
                        x + (double) playerHalfWidth,
                        y - (double) player.yOffset + (double) player.yOffset2 + (double) player.height,
                        z + (double) playerHalfWidth);

                Vec3 torso = null;

                // valid place to move player to?
                float var27 = 0.0625F;
                boolean emptySpot = mc.theWorld.getCollidingBoundingBoxes(player, bb).isEmpty();
				
                if (emptySpot)
                {
                    // don't call setPosition style functions to avoid shifting room origin
                    player.lastTickPosX = player.prevPosX = player.posX = x;
                    
                    if (!mc.vrSettings.simulateFalling)	{
                    	  player.lastTickPosY = player.prevPosY = player.posY = y;                	
                    }
                                        
                    player.lastTickPosZ = player.prevPosZ = player.posZ = z;
                    player.boundingBox.setBounds(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY + player.height, bb.maxZ);
                    player.fallDistance = 0.0F;

                    torso = getEstimatedTorsoPosition(x, y, z);


//                        float fallPadding = player.width * 0.0f;
//
//                        double paddedFallHalfWidth = playerHalfWidth + fallPadding;
//                        AxisAlignedBB bbFall = AxisAlignedBB.getBoundingBox(
//                                torso.xCoord - paddedFallHalfWidth,
//                                bb.minY,
//                                torso.zCoord - paddedFallHalfWidth,
//                                torso.xCoord + paddedFallHalfWidth,
//                                bb.maxY,
//                                torso.zCoord + paddedFallHalfWidth);
//
//                        int fallCount = 0;
//                        
//                        while (emptySpot && fallCount < 32)
//                        {
//                            bbFall.maxY -= 1.0f;
//                            bbFall.minY -= 1.0f;
//                            emptySpot = mc.theWorld.getCollidingBoundingBoxes(player, bbFall).isEmpty();
//                            fallCount++;
//                        }
//                        
//                        if (fallCount > 1)
//                        {
//                            if (mc.stereoProvider.getCurrentTimeSecs() >= fallTime)
//                            {
//                                double xOffset = torso.xCoord - x;
//                                double zOffset = torso.zCoord - z;
//
//                                float fallDist = 1.0f * (fallCount - 1);
//                                x += xOffset;
//                                y -= fallDist;
//                                z += zOffset;
//                                bb.minX += xOffset;
//                                bb.maxX += xOffset;
//                                bb.minY -= fallDist;
//                                bb.maxY -= fallDist;
//                                bb.minZ += zOffset;
//                                bb.maxZ += zOffset;
//                                player.lastTickPosX = player.prevPosX = player.posX = x;
//                                player.lastTickPosY = player.prevPosY = player.posY = y;
//                                player.lastTickPosZ = player.prevPosZ = player.posZ = z;
//                                player.boundingBox.setBounds(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
//                                player.fallDistance = fallDist;
//                                roomOrigin.xCoord += xOffset;
//                                roomOrigin.yCoord -= fallDist;
//                                roomOrigin.zCoord += zOffset;
//
//                                playFootstepSound(mc, player.posX, player.posY-1.62f, player.posZ);
//                            }
//                        }
//                        else
//                        {
//                            fallTime = mc.stereoProvider.getCurrentTimeSecs() + 0.25;
//                        }
//                	}
                    		}

    //             test for climbing up a block
                if (mc.vrSettings.walkUpBlocks && player.fallDistance == 0)
                {
                    if (torso == null)
                    {
                        torso = getEstimatedTorsoPosition(x, y, z);
                    }

                    // is the player significantly inside a block?
                    float climbShrink = player.width * 0.45f;
                    double shrunkClimbHalfWidth = playerHalfWidth - climbShrink;
                    AxisAlignedBB bbClimb = AxisAlignedBB.getBoundingBox(
                            torso.xCoord - shrunkClimbHalfWidth,
                            bb.minY,
                            torso.zCoord - shrunkClimbHalfWidth,
                            torso.xCoord + shrunkClimbHalfWidth,
                            bb.maxY,
                            torso.zCoord + shrunkClimbHalfWidth);

                    emptySpot = mc.theWorld.getCollidingBoundingBoxes(player, bbClimb).isEmpty();
                    if (!emptySpot)
                    {
                        // is 1 block up empty?
                        double xOffset = torso.xCoord - x;
                        double zOffset = torso.zCoord - z;
                        bb.minX += xOffset;
                        bb.maxX += xOffset;
                        bb.minY += 1.0f;
                        bb.maxY += 1.0f;
                        bb.minZ += zOffset;
                        bb.maxZ += zOffset;
                        emptySpot = mc.theWorld.getCollidingBoundingBoxes(player, bb).isEmpty();
                        if (emptySpot)
                        {
                            x += xOffset;
                            y += 1.0f;
                            z += zOffset;
                            player.lastTickPosX = player.prevPosX = player.posX = x;
                            player.lastTickPosY = player.prevPosY = player.posY = y;
                            player.lastTickPosZ = player.prevPosZ = player.posZ = z;
                            player.boundingBox.setBounds(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
                            
							roomOrigin.xCoord += xOffset;
                            roomOrigin.yCoord += 1.0f;
                            roomOrigin.zCoord += zOffset;

                            Vec3 look = player.getLookVec();
                            Vec3 forward = Vec3.createVectorHelper(look.xCoord,0,look.zCoord).normalize();
                            playFootstepSound(mc,
                                    player.posX + forward.xCoord * 0.4f,
                                    player.posY-1.62f,
                                    player.posZ + forward.zCoord * 0.4f);
                        }
                    }
                }
            
	}
	
    public void playFootstepSound( Minecraft mc, double x, double y, double z )
    {
        Block block = mc.theWorld.getBlock(MathHelper.floor_double(x),
                MathHelper.floor_double(y - 0.5f),
                MathHelper.floor_double(z));

        if (block != null && block.getMaterial() != Material.air)
        {
            mc.getSoundHandler().playSound(new PositionedSoundRecord(new ResourceLocation(block.stepSound.getStepSound()),
                    (block.stepSound.getVolume() + 1.0F) / 8.0F,
                    block.stepSound.getFrequency() * 0.5F,
                    (float) x, (float) y, (float) z));
        }
    }

    // use simple neck modeling to estimate torso location
    public Vec3 getEstimatedTorsoPosition(double x, double y, double z)
    {
        Entity player = Minecraft.getMinecraft().thePlayer;
        Vec3 look = player.getLookVec();
        Vec3 forward = Vec3.createVectorHelper(look.xCoord, 0, look.zCoord).normalize();
        float factor = (float)look.yCoord * 0.25f;
        Vec3 torso = Vec3.createVectorHelper(
                x + forward.xCoord * factor,
                y + forward.yCoord * factor,
                z + forward.zCoord * factor);

        return torso;
    }

    public static Vec3 getTeleportTraceStart(Minecraft mc)
    {
        return mc.lookaimController.getAimSource(1);
    }

    public static Matrix4f getTeleportAimRotation(Minecraft mc)
    {
        return mc.lookaimController.getAimRotation(1);
    }

    public static Vec3 getTeleportAim(Minecraft mc)
    {
        Matrix4f handRotation = getTeleportAimRotation(mc);
        Vector3f forward = new Vector3f(0,0,1);
        Vector3f handDirection = handRotation.transform(forward);
        return Vec3.createVectorHelper(handDirection.x, -handDirection.y, handDirection.z);
    }

    public void updateTeleportArc(Minecraft mc, Entity player)
    {
        Vec3 start = getTeleportTraceStart(mc);
        Vec3 tiltedAim = getTeleportAim(mc);
        Matrix4f handRotation = getTeleportAimRotation(mc);

        // extract hand roll
        Quatf handQuat = OpenVRUtil.convertMatrix4ftoRotationQuat(handRotation);
        EulerOrient euler = OpenVRUtil.getEulerAnglesDegYXZ(handQuat);

        int maxSteps = 50;
        movementTeleportArc[0].xCoord = start.xCoord;
        movementTeleportArc[0].yCoord = start.yCoord;
        movementTeleportArc[0].zCoord = start.zCoord;
        movementTeleportArcSteps = 1;

        // if aiming too high, don't trace a full arc
        float horizontalAimDist = (float)Math.sqrt(tiltedAim.xCoord*tiltedAim.xCoord + tiltedAim.zCoord*tiltedAim.zCoord);
//        float pitch = (float)Math.atan2(tiltedAim.yCoord, horizontalAimDist);
//        if (pitch > (float)Math.PI * 0.25f)
//        {
//            maxSteps = 6;
//        }

        // calculate gravity vector for arc
        float gravityAcceleration = 0.098f;
        Matrix4f rollCounter = OpenVRUtil.rotationZMatrix((float)MathHelper.deg2Rad*euler.roll);
        Matrix4f gravityTilt = OpenVRUtil.rotationXMatrix((float)Math.PI * -0.5f);
        Matrix4f gravityRotation = Matrix4f.multiply(Matrix4f.multiply(handRotation, rollCounter), gravityTilt);
        Vector3f forward = new Vector3f(0,0,1);
        Vector3f gravityDirection = gravityRotation.transform(forward);
        Vec3 gravity = Vec3.createVectorHelper(gravityDirection.x, -gravityDirection.y, gravityDirection.z);
        gravity.xCoord *= gravityAcceleration;
        gravity.yCoord *= gravityAcceleration;
        gravity.zCoord *= gravityAcceleration;

        // calculate initial move step
        float speed = 0.5f;
        Vec3 velocity = Vec3.createVectorHelper(
                tiltedAim.xCoord * speed,
                tiltedAim.yCoord * speed,
                tiltedAim.zCoord * speed );

        Vec3 pos = Vec3.createVectorHelper(start.xCoord, start.yCoord, start.zCoord);
        Vec3 newPos = Vec3.createVectorHelper(0,0,0);

        // trace arc
        for (int i=movementTeleportArcSteps;i<maxSteps;i++)
        {
        	if (i*4 > teleportEnergy) {
        		break;
        		}
        	
            newPos.xCoord = pos.xCoord + velocity.xCoord;
            newPos.yCoord = pos.yCoord + velocity.yCoord;
            newPos.zCoord = pos.zCoord + velocity.zCoord;

            MovingObjectPosition collision = mc.theWorld.rayTraceBlocks(pos, newPos, !mc.thePlayer.isInWater(), true, false);
			
            if (collision != null && collision.typeOfHit != MovingObjectPosition.MovingObjectType.MISS)
            {
                movementTeleportArc[i].xCoord = collision.hitVec.xCoord;
                movementTeleportArc[i].yCoord = collision.hitVec.yCoord;
                movementTeleportArc[i].zCoord = collision.hitVec.zCoord;
                movementTeleportArcSteps = i + 1;

                Vec3 traceDir = pos.subtract(newPos).normalize();
                Vec3 reverseEpsilon = Vec3.createVectorHelper(-traceDir.xCoord * 0.02, -traceDir.yCoord * 0.02, -traceDir.zCoord * 0.02);

                checkAndSetTeleportDestination(mc, player, start, collision, reverseEpsilon);
                          
                break;
            }

            pos.xCoord = newPos.xCoord;
            pos.yCoord = newPos.yCoord;
            pos.zCoord = newPos.zCoord;

            movementTeleportArc[i].xCoord = newPos.xCoord;
            movementTeleportArc[i].yCoord = newPos.yCoord;
            movementTeleportArc[i].zCoord = newPos.zCoord;
            movementTeleportArcSteps = i + 1;

            velocity.xCoord += gravity.xCoord;
            velocity.yCoord += gravity.yCoord;
            velocity.zCoord += gravity.zCoord;
        }
    }

    public void updateTeleportDestinations(EntityRenderer renderer, Minecraft mc, Entity player)
    {
        mc.mcProfiler.startSection("updateTeleportDestinations");

        // no teleporting if on a server that disallows teleporting
        if (restrictedViveClient)
        {
            movementTeleportDestination.xCoord = 0.0;
            movementTeleportDestination.yCoord = 0.0;
            movementTeleportDestination.zCoord = 0.0;
            movementTeleportArcSteps = 0;
            return;
        }

        if (vrMovementStyle.arcAiming)
        {
            movementTeleportDestination.xCoord = 0.0;
            movementTeleportDestination.yCoord = 0.0;
            movementTeleportDestination.zCoord = 0.0;

            if (movementTeleportProgress>0.0f)
            {
                updateTeleportArc(mc, player);
            }
        }
        else
        {
            Vec3 start = getTeleportTraceStart(mc);
            Vec3 aimDir = getTeleportAim(mc);

            // setup teleport forwards to the mouse cursor
            double movementTeleportDistance = 250.0;
            Vec3 movementTeleportPos = start.addVector(
                    aimDir.xCoord * movementTeleportDistance,
                    aimDir.yCoord * movementTeleportDistance,
                    aimDir.zCoord * movementTeleportDistance);
            MovingObjectPosition collision = mc.theWorld.rayTraceBlocks(start, movementTeleportPos, !mc.thePlayer.isInWater(), true, false);
            Vec3 traceDir = start.subtract(movementTeleportPos).normalize();
            Vec3 reverseEpsilon = Vec3.createVectorHelper(-traceDir.xCoord * 0.02, -traceDir.yCoord * 0.02, -traceDir.zCoord * 0.02);

            // don't update while charging up a teleport
            if (movementTeleportProgress != 0)
                return;

            if (collision != null && collision.typeOfHit != MovingObjectPosition.MovingObjectType.MISS)
            {
                checkAndSetTeleportDestination(mc, player, start, collision, reverseEpsilon);
            }
        }
        mc.mcProfiler.endSection();
    }

    // look for a valid place to stand on the block that the trace collided with
    private boolean checkAndSetTeleportDestination(Minecraft mc, Entity player, Vec3 start, MovingObjectPosition collision, Vec3 reverseEpsilon)
    {
        boolean bFoundValidSpot = false;

        
		if (collision.sideHit != 1) 
		{ //sides
		//jrbudda require arc hitting top of block.	unless ladder or vine.
			Block testClimb = player.worldObj.getBlock(collision.blockX, collision.blockY, collision.blockZ);
		//	System.out.println(testClimb.getUnlocalizedName() + " " + collision.typeOfHit + " " + collision.sideHit);
			   				   			   
			if ( testClimb == Blocks.ladder || testClimb == Blocks.vine) {
			            Vec3 dest = Vec3.createVectorHelper(collision.blockX+0.5, collision.blockY + 0.5, collision.blockZ+0.5);
                        movementTeleportDestination.xCoord = dest.xCoord;
                        movementTeleportDestination.yCoord = dest.yCoord;
                        movementTeleportDestination.zCoord = dest.zCoord;
                        movementTeleportDestinationSideHit = collision.sideHit;
						return true; //really should check if the block above is passable. Maybe later.
			} else {
					if (!mc.thePlayer.capabilities.allowFlying && mc.vrSettings.vrLimitedSurvivalTeleport) {return false;} //if creative, check if can hop on top.
			}
		}
		
        for ( int k = 0; k < 1 && !bFoundValidSpot; k++ )
        {
            Vec3 hitVec = collision.hitVec;// ( k == 1 ) ? collision.hitVec.addVector(-reverseEpsilon.xCoord, -reverseEpsilon.yCoord, -reverseEpsilon.zCoord)
                    						//: collision.hitVec.addVector(reverseEpsilon.xCoord, reverseEpsilon.yCoord, reverseEpsilon.zCoord);

            Vec3 debugPos = Vec3.createVectorHelper(
                    MathHelper.floor_double(hitVec.xCoord) + 0.5,
                    MathHelper.floor_double(hitVec.yCoord),
                    MathHelper.floor_double(hitVec.zCoord) + 0.5);

            int bx = collision.blockX;
            int bz = collision.blockZ;

            // search for a solid block with two empty blocks above it
            int startBlockY = collision.blockY -1 ; 
            startBlockY = Math.max(startBlockY, 0);
            for (int by = startBlockY; by < startBlockY + 2; by++)
            {
                if (canStand(player.worldObj,bx, by, bz))
                {
                    float maxTeleportDist = 16.0f;

                    float var27 = 0.0625F; //uhhhh?
                    
                    double ox = hitVec.xCoord - player.posX;
                    double oy = by + 1 - player.posY;
                    double oz = hitVec.zCoord - player.posZ;
                    AxisAlignedBB bb = player.boundingBox.copy().contract((double)var27, (double)var27, (double)var27).offset(ox, oy, oz); 
                    bb.minY = by+1f;
                    bb.maxY = by+2.8f;
                    boolean emptySpotReq = mc.theWorld.getCollidingBoundingBoxes(player,bb).isEmpty();
                             
                    double ox2 = bx + 0.5f - player.posX;
                    double oy2 = by + 1.0f - player.posY;
                    double oz2 = bz + 0.5f - player.posZ;
                    AxisAlignedBB bb2 = player.boundingBox.copy().contract((double)var27, (double)var27, (double)var27).offset(ox2, oy2, oz2);
                    bb2.minY = by+1f;
                    bb2.maxY = by+2.8f;
                    boolean emptySpotCenter = mc.theWorld.getCollidingBoundingBoxes(player,bb2).isEmpty();
                    
                    List l = mc.theWorld.getCollidingBoundingBoxes(player,bb2);
                  
                    Vec3 dest;
                    
                    //teleport to exact spot unless collision, then teleport to center.
                    
                    if (emptySpotReq) {           	
                    	dest = Vec3.createVectorHelper(hitVec.xCoord, by+1,hitVec.zCoord);
                    }
                   else {
                    	dest = Vec3.createVectorHelper(bx + 0.5f, by + 1f, bz + 0.5f);
                    }
                            
                   //System.out.println(dest.toString() + " " + emptySpotReq + " " + emptySpotCenter);
//                    
//          //          System.out.println(hitVec.xCoord + " " + hitVec.yCoord + " " + hitVec.zCoord + " " +emptySpotCenter + " " + emptySpotReq + " " +bx + " " + by + " " + bz + bb.minX + " " + bb.minY + " " + bb.minZ);
//                    for (Object li : l) {
//                    	   System.out.println(li.toString());
//					}
;                    


                    if (start.distanceTo(dest) <= maxTeleportDist && (emptySpotReq || emptySpotCenter))
                    {
                   	
                     	
                        movementTeleportDestination.xCoord = dest.xCoord;
                        movementTeleportDestination.yCoord = dest.yCoord;
                        movementTeleportDestination.zCoord = dest.zCoord;
                        movementTeleportDestinationSideHit = collision.sideHit;

                        debugPos.xCoord = bx + 0.5;
                        debugPos.yCoord = by + 1;
                        debugPos.zCoord = bz + 0.5;

                        bFoundValidSpot = true;
                 
                        break;
                        
                      }
                
                }
            }
        }
        
        if(bFoundValidSpot) { movementTeleportDistance = start.distanceTo(movementTeleportDestination);}
        
        return bFoundValidSpot;
    }

    private boolean canStand(World w, int bx, int by, int bz){
    	
    	return w.getBlock(bx,  by,  bz).isCollidable() && w.getBlock(bx,  by+1,  bz).isPassable(w, bx, by+1, bz) &&  w.getBlock(bx,  by+2,  bz).isPassable(w, bx, by+2, bz);
 
    }
    
    // rough interpolation between arc locations
    public Vec3 getInterpolatedArcPosition(float progress)
    {
        // not enough points to interpolate or before start
        if (movementTeleportArcSteps == 1 || progress <= 0.0f)
        {
            return Vec3.createVectorHelper(
                    movementTeleportArc[0].xCoord,
                    movementTeleportArc[0].yCoord,
                    movementTeleportArc[0].zCoord);
        }

        // past end of arc
        if (progress>=1.0f)
        {
            return Vec3.createVectorHelper(
                    movementTeleportArc[movementTeleportArcSteps-1].xCoord,
                    movementTeleportArc[movementTeleportArcSteps-1].yCoord,
                    movementTeleportArc[movementTeleportArcSteps-1].zCoord);
        }

        // which two points are we between?
        float stepFloat = progress * (float)(movementTeleportArcSteps - 1);
        int step = (int) Math.floor(stepFloat);

        double deltaX = movementTeleportArc[step+1].xCoord - movementTeleportArc[step].xCoord;
        double deltaY = movementTeleportArc[step+1].yCoord - movementTeleportArc[step].yCoord;
        double deltaZ = movementTeleportArc[step+1].zCoord - movementTeleportArc[step].zCoord;

        float stepProgress = stepFloat - step;

        return Vec3.createVectorHelper(
                movementTeleportArc[step].xCoord + deltaX * stepProgress,
                movementTeleportArc[step].yCoord + deltaY * stepProgress,
                movementTeleportArc[step].zCoord + deltaZ * stepProgress);
    }


    public Vec3 getTeleportSlide()
    {
        if (!vrMovementStyle.cameraSlide)
        {
            return Vec3.createVectorHelper(0,0,0);
        }
        long delta = System.nanoTime() - this.teleportSlideStartTime;
        float speed = 400.0f;
        double slideTime = (double)delta / (double)1000000000L;

        Vec3 dir = teleportSlide.normalize();
        dir.xCoord = -dir.xCoord * slideTime * speed + teleportSlide.xCoord;
        dir.yCoord = -dir.yCoord * slideTime * speed + teleportSlide.yCoord;
        dir.zCoord = -dir.zCoord * slideTime * speed + teleportSlide.zCoord;
        if (dir.dotProduct(teleportSlide)<=0)
        {
            dir.xCoord = dir.yCoord = dir.zCoord = 0;
        }
        return dir;
    }

    private Vec3 lastWeaponEndAir = Vec3.createVectorHelper(0,0,0);
    private boolean lastWeaponSolid = false;

    public void updateSwingAttack()
    {
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;

        if (!mc.vrSettings.weaponCollision)
            return;

        mc.mcProfiler.startSection("updateSwingAttack");

        Vec3 handPos = mc.lookaimController.getAimSource(0);
        Matrix4f handRotation = mc.lookaimController.getAimRotation(0);
        Vector3f forward = new Vector3f(0,0,1);
        Vector3f handDirection = handRotation.transform(forward);
        float weaponLength = 0.3f;
        Vec3 weaponEnd = Vec3.createVectorHelper(
                handPos.xCoord + handDirection.x * weaponLength,
                handPos.yCoord - handDirection.y * weaponLength,
                handPos.zCoord + handDirection.z * weaponLength);

        Vec3 velocity = mc.lookaimController.getSmoothedAimVelocity(0);
        float speed = (float)velocity.lengthVector();
        Vec3 dir = velocity.normalize();
        Vec3 traceTo = Vec3.createVectorHelper(
                weaponEnd.xCoord + dir.xCoord * 0.1f,
                weaponEnd.yCoord + dir.yCoord * 0.1f,
                weaponEnd.zCoord + dir.zCoord * 0.1f);

        int bx = (int) MathHelper.floor_double(weaponEnd.xCoord);
        int by = (int) MathHelper.floor_double(weaponEnd.yCoord);
        int bz = (int) MathHelper.floor_double(weaponEnd.zCoord);

        boolean hitAnEntity = false;
        Material material = mc.theWorld.getBlock(bx, by, bz).getMaterial();
        if (material != Material.air)
        {
            // every time end of weapon enters a solid for the first time, trace from our previous air position
            // and damage the block it collides with
            if (speed >= 1.5f && !lastWeaponSolid)
            {
                MovingObjectPosition col = mc.theWorld.rayTraceBlocks(lastWeaponEndAir, weaponEnd, false, true, true);
                if (col != null && col.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
                {
                    Block block = mc.theWorld.getBlock(col.blockX, col.blockY, col.blockZ);
                    if (block.getMaterial() != Material.air)
                    {
                        float hardness = block.getPlayerRelativeBlockHardness(mc.thePlayer, mc.thePlayer.worldObj, col.blockX, col.blockY, col.blockZ);
                        System.out.println("hardness=" + hardness);
                        if (hardness * 4.0f > 1.0f)
                        {
                            mc.playerController.onPlayerDestroyBlock(col.blockX, col.blockY, col.blockZ, col.sideHit);
                        } else
                        {
                            mc.playerController.clearBlockHitDelay();
                            for (int i = 0; i < 4; i++)
                            {
                                mc.playerController.onPlayerDamageBlock(col.blockX, col.blockY, col.blockZ, col.sideHit);
                            }
                        }
                        lastWeaponSolid = true;
                        mc.lookaimController.triggerHapticPulse(0, 1000);
                        System.out.println("Hit block speed =" + speed);
                    }
                }
            }
        }
        else
        {
            float bbSize = 0.1f;
            AxisAlignedBB weaponBB = AxisAlignedBB.getBoundingBox(
                    weaponEnd.xCoord - bbSize,
                    weaponEnd.yCoord - bbSize,
                    weaponEnd.zCoord - bbSize,
                    weaponEnd.xCoord + bbSize,
                    weaponEnd.yCoord + bbSize,
                    weaponEnd.zCoord + bbSize
            );
            List entities = mc.theWorld.getEntitiesWithinAABBExcludingEntity(
                    mc.renderViewEntity, weaponBB);
            for (int e = 0; e < entities.size(); ++e)
            {
                Entity hitEntity = (Entity) entities.get(e);
                if (hitEntity.canBeCollidedWith())
                {
                    float speedThreshold = 1.5f;
                    boolean monster = (hitEntity instanceof EntityMob);
                    if (!monster)
                        speedThreshold = 2.5f;
                    if (speed>=speedThreshold && !lastWeaponSolid)
                    {
                        //player.worldObj.spawnParticle("reddust", weaponEnd.xCoord, weaponEnd.yCoord, weaponEnd.zCoord, 0, 0.1, 0);

  //                      System.out.println("Sword hit entity " + hitEntity);
                        mc.playerController.attackEntity(player, hitEntity);
                        mc.lookaimController.triggerHapticPulse(0, 1000);
                        lastWeaponSolid = true;
                    }

                }
                hitAnEntity = true;
            }
        }

        if (!hitAnEntity && material == Material.air)
        {
            lastWeaponEndAir.xCoord = weaponEnd.xCoord;
            lastWeaponEndAir.yCoord = weaponEnd.yCoord;
            lastWeaponEndAir.zCoord = weaponEnd.zCoord;
            lastWeaponSolid = false;
        }
        mc.mcProfiler.endSection();
    }
	
	public boolean getFreeMoveMode() { return restrictedViveClient; }
	
	public void setFreeMoveMode(boolean free) { 
		restrictedViveClient = free;
		if(Minecraft.getMinecraft().thePlayer != null) {
			Minecraft.getMinecraft().thePlayer.setPosition(Minecraft.getMinecraft().thePlayer.posX, Minecraft.getMinecraft().thePlayer.posY, Minecraft.getMinecraft().thePlayer.posZ); //reset room origin on mode change.
		}		
		}
	

	public float getTeleportEnergy () {return teleportEnergy;}
	
}
