package com.yourname.prophunt;

import com.yourname.prophunt.commands.PropHuntCommands;
import com.yourname.prophunt.config.PropHuntConfig;
import com.yourname.prophunt.game.GameManager;
import com.yourname.prophunt.game.PropHuntGame;
import com.yourname.prophunt.network.NetworkHandler;
import com.yourname.prophunt.sounds.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(PropHuntMod.MODID)
public class PropHuntMod {
    public static final String MODID = "prophunt";
    public static final Logger LOGGER = LoggerFactory.getLogger(PropHuntMod.class);

    private static GameManager gameManager;

    public PropHuntMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Prop Hunt Mod");

        // Register config
        modContainer.registerConfig(ModConfig.Type.COMMON, PropHuntConfig.COMMON_SPEC);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);

        // Register sounds
        ModSounds.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);

        gameManager = new GameManager();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Prop Hunt common setup");
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        LOGGER.info("Registering network payloads");
        NetworkHandler.register(event);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        PropHuntCommands.register(event.getDispatcher());
        LOGGER.info("Prop Hunt commands registered");
    }

    private void onServerTick(ServerTickEvent.Post event) {
        if (gameManager != null) {
            gameManager.tick(event.getServer());
        }
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LOGGER.info("Player logged out: " + player.getName().getString());

            if (gameManager != null) {
                PropHuntGame game = gameManager.getCurrentGame();
                if (game != null && game.isPlayerInGame(player)) {
                    game.removePlayer(player);
                    LOGGER.info("Removed player from PropHunt game: " + player.getName().getString());
                }
            }
        }
    }

    private void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping - cleaning up PropHunt games");

        if (gameManager != null) {
            PropHuntGame game = gameManager.getCurrentGame();
            if (game != null) {
                game.stop(event.getServer());
                LOGGER.info("PropHunt game stopped due to server shutdown");
            }
        }
    }

    public static GameManager getGameManager() {
        return gameManager;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
