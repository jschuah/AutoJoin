package org.mineacademy.orion;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import net.ess3.api.IWarps;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
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
import java.util.*;

import static org.bukkit.Bukkit.getServer;


public class PlayerListener implements Listener {

	private static final List<String> worlds = Arrays.asList("plai_one", "plai_two", "plai_three");
	private static final List<String> skins =
			Arrays.asList("SkyTheKidRS", "Herobrine", "CaptainSparklez", "Notch", "EmileWRX", "MiriMine", "_Dragon33_", "olliknolli", "PizzaKiing97",
					"puppymom64", "MatthawkC", "Demonspider_", "prowti", "mohamed", "emsigo", "Rauster", "GreenWylf", "Palphair",
					"Davie504", "KingAsiimov", "ArigatoSashimi", "Frank", "Blue_Orion", "Redsoxboy");
	private static final Map<String, Group> warpOccupancy = new HashMap<>();
	private static final List<String> warps = Arrays.asList("r1", "r2", "r3", "r5");
	private TeleportTask teleportTask;
	private final Map<Player, BukkitTask> countdownTasks = new HashMap<>();
	private final int capacityNum = 3;
	private BukkitTask groupCountdownTask;
	private long groupTimeRemaining;
	private Set<Player> extendVotes = new HashSet<>();
	private int maxExtensionCount = 3; // Maximum number of times the group can extend the timer
	private int extendCount = 0; // Number of times the timer has been extended
	private int extendInterval = 30; // Extension Amount
	private Map<UUID, Integer> playerExtensionCounts = new HashMap<>();
	private static final List<String> genderNeutralNames = Arrays.asList("Alex", "Casey", "Jordan", "Taylor", "Jamie", "Robin", "Charlie", "Avery", "Morgan", "Riley", "Bailey", "Peyton", "Cameron", "Devon", "Drew", "Reese", "Sydney", "Skyler", "Pat", "Sam");
	private static String worldGlobal = "";

	@EventHandler
	public void onJoin(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();

		if (AutoJoin.predefinedNames.contains(player.getName())) {
			Bukkit.getScheduler().runTask(AutoJoin.getInstance(), () -> {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv tp " + player.getName() + " BedWars");
			});
		} else {
			Bukkit.getScheduler().runTask(AutoJoin.getInstance(), () -> {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv tp " + player.getName() + " Lobby");
			});
		}

		// Reset the extension count when player joins a new group
		playerExtensionCounts.put(player.getUniqueId(), 0);


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

		if (AutoJoin.currentGroup.size() < capacityNum){
			Bukkit.getScheduler().runTask(AutoJoin.getInstance(), () -> {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "§ebroadcast there are currently "
						+ AutoJoin.currentGroup.size() + " out of a possible " + capacityNum +
						" players waiting to join a session. Join the session and play Minecraft for Science!");
			});
		}
		if (AutoJoin.currentGroup.size() == 1) {
			// Schedule a task to send email after X minutes
			Bukkit.getScheduler().runTaskLater(AutoJoin.getInstance(), () -> {
				if (AutoJoin.currentGroup.size() == 1) {
					// Only one player has been in the group for 1 minute
					sendEmail("chuah.jun.sean@gmail.com", "Only one player",
							"There's only one player left in the server and they've been waiting for 3 minutes.");
				}
			}, 20L * 60L * 1L); //After 2 Minutes, EMAIL WILL BE SENT
		}
	}

	public void sendEmail(String recipient, String subject, String body) {
		Player player = AutoJoin.currentGroup.get(AutoJoin.currentGroup.size() - 1);  // Retrieves the last player
		final String username = "plaicraftmc@gmail.com";
		final String appPassword = "wtqxlnpvmovygeis";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		jakarta.mail.Session session = jakarta.mail.Session.getInstance(props, new jakarta.mail.Authenticator() {
			@Override
			protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
				return new jakarta.mail.PasswordAuthentication(username, appPassword);
			}
		});

		try {
			jakarta.mail.Message message = new jakarta.mail.internet.MimeMessage(session);
			message.setFrom(new InternetAddress(username));
			message.setRecipients(jakarta.mail.Message.RecipientType.TO, InternetAddress.parse(recipient));
			message.setSubject(subject);
			message.setText(body);

			Transport.send(message);

			System.out.println("Email sent successfully");
			player.sendMessage("§bAn email has been sent due to low group size.");

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

	private void sendActionBar(Player player, String message, int groupSize) {
		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message + " Current Group Size: " + groupSize + "/3"));
	}

	private void startCountdown(Player player, long waitTimeTicks) {
		if (groupCountdownTask != null) {
			groupCountdownTask.cancel();
		}

		groupTimeRemaining = waitTimeTicks;

		groupCountdownTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (groupTimeRemaining <= 0) {
					this.cancel();
					if (AutoJoin.currentGroup.contains(player)) {
						teleportGroup();
					}
					groupTimeRemaining -= 20;
				} else if (groupTimeRemaining <= 30 * 20) {
					for (Player groupPlayer : AutoJoin.currentGroup) {
						sendActionBar(groupPlayer, "Teleporting in " + groupTimeRemaining / 20 + " seconds...", AutoJoin.currentGroup.size());
						if (groupTimeRemaining % (10 * 20) == 0) {
							extendVotes.clear();
							sendChatExtensionOption(groupPlayer, groupTimeRemaining); // Pass groupTimeRemaining here
						}
					}
					groupTimeRemaining -= 20;
				} else {
					for (Player groupPlayer : AutoJoin.currentGroup) {
						sendActionBar(groupPlayer, "Teleporting in " + groupTimeRemaining / 20 + " seconds...", AutoJoin.currentGroup.size());
					}
					groupTimeRemaining -= 20;
				}
			}
		}.runTaskTimer(AutoJoin.getInstance(), 0, 20);
	}


	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		String cmd = event.getMessage().split(" ")[0];
		UUID playerUUID = player.getUniqueId();

		if (cmd.equalsIgnoreCase("/extend")) {
			event.setCancelled(true); // Cancel the event so the command doesn't actually run

			if (groupCountdownTask != null && playerExtensionCounts.getOrDefault(playerUUID, 0) < maxExtensionCount) {
				voteToExtend(player);
			} else {
				TextComponent message = new TextComponent("You cannot extend anymore.");
				for (Player groupPlayer : AutoJoin.currentGroup) {
					groupPlayer.spigot().sendMessage(message);
				}
			}
		}
	}


	private void sendChatExtensionOption(Player player, long timeRemaining) {
		TextComponent message = new TextComponent("Teleporting in " + timeRemaining / 20 + " seconds. Click here to extend the timer.");
		message.setColor(ChatColor.GREEN);
		message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/extend"));
		message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to extend the timer!").create()));
		player.spigot().sendMessage(message);
	}

	private void voteToExtend(Player player) {
		extendVotes.add(player);
		UUID playerUUID = player.getUniqueId();

		if (extendVotes.size() < AutoJoin.currentGroup.size()) {
			String message = "Extend votes: " + extendVotes.size() + "/" + AutoJoin.currentGroup.size() + ". Everybody has to vote to extend the timer.";
			for (Player groupPlayer : AutoJoin.currentGroup) {
				groupPlayer.sendMessage(message);
			}
		} else {
			extendTimer();

			// Increase the count of extensions for this player
			playerExtensionCounts.put(playerUUID, playerExtensionCounts.getOrDefault(playerUUID, 0) + 1);
		}
	}



	private void extendTimer() {
		if (extendVotes.size() >= AutoJoin.currentGroup.size()) {
			groupTimeRemaining += extendInterval * 20; // Extend by 30 seconds
			TextComponent message = new TextComponent("The timer has been extended by " + extendInterval + " seconds");
			for (Player groupPlayer : AutoJoin.currentGroup) {
				groupPlayer.spigot().sendMessage(message);
			}
			extendVotes.clear(); // Clear the votes for the next round
			extendCount++; // Increase the count of extensions
		}
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

				worldGlobal = worldName;

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
					// Teleport the player
					groupPlayer.teleport(randomLocation);

					groupPlayer.setBedSpawnLocation(randomLocation, true);
					AutoJoin.respawnLocations.put(groupPlayer.getName(), randomLocation);

					groupPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 1));

					groupPlayer.sendTitle("§6Have Fun!", "§6World: " + world.getName() + " | Time: " + getCurrentUTCTime(), 10, 70, 20);

					// Generate a random name for the session
					String randomName = genderNeutralNames.get(AutoJoin.random.nextInt(genderNeutralNames.size()));

					// Inform the player of their name for the session
					groupPlayer.sendMessage(ChatColor.BOLD + "" + ChatColor.DARK_PURPLE + "Your name for the session is " + randomName);

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

					// Reset the extension count when player joins a new group
					playerExtensionCounts.put(groupPlayer.getUniqueId(), 0);
				}

				// Send a POST to the ip-addresses of the EC2 instances so they can create a new
				// file with the proper names for S3 bucket location.
				ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
				for (final Player groupPlayer : AutoJoin.currentGroup) {
					sendPostRequestForPlayer(groupPlayer, world.getName(), utc);
				}

				// Clear the current group
				AutoJoin.currentGroup.clear();

			}, 20L);
		}
	}



	//TODO: Review
	// Separate function for HTTP POST request
	private void sendPostRequestForPlayer(final Player groupPlayer, String currentWorld, ZonedDateTime utc) {
		String playerName = groupPlayer.getName();
		// Get current time

		// Create a JSON object with the data
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("username", playerName);
		jsonObject.put("currentWorld", currentWorld);
		jsonObject.put("time", utc.toString());
		jsonObject.put("endPoint", "world-data");

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

	//TODO: Review
	private void sendPostRequestForAFKPlayer(final Player groupPlayer, String currentWorld, ZonedDateTime utc, String instanceId) {
		String playerName = groupPlayer.getName();

		// Create a JSON object with the data
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("username", playerName);
		jsonObject.put("currentWorld", currentWorld);
		jsonObject.put("time", utc.toString());
		jsonObject.put("instanceId", instanceId);
		jsonObject.put("status", "afk-kicked");  // Status indicating player was kicked due to being AFK

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

		UUID playerUUID = event.getPlayer().getUniqueId();

		// Decrease the extendCount for the player when they leave the server
		if (playerExtensionCounts.containsKey(playerUUID) && playerExtensionCounts.get(playerUUID) > 0) {
			playerExtensionCounts.put(playerUUID, playerExtensionCounts.get(playerUUID) - 1);
		}

	}

//TODO: REVIEW
	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		Player player = event.getPlayer();

		// Get EssentialsX plugin
		Plugin plugin = getServer().getPluginManager().getPlugin("Essentials");

		// Make sure Essentials is loaded
		if (plugin == null || !(plugin instanceof Essentials)) {
			// EssentialsX isn't loaded
			return;
		}

		Essentials essentials = (Essentials) plugin;

		// Get the Essentials user
		User user = essentials.getUser(player);

		// Check if the player is AFK
		if (user.isAfk()) {
			sendPostRequestForAFKPlayer(player, worldGlobal, ZonedDateTime.now(ZoneOffset.UTC), "instanceId");
		}

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
			if (this.initialGroupSize <= capacityNum) {
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
					return 2 * 60 * 1000;  // 5 minutes
				case 2:
					return 1 * 60 * 1000;   // 2 minutes
				default:
					return 0;               // Instant teleport for 3 or more players
			}
		}

		private long getRemainingTime(int groupSize, long currentTime) {
			return getWaitTime(groupSize) - (currentTime - startTime);
		}
	}
}
