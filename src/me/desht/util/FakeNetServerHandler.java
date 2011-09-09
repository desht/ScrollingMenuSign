package me.desht.util;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.NetServerHandler;
import net.minecraft.server.NetworkManager;
import net.minecraft.server.Packet;
import net.minecraft.server.Packet10Flying;
import net.minecraft.server.Packet3Chat;

public class FakeNetServerHandler extends NetServerHandler {
	public FakeNetServerHandler(MinecraftServer minecraftserver, NetworkManager networkmanager, EntityPlayer entityplayer)
	{
		super(minecraftserver, networkmanager, entityplayer);
	}

	//In space, no one can hear you scream.
//	@Override
//	public void sendPacket(Packet packet)
//	{
//		//For real, don't say ANYTHING to the fake player.
//		if (!(packet instanceof Packet3Chat))
//			super.sendPacket(packet);
//	}

	//I believe I can fly!
	@Override
	public void a(Packet10Flying packet10flying) {
		//Don't do anything here.  ESPECIALLY don't call super.a; you'll get kicked.
	}

}
