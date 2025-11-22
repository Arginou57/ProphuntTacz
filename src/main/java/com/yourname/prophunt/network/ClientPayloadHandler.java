package com.yourname.prophunt.network;

import com.yourname.prophunt.client.PropHuntHudRenderer;
import com.yourname.prophunt.client.ClientPlayerDataCache;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void handleHudSync(final HudSyncPayload payload, final IPayloadContext context) {
        // Update HUD on the client thread
        context.enqueueWork(() -> {
            PropHuntHudRenderer.setHudEnabled(payload.isGameActive());
            PropHuntHudRenderer.setGameTimeRemaining(payload.gameTimeRemaining());
            PropHuntHudRenderer.setGameState(payload.gameState());
        });
    }

    public static void handleHudMessage(final HudMessagePayload payload, final IPayloadContext context) {
        // Display message on HUD
        context.enqueueWork(() -> {
            PropHuntHudRenderer.addMessage(payload.message());
        });
    }

    public static void handlePlayerTeamSync(final PlayerTeamSyncPayload payload, final IPayloadContext context) {
        // Update player team data on the client thread
        context.enqueueWork(() -> {
            ClientPlayerDataCache.updateAllTeams(payload.playerTeams());
        });
    }

    public static void handleFreezeSync(final FreezeSyncPayload payload, final IPayloadContext context) {
        // Update freeze bar on the client thread
        context.enqueueWork(() -> {
            PropHuntHudRenderer.setFreezeBarProgress(payload.freezeProgress());
            PropHuntHudRenderer.setFrozen(payload.isFrozen());
            PropHuntHudRenderer.setInCooldown(payload.isInCooldown());
            PropHuntHudRenderer.setFreezeSecondsRemaining(payload.secondsRemaining());
        });
    }
}
