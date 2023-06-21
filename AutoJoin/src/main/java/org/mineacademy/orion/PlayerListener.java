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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class PlayerListener implements Listener {

	private static final List<String> worlds = Arrays.asList("plai_one", "plai_two", "plai_three");  // replace with your world names
	private static final List<String> skins = Arrays.asList("SkyTheKidRS", "Herobrine", "CaptainSparklez", "Notch");

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
		Bukkit.getScheduler().runTaskLater(AutoJoin.getInstance(), () -> {
			final World world = Bukkit.getWorld(worlds.get(AutoJoin.random.nextInt(worlds.size())));

			int x = AutoJoin.random.nextInt(1000) - 500;
			final int z = AutoJoin.random.nextInt(1000) - 500;

			for (final Player groupPlayer : AutoJoin.currentGroup) {
				final int y = world.getHighestBlockYAt(x, z);

				final Location randomLocation = new Location(world, x, y, z);

				groupPlayer.teleport(randomLocation);
				groupPlayer.setBedSpawnLocation(randomLocation, true);
				AutoJoin.respawnLocations.put(groupPlayer.getName(), randomLocation);

				groupPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 1));

				groupPlayer.sendTitle("§6Type /Audio if you haven't!", getCurrentUTCTime(), 10, 70, 20);

				groupPlayer.playSound(groupPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);

				String skin = skins.get(AutoJoin.random.nextInt(skins.size()));

				Bukkit.getScheduler().runTask(AutoJoin.getInstance(), () -> {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "skin set " + groupPlayer.getName() + " " + skin);
				});

				x += 1;
			}

			AutoJoin.currentGroup.clear();
		}, 20L);
	}

	private String generateRandomAlphanumericString() {
		String alphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < 5; i++) {
			int character = (int)(Math.random() * alphaNumericString.length());
			builder.append(alphaNumericString.charAt(character));
		}
		return builder.toString();
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		World world = player.getWorld();

		if (worlds.contains(world.getName())) {
			Bukkit.getScheduler().runTaskLater(AutoJoin.getInstance(), () -> player.spigot().respawn(), 1L);
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
		if (!event.isBedSpawn() && AutoJoin.respawnLocations.containsKey(player.getName())) {
			event.setRespawnLocation(AutoJoin.respawnLocations.get(player.getName()));

			player.sendTitle("§cYou've been reborn!", getCurrentUTCTime(), 10, 70, 20);

			boolean blindnessAdded = player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));
			if (!blindnessAdded) {
				AutoJoin.getInstance().getLogger().info("Could not apply blindness effect to the player");
			}

			player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 5.0f, 1.0f);
			if (!player.getWorld().getPlayers().contains(player)) {
				AutoJoin.getInstance().getLogger().info("Player not found in the world when trying to play the sound");
			}
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		if (AutoJoin.currentGroup.contains(player)) {
			AutoJoin.currentGroup.remove(player);
		}
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		Player player = event.getPlayer();

		if (AutoJoin.currentGroup.contains(player)) {
			AutoJoin.currentGroup.remove(player);
		}
	}

	private String getCurrentUTCTime() {
		ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		return utc.format(formatter);
	}

}
