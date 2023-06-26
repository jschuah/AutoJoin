package org.mineacademy.orion;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.mineacademy.fo.plugin.SimplePlugin;

import java.util.*;

public class AutoJoin extends SimplePlugin {

	public static final int GROUP_SIZE = 2;
	public static List<String> predefinedNames = new ArrayList<>();  // Fill this with predefined names
	public static List<Player> currentGroup = new ArrayList<>();
	public static Map<String, Location> respawnLocations = new HashMap<>();
	public static Random random = new Random();

	private static AutoJoin instance;  // This is the new line

	@Override
	protected void onPluginStart() {
		getLogger().info("AutoJoin starting...");

		registerEvents(new PlayerListener());

		predefinedNames.add("PLAI_1");
		predefinedNames.add("PLAI_2");
		predefinedNames.add("PLAI_5");
		predefinedNames.add("PLAI_0");
		predefinedNames.add("KingAsiimov");
		predefinedNames.add("KingAsiimovX");
		predefinedNames.add("ArigatoSashimi");

		instance = this;  // Set the instance
	}


	public static AutoJoin getInstance() {
		return instance;  // Getter for the instance
	}
}
