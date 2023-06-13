package org.mineacademy.orion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.mineacademy.fo.Common;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.Arrays;
import java.util.List;

public class PlayerListener implements Listener {

	private static final List<String> worlds = Arrays.asList("plai_one", "plai_two", "plai_three");  // replace with your world names

	@EventHandler
	public void onJoin(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();

		// Check if the player is in the predefined list and the current group is less than the group size
		if (AutoJoin.predefinedNames.contains(player.getName()) && AutoJoin.currentGroup.size() < AutoJoin.GROUP_SIZE) {

			AutoJoin.currentGroup.add(player);
			Common.tellLater(2, player, "§b You've been automatically assigned a group!");
			Common.tellLater(2, player, "§b Teleportation will commence once there are enough players.");

			// When a group of GROUP_SIZE players is formed
			if (AutoJoin.currentGroup.size() == AutoJoin.GROUP_SIZE) {
				teleportGroup();
			}
		}
	}

	private void teleportGroup() {
		Bukkit.getScheduler().runTaskLater(AutoJoin.getInstance(), new Runnable() {
			@Override
			public void run() {
				final World world = Bukkit.getWorld(worlds.get(AutoJoin.random.nextInt(worlds.size())));

				// Generate random X and Z coordinates within a certain range
				int x = AutoJoin.random.nextInt(1000) - 500;  // Value will be between -500 and 500
				final int z = AutoJoin.random.nextInt(1000) - 500;  // Value will be between -500 and 500

				// Teleport all players in the group to this location and set it as their respawn point
				for (final Player groupPlayer : AutoJoin.currentGroup) {
					// Get the highest Y value at that X, Z coordinate
					final int y = world.getHighestBlockYAt(x, z);

					// Create a location object with these coordinates
					final Location randomLocation = new Location(world, x, y, z);

					groupPlayer.teleport(randomLocation);
					groupPlayer.setBedSpawnLocation(randomLocation, true);
					AutoJoin.respawnLocations.put(groupPlayer.getName(), randomLocation);

					// Apply a blindness effect to the player for 5 seconds
					groupPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 1));

					// Show a title to the player
					groupPlayer.sendTitle("§6Type /Audio if you haven't!", "", 10, 70, 20);

					// Play a loud entity sound
					groupPlayer.playSound(groupPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);

					x += 1;  // Increment the x coordinate for next player
				}

				// Then clear the current group
				AutoJoin.currentGroup.clear();
			}
		}, 20L);  // 20 ticks = 1 second
	}


	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		World world = player.getWorld();

		// Check if the player is in one of the specified worlds
		if (worlds.contains(world.getName())) {
			Bukkit.getScheduler().runTaskLater(AutoJoin.getInstance(), new Runnable() {
				@Override
				public void run() {
					player.spigot().respawn();
				}
			}, 1L);
		}
	}

	@EventHandler
	public void onPluginDisable(PluginDisableEvent event) {
		if (event.getPlugin().equals(AutoJoin.getInstance())) {
			AutoJoin.currentGroup.clear();
		}
	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		// Check if the respawn event is due to a player death and not due to using a bed or /kill command
		if (!event.isBedSpawn() && AutoJoin.respawnLocations.containsKey(player.getName())) {
			event.setRespawnLocation(AutoJoin.respawnLocations.get(player.getName()));

			// Show a title to the player
			player.sendTitle("§cYou've been reborn!", "", 10, 70, 20);

			// Apply a blindness effect to the player for 5 seconds
			boolean blindnessAdded = player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));
			if (!blindnessAdded) {
				AutoJoin.getInstance().getLogger().info("Could not apply blindness effect to the player");
			}

			// Play a loud entity sound
			player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 5.0f, 1.0f);
			if (!player.getWorld().getPlayers().contains(player)) {
				AutoJoin.getInstance().getLogger().info("Player not found in the world when trying to play the sound");
			}
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		// Remove player from the current group if they are in it
		if (AutoJoin.currentGroup.contains(player)) {
			AutoJoin.currentGroup.remove(player);
		}
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		Player player = event.getPlayer();

		// Remove player from the current group if they are in it
		if (AutoJoin.currentGroup.contains(player)) {
			AutoJoin.currentGroup.remove(player);
		}
	}
}



