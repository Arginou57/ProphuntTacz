package com.yourname.prophunt.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yourname.prophunt.PropHuntMod;
import com.yourname.prophunt.game.GameManager;
import com.yourname.prophunt.game.PropHuntGame;
import com.yourname.prophunt.game.PropTransformation;
import com.yourname.prophunt.game.BlockAliasManager;
import com.yourname.prophunt.game.BlockStateParser;
import com.yourname.prophunt.game.MapManager;
import com.yourname.prophunt.game.PlayerMapPoints;
import com.yourname.prophunt.game.HunterLoadout;
import com.yourname.prophunt.teams.PropHuntTeam;
import com.yourname.prophunt.teams.TeamType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class PropHuntCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("prophunt")
            .then(Commands.literal("start")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("mapname", StringArgumentType.string())
                    .executes(PropHuntCommands::startGameWithMap)))
            .then(Commands.literal("stop")
                .requires(source -> source.hasPermission(2))
                .executes(PropHuntCommands::stopGame))
            .then(Commands.literal("create")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("mapname", StringArgumentType.string())
                    .executes(PropHuntCommands::createMap)))
            .then(Commands.literal("maps")
                .executes(PropHuntCommands::listMaps))
            .then(Commands.literal("setspawn")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("mapname", StringArgumentType.string())
                    .executes(PropHuntCommands::setSpawn)))
            .then(Commands.literal("sethunterspawn")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("mapname", StringArgumentType.string())
                    .executes(PropHuntCommands::setHunterSpawn)))
            .then(Commands.literal("setpropsspawn")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("mapname", StringArgumentType.string())
                    .executes(PropHuntCommands::setPropSpawn)))
            .then(Commands.literal("join")
                .executes(PropHuntCommands::joinGame))
            .then(Commands.literal("leave")
                .executes(PropHuntCommands::leaveGame))
            .then(Commands.literal("addblock")
                .then(Commands.argument("mapname", StringArgumentType.string())
                    .then(Commands.literal("hand")
                        .executes(PropHuntCommands::addBlockToMap))))
            .then(Commands.literal("transform")
                .executes(PropHuntCommands::transformNearby)
                .then(Commands.literal("hand")
                    .executes(PropHuntCommands::transformHand))
                .then(Commands.argument("block", StringArgumentType.string())
                    .executes(PropHuntCommands::transformSpecific)))
            .then(Commands.literal("revert")
                .executes(PropHuntCommands::revert))
            .then(Commands.literal("status")
                .executes(PropHuntCommands::showStatus))
            .then(Commands.literal("mapinfo")
                .then(Commands.argument("mapname", StringArgumentType.string())
                    .executes(PropHuntCommands::showMapInfo)))
            .then(Commands.literal("testmode")
                .requires(source -> source.hasPermission(2))
                .executes(PropHuntCommands::testMode))
            .then(Commands.literal("hunteraddweapon")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("hand")
                    .executes(PropHuntCommands::hunterAddWeapon)))
            .then(Commands.literal("addammo")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("hand")
                    .executes(PropHuntCommands::addAmmo)))
            .then(Commands.literal("hunterloadout")
                .executes(PropHuntCommands::showHunterLoadout))
            .then(Commands.literal("clearloadout")
                .requires(source -> source.hasPermission(2))
                .executes(PropHuntCommands::clearHunterLoadout))
            .then(Commands.literal("showblock")
                .then(Commands.argument("mapname", StringArgumentType.string())
                    .executes(PropHuntCommands::showBlockList)))
            .then(Commands.literal("clearblock")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("mapname", StringArgumentType.string())
                    .executes(PropHuntCommands::clearBlockList)))
            .then(Commands.literal("mapremove")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("mapname", StringArgumentType.string())
                    .executes(PropHuntCommands::removeMap)))
        );
    }

    private static int startGame(CommandContext<CommandSourceStack> context) {
        GameManager manager = PropHuntMod.getGameManager();

        System.out.println("[PropHunt] Start game command called (no map)");
        if (manager.hasActiveGame()) {
            System.out.println("[PropHunt] Game already in progress!");
            context.getSource().sendFailure(Component.literal("§cA game is already in progress!"));
            return 0;
        }

        System.out.println("[PropHunt] Calling manager.startGame()");
        if (manager.startGame(context.getSource().getServer())) {
            System.out.println("[PropHunt] Game started successfully!");
            context.getSource().sendSuccess(() -> Component.literal("§aGame started!"), true);
            return 1;
        } else {
            System.out.println("[PropHunt] Failed to start game!");
            context.getSource().sendFailure(Component.literal("§cFailed to start game!"));
            return 0;
        }
    }

    private static int startGameWithMap(CommandContext<CommandSourceStack> context) {
        String mapName = StringArgumentType.getString(context, "mapname");
        GameManager manager = PropHuntMod.getGameManager();

        System.out.println("[PropHunt] Start game with map command called: " + mapName);
        if (manager.hasActiveGame()) {
            System.out.println("[PropHunt] Game already in progress!");
            context.getSource().sendFailure(Component.literal("§cA game is already in progress!"));
            return 0;
        }

        if (!MapManager.mapExists(mapName)) {
            System.out.println("[PropHunt] Map not found: " + mapName);
            context.getSource().sendFailure(Component.literal("§cMap not found: " + mapName));
            context.getSource().sendFailure(Component.literal("§eAvailable maps: " + String.join(", ", MapManager.getMapNames())));
            return 0;
        }

        System.out.println("[PropHunt] Calling manager.startGameWithMap() for map: " + mapName);
        if (manager.startGameWithMap(context.getSource().getServer(), mapName)) {
            System.out.println("[PropHunt] Game started successfully with map: " + mapName);
            context.getSource().sendSuccess(() -> Component.literal("§aGame started with map: §e" + mapName), true);
            return 1;
        } else {
            System.out.println("[PropHunt] Failed to start game with map!");
            context.getSource().sendFailure(Component.literal("§cFailed to start game!"));
            return 0;
        }
    }

    private static int createMap(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can create maps!"));
            return 0;
        }

        String mapName = StringArgumentType.getString(context, "mapname");

        if (mapName.isEmpty() || mapName.length() > 32) {
            context.getSource().sendFailure(Component.literal("§cMap name must be 1-32 characters!"));
            return 0;
        }

        if (MapManager.mapExists(mapName)) {
            context.getSource().sendFailure(Component.literal("§cMap already exists: " + mapName));
            return 0;
        }

        // Default: Create a region 300x300 around the player, y=0 to y=256
        int centerX = (int) player.getX();
        int centerZ = (int) player.getZ();
        int radius = 150;

        int minX = centerX - radius;
        int maxX = centerX + radius;
        int minZ = centerZ - radius;
        int maxZ = centerZ + radius;
        int minY = 0;
        int maxY = 256;

        if (MapManager.saveMap(mapName, minX, minY, minZ, maxX, maxY, maxZ, player.getName().getString())) {
            player.sendSystemMessage(Component.literal("§aMap created: §e" + mapName));
            player.sendSystemMessage(Component.literal("§7Region: (" + minX + ", " + minY + ", " + minZ + ") to (" + maxX + ", " + maxY + ", " + maxZ + ")"));
            int sizeX = maxX - minX + 1;
            int sizeY = maxY - minY + 1;
            int sizeZ = maxZ - minZ + 1;
            player.sendSystemMessage(Component.literal("§7Size: " + sizeX + "x" + sizeY + "x" + sizeZ));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§cFailed to create map!"));
            return 0;
        }
    }

    private static int listMaps(CommandContext<CommandSourceStack> context) {
        var maps = MapManager.getAllMaps();

        if (maps.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§eNo maps available"), false);
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("§6=== Available Maps ==="), false);
        for (var map : maps) {
            context.getSource().sendSuccess(() -> Component.literal("§e• " + map.getFormattedInfo()), false);
        }
        return 1;
    }


    private static int setSpawn(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can set spawn!"));
            return 0;
        }

        String mapName = StringArgumentType.getString(context, "mapname");

        if (!MapManager.mapExists(mapName)) {
            context.getSource().sendFailure(Component.literal("§cMap not found: " + mapName));
            return 0;
        }

        int x = (int) player.getX();
        int y = (int) player.getY();
        int z = (int) player.getZ();

        if (MapManager.setSpawnPoint(mapName, x, y, z)) {
            player.sendSystemMessage(Component.literal("§aSpawn set for map §e" + mapName + " §aat (" + x + ", " + y + ", " + z + ")"));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§cFailed to set spawn!"));
            return 0;
        }
    }

    private static int setHunterSpawn(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can set hunter spawn!"));
            return 0;
        }

        String mapName = StringArgumentType.getString(context, "mapname");

        if (!MapManager.mapExists(mapName)) {
            context.getSource().sendFailure(Component.literal("§cMap not found: " + mapName));
            return 0;
        }

        int x = (int) player.getX();
        int y = (int) player.getY();
        int z = (int) player.getZ();

        if (MapManager.setHunterSpawn(mapName, x, y, z)) {
            player.sendSystemMessage(Component.literal("§cHunter spawn set for map §e" + mapName + " §aat (" + x + ", " + y + ", " + z + ")"));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§cFailed to set hunter spawn!"));
            return 0;
        }
    }

    private static int setPropSpawn(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can set prop spawn!"));
            return 0;
        }

        String mapName = StringArgumentType.getString(context, "mapname");

        if (!MapManager.mapExists(mapName)) {
            context.getSource().sendFailure(Component.literal("§cMap not found: " + mapName));
            return 0;
        }

        int x = (int) player.getX();
        int y = (int) player.getY();
        int z = (int) player.getZ();

        if (MapManager.setPropSpawn(mapName, x, y, z)) {
            player.sendSystemMessage(Component.literal("§aProps spawn set for map §e" + mapName + " §aat (" + x + ", " + y + ", " + z + ")"));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§cFailed to set prop spawn!"));
            return 0;
        }
    }

    private static int stopGame(CommandContext<CommandSourceStack> context) {
        GameManager manager = PropHuntMod.getGameManager();

        if (!manager.hasActiveGame()) {
            context.getSource().sendFailure(Component.literal("§cNo active game to stop!"));
            return 0;
        }

        manager.stopGame(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.literal("§aGame stopped!"), true);
        return 1;
    }

    private static int joinGame(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can join!"));
            return 0;
        }

        GameManager manager = PropHuntMod.getGameManager();

        System.out.println("[PropHunt] Join command called by " + player.getName().getString());
        if (manager.addPlayer(player)) {
            System.out.println("[PropHunt] Player " + player.getName().getString() + " successfully joined!");
            player.sendSystemMessage(Component.literal("§aYou joined the Prop Hunt game!"));
            return 1;
        } else {
            System.out.println("[PropHunt] Player " + player.getName().getString() + " failed to join!");
            player.sendSystemMessage(Component.literal("§cFailed to join game!"));
            return 0;
        }
    }

    private static int leaveGame(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can leave!"));
            return 0;
        }

        GameManager manager = PropHuntMod.getGameManager();
        manager.removePlayer(player);
        player.sendSystemMessage(Component.literal("§eYou left the game!"));
        return 1;
    }

    private static int addBlockToMap(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can add blocks!"));
            return 0;
        }

        String mapName = StringArgumentType.getString(context, "mapname");

        if (!MapManager.mapExists(mapName)) {
            player.sendSystemMessage(Component.literal("§cMap not found: " + mapName));
            return 0;
        }

        // Get the block in the player's main hand
        var itemStack = player.getMainHandItem();
        if (itemStack.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cYou must be holding a block to add!"));
            return 0;
        }

        // Check if the item is a block item
        if (!(itemStack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) {
            player.sendSystemMessage(Component.literal("§cYou must be holding a block item!"));
            return 0;
        }

        // Get the block state
        BlockState blockState = blockItem.getBlock().defaultBlockState();

        // Extract NBT data from the item (for player heads, etc.)
        CompoundTag nbtData = null;
        var blockEntityData = itemStack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            nbtData = blockEntityData.copyTag();
        }

        // Add block to map with NBT
        if (MapManager.addBlockToMap(mapName, blockState, nbtData)) {
            String blockName = blockState.getBlock().getName().getString();
            if (nbtData != null) {
                player.sendSystemMessage(Component.literal("§aBlock added to map §e" + mapName + "§a: " + blockName + " §7(with NBT)"));
            } else {
                player.sendSystemMessage(Component.literal("§aBlock added to map §e" + mapName + "§a: " + blockName));
            }
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§cFailed to add block to map!"));
            return 0;
        }
    }

    private static int transformNearby(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can transform!"));
            return 0;
        }

        GameManager manager = PropHuntMod.getGameManager();
        PropHuntGame game = manager.getCurrentGame();

        if (game == null || !game.isActive()) {
            player.sendSystemMessage(Component.literal("§cNo active game!"));
            return 0;
        }

        PropHuntTeam team = game.getPlayerTeam(player.getUUID());
        if (team == null || team.getType() != TeamType.PROPS) {
            player.sendSystemMessage(Component.literal("§cOnly props can transform!"));
            return 0;
        }

        PropTransformation transformation = game.getTransformation(player.getUUID());
        if (transformation == null) {
            transformation = new PropTransformation(player);
            game.setTransformation(player.getUUID(), transformation);
        }

        transformation.transformIntoNearbyBlock();
        return 1;
    }

    private static int transformSpecific(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can transform!"));
            return 0;
        }

        String blockInput = StringArgumentType.getString(context, "block");

        GameManager manager = PropHuntMod.getGameManager();
        PropHuntGame game = manager.getCurrentGame();

        if (game == null || !game.isActive()) {
            player.sendSystemMessage(Component.literal("§cNo active game!"));
            return 0;
        }

        PropHuntTeam team = game.getPlayerTeam(player.getUUID());
        if (team == null || team.getType() != TeamType.PROPS) {
            player.sendSystemMessage(Component.literal("§cOnly props can transform!"));
            return 0;
        }

        BlockState state = null;
        CompoundTag nbtTag = null;

        // First, check if it's an alias
        if (BlockAliasManager.hasAlias(blockInput)) {
            BlockAliasManager.BlockAliasData aliasData = BlockAliasManager.getBlockData(blockInput);
            state = aliasData.blockState;
            nbtTag = aliasData.nbtTag;
        } else {
            // Try to parse as a block with optional NBT syntax
            try {
                BlockStateParser.ParsedBlockState parsed = BlockStateParser.parse(blockInput);
                state = parsed.blockState;
                nbtTag = parsed.nbtTag;
            } catch (CommandSyntaxException e) {
                player.sendSystemMessage(Component.literal("§cInvalid block: " + e.getMessage()));
                return 0;
            }
        }

        PropTransformation transformation = game.getTransformation(player.getUUID());
        if (transformation == null) {
            transformation = new PropTransformation(player);
            game.setTransformation(player.getUUID(), transformation);
        }

        // Transform with NBT data if available
        if (nbtTag != null) {
            transformation.transformIntoBlockWithNBT(state, nbtTag);
        } else {
            transformation.transformIntoBlock(state);
        }

        return 1;
    }

    private static int transformHand(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can use this command!"));
            return 0;
        }

        // Get the item in the player's main hand
        var itemStack = player.getMainHandItem();
        if (itemStack.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cYou must be holding a block!"));
            return 0;
        }

        // Log item details in chat
        player.sendSystemMessage(Component.literal("§6=== TRANSFORM HAND DEBUG ==="));

        String itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        player.sendSystemMessage(Component.literal("§eItem ID: §f" + itemId));

        String displayName = itemStack.getHoverName().getString();
        player.sendSystemMessage(Component.literal("§eDisplay Name: §f" + displayName));

        // Log components
        if (itemStack.getComponents() != null) {
            String components = itemStack.getComponents().toString();
            player.sendSystemMessage(Component.literal("§eComponents: §f" + components));
            System.out.println("[PropHunt] Components: " + components);
        }

        // Save item data
        try {
            var savedTag = itemStack.save(player.level().registryAccess());
            String saveData = savedTag.toString();
            player.sendSystemMessage(Component.literal("§eSave Data: §f" + saveData));
            System.out.println("[PropHunt] Full Item Save Data: " + saveData);
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§cCould not save item data: " + e.getMessage()));
        }

        // Check if the item is a block item
        if (!(itemStack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) {
            player.sendSystemMessage(Component.literal("§cYou must be holding a block item!"));
            return 0;
        }

        // Get the block state
        BlockState blockState = blockItem.getBlock().defaultBlockState();
        player.sendSystemMessage(Component.literal("§eBlockState: §f" + blockState.toString()));

        // Spawn a BlockDisplay entity under the player
        player.sendSystemMessage(Component.literal("§aSpawning BlockDisplay under you..."));

        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) player.level();
        net.minecraft.world.entity.Display.BlockDisplay displayEntity =
            new net.minecraft.world.entity.Display.BlockDisplay(net.minecraft.world.entity.EntityType.BLOCK_DISPLAY, serverLevel);

        // Position under the player
        double x = player.getX() - 0.5;
        double y = player.getY();
        double z = player.getZ() - 0.5;
        displayEntity.setPos(x, y, z);

        // Add to world first
        serverLevel.addFreshEntity(displayEntity);

        // Set all properties via NBT
        CompoundTag entityData = new CompoundTag();
        displayEntity.saveWithoutId(entityData);

        // Set block state via NBT
        CompoundTag blockStateNBT = net.minecraft.nbt.NbtUtils.writeBlockState(blockState);
        entityData.put("block_state", blockStateNBT);

        // Try to copy NBT from the item (for player heads, etc.)
        // In 1.21+, we need to check for block_entity_data component
        var blockEntityData = itemStack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            player.sendSystemMessage(Component.literal("§aFound block_entity_data component!"));
            System.out.println("[PropHunt] Block Entity Data: " + blockEntityData);

            // For player heads, the profile data needs to be in block_state
            // Get the custom data and merge into block_state
            CompoundTag customNbt = blockEntityData.copyTag();
            player.sendSystemMessage(Component.literal("§eBlock Entity NBT: §f" + customNbt.toString()));

            // Merge custom NBT into block_state
            for (String key : customNbt.getAllKeys()) {
                blockStateNBT.put(key, customNbt.get(key));
            }
            entityData.put("block_state", blockStateNBT);
        }

        // Set display properties
        entityData.putByte("billboard", (byte) 0); // FIXED
        entityData.putFloat("shadow_radius", 0.5f);

        // Load all NBT data into the entity
        displayEntity.load(entityData);

        player.sendSystemMessage(Component.literal("§aBlockDisplay spawned at (" + x + ", " + y + ", " + z + ")"));
        player.sendSystemMessage(Component.literal("§6=== END DEBUG ==="));

        return 1;
    }

    private static int revert(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can revert!"));
            return 0;
        }

        GameManager manager = PropHuntMod.getGameManager();
        PropHuntGame game = manager.getCurrentGame();

        if (game == null) {
            player.sendSystemMessage(Component.literal("§cNo active game!"));
            return 0;
        }

        PropTransformation transformation = game.getTransformation(player.getUUID());
        if (transformation != null) {
            transformation.revert();
            return 1;
        }

        player.sendSystemMessage(Component.literal("§cYou are not transformed!"));
        return 0;
    }

    private static int showStatus(CommandContext<CommandSourceStack> context) {
        GameManager manager = PropHuntMod.getGameManager();
        PropHuntGame game = manager.getCurrentGame();

        if (game == null) {
            context.getSource().sendSuccess(() -> Component.literal("§eNo active game"), false);
            return 1;
        }

        context.getSource().sendSuccess(() -> Component.literal("§6=== Prop Hunt Status ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("§eState: §f" + game.getState()), false);
        context.getSource().sendSuccess(() -> Component.literal("§ePlayers: §f" + game.getPlayers().size()), false);

        var scores = game.getScores();
        context.getSource().sendSuccess(() -> Component.literal("§aProps Score: §f" + scores.get(TeamType.PROPS)), false);
        context.getSource().sendSuccess(() -> Component.literal("§cHunters Score: §f" + scores.get(TeamType.HUNTERS)), false);

        return 1;
    }

    private static int showMapInfo(CommandContext<CommandSourceStack> context) {
        String mapName = StringArgumentType.getString(context, "mapname");

        if (!MapManager.mapExists(mapName)) {
            context.getSource().sendFailure(Component.literal("§cMap not found: " + mapName));
            return 0;
        }

        MapManager.GameMapData mapData = MapManager.getMap(mapName);
        if (mapData == null) {
            context.getSource().sendFailure(Component.literal("§cCouldn't load map data!"));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("§6=== Map Info: " + mapName + " ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("§eRegion: (" + mapData.minX + ", " + mapData.minY + ", " + mapData.minZ + ") to (" + mapData.maxX + ", " + mapData.maxY + ", " + mapData.maxZ + ")"), false);
        context.getSource().sendSuccess(() -> Component.literal("§eSize: " + (mapData.maxX - mapData.minX + 1) + "x" + (mapData.maxY - mapData.minY + 1) + "x" + (mapData.maxZ - mapData.minZ + 1)), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Created by: " + mapData.createdBy), false);
        context.getSource().sendSuccess(() -> Component.literal(""), false);
        context.getSource().sendSuccess(() -> Component.literal("§c[HUNTERS SPAWN] (§f" + mapData.hunterSpawnX + ", " + mapData.hunterSpawnY + ", " + mapData.hunterSpawnZ + "§c)"), false);
        context.getSource().sendSuccess(() -> Component.literal("§a[PROPS SPAWN] (§f" + mapData.propSpawnX + ", " + mapData.propSpawnY + ", " + mapData.propSpawnZ + "§a)"), false);
        return 1;
    }

    private static int testMode(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can use test mode!"));
            return 0;
        }

        GameManager manager = PropHuntMod.getGameManager();

        // Stop any existing game
        if (manager.hasActiveGame()) {
            manager.stopGame(context.getSource().getServer());
        }

        // Add player to game
        if (!manager.addPlayer(player)) {
            context.getSource().sendFailure(Component.literal("§cFailed to join game!"));
            return 0;
        }

        // Start game in test mode (will assign player to Props team)
        if (manager.startTestMode(context.getSource().getServer(), player)) {
            player.sendSystemMessage(Component.literal("§a§l=== TEST MODE ACTIVATED ==="));
            player.sendSystemMessage(Component.literal("§eYou are now a Prop! Test commands:"));
            player.sendSystemMessage(Component.literal("§a/prophunt transform <block> §7- Transform into a block"));
            player.sendSystemMessage(Component.literal("§a/prophunt revert §7- Return to normal"));
            player.sendSystemMessage(Component.literal("§a/prophunt stop §7- Stop test mode"));
            player.sendSystemMessage(Component.literal("§6Example: §f/prophunt transform oak_planks"));
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("§cFailed to start test mode!"));
            return 0;
        }
    }

    private static int hunterAddWeapon(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can use this command!"));
            return 0;
        }

        // Get item from player's main hand
        var itemStack = player.getMainHandItem();
        if (itemStack.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cYou must hold an item in your hand!"));
            return 0;
        }

        HunterLoadout loadout = HunterLoadout.getInstance();
        loadout.addWeapon(itemStack);

        player.sendSystemMessage(Component.literal(
            "§aAdded §e" + itemStack.getHoverName().getString() + " x" + itemStack.getCount()
            + "§a to hunter loadout!"));
        return 1;
    }

    private static int addAmmo(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can use this command!"));
            return 0;
        }

        // Get item from player's main hand
        var itemStack = player.getMainHandItem();
        if (itemStack.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cYou must hold an item in your hand!"));
            return 0;
        }

        HunterLoadout loadout = HunterLoadout.getInstance();
        loadout.addAmmo(itemStack);

        player.sendSystemMessage(Component.literal(
            "§aAdded ammo §e" + itemStack.getHoverName().getString() + " x" + itemStack.getCount()
            + "§a (will be given on each kill)!"));
        return 1;
    }

    private static int showHunterLoadout(CommandContext<CommandSourceStack> context) {
        HunterLoadout loadout = HunterLoadout.getInstance();

        if (loadout.getWeaponCount() == 0 && loadout.getAmmoCount() == 0) {
            context.getSource().sendSuccess(() -> Component.literal("§eNo items configured in hunter loadout"), false);
            return 1;
        }

        context.getSource().sendSuccess(() -> Component.literal("§6=== HUNTER LOADOUT ==="), false);

        String[] lines = loadout.getLoadoutInfo().split("\n");
        for (String line : lines) {
            if (!line.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal(line), false);
            }
        }
        return 1;
    }

    private static int clearHunterLoadout(CommandContext<CommandSourceStack> context) {
        HunterLoadout loadout = HunterLoadout.getInstance();
        loadout.clear();
        context.getSource().sendSuccess(() -> Component.literal("§aHunter loadout cleared!"), true);
        return 1;
    }

    private static int showBlockList(CommandContext<CommandSourceStack> context) {
        String mapName = StringArgumentType.getString(context, "mapname");

        if (!MapManager.mapExists(mapName)) {
            context.getSource().sendFailure(Component.literal("§cMap not found: " + mapName));
            return 0;
        }

        MapManager.GameMapData mapData = MapManager.getMap(mapName);
        if (mapData == null) {
            context.getSource().sendFailure(Component.literal("§cFailed to load map data!"));
            return 0;
        }

        if (mapData.propBlocks.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§eNo blocks configured for map §e" + mapName), false);
            return 1;
        }

        context.getSource().sendSuccess(() -> Component.literal("§6=== PROP BLOCKS FOR MAP: " + mapName + " ==="), false);
        for (int i = 0; i < mapData.propBlocks.size(); i++) {
            var blockData = mapData.propBlocks.get(i);
            final int index = i + 1;
            final String blockName = blockData.blockState.getBlock().getName().getString();
            final boolean hasNbt = blockData.nbtData != null;
            if (hasNbt) {
                context.getSource().sendSuccess(() -> Component.literal("§e" + index + ". " + blockName + " §7(NBT)"), false);
            } else {
                context.getSource().sendSuccess(() -> Component.literal("§e" + index + ". " + blockName), false);
            }
        }
        return 1;
    }

    private static int clearBlockList(CommandContext<CommandSourceStack> context) {
        String mapName = StringArgumentType.getString(context, "mapname");

        if (!MapManager.mapExists(mapName)) {
            context.getSource().sendFailure(Component.literal("§cMap not found: " + mapName));
            return 0;
        }

        MapManager.GameMapData mapData = MapManager.getMap(mapName);
        if (mapData == null) {
            context.getSource().sendFailure(Component.literal("§cFailed to load map data!"));
            return 0;
        }

        int count = mapData.propBlocks.size();
        mapData.propBlocks.clear();
        context.getSource().sendSuccess(() -> Component.literal(
            "§a" + count + " blocks cleared from map §e" + mapName), true);
        return 1;
    }

    private static int removeMap(CommandContext<CommandSourceStack> context) {
        String mapName = StringArgumentType.getString(context, "mapname");

        if (!MapManager.mapExists(mapName)) {
            context.getSource().sendFailure(Component.literal("§cMap not found: " + mapName));
            return 0;
        }

        if (MapManager.deleteMap(mapName)) {
            context.getSource().sendSuccess(() -> Component.literal("§aMap §e" + mapName + " §ahas been deleted!"), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("§cFailed to delete map: " + mapName));
            return 0;
        }
    }
}
