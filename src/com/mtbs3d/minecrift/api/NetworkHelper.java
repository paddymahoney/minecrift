package com.mtbs3d.minecrift.api;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Charsets;

import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

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
}
