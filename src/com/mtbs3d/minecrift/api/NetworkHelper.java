package com.mtbs3d.minecrift.api;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Charsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.util.Vec3;

public class NetworkHelper {

	public enum PacketDiscriminators {
		VERSION,
		POSITIONS,
		DAMAGE	
	}

	private final static String channel = "Vivecraft";
	
	public static C17PacketCustomPayload getVivecraftClientPacket(PacketDiscriminators command, byte[] payload)
	{
		payload = ArrayUtils.add(payload,0,(byte)command.ordinal());
        return (new C17PacketCustomPayload(channel, payload));
	}
	
	public static S3FPacketCustomPayload getVivecraftServerPacket(PacketDiscriminators command, byte[] payload)
	{
		payload = ArrayUtils.add(payload,0,(byte)command.ordinal());
        return (new S3FPacketCustomPayload(channel, payload));
	}
	
	
	public static void sendVRPlayerPositions(IRoomscaleAdapter player) {
		
//		ByteBuf buffer = Unpooled.buffer();
//		
//		for(int i=0; i < 2; i++){
//			Vec3 c =	player.getControllerPos_World(i);	
//			buffwr.writeFloat((float) c.xCoord);
//		}
//		
//		buffer.writeByte(id | (handsSwapped ? 0x80 : 0));
//		buffer.writeFloat((float)position.xCoord);
//		buffer.writeFloat((float)position.yCoord);
//		buffer.writeFloat((float)position.zCoord);
//		buffer.writeFloat(rotW);
//		buffer.writeFloat(rotX);
//		buffer.writeFloat(rotY);
//		buffer.writeFloat(rotZ);
		
		
	}
	
}
