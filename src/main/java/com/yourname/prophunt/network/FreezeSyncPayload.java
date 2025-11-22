package com.yourname.prophunt.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.yourname.prophunt.PropHuntMod;

public record FreezeSyncPayload(float freezeProgress, boolean isFrozen, boolean isInCooldown, int secondsRemaining) implements CustomPacketPayload {
    public static final Type<FreezeSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PropHuntMod.MODID, "freeze_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FreezeSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, FreezeSyncPayload::freezeProgress,
            ByteBufCodecs.BOOL, FreezeSyncPayload::isFrozen,
            ByteBufCodecs.BOOL, FreezeSyncPayload::isInCooldown,
            ByteBufCodecs.INT, FreezeSyncPayload::secondsRemaining,
            FreezeSyncPayload::new
    );

    @Override
    public Type<FreezeSyncPayload> type() {
        return TYPE;
    }
}
