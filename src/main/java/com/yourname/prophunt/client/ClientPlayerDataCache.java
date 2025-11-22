package com.yourname.prophunt.client;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientPlayerDataCache {
    private static final Map<UUID, String> playerTeams = new HashMap<>();

    public static void setPlayerTeam(UUID playerId, String teamType) {
        playerTeams.put(playerId, teamType);
    }

    public static String getPlayerTeam(UUID playerId) {
        return playerTeams.getOrDefault(playerId, "UNKNOWN");
    }

    public static String getPlayerTeam(Player player) {
        return getPlayerTeam(player.getUUID());
    }

    public static void updateAllTeams(Map<UUID, String> teams) {
        playerTeams.clear();
        playerTeams.putAll(teams);
    }

    public static void clear() {
        playerTeams.clear();
    }
}
