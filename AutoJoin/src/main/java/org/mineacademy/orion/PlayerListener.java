package org.mineacademy.orion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.mineacademy.fo.Common;
import java.util.Arrays;
import java.util.List;


public class PlayerListener implements Listener {

	private static final List<String> worlds = Arrays.asList("plai_one", "plai_two", "plai_three");  // replace with your world names

	@EventHandler
	public void onJoin(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();

		// Check if the player is in the predefined list
		if (AutoJoin.predefinedNames.contains(player.getName())) {
			AutoJoin.currentGroup.add(player);
			Common.tellLater(2, player, "You've been automatically assigned a group!");
			Common.tellLater(2, player, "Teleportation will commence once player limit reached.");

			// When a group of 2 players is formed
			if (AutoJoin.currentGroup.size() == AutoJoin.GROUP_SIZE) {

				final World world = Bukkit.getWorld(worlds.get(AutoJoin.random.nextInt(worlds.size())));

				// Generate random X and Z coordinates within a certain range
				final int x = AutoJoin.random.nextInt(1000) - 500;  // Value will be between -500 and 500
				final int z = AutoJoin.random.nextInt(1000) - 500;  // Value will be between -500 and 500

				// Get the highest Y value at that X, Z coordinate
				final int y = world.getHighestBlockYAt(x, z);

				// Create a location object with these coordinates
				final Location randomLocation = new Location(world, x, y, z);

				// Teleport all players in the group to this location
				for (final Player groupPlayer : AutoJoin.currentGroup) {
					groupPlayer.teleport(randomLocation);
				}

				// Then clear the current group
				AutoJoin.currentGroup.clear();
			}
		}
	}
}
