package com.mtbs3d.minecrift.gameplay;

import com.mtbs3d.minecrift.provider.MCOpenVR;

import de.fruitfly.ovr.structs.Matrix4f;
import de.fruitfly.ovr.structs.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;

public class BowTracker {

	
	private double lastcontrollersDist;
	private double lastcontrollersDot;	
	private double controllersDist;
	private double controllersDot;
	private double currentDraw;
	private double lastDraw;
	public boolean isDrawing; 
	private boolean pressed, lastpressed;	
	
	private boolean canDraw, lastcanDraw;
	
	
	private Vec3 leftHandAim;
	
	private final double notchDistThreshold = 0.3;
	private final double notchDotThreshold = 10;
	private double maxDraw = .7;

	private Vec3 aim;
	
	public Vec3 getAimVector(){
		return aim;
//		if(isDrawing)return aim;
//		return leftHandAim;
	}
		
	public double getDrawPercent(){
		return currentDraw / maxDraw;	
	}
	
	public boolean isNotched(){
		return canDraw || isDrawing;	
	}
	
	public boolean isActive(EntityPlayerSP p){
		if(p == null) return false;
		if(p.isDead) return false;
		if(p.isPlayerSleeping()) return false;
		if(p.inventory == null) return false;
		if(p.inventory.getCurrentItem() == null) return false;
		return	p.inventory.getCurrentItem().getItem() instanceof ItemBow;
	}
	
	float tsNotch = 0;
	
	int hapcounter = 0;
	
	public void doProcess(MCOpenVR provider, EntityPlayerSP player){

		if (!isActive(player)){			
			isDrawing = false;
			return;
		}

		ItemStack bow = player.inventory.getCurrentItem();

		lastcontrollersDist = controllersDist;
		lastcontrollersDot = controllersDot;
		lastpressed = pressed;
		lastDraw = currentDraw;
		lastcanDraw = canDraw;
		maxDraw = Minecraft.getMinecraft().thePlayer.height * 0.25;

		Vec3 rightPos = provider.getAimSource(0);
		Vec3 leftPos = provider.getAimSource(1);
		controllersDist = leftPos.distanceTo(rightPos);
		
		aim = rightPos.subtract(leftPos).normalize();

		Vector3f forward = new Vector3f(0,0,1);

		Matrix4f rv4 = provider.getAimRotation(0);
		Vector3f rightAim = rv4.transform(forward);

		Matrix4f lv4 = provider.getAimRotation(1);
		Vector3f leftAim = lv4.transform(forward);
		Vector3f leftforeward = lv4.transform(new Vector3f(0,1,0));

		leftHandAim = Vec3.createVectorHelper(leftAim.x, leftAim.y, leftAim.z);

		controllersDot = 180 / Math.PI * Math.acos(leftforeward.dot(rightAim));

		pressed = Minecraft.getMinecraft().gameSettings.keyBindAttack.getIsKeyPressed();

		boolean infiniteAmmo = player.capabilities.isCreativeMode || EnchantmentHelper.getEnchantmentLevel(Enchantment.infinity.effectId, bow) > 0;

		if( controllersDist <= notchDistThreshold && controllersDot <= notchDotThreshold && (infiniteAmmo || player.inventory.hasItem(Items.arrow)))
		{
			//can draw
			canDraw = true;
			tsNotch = Minecraft.getSystemTime();
			
			if(!isDrawing){
				player.itemInUse = bow;
				player.itemInUseCount = bow.getMaxItemUseDuration() - 1 ;				
			}

		} else if((Minecraft.getSystemTime() - tsNotch) > 500) {
			canDraw = false;
			player.itemInUse = null; //client draw only
		}
			
		if (!isDrawing && canDraw  && pressed && !lastpressed) {
			//draw     	    	
			isDrawing = true;
			Minecraft.getMinecraft().playerController.sendUseItem(player, player.worldObj, bow);//server
		}

		if(isDrawing && !pressed && lastpressed && getDrawPercent() > 0.0) {
			//fire!
			provider.triggerHapticPulse(0, 500); 	
			provider.triggerHapticPulse(1, 3000); 	
			Minecraft.getMinecraft().playerController.onStoppedUsingItem(player); //server
			isDrawing = false;     	
		}
		
		if(!pressed){
			isDrawing = false;
		}
		
		if (!isDrawing && canDraw && !lastcanDraw) {
			provider.triggerHapticPulse(1, 800);
			provider.triggerHapticPulse(0, 800); 	
			//notch     	    	
		}
		
		if(isDrawing){
			currentDraw = controllersDist - notchDistThreshold ;
			if (currentDraw > maxDraw) currentDraw = maxDraw;		
			
			int hap = 0;
			if (getDrawPercent() > 0 ) hap = (int) (getDrawPercent() * 1000)+ 200;
		
			int use = (int) (bow.getMaxItemUseDuration() - getDrawPercent() * bow.getMaxItemUseDuration());
			if	(use >= bow.getMaxItemUseDuration()) use = bow.getMaxItemUseDuration() -1;
			player.itemInUse = bow;;//client draw only
			player.itemInUseCount = use -1; //do this cause the above doesnt set the counts if same item.
			hapcounter ++ ;
			if (hapcounter % 4 == 0)
				provider.triggerHapticPulse(0, hap);     


		} else {
			hapcounter = 0;
		}


	}
	
	
	
	
	
}

