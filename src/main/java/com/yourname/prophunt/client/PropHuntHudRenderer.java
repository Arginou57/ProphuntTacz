package com.yourname.prophunt.client;

import com.yourname.prophunt.PropHuntMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@EventBusSubscriber(modid = PropHuntMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class PropHuntHudRenderer {

    private static boolean hudEnabled = false;
    private static int gameTimeRemaining = 0;
    private static String gameState = "WAITING";
    private static boolean isTransformed = false;
    private static boolean isFrozen = false;
    private static float freezeBarProgress = 1.0f;
    private static boolean isInCooldown = false;
    private static int freezeSecondsRemaining = 30;

    // Message queue for HUD messages
    private static final Deque<HudMessage> messageQueue = new LinkedList<>();
    private static final int MAX_MESSAGES = 5;
    private static final int MESSAGE_DURATION = 100; // ticks (5 seconds)

    public static void setHudEnabled(boolean enabled) {
        hudEnabled = enabled;
    }

    public static void setGameTimeRemaining(int time) {
        gameTimeRemaining = time;
    }

    public static void setGameState(String state) {
        gameState = state;
    }

    public static void setTransformed(boolean transformed) {
        isTransformed = transformed;
    }

    public static void setFrozen(boolean frozen) {
        isFrozen = frozen;
    }

    public static void setFreezeBarProgress(float progress) {
        freezeBarProgress = progress;
    }

    public static void setInCooldown(boolean cooldown) {
        isInCooldown = cooldown;
    }

    public static void setFreezeSecondsRemaining(int seconds) {
        freezeSecondsRemaining = seconds;
    }

    public static void addMessage(String message) {
        if (messageQueue.size() >= MAX_MESSAGES) {
            messageQueue.removeFirst();
        }
        messageQueue.add(new HudMessage(message, MESSAGE_DURATION));
    }

    private static class HudMessage {
        String text;
        int duration;

        HudMessage(String text, int duration) {
            this.text = text;
            this.duration = duration;
        }

        void tick() {
            duration--;
        }

        boolean isExpired() {
            return duration <= 0;
        }

        float getAlpha() {
            // Fade out in last 10 ticks
            if (duration < 10) {
                return duration / 10.0f;
            }
            return 1.0f;
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // Update message queue
        messageQueue.removeIf(HudMessage::isExpired);
        messageQueue.forEach(HudMessage::tick);

        // Auto-detect if HUD should be enabled (when player has invisibility = transformed)
        boolean autoHudEnabled = mc.player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);

        if (!hudEnabled && !autoHudEnabled) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // HUD position (top right corner)
        int hudX = screenWidth - 220;
        int hudY = 10;
        int lineHeight = 12;

        // Get all players in the world
        ClientLevel level = mc.level;
        List<Player> players = new ArrayList<>(level.players());

        // Draw background
        int hudWidth = 210;
        int hudHeight = 10 + lineHeight * (players.size() + 3);
        guiGraphics.fill(hudX - 5, hudY - 5, hudX + hudWidth, hudY + hudHeight, 0x80000000);

        // Draw title with game state
        String title = "§6§lPROP HUNT";
        guiGraphics.drawString(font, title, hudX, hudY, 0xFFFFFF);
        hudY += lineHeight + 3;

        // Draw game state and time
        String stateText = "§e" + gameState;
        guiGraphics.drawString(font, stateText, hudX, hudY, 0xFFFFFF);
        hudY += lineHeight;

        // Draw game time remaining
        int minutes = gameTimeRemaining / 1200;
        int seconds = (gameTimeRemaining % 1200) / 20;
        String timeText = String.format("§eTime: §f%02d:%02d", minutes, seconds);
        guiGraphics.drawString(font, timeText, hudX, hudY, 0xFFFFFF);
        hudY += lineHeight + 3;

        // Draw freeze bar if transformed (auto-detect using player effects)
        if (autoHudEnabled) {
            String barLabel;
            int barColor;
            float barProgress;

            // Use synchronized values from server
            if (isFrozen) {
                // Frozen state (red, depleting)
                barLabel = "§cFrozen: " + freezeSecondsRemaining + "s";
                barColor = 0xFFFF0000; // Rouge - se vide pendant le freeze
                barProgress = freezeBarProgress;
            } else if (isInCooldown) {
                // Cooldown state (orange, filling)
                barLabel = "§6Recharging...";
                barColor = 0xFFFF8800; // Orange - en recharge
                barProgress = freezeBarProgress;
            } else {
                // Ready state (green)
                barLabel = "§aReady: " + freezeSecondsRemaining + "s";
                barColor = 0xFF00FF00; // Vert - prêt à utiliser
                barProgress = freezeBarProgress;
            }

            guiGraphics.drawString(font, barLabel, hudX, hudY, 0xFFFFFF);
            hudY += lineHeight;

            // Draw bar background
            int barWidth = 180;
            int barHeight = 8;
            guiGraphics.fill(hudX, hudY, hudX + barWidth, hudY + barHeight, 0xFF333333);

            // Draw bar fill
            int fillWidth = (int) (barWidth * barProgress);
            guiGraphics.fill(hudX, hudY, hudX + fillWidth, hudY + barHeight, barColor);

            // Draw bar border
            guiGraphics.fill(hudX, hudY, hudX + barWidth, hudY + 1, 0xFFFFFFFF); // Top
            guiGraphics.fill(hudX, hudY + barHeight - 1, hudX + barWidth, hudY + barHeight, 0xFFFFFFFF); // Bottom
            guiGraphics.fill(hudX, hudY, hudX + 1, hudY + barHeight, 0xFFFFFFFF); // Left
            guiGraphics.fill(hudX + barWidth - 1, hudY, hudX + barWidth, hudY + barHeight, 0xFFFFFFFF); // Right

            hudY += barHeight + 5;
        }

        // Draw separator
        guiGraphics.fill(hudX, hudY, hudX + hudWidth - 10, hudY + 1, 0xFFFFFFFF);
        hudY += 5;

        // Draw player list
        for (Player player : players) {
            // Get team color from client cache
            String teamType = ClientPlayerDataCache.getPlayerTeam(player);
            String colorCode;
            String teamLabel;

            if ("PROPS".equals(teamType)) {
                colorCode = "§a"; // Green for props
                teamLabel = "[PROP]";
            } else if ("HUNTERS".equals(teamType)) {
                colorCode = "§c"; // Red for hunters
                teamLabel = "[HUNT]";
            } else {
                colorCode = "§f"; // White for unknown
                teamLabel = "[????]";
            }

            // Player name with team label
            String playerName = colorCode + teamLabel + " §f" + player.getName().getString();

            // HP (hearts)
            int health = (int) Math.ceil(player.getHealth());
            int maxHealth = (int) Math.ceil(player.getMaxHealth());
            String hpText = String.format("§f❤ %d/%d", health, maxHealth);

            // Draw player info
            String playerInfo = playerName + " " + hpText;
            guiGraphics.drawString(font, playerInfo, hudX, hudY, 0xFFFFFF);

            hudY += lineHeight;
        }

        // Draw messages at bottom center of screen
        if (!messageQueue.isEmpty()) {
            int messageStartY = screenHeight - 60;
            int messageX = screenWidth / 2;

            for (HudMessage message : messageQueue) {
                int alpha = (int) (message.getAlpha() * 255);
                int color = 0xFFFFFFFF | (alpha << 24);

                guiGraphics.drawCenteredString(font, message.text, messageX, messageStartY, color);
                messageStartY -= lineHeight + 2;
            }
        }
    }
}
