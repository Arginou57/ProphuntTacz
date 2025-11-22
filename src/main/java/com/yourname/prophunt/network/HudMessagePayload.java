package com.yourname.prophunt.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.yourname.prophunt.PropHuntMod;

public record HudMessagePayload(String message) implements CustomPacketPayload {
    public static final Type<HudMessagePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PropHuntMod.MODID, "hud_message"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HudMessagePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, HudMessagePayload::message,
            HudMessagePayload::new
    );

    @Override
    public Type<HudMessagePayload> type() {
        return TYPE;
    }
}
