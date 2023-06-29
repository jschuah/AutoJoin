package org.mineacademy.orion;

import com.earth2me.essentials.Essentials;
import net.ess3.api.IWarps;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.json.JSONObject;
import org.mineacademy.fo.Common;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerListener implements Listener {

	//TODO: Players get teleported to different Warps.

	private static final List<String> worlds = Arrays.asList("plai_one", "plai_two", "plai_three");
	private static final List<String> skins =
			Arrays.asList("SkyTheKidRS", "Herobrine", "CaptainSparklez", "Notch", "EmileWRX", "MiriMine", "_Dragon33_", "olliknolli", "PizzaKiing97",
					"puppymom64", "MatthawkC", "Demonspider_", "prowti", "mohamed", "emsigo", "Rauster", "GreenWylf", "Palphair",
					"Davie504", "KingAsiimov", "ArigatoSashimi", "Frank", "Blue_Orion", "Redsoxboy");
	private static final Map<String, Group> warpOccupancy = new HashMap<>();
	private static final List<String> warps = Arrays.asList("r1", "r2", "r3", "r5");
	private TeleportTask teleportTask;
	private final Map<Player, BukkitTask> countdownTasks = new HashMap<>();


	@EventHandler
	public void onJoin(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();

		// Get the lobby world
		World lobbyWorld = Bukkit.getWorld("lobby"); // replace "lobby" with your lobby world name
		if (lobbyWorld == null) {
			// Handle the case where the world doesn't exist
			// Maybe send a message to the player or log an error
			return;
		}

		// Get the spawn location for the lobby world
		Location lobbySpawnLocation = lobbyWorld.getSpawnLocation();

		// Teleport the player to the lobby spawn location
		player.teleport(lobbySpawnLocation);

		if (AutoJoin.predefinedNames.contains(player.getName())) {
			AutoJoin.currentGroup.add(player);
			Common.tellLater(2, player, "§b You've been automatically assigned to a group!");
			Common.tellLater(2, player, "§b Teleportation will commence once there are enough players or the wait time has passed.");

			if (teleportTask == null || teleportTask.isCancelled()) {
				teleportTask = new TeleportTask(this);
				long waitTimeMillis = teleportTask.getWaitTime(AutoJoin.currentGroup.size());
				long waitTimeTicks = waitTimeMillis / 50;
				teleportTask.runTaskLater(AutoJoin.getInstance(), waitTimeTicks);
			}

			// update all countdowns
			long waitTimeMillis = teleportTask.getWaitTime(AutoJoin.currentGroup.size());
			long waitTimeTicks = waitTimeMillis / 50;
			updateCountdowns(waitTimeTicks);
		}
	}






	private void sendActionBar(Player player, String message, int groupSize) {
		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message + " Current Group Size: " + groupSize + "/3"));
	}

	private void startCountdown(Player player, long waitTimeTicks) {
		if (countdownTasks.containsKey(player)) {
			countdownTasks.get(player).cancel();
		}

		BukkitTask countdownTask = new BukkitRunnable() {
			private long timeRemaining = waitTimeTicks;

			@Override
			public void run() {
				if (timeRemaining <= 0) {
					this.cancel();
					countdownTasks.remove(player);
					if (AutoJoin.currentGroup.contains(player)) {
						teleportGroup();
					}
				} else {
					sendActionBar(player, "Teleporting in " + timeRemaining / 20 + " seconds...", AutoJoin.currentGroup.size());
					timeRemaining -= 20;
				}
			}
		}.runTaskTimer(AutoJoin.getInstance(), 0, 20);

		countdownTasks.put(player, countdownTask);
	}



	protected void teleportGroup() {
		if (!AutoJoin.currentGroup.isEmpty()) {
			Bukkit.getScheduler().runTaskLater(AutoJoin.getInstance(), () -> {
				final String worldName = worlds.get(AutoJoin.random.nextInt(worlds.size()));
				final World world = Bukkit.getWorld(worldName);

				int x = AutoJoin.random.nextInt(1000) - 500;
				final int z = AutoJoin.random.nextInt(1000) - 500;

				// pick a random warp for all in group
				String randomWarp = null;
				Location randomLocation = null;

				if (worldName.equals("plai_three")) {
					randomWarp = warps.get(AutoJoin.random.nextInt(warps.size()));
					try {
						Essentials ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
						IWarps warpAPI = ess.getWarps();
						randomLocation = warpAPI.getWarp(randomWarp);
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}
				}

				// If we're not in plai_three, generate a new location for each player
				if (randomLocation == null) {
					final int y = world.getHighestBlockYAt(x, z);
					randomLocation = new Location(world, x, y, z);
				}

				// Check if the selected warp is occupied
				if (warpOccupancy.computeIfAbsent(randomWarp, k -> new Group(AutoJoin.currentGroup)).isEmpty()) {
					// Warp is occupied, skip this teleportation attempt
					return;
				}

				for (final Player groupPlayer : AutoJoin.currentGroup) {
					groupPlayer.teleport(randomLocation);
					groupPlayer.setBedSpawnLocation(randomLocation, true);
					AutoJoin.respawnLocations.put(groupPlayer.getName(), randomLocation);

					groupPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 1));

					groupPlayer.sendTitle("§6Type /Audio now!", "§6World: " + world.getName() + " | Time: " + getCurrentUTCTime(), 10, 70, 20);

					groupPlayer.playSound(groupPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);

					String skin = skins.get(AutoJoin.random.nextInt(skins.size()));

					Bukkit.getScheduler().runTask(AutoJoin.getInstance(), () -> {
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "skin set " + groupPlayer.getName() + " " + skin);
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clear " + groupPlayer.getName());
					});

					x += 1;

					// Cancel any ongoing countdown for the player
					BukkitTask countdownTask = countdownTasks.remove(groupPlayer);
					if (countdownTask != null) {
						countdownTask.cancel();
					}
				}

				// Send a POST to the ip-addresses of the EC2 instances so they can create a new
				// file with the proper names for S3 bucket location.
				for (final Player groupPlayer : AutoJoin.currentGroup) {
					sendPostRequestForPlayer(groupPlayer, world.getName());
				}

				for (Player groupPlayer : AutoJoin.currentGroup) {
					BukkitTask countdownTask = countdownTasks.remove(groupPlayer);
					if (countdownTask != null) {
						countdownTask.cancel();
					}
				}

				AutoJoin.currentGroup.clear();
			}, 20L);
		}
	}

	// Separate function for HTTP POST request
	private void sendPostRequestForPlayer(final Player groupPlayer, String currentWorld) {
		String playerName = groupPlayer.getName();
		// Get current time
		ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);

		// Create a JSON object with the data
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("username", playerName);
		jsonObject.put("currentWorld", currentWorld);
		jsonObject.put("time", utc.toString());

		// Convert the JSON object to a string
		String jsonInputString = jsonObject.toString();

		try {
			// Create a Url object from the IP address
			URL url = new URL("https://2i3bwawhp6.execute-api.us-west-2.amazonaws.com/connect-mc-server-to-ec2");

			// Open a connection to the URL
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			// Set the request method to POST
			conn.setRequestMethod("POST");

			// Set the request content type to application/json
			conn.setRequestProperty("Content-Type", "application/json; utf-8");

			// Enable input and output streams
			conn.setDoOutput(true);

			// Write the JSON input string to the output stream
			try (OutputStream os = conn.getOutputStream()) {
				byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
				os.write(input, 0, input.length);
			}

			// Get the response code
			int responseCode = conn.getResponseCode();
			System.out.println("POST Response Code :: " + responseCode);

			// Close the connection
			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void broadcastToGroup(String message) {
		for (Player groupPlayer : AutoJoin.currentGroup) {
			groupPlayer.sendMessage(message);
		}
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

	private void updateCountdowns(long waitTimeTicks) {
		for (Player groupPlayer : AutoJoin.currentGroup) {
			startCountdown(groupPlayer, waitTimeTicks);
		}
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
		World world = player.getWorld();
		if (!event.isBedSpawn() && AutoJoin.respawnLocations.containsKey(player.getName())) {
			event.setRespawnLocation(AutoJoin.respawnLocations.get(player.getName()));

			player.sendTitle("§cYou've been reborn!", "§6World: " + world.getName() + " | Time: " + getCurrentUTCTime(), 10, 70, 20);

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

		for (Map.Entry<String, Group> entry : warpOccupancy.entrySet()) {
			if (entry.getValue().isPlayerInGroup(player)) {
				entry.getValue().removePlayer(player);
				if (entry.getValue().isEmpty()) {
					// The group is empty, free up the warp
					warpOccupancy.remove(entry.getKey());
				}
				break;
			}
		}

		BukkitTask countdownTask = countdownTasks.remove(player);
		if (countdownTask != null) {
			countdownTask.cancel();
		}
	}


	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		Player player = event.getPlayer();

		if (AutoJoin.currentGroup.contains(player)) {
			AutoJoin.currentGroup.remove(player);
		}

		BukkitTask countdownTask = countdownTasks.remove(player);
		if (countdownTask != null) {
			countdownTask.cancel();
		}
	}

	private String getCurrentUTCTime() {
		ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		return utc.format(formatter);
	}

	private class TeleportTask extends BukkitRunnable {
		private final PlayerListener playerListener;
		private final long startTime;
		private final int initialGroupSize;

		public TeleportTask(PlayerListener playerListener) {
			this.playerListener = playerListener;
			this.startTime = System.currentTimeMillis();
			this.initialGroupSize = AutoJoin.currentGroup.size();
		}

		@Override
		public void run() {
			// Teleport group if the initial size was less than or equal to 3
			if (this.initialGroupSize <= 3) {
				long elapsedTime = System.currentTimeMillis() - startTime;
				long remainingTime = getWaitTime(this.initialGroupSize) - elapsedTime;
				if (remainingTime <= 0) {
					playerListener.teleportGroup();
				} else {
					this.runTaskLater(AutoJoin.getInstance(), remainingTime / 50);
				}
			} else {
				playerListener.teleportGroup();
			}
		}

		private long getWaitTime(int groupSize) {
			switch (groupSize) {
				case 1:
					return 2 * 60 * 1000;  // 2 minutes
				case 2:
					return 1 * 60 * 1000;   // 1 minutes
				default:
					return 0;               // Instant teleport for 3 or more players
			}
		}

		private long getRemainingTime(int groupSize, long currentTime) {
			return getWaitTime(groupSize) - (currentTime - startTime);
		}
	}
}
