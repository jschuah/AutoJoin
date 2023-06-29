package org.mineacademy.orion;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Group {
	private List<Player> players;

	public Group(List<Player> players) {
		this.players = new ArrayList<>(players);
	}

	public List<Player> getPlayers() {
		return players;
	}

	public boolean isPlayerInGroup(Player player){
		return players.contains(player);
	}

	public void removePlayer(Player player){
		players.remove(player);
	}

	public boolean isEmpty(){
		return players.isEmpty();
	}
}
