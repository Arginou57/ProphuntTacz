package com.yourname.prophunt.game;

import com.yourname.prophunt.network.HudSyncPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class GameManager {
    private final Map<UUID, PropHuntGame> activeGames = new HashMap<>();
    private PropHuntGame currentGame;
    private MapManager.GameMapData currentMap = null;
    private int syncTicker = 0;

    public void tick(MinecraftServer server) {
        if (currentGame != null) {
            currentGame.tick(server);

            // Sync HUD every 2 ticks (10 times per second)
            syncTicker++;
            if (syncTicker >= 2) {
                syncTicker = 0;
                syncHudToPlayers(server);
            }
        }
    }

    private void syncHudToPlayers(MinecraftServer server) {
        if (currentGame == null) return;

        int remainingTime = currentGame.getRemainingTime();
        String gameState = currentGame.getState().toString();
        boolean isActive = currentGame.isActive();

        HudSyncPayload payload = new HudSyncPayload(remainingTime, isActive, gameState);

        // Send to all players in the game
        for (ServerPlayer player : currentGame.getPlayers()) {
            PacketDistributor.sendToPlayer(player, payload);
        }

        // Sync player team data
        syncPlayerTeamsToPlayers(server);
    }

    private void syncPlayerTeamsToPlayers(MinecraftServer server) {
        if (currentGame == null) return;

        java.util.Map<java.util.UUID, String> playerTeams = new java.util.HashMap<>();
        for (ServerPlayer player : currentGame.getPlayers()) {
            com.yourname.prophunt.teams.PropHuntTeam team = currentGame.getPlayerTeam(player.getUUID());
            if (team != null) {
                playerTeams.put(player.getUUID(), team.getType().toString());
            }
        }

        com.yourname.prophunt.network.PlayerTeamSyncPayload payload =
            new com.yourname.prophunt.network.PlayerTeamSyncPayload(playerTeams);

        // Send to all players in the game
        for (ServerPlayer player : currentGame.getPlayers()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public void broadcastHudMessage(String message) {
        if (currentGame == null) return;

        com.yourname.prophunt.network.HudMessagePayload payload =
            new com.yourname.prophunt.network.HudMessagePayload(message);

        // Send to all players in the game
        for (ServerPlayer player : currentGame.getPlayers()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public PropHuntGame createGame() {
        currentGame = new PropHuntGame();
        return currentGame;
    }

    public boolean startGame(MinecraftServer server) {
        System.out.println("[PropHunt GameManager] startGame called");
        if (currentGame == null) {
            System.out.println("[PropHunt GameManager] currentGame is NULL!");
            return false;
        }

        int playerCount = currentGame.getPlayers().size();
        System.out.println("[PropHunt GameManager] Player count: " + playerCount);
        if (playerCount < 2) {
            broadcastMessage(server, Component.literal("§cNeed at least 2 players to start!"));
            return false;
        }

        System.out.println("[PropHunt GameManager] Starting game...");
        currentGame.start(server);
        return true;
    }

    public boolean startGameWithMap(MinecraftServer server, String mapName) {
        System.out.println("[PropHunt GameManager] startGameWithMap called for map: " + mapName);

        // Stop existing game if one is running
        if (currentGame != null && currentGame.hasStarted()) {
            System.out.println("[PropHunt GameManager] Stopping existing game");
            stopGame(server);
        }

        // Create new game
        if (currentGame == null) {
            currentGame = new PropHuntGame();
        }

        System.out.println("[PropHunt GameManager] Getting map data for: " + mapName);
        MapManager.GameMapData mapData = MapManager.getMap(mapName);
        if (mapData == null) {
            System.out.println("[PropHunt GameManager] Map not found!");
            broadcastMessage(server, Component.literal("§cMap not found: " + mapName));
            return false;
        }

        System.out.println("[PropHunt GameManager] Map found! Setting currentMap and starting lobby...");
        currentMap = mapData;
        currentGame.setCurrentMap(mapData);
        System.out.println("[PropHunt GameManager] currentGame.setCurrentMap() called");
        currentGame.start(server);
        return true;
    }

    public boolean startTestMode(MinecraftServer server, ServerPlayer player) {
        if (currentGame == null) {
            return false;
        }

        // Start game in test mode (allows 1 player)
        currentGame.startTestMode(server, player);
        return true;
    }

    public void stopGame(MinecraftServer server) {
        if (currentGame != null) {
            currentGame.stop(server);
            currentGame = null;
        }
    }

    public boolean addPlayer(ServerPlayer player) {
        if (currentGame == null) {
            player.sendSystemMessage(Component.literal("§cNo active lobby! Use /prophunt start [mapname] first"));
            return false;
        }

        // Only allow joining during LOBBY state
        if (currentGame.getState() != GameState.LOBBY) {
            player.sendSystemMessage(Component.literal("§cGame already in progress! Lobby has closed."));
            return false;
        }

        return currentGame.addPlayer(player);
    }

    public void removePlayer(ServerPlayer player) {
        if (currentGame != null) {
            currentGame.removePlayer(player);
        }
    }

    public PropHuntGame getCurrentGame() {
        return currentGame;
    }

    public boolean hasActiveGame() {
        return currentGame != null && currentGame.isActive();
    }

    private void broadcastMessage(MinecraftServer server, Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }
}
