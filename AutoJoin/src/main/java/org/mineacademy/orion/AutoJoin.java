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

	private static AutoJoin instance;

	@Override
	protected void onPluginStart() {
		getLogger().info("AutoJoin starting...");

		registerEvents(new PlayerListener());


		//TODO: ADD NAMES HERE
		predefinedNames.add("PLAI_1");
		predefinedNames.add("PLAI_2");
		predefinedNames.add("PLAI_5");
		predefinedNames.add("PLAI_0");
		predefinedNames.add("KingAsiimov");
		predefinedNames.add("KingAsiimovX");
		predefinedNames.add("ArigatoSashimi");
		predefinedNames.add("PLAI_6");

		instance = this;
	}


	public static AutoJoin getInstance() {
		return instance;
	}
}
