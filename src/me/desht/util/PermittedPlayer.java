package me.desht.util;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.ItemInWorldManager;
import net.minecraft.server.NetServerHandler;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

/**
 * Just like a regular player, but the hasPermission() method always returns
 * true, which means this sort of player will always be able to do stuff with
 * plugins which use superperms.
 * 
 * @author desht
 *
 */
public class PermittedPlayer extends CraftPlayer {

	/**
	 * Convenience method.  Create a new PermittedPlayer from the given Player.
	 * 
	 * @param player	The player to use
	 * @return			The permitted player
	 */
	public static PermittedPlayer fromPlayer(Player player) {
		CraftServer cServer = (CraftServer) Bukkit.getServer();
		CraftWorld cWorld = (CraftWorld) player.getWorld();
		EntityPlayer fakeEntityPlayer = new EntityPlayer(cServer.getHandle().server,
		                                                 cWorld.getHandle(), player.getName(), new ItemInWorldManager(cWorld.getHandle()));
		NetServerHandler playerNSH = ((CraftPlayer)player).getHandle().netServerHandler;
		FakeNetServerHandler fakeNSH = new FakeNetServerHandler(cServer.getServer(), playerNSH.networkManager, fakeEntityPlayer);
		playerNSH.networkManager.a(playerNSH);
        fakeEntityPlayer.netServerHandler = fakeNSH;

		return new PermittedPlayer(cServer, fakeEntityPlayer);
	}

	public PermittedPlayer(CraftServer server, EntityPlayer entity) {
		super(server, entity);
	}

	@Override
	public boolean hasPermission(String perm) {
		System.out.println("haspermission str: " + perm);
		return true;
	}
	
	@Override
	public boolean hasPermission(Permission perm) {
		System.out.println("haspermission perm: " + perm.getName());
		return true;
	}
	
}
