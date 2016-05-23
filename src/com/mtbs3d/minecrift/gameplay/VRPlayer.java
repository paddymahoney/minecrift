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
import net.minecraft.client.particle.EntityVRTeleportFX;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;

import java.util.List;
import java.util.Random;

// VIVE
public class VRPlayer
{
    public double lastRoomUpdateTime = 0;
    public Vec3 movementTeleportDestination = Vec3.createVectorHelper(0.0,0.0,0.0);
    public int movementTeleportDestinationSideHit;
    public double movementTeleportProgress;
    public Vec3 roomOrigin = Vec3.createVectorHelper(0,0,0);
    public Vec3 teleportSlide = Vec3.createVectorHelper(0,0,0);
    public long teleportSlideStartTime = 0;
    public VRMovementStyle vrMovementStyle = new VRMovementStyle();
    public Vec3[] movementTeleportArc = new Vec3[50];
    public int movementTeleportArcSteps = 0;
    public boolean restrictedViveClient = true;        // true when connected to another server that doesn't have this mod
	public boolean useLControllerForRestricedMovement = false;
    public double lastTeleportArcDisplayOffset = 0;
    public double fallTime = 0;

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

    public void teleportRoomOrigin(double x, double y, double z)
    {
        if (Thread.currentThread().getName().equals("Server thread"))
            return;

        Minecraft mc = Minecraft.getMinecraft();
        boolean bStartingUp = (roomOrigin.xCoord==0 && roomOrigin.yCoord==0 && roomOrigin.zCoord==0);
        boolean bRestricted = !bStartingUp && restrictedViveClient;

        if (mc.positionTracker == null || !mc.positionTracker.isInitialized() || bRestricted)
        {
            if (lastRoomUpdateTime==0
                    || mc.stereoProvider.getCurrentTimeSecs() - lastRoomUpdateTime >= mc.vrSettings.restrictedCameraUpdateInterval)
            {
                roomOrigin.xCoord = x;
                roomOrigin.yCoord = y - 1.62f;
                roomOrigin.zCoord = z;
                lastRoomUpdateTime = mc.stereoProvider.getCurrentTimeSecs();
            }
        }
        else
        {
            teleportSlide.xCoord = 0;
            teleportSlide.yCoord = 0;
            teleportSlide.zCoord = 0;
            Vec3 hmdOffset = mc.positionTracker.getCenterEyePosition();
            double newX = x + hmdOffset.xCoord;
            double newY = y - 1.62f;
            double newZ = z + hmdOffset.zCoord;
            teleportSlide.xCoord = roomOrigin.xCoord - newX;
            teleportSlide.yCoord = roomOrigin.yCoord - newY;
            teleportSlide.zCoord = roomOrigin.zCoord - newZ;
            roomOrigin.xCoord = newX;
            roomOrigin.yCoord = newY;
            roomOrigin.zCoord = newZ;
            teleportSlideStartTime = System.nanoTime();
        }
    }

    public void onLivingUpdate(EntityPlayerSP player, Minecraft mc, Random rand)
    {
        updateSwingAttack();
		
        // don't do teleport movement if on a server that doesn't have this mod installed
        if (restrictedViveClient) {
			  return; //let mc handle look direction movement
			// controller vs gaze movement is handled in Entity.java > moveFlying
          }

					
        mc.mcProfiler.startSection("VRPlayerOnLivingUpdate");

        boolean doTeleport = false;
        Vec3 dest = null;

        if (player.movementInput.moveForward != 0)
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
        else
        {
            if (vrMovementStyle.teleportOnRelease && movementTeleportProgress>=1.0f)
            {
                dest = movementTeleportDestination;
                doTeleport = true;
            }
            player.movementTeleportTimer = 0;
            movementTeleportProgress = 0;
        }

        if (doTeleport && dest!=null && (dest.xCoord != 0 || dest.yCoord !=0 || dest.zCoord != 0))
        {
            float teleportDistance = (float)MathHelper.sqrt_double(dest.squareDistanceTo(player.posX, player.posY, player.posZ));
            boolean playTeleportSound = teleportDistance > 0.0f && vrMovementStyle.endTeleportingSound != null;
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

            player.setPositionAndUpdate(dest.xCoord, dest.yCoord, dest.zCoord);
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
        else
        {
            float playerHalfWidth = player.width / 2.0F;
            if (!restrictedViveClient)
            {
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
                    player.lastTickPosY = player.prevPosY = player.posY = y;
                    player.lastTickPosZ = player.prevPosZ = player.posZ = z;
                    player.boundingBox.setBounds(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
                    player.fallDistance = 0.0F;

                    torso = getEstimatedTorsoPosition(x, y, z);

                    // test falling
                    if (mc.vrSettings.simulateFalling && mc.playerController.isNotCreative())
                    {
                        float fallPadding = player.width * 0.0f;

                        double paddedFallHalfWidth = playerHalfWidth + fallPadding;
                        AxisAlignedBB bbFall = AxisAlignedBB.getBoundingBox(
                                torso.xCoord - paddedFallHalfWidth,
                                bb.minY,
                                torso.zCoord - paddedFallHalfWidth,
                                torso.xCoord + paddedFallHalfWidth,
                                bb.maxY,
                                torso.zCoord + paddedFallHalfWidth);

                        int fallCount = 0;
                        while (emptySpot && fallCount < 32)
                        {
                            bbFall.maxY -= 1.0f;
                            bbFall.minY -= 1.0f;
                            emptySpot = mc.theWorld.getCollidingBoundingBoxes(player, bbFall).isEmpty();
                            fallCount++;
                        }
                        if (fallCount > 1)
                        {
                            if (mc.stereoProvider.getCurrentTimeSecs() >= fallTime)
                            {
                                double xOffset = torso.xCoord - x;
                                double zOffset = torso.zCoord - z;

                                float fallDist = 1.0f * (fallCount - 1);
                                x += xOffset;
                                y -= fallDist;
                                z += zOffset;
                                bb.minX += xOffset;
                                bb.maxX += xOffset;
                                bb.minY -= fallDist;
                                bb.maxY -= fallDist;
                                bb.minZ += zOffset;
                                bb.maxZ += zOffset;
                                player.lastTickPosX = player.prevPosX = player.posX = x;
                                player.lastTickPosY = player.prevPosY = player.posY = y;
                                player.lastTickPosZ = player.prevPosZ = player.posZ = z;
                                player.boundingBox.setBounds(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
                                player.fallDistance = fallDist;
                                roomOrigin.xCoord += xOffset;
                                roomOrigin.yCoord -= fallDist;
                                roomOrigin.zCoord += zOffset;

                                playFootstepSound(mc, player.posX, player.posY-1.62f, player.posZ);
                            }
                        }
                        else
                        {
                            fallTime = mc.stereoProvider.getCurrentTimeSecs() + 0.25;
                        }
                    }
                }

                // test for climbing up a block
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
        }
        mc.mcProfiler.endSection();
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
        float pitch = (float)Math.atan2(tiltedAim.yCoord, horizontalAimDist);
        if (pitch > (float)Math.PI * 0.25f)
        {
            maxSteps = 4;
        }

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
            newPos.xCoord = pos.xCoord + velocity.xCoord;
            newPos.yCoord = pos.yCoord + velocity.yCoord;
            newPos.zCoord = pos.zCoord + velocity.zCoord;

            MovingObjectPosition collision = mc.theWorld.rayTraceBlocks(pos, newPos, false, true, true);
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
            MovingObjectPosition collision = mc.theWorld.rayTraceBlocks(start, movementTeleportPos, true, false, true);
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

		if (!(mc.thePlayer.capabilities.allowFlying) && collision.sideHit != 1 && vrMovementStyle.arcAiming) {
		//jrbudda require arc hitting top of block.	unless ladder or vine.
			Block testClimb = player.worldObj.getBlock(collision.blockX, collision.blockY, collision.blockZ);
			if(!( testClimb == Blocks.ladder || testClimb == Blocks.vine)) return false;
		}
		
        for ( int k = 0; k < 2 && !bFoundValidSpot; k++ )
        {
            Vec3 hitVec = ( k == 0 ) ? collision.hitVec.addVector(-reverseEpsilon.xCoord, -reverseEpsilon.yCoord, -reverseEpsilon.zCoord)
                    : collision.hitVec.addVector(reverseEpsilon.xCoord, reverseEpsilon.yCoord, reverseEpsilon.zCoord);

            Vec3 debugPos = Vec3.createVectorHelper(
                    MathHelper.floor_double(hitVec.xCoord) + 0.5,
                    MathHelper.floor_double(hitVec.yCoord),
                    MathHelper.floor_double(hitVec.zCoord) + 0.5);

            int bx = (int) MathHelper.floor_double(hitVec.xCoord);
            int bz = (int) MathHelper.floor_double(hitVec.zCoord);

            // search for a solid block with two empty blocks above it
            int startBlockY = MathHelper.floor_double(hitVec.yCoord) - 1;
            startBlockY = Math.max(startBlockY, 0);
            for (int by = startBlockY; by < startBlockY + 2; by++)
            {
                Block testPos = player.worldObj.getBlock(bx, by, bz);
                Block testPos2 = player.worldObj.getBlock(bx, by + 1, bz);
                Block testPos3 = player.worldObj.getBlock(bx, by + 2, bz);

                boolean bSolid = testPos!=null && !testPos.isPassable(player.worldObj, bx, by, bz);
                boolean bSolid2 = testPos2!=null && !testPos2.isPassable(player.worldObj, bx, by + 1, bz);
                boolean bSolid3 = testPos3!=null && !testPos3.isPassable(player.worldObj, bx, by + 2, bz);

                if (bSolid && !bSolid2 && !bSolid3)
                {
                    float maxTeleportDist = 16.0f;

                    float var27 = 0.0625F;
                    boolean emptySpot = mc.theWorld.getCollidingBoundingBoxes(player, player.boundingBox.copy().contract((double)var27, (double)var27, (double)var27)).isEmpty();
                    Vec3 dest = Vec3.createVectorHelper(bx+0.5, by+1, bz+0.5);
                    if (start.distanceTo(dest) <= maxTeleportDist && emptySpot)
                    {
                        movementTeleportDestination.xCoord = dest.xCoord;
                        movementTeleportDestination.yCoord = dest.yCoord;
                        movementTeleportDestination.zCoord = dest.zCoord;
                        movementTeleportDestinationSideHit = collision.sideHit;

                        debugPos.xCoord = bx + 0.5;
                        debugPos.yCoord = by + 1;
                        debugPos.zCoord = bz + 0.5;

                        bFoundValidSpot = true;
                    }

                    break;
                }
            }
        }
        return bFoundValidSpot;
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

                        System.out.println("Sword hit entity " + hitEntity);
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
}
