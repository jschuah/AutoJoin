// AutoJoin class:

package org.mineacademy.orion;

import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AutoJoin extends SimplePlugin {

	public static final int GROUP_SIZE = 2;
	public static List<String> predefinedNames = new ArrayList<>();  // Fill this with predefined names
	public static List<Player> currentGroup = new ArrayList<>();
	public static Random random = new Random();

	@Override
	protected void onPluginStart() {
		getLogger().info("All works, captain!");
		System.out.println("All works from system out, boss!");
		Common.log("Hello!");

		registerEvents(new PlayerListener());

		predefinedNames.add("KingAsiimov");
		predefinedNames.add("KingAsiimovX");
		predefinedNames.add("ArigatoSashimi");
	}
}