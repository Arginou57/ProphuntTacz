package com.yourname.prophunt.network;

import com.yourname.prophunt.PropHuntMod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class NetworkHandler {

    public static void register(RegisterPayloadHandlersEvent event) {
        PropHuntMod.LOGGER.info("Registering network handlers");

        var registrar = event.registrar(PropHuntMod.MODID)
                .optional();

        registrar.playToClient(
                HudSyncPayload.TYPE,
                HudSyncPayload.CODEC,
                ClientPayloadHandler::handleHudSync
        );

        registrar.playToClient(
                HudMessagePayload.TYPE,
                HudMessagePayload.CODEC,
                ClientPayloadHandler::handleHudMessage
        );

        registrar.playToClient(
                PlayerTeamSyncPayload.TYPE,
                PlayerTeamSyncPayload.CODEC,
                ClientPayloadHandler::handlePlayerTeamSync
        );

        registrar.playToClient(
                FreezeSyncPayload.TYPE,
                FreezeSyncPayload.CODEC,
                ClientPayloadHandler::handleFreezeSync
        );
    }
}
