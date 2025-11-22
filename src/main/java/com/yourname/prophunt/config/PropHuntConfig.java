package com.yourname.prophunt.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Configuration du mod Prop Hunt
 * Le fichier de config est généré automatiquement dans config/prophunt-common.toml
 */
public class PropHuntConfig {

    public static final CommonConfig COMMON;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        Pair<CommonConfig, ModConfigSpec> commonPair = new ModConfigSpec.Builder().configure(CommonConfig::new);
        COMMON = commonPair.getLeft();
        COMMON_SPEC = commonPair.getRight();
    }

    public static class CommonConfig {
        // Freeze settings
        public final ModConfigSpec.IntValue freezeMaxTime;
        public final ModConfigSpec.IntValue freezeCooldownTime;
        public final ModConfigSpec.IntValue freezeRegenRate;

        // Sound settings
        public final ModConfigSpec.IntValue propSoundInterval;

        // Game settings
        public final ModConfigSpec.IntValue hidePhaseTime;
        public final ModConfigSpec.IntValue gameTime;
        public final ModConfigSpec.IntValue minPlayers;
        public final ModConfigSpec.DoubleValue hunterRatio;


        public CommonConfig(ModConfigSpec.Builder builder) {
            builder.comment("Prop Hunt Configuration").push("freeze");

            freezeMaxTime = builder
                .comment("Maximum freeze time in seconds (how long a prop can stay frozen)")
                .defineInRange("maxFreezeTime", 30, 5, 120);

            freezeCooldownTime = builder
                .comment("Cooldown time in seconds after freeze is exhausted")
                .defineInRange("freezeCooldown", 30, 5, 120);

            freezeRegenRate = builder
                .comment("Freeze regeneration multiplier when not sneaking (0 = no regen, 2 = 2x faster regen)")
                .defineInRange("freezeRegenRate", 2, 0, 10);

            builder.pop();

            builder.push("sound");

            propSoundInterval = builder
                .comment("Interval in seconds between prop whistle sounds during PLAYING phase")
                .defineInRange("propSoundInterval", 30, 10, 120);

            builder.pop();

            builder.push("game");

            hidePhaseTime = builder
                .comment("Hide phase duration in seconds (time props have to hide before hunters are released)")
                .defineInRange("hidePhaseTime", 30, 10, 120);

            gameTime = builder
                .comment("Total game time in seconds")
                .defineInRange("gameTime", 300, 60, 1800);

            minPlayers = builder
                .comment("Minimum number of players to start a game")
                .defineInRange("minPlayers", 2, 1, 10);

            hunterRatio = builder
                .comment("Ratio of players that become hunters (0.33 = 1/3 of players)")
                .defineInRange("hunterRatio", 0.33, 0.1, 0.9);

            builder.pop();
        }
    }

    // Helper methods to get config values in ticks
    public static int getMaxFreezeTimeTicks() {
        return COMMON.freezeMaxTime.get() * 20;
    }

    public static int getFreezeCooldownTicks() {
        return COMMON.freezeCooldownTime.get() * 20;
    }

    public static int getFreezeRegenRate() {
        return COMMON.freezeRegenRate.get();
    }

    public static int getPropSoundIntervalTicks() {
        return COMMON.propSoundInterval.get() * 20;
    }

    public static int getHidePhaseTimeTicks() {
        return COMMON.hidePhaseTime.get() * 20;
    }

    public static int getGameTimeTicks() {
        return COMMON.gameTime.get() * 20;
    }

    public static int getMinPlayers() {
        return COMMON.minPlayers.get();
    }

    public static double getHunterRatio() {
        return COMMON.hunterRatio.get();
    }

    public static int getHunterCount(int totalPlayers) {
        return Math.max(1, (int)(totalPlayers * getHunterRatio()));
    }

    // Legacy static fields for backwards compatibility
    public static int MAX_GAME_TIME_TICKS = 6000;
    public static int HIDE_TIME_TICKS = 600;
    public static int MIN_PLAYERS = 2;
    public static double HUNTER_RATIO = 0.33;

    /**
     * Sync legacy static fields with config values
     */
    public static void syncLegacyFields() {
        MAX_GAME_TIME_TICKS = getGameTimeTicks();
        HIDE_TIME_TICKS = getHidePhaseTimeTicks();
        MIN_PLAYERS = getMinPlayers();
        HUNTER_RATIO = getHunterRatio();
    }
}
