package com.yourname.prophunt.game;

import com.yourname.prophunt.config.PropHuntConfig;
import com.yourname.prophunt.sounds.ModSounds;
import com.yourname.prophunt.teams.PropHuntTeam;
import com.yourname.prophunt.teams.TeamType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Supplier;

import com.yourname.prophunt.game.HunterLoadout;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.network.chat.Style;
import com.yourname.prophunt.network.HudSyncPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public class PropHuntGame {
    private GameState state = GameState.LOBBY;
    private final Map<UUID, ServerPlayer> players = new HashMap<>();
    private final Map<UUID, PropHuntTeam> playerTeams = new HashMap<>();
    private final Map<UUID, PropTransformation> propTransformations = new HashMap<>();

    private int gameTimer = 0;
    private int maxGameTime = 6000; // 5 minutes (20 ticks/sec * 300 sec)
    private int hideTime = 600; // 30 seconds hiding time
    private int lobbyTime = 1200; // 60 seconds lobby countdown
    private Vec3 spawnLocation;
    private MapManager.GameMapData currentMap = null;

    private final Map<TeamType, Integer> scores = new HashMap<>();

    // Whistle system - props whistle one at a time
    private int whistleTimer = 0;
    private int whistleInterval = 600; // 30 seconds (from config)
    private final Queue<UUID> whistleQueue = new LinkedList<>();
    private int whistleDelayTimer = 0;
    private static final int WHISTLE_DELAY = 40; // 2 seconds between each prop

    // Hunter help item system - every minute hunters get an item to locate props
    private int hunterHelpTimer = 0;
    private static final int HUNTER_HELP_INTERVAL = 1200; // 1 minute (60 seconds * 20 ticks)
    private static final String HUNTER_HELP_ITEM_TAG = "prophunt_hunter_help";

    // Prop decoy item system - every minute props get an item to create decoy sounds
    private int propDecoyTimer = 0;
    private static final int PROP_DECOY_INTERVAL = 1200; // 1 minute (60 seconds * 20 ticks)
    private static final String PROP_DECOY_ITEM_TAG = "prophunt_prop_decoy";

    // Saved player inventories - restored when game ends or player leaves
    private final Map<UUID, List<ItemStack>> savedInventories = new HashMap<>();

    private final Random random = new Random();

    public PropHuntGame() {
        scores.put(TeamType.PROPS, 0);
        scores.put(TeamType.HUNTERS, 0);
    }

    public boolean addPlayer(ServerPlayer player) {
        if (state != GameState.LOBBY) {
            return false;
        }

        // Save player inventory before joining
        savePlayerInventory(player);

        players.put(player.getUUID(), player);

        int secondsLeft = (lobbyTime - gameTimer) / 20;
        player.sendSystemMessage(Component.literal("§aYou joined the Prop Hunt game!"));
        player.sendSystemMessage(Component.literal("§7Your inventory has been saved and will be restored after the game."));
        player.sendSystemMessage(Component.literal("§e" + secondsLeft + " seconds until the game starts..."));

        // Teleport to lobby spawn if map is set
        if (currentMap != null && player.level() instanceof ServerLevel serverLevel) {
            try {
                // Teleport to center of map as temporary lobby location
                double spawnX = currentMap.propSpawnX + 0.5;
                double spawnY = currentMap.propSpawnY;
                double spawnZ = currentMap.propSpawnZ + 0.5;
                player.teleportTo(serverLevel, spawnX, spawnY, spawnZ, 0, 0);
                player.sendSystemMessage(Component.literal("§7Teleported to lobby spawn!"));
            } catch (Exception e) {
                System.err.println("[PropHunt] Teleport to lobby spawn failed: " + e.getMessage());
            }
        }

        return true;
    }

    public boolean isPlayerInGame(ServerPlayer player) {
        return players.containsKey(player.getUUID());
    }

    public void removePlayer(ServerPlayer player) {
        // Cleanup transformation and display entities
        PropTransformation transformation = propTransformations.get(player.getUUID());
        if (transformation != null) {
            transformation.revert();
        }

        // Restore player's original inventory and remove all effects
        restorePlayerInventory(player);
        player.removeAllEffects();

        // Disable HUD for this player
        try {
            PacketDistributor.sendToPlayer(player, new HudSyncPayload(0, false, "LOBBY"));
        } catch (Exception e) {
            System.err.println("[PropHunt] Failed to send HUD disable packet: " + e.getMessage());
        }

        players.remove(player.getUUID());
        playerTeams.remove(player.getUUID());
        propTransformations.remove(player.getUUID());
    }

    public void start(MinecraftServer server) {
        state = GameState.LOBBY;
        gameTimer = 0;

        System.out.println("[PropHunt] ==================== LOBBY STARTED ====================");
        System.out.println("[PropHunt] State changed to: " + state);
        System.out.println("[PropHunt] lobbyTime = " + lobbyTime + " ticks (60 seconds)");
        System.out.println("[PropHunt] Waiting 60 seconds for players to join");

        // Broadcast that lobby is starting to ALL players on server
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal("§6=== PROP HUNT LOBBY OPENED ==="));
            player.sendSystemMessage(Component.literal("§e60 seconds to join! Use: §a/prophunt join"));
            player.sendSystemMessage(Component.literal("§7Waiting for more players..."));
        }

        System.out.println("[PropHunt] Lobby started - waiting 60 seconds for players to join");

        // Debug: Log map status
        System.out.println("[PropHunt] Starting lobby - currentMap is: " + (currentMap == null ? "NULL" : "SET to " + currentMap.name));
        if (currentMap != null) {
            System.out.println("[PropHunt] Hunter spawn: (" + currentMap.hunterSpawnX + ", " + currentMap.hunterSpawnY + ", " + currentMap.hunterSpawnZ + ")");
            System.out.println("[PropHunt] Prop spawn: (" + currentMap.propSpawnX + ", " + currentMap.propSpawnY + ", " + currentMap.propSpawnZ + ")");
        }
    }

    public void tick(MinecraftServer server) {
        // Don't tick if game is finished or ending
        if (state == GameState.FINISHED || state == GameState.ENDING) {
            return;
        }

        gameTimer++;

        // Update all active transformations
        for (PropTransformation transformation : propTransformations.values()) {
            transformation.setGameState(state); // Set current game state
            transformation.update();
        }

        // Keep all players fully fed during active game phases
        // And give props Speed 2 effect
        if (state == GameState.HIDING || state == GameState.PLAYING) {
            for (ServerPlayer player : players.values()) {
                if (player != null && player.isAlive()) {
                    player.getFoodData().setFoodLevel(20);
                    player.getFoodData().setSaturation(20.0f);

                    // Give props Speed 2 effect (refresh every 2 seconds to keep it active)
                    PropHuntTeam team = playerTeams.get(player.getUUID());
                    if (team != null && team.getType() == TeamType.PROPS && gameTimer % 40 == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false));
                    }
                }
            }
        }

        // Debug logs
        if (gameTimer % 200 == 0) { // Every 10 seconds
            System.out.println("[PropHunt TICK] State: " + state + ", Timer: " + gameTimer + ", Players: " + players.size());
        }

        switch (state) {
            case LOBBY:
                tickLobby(server);
                break;
            case HIDING:
                tickHiding(server);
                break;
            case PLAYING:
                tickPlaying(server);
                break;
            default:
                break;
        }
    }

    private void tickLobby(MinecraftServer server) {
        if (gameTimer >= lobbyTime) {
            System.out.println("[PropHunt] ==================== LOBBY TIME EXPIRED ====================");
            System.out.println("[PropHunt] gameTimer (" + gameTimer + ") >= lobbyTime (" + lobbyTime + ")");
            // Time's up, start the game phase
            startGamePhase(server);
        } else if (gameTimer % 200 == 0) { // Every 10 seconds
            int secondsLeft = (lobbyTime - gameTimer) / 20;
            String message = "§e" + secondsLeft + " seconds to join!";

            System.out.println("[PropHunt] Lobby countdown: " + secondsLeft + "s remaining (gameTimer: " + gameTimer + ")");

            // Send to all server players
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(Component.literal(message));
            }

            // Also send to game players for HUD display
            broadcastToHud(message);
        }
    }

    private void startGamePhase(MinecraftServer server) {
        System.out.println("[PropHunt] ==================== STARTING GAME PHASE ====================");
        System.out.println("[PropHunt] Current player count: " + players.size());

        // Check if we have at least 2 players
        if (players.size() < 2) {
            System.out.println("[PropHunt] ❌ Cannot start game - need at least 2 players (currently: " + players.size() + ")");

            // Notify all server players that game couldn't start
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(Component.literal("§cGame cannot start - need at least 2 players! (Currently: " + players.size() + ")"));
            }

            // Reset timer and stay in LOBBY for more 60 seconds
            state = GameState.LOBBY;
            gameTimer = 0;

            // Broadcast restart of lobby timer
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(Component.literal("§e60 second lobby restarted!"));
            }
            return;
        }

        System.out.println("[PropHunt] ✓ Enough players! Starting game...");

        state = GameState.STARTING;
        gameTimer = 0;

        // Assign teams first
        assignTeams();
        System.out.println("[PropHunt] Teams assigned!");

        // Broadcast role assignment to all server players
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal("§6=== GAME STARTING ==="));
            player.sendSystemMessage(Component.literal("§eRoles assigned! Props have 30 seconds to hide!"));
        }

        // Send team info to game players
        for (ServerPlayer player : players.values()) {
            PropHuntTeam team = playerTeams.get(player.getUUID());
            if (team != null) {
                String teamName = team.getType() == TeamType.HUNTERS ? "§cHunter" : "§aProp";
                player.sendSystemMessage(Component.literal("§6You are a " + teamName + "!"));
            }
        }

        // Setup players and distribute blocks
        for (ServerPlayer player : players.values()) {
            PropHuntTeam team = playerTeams.get(player.getUUID());
            if (team != null) {
                setupPlayer(player, team);
            }
        }

        state = GameState.HIDING;
    }

    private void tickHiding(MinecraftServer server) {
        if (gameTimer >= hideTime) {
            startHunting(server);
        } else if (gameTimer % 200 == 0) { // Every 10 seconds
            int secondsLeft = (hideTime - gameTimer) / 20;
            broadcastToHud("§e" + secondsLeft + " seconds until hunters are released!");
        }
    }

    private void startHunting(MinecraftServer server) {
        state = GameState.PLAYING;
        gameTimer = 0;

        // Remove blindness from hunters
        for (Map.Entry<UUID, PropHuntTeam> entry : playerTeams.entrySet()) {
            if (entry.getValue().getType() == TeamType.HUNTERS) {
                ServerPlayer hunter = players.get(entry.getKey());
                if (hunter != null) {
                    hunter.removeEffect(MobEffects.BLINDNESS);
                    hunter.sendSystemMessage(Component.literal("§cGo hunt the props!"));
                }
            }
        }

        broadcastToHud("§6=== HUNTING PHASE STARTED ===");
    }

    private void tickPlaying(MinecraftServer server) {
        if (gameTimer >= maxGameTime) {
            System.out.println("[PropHunt] ==================== GAME TIME EXPIRED ====================");
            System.out.println("[PropHunt] Props survived 5 minutes and WIN!");
            endGame(server, TeamType.PROPS);
            return;
        }

        // Check win conditions
        boolean allPropsEliminated = true;
        int propCount = 0;
        int hunterCount = 0;

        for (Map.Entry<UUID, PropHuntTeam> entry : playerTeams.entrySet()) {
            if (entry.getValue().getType() == TeamType.PROPS) {
                propCount++;
                ServerPlayer player = players.get(entry.getKey());
                if (player != null && player.isAlive()) {
                    allPropsEliminated = false;
                }
            } else if (entry.getValue().getType() == TeamType.HUNTERS) {
                hunterCount++;
            }
        }

        // Hunters win if all props are eliminated
        if (allPropsEliminated && propCount > 0) {
            System.out.println("[PropHunt] ==================== ALL PROPS ELIMINATED ====================");
            System.out.println("[PropHunt] Hunters WIN! (All " + propCount + " props eliminated)");
            endGame(server, TeamType.HUNTERS);
            return;
        }

        // Props win if all remaining players are hunters (all props converted)
        if (propCount == 0 && hunterCount > 0) {
            System.out.println("[PropHunt] ==================== ALL PROPS CONVERTED ====================");
            System.out.println("[PropHunt] Hunters WIN! (All props converted to hunters before game end)");
            endGame(server, TeamType.HUNTERS);
            return;
        }

        // Time remaining notifications
        if (gameTimer % 1200 == 0) { // Every minute
            int minutesLeft = (maxGameTime - gameTimer) / 1200;
            if (minutesLeft > 0) {
                broadcastToHud("§e" + minutesLeft + " minute(s) remaining!");
            }
        }

        // Whistle system - props whistle one at a time
        whistleTimer++;
        int configWhistleInterval = PropHuntConfig.getPropSoundIntervalTicks();

        // Every X seconds, queue all props to whistle
        if (whistleTimer >= configWhistleInterval) {
            whistleTimer = 0;
            whistleQueue.clear();

            // Add all living props to the whistle queue
            for (Map.Entry<UUID, PropHuntTeam> entry : playerTeams.entrySet()) {
                if (entry.getValue().getType() == TeamType.PROPS) {
                    ServerPlayer player = players.get(entry.getKey());
                    if (player != null && player.isAlive()) {
                        whistleQueue.add(entry.getKey());
                    }
                }
            }

            whistleDelayTimer = 0; // Reset delay timer
        }

        // Process whistle queue - one prop every 2 seconds
        if (!whistleQueue.isEmpty()) {
            whistleDelayTimer++;

            if (whistleDelayTimer >= WHISTLE_DELAY) {
                whistleDelayTimer = 0;

                UUID propUUID = whistleQueue.poll();
                if (propUUID != null) {
                    ServerPlayer propPlayer = players.get(propUUID);
                    if (propPlayer != null && propPlayer.isAlive() && propPlayer.level() instanceof ServerLevel serverLevel) {
                        Vec3 pos = propPlayer.position();

                        // Play whistle sound at prop location
                        serverLevel.playSound(
                            null,
                            pos.x, pos.y, pos.z,
                            ModSounds.PROP_WHISTLE.get(),
                            SoundSource.PLAYERS,
                            1.0f, 1.0f
                        );

                        // Spawn note particle above the prop
                        serverLevel.sendParticles(
                            ParticleTypes.NOTE,
                            pos.x, pos.y + 2.0, pos.z,
                            5, // count
                            0.3, 0.2, 0.3, // spread
                            0.0 // speed
                        );

                        System.out.println("[PropHunt] Whistle: " + propPlayer.getName().getString() + " at " + pos);
                    }
                }
            }
        }

        // Hunter help item system - give item every minute
        hunterHelpTimer++;
        if (hunterHelpTimer >= HUNTER_HELP_INTERVAL) {
            hunterHelpTimer = 0;
            giveHunterHelpItems();
        }

        // Check if any hunter has the help item in offhand
        checkHunterHelpItemUsage();

        // Prop decoy item system - give item every minute
        propDecoyTimer++;
        if (propDecoyTimer >= PROP_DECOY_INTERVAL) {
            propDecoyTimer = 0;
            givePropDecoyItems();
        }

        // Check if any prop has the decoy item in offhand
        checkPropDecoyItemUsage();
    }

    /**
     * Give hunter help items to all hunters
     */
    private void giveHunterHelpItems() {
        for (Map.Entry<UUID, PropHuntTeam> entry : playerTeams.entrySet()) {
            if (entry.getValue().getType() == TeamType.HUNTERS) {
                ServerPlayer hunter = players.get(entry.getKey());
                if (hunter != null && hunter.isAlive()) {
                    ItemStack helpItem = createHunterHelpItem();
                    hunter.getInventory().add(helpItem);
                    hunter.sendSystemMessage(Component.literal("§6You received a §eProp Locator§6! Put it in your offhand to use it."));
                }
            }
        }
        broadcastToHud("§6Hunters received Prop Locators!");
    }

    /**
     * Create the hunter help item (compass with custom lore)
     */
    private ItemStack createHunterHelpItem() {
        ItemStack item = new ItemStack(Items.COMPASS);
        item.set(DataComponents.CUSTOM_NAME, Component.literal("§6§lProp Locator").withStyle(Style.EMPTY.withItalic(false)));

        List<Component> loreLines = List.of(
            Component.literal("§7Switch to offhand to help hunters").withStyle(Style.EMPTY.withItalic(false)),
            Component.literal("§7Plays a sound on a random prop!").withStyle(Style.EMPTY.withItalic(false)),
            Component.literal("§8" + HUNTER_HELP_ITEM_TAG).withStyle(Style.EMPTY.withItalic(false))
        );
        item.set(DataComponents.LORE, new ItemLore(loreLines));

        return item;
    }

    /**
     * Check if any hunter has the help item in their offhand and use it
     */
    private void checkHunterHelpItemUsage() {
        for (Map.Entry<UUID, PropHuntTeam> entry : playerTeams.entrySet()) {
            if (entry.getValue().getType() == TeamType.HUNTERS) {
                ServerPlayer hunter = players.get(entry.getKey());
                if (hunter != null && hunter.isAlive()) {
                    ItemStack offhandItem = hunter.getOffhandItem();

                    // Check if it's our hunter help item
                    if (isHunterHelpItem(offhandItem)) {
                        // Use the item - play sound on random prop
                        useHunterHelpItem(hunter);

                        // Remove the item from offhand
                        hunter.getInventory().offhand.set(0, ItemStack.EMPTY);
                    }
                }
            }
        }
    }

    /**
     * Check if an item is a hunter help item
     */
    private boolean isHunterHelpItem(ItemStack item) {
        if (item.isEmpty() || !item.is(Items.COMPASS)) {
            return false;
        }

        ItemLore lore = item.get(DataComponents.LORE);
        if (lore == null) {
            return false;
        }

        // Check if lore contains our tag
        for (Component line : lore.lines()) {
            if (line.getString().contains(HUNTER_HELP_ITEM_TAG)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Use the hunter help item - play random sound on a random living prop
     */
    private void useHunterHelpItem(ServerPlayer hunter) {
        // Find all living props
        List<ServerPlayer> livingProps = new ArrayList<>();
        for (Map.Entry<UUID, PropHuntTeam> entry : playerTeams.entrySet()) {
            if (entry.getValue().getType() == TeamType.PROPS) {
                ServerPlayer prop = players.get(entry.getKey());
                if (prop != null && prop.isAlive()) {
                    livingProps.add(prop);
                }
            }
        }

        if (livingProps.isEmpty()) {
            hunter.sendSystemMessage(Component.literal("§cNo props left to locate!"));
            return;
        }

        // Pick a random prop
        ServerPlayer targetProp = livingProps.get(random.nextInt(livingProps.size()));

        // Pick a random sound
        List<Supplier<SoundEvent>> sounds = ModSounds.getHunterHelpSounds();
        SoundEvent randomSound = sounds.get(random.nextInt(sounds.size())).get();

        // Play the sound at the prop's location for ALL players in the game
        if (targetProp.level() instanceof ServerLevel serverLevel) {
            Vec3 propPos = targetProp.position();

            // Play sound at prop location with very high volume for long range
            // Send to all players in the game so they can hear it from anywhere
            for (ServerPlayer gamePlayer : players.values()) {
                if (gamePlayer != null && gamePlayer.isAlive()) {
                    // Play the sound at the PROP's position for each player
                    gamePlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        net.minecraft.core.Holder.direct(randomSound),
                        SoundSource.PLAYERS,
                        propPos.x, propPos.y, propPos.z,
                        4.0f, // Very loud volume
                        1.0f, // Normal pitch
                        serverLevel.getRandom().nextLong()
                    ));
                }
            }

            // Send particles above the prop
            serverLevel.sendParticles(
                ParticleTypes.NOTE,
                propPos.x, propPos.y + 2.5, propPos.z,
                10, // more particles
                0.5, 0.3, 0.5,
                0.0
            );

            hunter.sendSystemMessage(Component.literal("§aProp Locator used! Listen for the sound..."));
            broadcastToHud("§6" + hunter.getName().getString() + " §eused a Prop Locator!");

            System.out.println("[PropHunt] Hunter " + hunter.getName().getString() + " used Prop Locator on " + targetProp.getName().getString() + " at (" + propPos.x + ", " + propPos.y + ", " + propPos.z + ")");
        }
    }

    /**
     * Give prop decoy items to all props
     */
    private void givePropDecoyItems() {
        for (Map.Entry<UUID, PropHuntTeam> entry : playerTeams.entrySet()) {
            if (entry.getValue().getType() == TeamType.PROPS) {
                ServerPlayer prop = players.get(entry.getKey());
                if (prop != null && prop.isAlive()) {
                    ItemStack decoyItem = createPropDecoyItem();
                    prop.getInventory().add(decoyItem);
                    prop.sendSystemMessage(Component.literal("§aYou received a §eDecoy§a! Put it in your offhand to distract hunters."));
                }
            }
        }
        broadcastToHud("§aProps received Decoys!");
    }

    /**
     * Create the prop decoy item (feather with custom lore)
     */
    private ItemStack createPropDecoyItem() {
        ItemStack item = new ItemStack(Items.FEATHER);
        item.set(DataComponents.CUSTOM_NAME, Component.literal("§a§lDecoy").withStyle(Style.EMPTY.withItalic(false)));

        List<Component> loreLines = List.of(
            Component.literal("§7Switch to offhand to use").withStyle(Style.EMPTY.withItalic(false)),
            Component.literal("§7Plays a sound on a random prop!").withStyle(Style.EMPTY.withItalic(false)),
            Component.literal("§8" + PROP_DECOY_ITEM_TAG).withStyle(Style.EMPTY.withItalic(false))
        );
        item.set(DataComponents.LORE, new ItemLore(loreLines));

        return item;
    }

    /**
     * Check if any prop has the decoy item in their offhand and use it
     */
    private void checkPropDecoyItemUsage() {
        for (Map.Entry<UUID, PropHuntTeam> entry : playerTeams.entrySet()) {
            if (entry.getValue().getType() == TeamType.PROPS) {
                ServerPlayer prop = players.get(entry.getKey());
                if (prop != null && prop.isAlive()) {
                    ItemStack offhandItem = prop.getOffhandItem();

                    // Check if it's our prop decoy item
                    if (isPropDecoyItem(offhandItem)) {
                        // Use the item - play fart sound on random prop
                        usePropDecoyItem(prop);

                        // Remove the item from offhand
                        prop.getInventory().offhand.set(0, ItemStack.EMPTY);
                    }
                }
            }
        }
    }

    /**
     * Check if an item is a prop decoy item
     */
    private boolean isPropDecoyItem(ItemStack item) {
        if (item.isEmpty() || !item.is(Items.FEATHER)) {
            return false;
        }

        ItemLore lore = item.get(DataComponents.LORE);
        if (lore == null) {
            return false;
        }

        // Check if lore contains our tag
        for (Component line : lore.lines()) {
            if (line.getString().contains(PROP_DECOY_ITEM_TAG)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Use the prop decoy item - play fart sound on a random living prop (including self)
     */
    private void usePropDecoyItem(ServerPlayer user) {
        // Find all living props
        List<ServerPlayer> livingProps = new ArrayList<>();
        for (Map.Entry<UUID, PropHuntTeam> entry : playerTeams.entrySet()) {
            if (entry.getValue().getType() == TeamType.PROPS) {
                ServerPlayer prop = players.get(entry.getKey());
                if (prop != null && prop.isAlive()) {
                    livingProps.add(prop);
                }
            }
        }

        if (livingProps.isEmpty()) {
            user.sendSystemMessage(Component.literal("§cNo props to decoy!"));
            return;
        }

        // Pick a random prop (can be any prop including user)
        ServerPlayer targetProp = livingProps.get(random.nextInt(livingProps.size()));

        // Play fart sound at the target prop's location for ALL players
        if (targetProp.level() instanceof ServerLevel serverLevel) {
            Vec3 propPos = targetProp.position();

            // Send sound to all players in the game at the PROP's position
            for (ServerPlayer gamePlayer : players.values()) {
                if (gamePlayer != null && gamePlayer.isAlive()) {
                    gamePlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        net.minecraft.core.Holder.direct(ModSounds.PROP_DECOY_FART.get()),
                        SoundSource.PLAYERS,
                        propPos.x, propPos.y, propPos.z,
                        4.0f, // Loud volume
                        1.0f, // Normal pitch
                        serverLevel.getRandom().nextLong()
                    ));
                }
            }

            // Send particles
            serverLevel.sendParticles(
                ParticleTypes.CLOUD,
                propPos.x, propPos.y + 1.0, propPos.z,
                8,
                0.3, 0.2, 0.3,
                0.02
            );

            user.sendSystemMessage(Component.literal("§aDecoy used! A sound was played..."));
            System.out.println("[PropHunt] Prop " + user.getName().getString() + " used Decoy on " + targetProp.getName().getString() + " at (" + propPos.x + ", " + propPos.y + ", " + propPos.z + ")");
        }
    }

    private void assignTeams() {
        List<ServerPlayer> playerList = new ArrayList<>(players.values());
        Collections.shuffle(playerList);

        int hunterCount = Math.max(1, playerList.size() / 3); // 1/3 hunters

        for (int i = 0; i < playerList.size(); i++) {
            ServerPlayer player = playerList.get(i);
            TeamType teamType = i < hunterCount ? TeamType.HUNTERS : TeamType.PROPS;
            PropHuntTeam team = new PropHuntTeam(teamType);
            playerTeams.put(player.getUUID(), team);

            String teamName = teamType == TeamType.HUNTERS ? "§cHunter" : "§aProp";
            player.sendSystemMessage(Component.literal("§6You are a " + teamName + "!"));
        }
    }

    private void setupPlayer(ServerPlayer player, PropHuntTeam team) {
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getInventory().clearContent();

        // Debug: Initial status
        System.out.println("[PropHunt] setupPlayer called for " + player.getName().getString() + " (team: " + team.getType() + ")");
        System.out.println("[PropHunt] currentMap is: " + (currentMap == null ? "NULL" : currentMap.name));
        System.out.println("[PropHunt] player.level() instanceof ServerLevel: " + (player.level() instanceof ServerLevel));

        // Teleport to team-specific spawn if map is set
        if (currentMap != null && player.level() instanceof ServerLevel serverLevel) {
            double spawnX, spawnY, spawnZ;

            if (team.getType() == TeamType.HUNTERS) {
                spawnX = currentMap.hunterSpawnX + 0.5;
                spawnY = currentMap.hunterSpawnY;
                spawnZ = currentMap.hunterSpawnZ + 0.5;
            } else {
                spawnX = currentMap.propSpawnX + 0.5;
                spawnY = currentMap.propSpawnY;
                spawnZ = currentMap.propSpawnZ + 0.5;
            }

            // Debug log
            System.out.println("[PropHunt] Attempting to teleport " + player.getName().getString() + " to (" + spawnX + ", " + spawnY + ", " + spawnZ + ")");

            try {
                player.teleportTo(serverLevel, spawnX, spawnY, spawnZ, player.getYRot(), player.getXRot());
                System.out.println("[PropHunt] Teleport successful!");
                broadcastToHud("§e[SPAWN] " + player.getName().getString() + " spawned at map location");
            } catch (Exception e) {
                System.err.println("[PropHunt] Teleport failed: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("[PropHunt] No map set (currentMap=" + currentMap + ", isServerLevel=" + (player.level() instanceof ServerLevel) + "), using default spawn");
        }

        // Apply saturation effect to all players (infinite duration)
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, Integer.MAX_VALUE, 0, false, false));

        if (team.getType() == TeamType.HUNTERS) {
            // Give hunters blindness and paralysis during hiding phase
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, hideTime, 0, false, false));
            // Apply slowness level 255 to prevent movement during hiding
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, hideTime, 255, false, false));
            player.sendSystemMessage(Component.literal("§7Wait while props hide... You are frozen!"));
            broadcastToHud("§c§l[HUNTERS] " + player.getName().getString() + " is waiting...");

            // Equip hunter with configured weapons and ammunition
            HunterLoadout loadout = HunterLoadout.getInstance();
            loadout.equipHunter(player);
        } else {
            // Props get speed boost and random block transformation
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, hideTime, 1, false, false));
            broadcastToHud("§a§l[PROPS] " + player.getName().getString() + " is hiding!");

            // Distribute random block from map if available
            if (currentMap != null && !currentMap.propBlocks.isEmpty()) {
                MapManager.PropBlockData randomBlockData = currentMap.getRandomBlock();
                if (randomBlockData != null) {
                    PropTransformation transformation = new PropTransformation(player);
                    propTransformations.put(player.getUUID(), transformation);
                    // Transform with or without NBT based on block data
                    if (randomBlockData.nbtData != null) {
                        transformation.transformIntoBlockWithNBT(randomBlockData.blockState, randomBlockData.nbtData);
                    } else {
                        transformation.transformIntoBlock(randomBlockData.blockState);
                    }
                    player.sendSystemMessage(Component.literal("§eYou transformed into: §f" + randomBlockData.blockState.getBlock().getName().getString()));
                }
            }
        }
    }

    private void endGame(MinecraftServer server, TeamType winner) {
        state = GameState.ENDING;

        String winnerName = winner == TeamType.PROPS ? "§a§lPROP WIN!" : "§c§lHUNTER WIN!";
        broadcastToHud("§6=== GAME OVER ===");
        broadcastToHud(winnerName);

        // Send chat message to all players and play win sound
        SoundEvent winSound = winner == TeamType.PROPS ? ModSounds.PROP_WIN.get() : ModSounds.HUNTER_WIN.get();
        for (ServerPlayer player : players.values()) {
            player.sendSystemMessage(Component.literal("§6§l============================="));
            player.sendSystemMessage(Component.literal(winnerName));
            player.sendSystemMessage(Component.literal("§6§l============================="));

            // Play win sound to each player
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    winSound,
                    SoundSource.PLAYERS,
                    1.0f, 1.0f
                );
            }
        }

        // Award points
        int points = winner == TeamType.PROPS ? 100 : 50;
        scores.put(winner, scores.get(winner) + points);

        // Get server level for cleanup
        ServerLevel level = null;
        if (!players.isEmpty()) {
            ServerPlayer anyPlayer = players.values().iterator().next();
            if (anyPlayer.level() instanceof ServerLevel) {
                level = (ServerLevel) anyPlayer.level();
            }
        }

        // Cleanup transformations and display entities
        for (UUID playerUUID : new ArrayList<>(propTransformations.keySet())) {
            PropTransformation transformation = propTransformations.get(playerUUID);
            if (transformation != null) {
                transformation.revert();
            }
            propTransformations.remove(playerUUID);
        }

        // Restore inventories, remove effects and disable HUD for players
        for (ServerPlayer player : players.values()) {
            player.removeAllEffects();
            restorePlayerInventory(player);
            try {
                PacketDistributor.sendToPlayer(player, new HudSyncPayload(0, false, "FINISHED"));
            } catch (Exception e) {
                System.err.println("[PropHunt] Failed to send HUD disable packet: " + e.getMessage());
            }
        }

        // Clear all frozen blocks
        if (level != null) {
            FrozenPropBlockManager.clear(level);
        }

        // Clear players, teams and saved inventories
        players.clear();
        playerTeams.clear();
        savedInventories.clear();

        state = GameState.FINISHED;
        System.out.println("[PropHunt] Game finished and cleaned up");
    }

    public void stop(MinecraftServer server) {
        broadcastToHud("§cGame stopped by admin");

        // Get server level for cleanup
        ServerLevel level = null;
        if (!players.isEmpty()) {
            ServerPlayer anyPlayer = players.values().iterator().next();
            if (anyPlayer.level() instanceof ServerLevel) {
                level = (ServerLevel) anyPlayer.level();
            }
        }

        // Cleanup transformations and display entities
        for (UUID playerUUID : new ArrayList<>(propTransformations.keySet())) {
            PropTransformation transformation = propTransformations.get(playerUUID);
            if (transformation != null) {
                transformation.revert();
            }
            propTransformations.remove(playerUUID);
        }

        // Restore inventories, remove effects and disable HUD for players
        for (ServerPlayer player : players.values()) {
            player.removeAllEffects();
            restorePlayerInventory(player);
            try {
                PacketDistributor.sendToPlayer(player, new HudSyncPayload(0, false, "STOPPED"));
            } catch (Exception e) {
                System.err.println("[PropHunt] Failed to send HUD disable packet: " + e.getMessage());
            }
        }

        // Clear all frozen blocks
        if (level != null) {
            FrozenPropBlockManager.clear(level);
        }

        players.clear();
        playerTeams.clear();
        savedInventories.clear();
        state = GameState.LOBBY;
    }

    /**
     * Save a player's inventory before the game
     */
    private void savePlayerInventory(ServerPlayer player) {
        List<ItemStack> inventory = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            inventory.add(stack.copy()); // Copy to avoid reference issues
        }
        savedInventories.put(player.getUUID(), inventory);
        System.out.println("[PropHunt] Saved inventory for " + player.getName().getString() + " (" + inventory.size() + " slots)");
    }

    /**
     * Restore a player's inventory after the game
     */
    private void restorePlayerInventory(ServerPlayer player) {
        List<ItemStack> savedInventory = savedInventories.get(player.getUUID());
        if (savedInventory != null) {
            player.getInventory().clearContent();
            for (int i = 0; i < savedInventory.size() && i < player.getInventory().getContainerSize(); i++) {
                player.getInventory().setItem(i, savedInventory.get(i).copy());
            }
            savedInventories.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("§aYour inventory has been restored!"));
            System.out.println("[PropHunt] Restored inventory for " + player.getName().getString());
        }
    }

    /**
     * Remove all special PropHunt items from a player's inventory (kept for potential future use)
     */
    private void removeSpecialItems(ServerPlayer player) {
        // Remove from main inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isHunterHelpItem(stack) || isPropDecoyItem(stack)) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }

        // Remove from offhand
        if (isHunterHelpItem(player.getOffhandItem()) || isPropDecoyItem(player.getOffhandItem())) {
            player.getInventory().offhand.set(0, ItemStack.EMPTY);
        }
    }

    private void broadcastToAll(Component message) {
        for (ServerPlayer player : players.values()) {
            player.sendSystemMessage(message);
        }
    }

    public void broadcastToHud(String message) {
        com.yourname.prophunt.PropHuntMod.getGameManager().broadcastHudMessage(message);
    }

    public boolean isActive() {
        return state == GameState.HIDING || state == GameState.PLAYING;
    }

    public boolean hasStarted() {
        return state != GameState.LOBBY;
    }

    public GameState getState() {
        return state;
    }

    public Collection<ServerPlayer> getPlayers() {
        return players.values();
    }

    public PropHuntTeam getPlayerTeam(UUID playerId) {
        return playerTeams.get(playerId);
    }

    public PropTransformation getTransformation(UUID playerId) {
        return propTransformations.get(playerId);
    }

    public void setTransformation(UUID playerId, PropTransformation transformation) {
        propTransformations.put(playerId, transformation);
    }

    public void setCurrentMap(MapManager.GameMapData map) {
        this.currentMap = map;
    }

    public MapManager.GameMapData getCurrentMap() {
        return currentMap;
    }

    public Map<TeamType, Integer> getScores() {
        return scores;
    }

    public void startTestMode(MinecraftServer server, ServerPlayer player) {
        state = GameState.STARTING;
        gameTimer = 0;

        // Assign player to Props team
        PropHuntTeam team = new PropHuntTeam(TeamType.PROPS);
        playerTeams.put(player.getUUID(), team);

        // Setup player
        setupPlayer(player, team);

        // Start in PLAYING state (skip hiding phase for solo testing)
        state = GameState.PLAYING;

        player.sendSystemMessage(Component.literal("§a§lTest mode active! You are a Prop."));
        player.sendSystemMessage(Component.literal("§eYou can now use transformation commands."));
    }

    public int getRemainingTime() {
        if (state == GameState.HIDING) {
            return hideTime - gameTimer;
        } else if (state == GameState.PLAYING) {
            return maxGameTime - gameTimer;
        }
        return 0;
    }

    public void convertPropToHunter(UUID playerId) {
        // Change prop to hunter when they die
        if (playerTeams.containsKey(playerId)) {
            PropHuntTeam team = playerTeams.get(playerId);
            if (team.getType() == TeamType.PROPS) {
                // Play death sound at prop location
                ServerPlayer dyingProp = players.get(playerId);
                if (dyingProp != null && dyingProp.level() instanceof ServerLevel serverLevel) {
                    Vec3 pos = dyingProp.position();
                    serverLevel.playSound(
                        null,
                        pos.x, pos.y, pos.z,
                        ModSounds.PROP_DEATH.get(),
                        SoundSource.PLAYERS,
                        1.0f, 1.0f
                    );
                }

                // Cleanup transformation and display entities
                PropTransformation transformation = propTransformations.get(playerId);
                if (transformation != null) {
                    transformation.revert();
                    propTransformations.remove(playerId);
                }

                PropHuntTeam hunterTeam = new PropHuntTeam(TeamType.HUNTERS);
                playerTeams.put(playerId, hunterTeam);

                ServerPlayer player = players.get(playerId);
                if (player != null) {
                    // Respawn player at hunter spawn
                    if (currentMap != null && player.level() instanceof ServerLevel serverLevel) {
                        try {
                            double spawnX = currentMap.hunterSpawnX + 0.5;
                            double spawnY = currentMap.hunterSpawnY;
                            double spawnZ = currentMap.hunterSpawnZ + 0.5;

                            // Teleport and respawn
                            player.teleportTo(serverLevel, spawnX, spawnY, spawnZ, 0, 0);
                            player.setHealth(player.getMaxHealth());
                            player.getFoodData().setFoodLevel(20);

                            // Give hunter loadout (weapons + ammo)
                            HunterLoadout.getInstance().equipHunter(player);

                            System.out.println("[PropHunt] " + player.getName().getString() + " respawned as Hunter at (" + spawnX + ", " + spawnY + ", " + spawnZ + ")");
                        } catch (Exception e) {
                            System.err.println("[PropHunt] Failed to respawn hunter: " + e.getMessage());
                        }
                    }

                    player.sendSystemMessage(Component.literal("§cYou were found! You are now a Hunter!"));
                    player.sendSystemMessage(Component.literal("§7Respawning at hunter spawn with weapons..."));
                    broadcastToHud("§6" + player.getName().getString() + " §ewas found and became a Hunter!");
                }
            }
        }
    }
}
