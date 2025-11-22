package com.yourname.prophunt.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.yourname.prophunt.PropHuntMod;
import com.yourname.prophunt.teams.TeamType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record PlayerTeamSyncPayload(Map<UUID, String> playerTeams) implements CustomPacketPayload {
    public static final Type<PlayerTeamSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PropHuntMod.MODID, "player_team_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerTeamSyncPayload> CODEC = StreamCodec.composite(
            StreamCodec.of(
                    (buf, map) -> {
                        buf.writeInt(map.size());
                        for (Map.Entry<UUID, String> entry : map.entrySet()) {
                            buf.writeUUID(entry.getKey());
                            buf.writeUtf(entry.getValue());
                        }
                    },
                    buf -> {
                        int size = buf.readInt();
                        Map<UUID, String> map = new HashMap<>();
                        for (int i = 0; i < size; i++) {
                            map.put(buf.readUUID(), buf.readUtf());
                        }
                        return map;
                    }
            ),
            PlayerTeamSyncPayload::playerTeams,
            PlayerTeamSyncPayload::new
    );

    @Override
    public Type<PlayerTeamSyncPayload> type() {
        return TYPE;
    }
}
