package com.yourname.prophunt.sounds;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, "prophunt");

    public static final Supplier<SoundEvent> PROP_WHISTLE = SOUND_EVENTS.register("prop_whistle",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("prophunt", "prop_whistle")));

    public static final Supplier<SoundEvent> PROP_DEATH = SOUND_EVENTS.register("prop_death",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("prophunt", "prop_death")));

    // Hunter help sounds - played on props when hunter uses help item
    public static final Supplier<SoundEvent> HUNTER_HELP_RICKROLL = SOUND_EVENTS.register("hunter_help_rickroll",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("prophunt", "hunter_help_rickroll")));

    public static final Supplier<SoundEvent> HUNTER_HELP_ELEVATOR = SOUND_EVENTS.register("hunter_help_elevator",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("prophunt", "hunter_help_elevator")));

    public static final Supplier<SoundEvent> HUNTER_HELP_RUN = SOUND_EVENTS.register("hunter_help_run",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("prophunt", "hunter_help_run")));

    public static final Supplier<SoundEvent> HUNTER_HELP_AMONGUS = SOUND_EVENTS.register("hunter_help_amongus",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("prophunt", "hunter_help_amongus")));

    public static final Supplier<SoundEvent> HUNTER_HELP_MILITARYTRUMPET = SOUND_EVENTS.register("hunter_help_militarytrumpet",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("prophunt", "hunter_help_militarytrumpet")));

    // Prop decoy sound - played on random prop to distract hunters
    public static final Supplier<SoundEvent> PROP_DECOY_FART = SOUND_EVENTS.register("prop_decoy_fart",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("prophunt", "prop_decoy_fart")));

    // Game end sounds
    public static final Supplier<SoundEvent> PROP_WIN = SOUND_EVENTS.register("prop_win",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("prophunt", "prop_win")));

    public static final Supplier<SoundEvent> HUNTER_WIN = SOUND_EVENTS.register("hunter_win",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("prophunt", "hunter_win")));

    // List of all hunter help sounds for random selection
    public static List<Supplier<SoundEvent>> getHunterHelpSounds() {
        return List.of(
            HUNTER_HELP_RICKROLL,
            HUNTER_HELP_ELEVATOR,
            HUNTER_HELP_RUN,
            HUNTER_HELP_AMONGUS,
            HUNTER_HELP_MILITARYTRUMPET
        );
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
