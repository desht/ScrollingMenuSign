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
public class FakePlayer extends CraftPlayer {
	
	/**
	 * Convenience method.  Create a new PermittedPlayer from the given Player.
	 * 
	 * @param player	The player to use
	 * @return			The fake player
	 */
	public static FakePlayer fromPlayer(Player player) {
		return fromPlayer(player, player.getName());
	}

	/**
	 * Convenience method.  Create a new PermittedPlayer from the given Player.
	 * 
	 * @param player	The player to use
	 * @param fakeName	The name for the fake player
	 * @return			The fake player
	 */
	public static FakePlayer fromPlayer(Player player, String fakeName) {	
		CraftServer cServer = (CraftServer) Bukkit.getServer();
		CraftWorld cWorld = (CraftWorld) player.getWorld();
		EntityPlayer fakeEntityPlayer = new EntityPlayer(cServer.getHandle().server,
		                                                 cWorld.getHandle(), fakeName, new ItemInWorldManager(cWorld.getHandle()));
		NetServerHandler playerNSH = ((CraftPlayer)player).getHandle().netServerHandler;
		FakeNetServerHandler fakeNSH = new FakeNetServerHandler(cServer.getServer(), playerNSH.networkManager, fakeEntityPlayer);
		playerNSH.networkManager.a(playerNSH);
        fakeEntityPlayer.netServerHandler = fakeNSH;

		return new FakePlayer(cServer, fakeEntityPlayer);
	}

	public FakePlayer(CraftServer server, EntityPlayer entity) {
		super(server, entity);
	}

	@Override
	public boolean hasPermission(String perm) {
		return true;
	}
	
	@Override
	public boolean hasPermission(Permission perm) {
		return true;
	}

}
