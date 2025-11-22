package com.yourname.prophunt.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import com.yourname.prophunt.PropHuntMod;

public record HudSyncPayload(int gameTimeRemaining, boolean isGameActive, String gameState) implements CustomPacketPayload {
    public static final Type<HudSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PropHuntMod.MODID, "hud_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HudSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, HudSyncPayload::gameTimeRemaining,
            ByteBufCodecs.BOOL, HudSyncPayload::isGameActive,
            ByteBufCodecs.STRING_UTF8, HudSyncPayload::gameState,
            HudSyncPayload::new
    );

    @Override
    public Type<HudSyncPayload> type() {
        return TYPE;
    }
}
