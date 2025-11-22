package com.yourname.prophunt.game;

import com.yourname.prophunt.PropHuntMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages saved game maps with region boundaries
 * Maps are stored as NBT files with coordinates
 */
public class MapManager {
    private static final File MAPS_DIRECTORY = new File("./prop_hunt_maps");
    private static final Map<String, GameMapData> loadedMaps = new HashMap<>();

    static {
        // Create maps directory if it doesn't exist
        if (!MAPS_DIRECTORY.exists()) {
            MAPS_DIRECTORY.mkdirs();
        }
        loadAllMaps();
    }

    /**
     * Data structure for a prop block with optional NBT data
     */
    public static class PropBlockData {
        public final BlockState blockState;
        public final CompoundTag nbtData; // For blocks with NBT like player heads

        public PropBlockData(BlockState blockState, CompoundTag nbtData) {
            this.blockState = blockState;
            this.nbtData = nbtData;
        }

        public PropBlockData(BlockState blockState) {
            this(blockState, null);
        }
    }

    /**
     * Game map data structure
     */
    public static class GameMapData {
        public final String name;
        public final int minX, minY, minZ;
        public final int maxX, maxY, maxZ;
        public int spawnX, spawnY, spawnZ; // General spawn (deprecated, kept for compatibility)
        public int hunterSpawnX, hunterSpawnY, hunterSpawnZ; // Hunter team spawn
        public int propSpawnX, propSpawnY, propSpawnZ; // Props team spawn
        public final long createdTime;
        public final String createdBy;
        public List<PropBlockData> propBlocks = new ArrayList<>(); // List of blocks for props with NBT

        public GameMapData(String name, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String createdBy) {
            this.name = name;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            int centerX = (minX + maxX) / 2;
            int centerZ = (minZ + maxZ) / 2;
            int topY = maxY + 1;
            // Default spawn: center of map for both teams
            this.spawnX = centerX;
            this.spawnY = topY;
            this.spawnZ = centerZ;
            this.hunterSpawnX = centerX;
            this.hunterSpawnY = topY;
            this.hunterSpawnZ = centerZ;
            this.propSpawnX = centerX;
            this.propSpawnY = topY;
            this.propSpawnZ = centerZ;
            this.createdTime = System.currentTimeMillis();
            this.createdBy = createdBy;
        }

        public GameMapData(String name, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, long createdTime, String createdBy) {
            this.name = name;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            int centerX = (minX + maxX) / 2;
            int centerZ = (minZ + maxZ) / 2;
            int topY = maxY + 1;
            this.spawnX = centerX;
            this.spawnY = topY;
            this.spawnZ = centerZ;
            this.hunterSpawnX = centerX;
            this.hunterSpawnY = topY;
            this.hunterSpawnZ = centerZ;
            this.propSpawnX = centerX;
            this.propSpawnY = topY;
            this.propSpawnZ = centerZ;
            this.createdTime = createdTime;
            this.createdBy = createdBy;
        }

        public int getWidth() {
            return maxX - minX + 1;
        }

        public int getHeight() {
            return maxY - minY + 1;
        }

        public int getLength() {
            return maxZ - minZ + 1;
        }

        public BoundingBox toBoundingBox() {
            return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        }

        public String getFormattedInfo() {
            return String.format("%s: %dx%dx%d (created by %s)",
                name, getWidth(), getHeight(), getLength(), createdBy);
        }

        public CompoundTag serializeToNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("name", name);
            tag.putInt("minX", minX);
            tag.putInt("minY", minY);
            tag.putInt("minZ", minZ);
            tag.putInt("maxX", maxX);
            tag.putInt("maxY", maxY);
            tag.putInt("maxZ", maxZ);
            tag.putInt("spawnX", spawnX);
            tag.putInt("spawnY", spawnY);
            tag.putInt("spawnZ", spawnZ);
            tag.putInt("hunterSpawnX", hunterSpawnX);
            tag.putInt("hunterSpawnY", hunterSpawnY);
            tag.putInt("hunterSpawnZ", hunterSpawnZ);
            tag.putInt("propSpawnX", propSpawnX);
            tag.putInt("propSpawnY", propSpawnY);
            tag.putInt("propSpawnZ", propSpawnZ);
            tag.putLong("createdTime", createdTime);
            tag.putString("createdBy", createdBy);

            // Save prop blocks (store as block names + NBT)
            ListTag blocksList = new ListTag();
            for (PropBlockData blockData : propBlocks) {
                CompoundTag blockTag = new CompoundTag();
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockData.blockState.getBlock());
                blockTag.putString("block_id", blockId.toString());
                // Save NBT data if present
                if (blockData.nbtData != null) {
                    blockTag.put("nbt_data", blockData.nbtData);
                }
                blocksList.add(blockTag);
            }
            tag.put("propBlocks", blocksList);

            return tag;
        }

        public static GameMapData deserializeFromNBT(CompoundTag tag) {
            GameMapData mapData = new GameMapData(
                tag.getString("name"),
                tag.getInt("minX"),
                tag.getInt("minY"),
                tag.getInt("minZ"),
                tag.getInt("maxX"),
                tag.getInt("maxY"),
                tag.getInt("maxZ"),
                tag.getLong("createdTime"),
                tag.getString("createdBy")
            );
            // Load spawn if available
            if (tag.contains("spawnX")) {
                mapData.spawnX = tag.getInt("spawnX");
                mapData.spawnY = tag.getInt("spawnY");
                mapData.spawnZ = tag.getInt("spawnZ");
            }
            // Load team-specific spawns if available
            if (tag.contains("hunterSpawnX")) {
                mapData.hunterSpawnX = tag.getInt("hunterSpawnX");
                mapData.hunterSpawnY = tag.getInt("hunterSpawnY");
                mapData.hunterSpawnZ = tag.getInt("hunterSpawnZ");
            }
            if (tag.contains("propSpawnX")) {
                mapData.propSpawnX = tag.getInt("propSpawnX");
                mapData.propSpawnY = tag.getInt("propSpawnY");
                mapData.propSpawnZ = tag.getInt("propSpawnZ");
            }

            // Load prop blocks if available
            if (tag.contains("propBlocks", Tag.TAG_LIST)) {
                ListTag blocksList = tag.getList("propBlocks", Tag.TAG_COMPOUND);
                for (int i = 0; i < blocksList.size(); i++) {
                    CompoundTag blockTag = (CompoundTag) blocksList.get(i);
                    String blockId = blockTag.getString("block_id");
                    try {
                        ResourceLocation resourceLocation = ResourceLocation.parse(blockId);
                        if (BuiltInRegistries.BLOCK.containsKey(resourceLocation)) {
                            BlockState block = BuiltInRegistries.BLOCK.get(resourceLocation).defaultBlockState();
                            // Load NBT data if present
                            CompoundTag nbtData = null;
                            if (blockTag.contains("nbt_data", Tag.TAG_COMPOUND)) {
                                nbtData = blockTag.getCompound("nbt_data");
                            }
                            mapData.propBlocks.add(new PropBlockData(block, nbtData));
                        }
                    } catch (Exception e) {
                        PropHuntMod.LOGGER.warn("Failed to parse block id: " + blockId);
                    }
                }
            }

            return mapData;
        }

        public PropBlockData getRandomBlock() {
            if (propBlocks.isEmpty()) {
                return null;
            }
            return propBlocks.get((int) (Math.random() * propBlocks.size()));
        }
    }

    /**
     * Save a new map
     */
    public static boolean saveMap(String mapName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String createdBy) {
        if (mapName == null || mapName.isEmpty()) {
            return false;
        }

        // Check if map already exists
        if (loadedMaps.containsKey(mapName.toLowerCase())) {
            return false;
        }

        GameMapData mapData = new GameMapData(mapName, minX, minY, minZ, maxX, maxY, maxZ, createdBy);
        loadedMaps.put(mapName.toLowerCase(), mapData);

        // Save to file
        try {
            Path mapPath = new File(MAPS_DIRECTORY, mapName.toLowerCase() + ".dat").toPath();
            CompoundTag tag = mapData.serializeToNBT();
            NbtIo.writeCompressed(tag, mapPath);
            PropHuntMod.LOGGER.info("Saved map: " + mapName);
            return true;
        } catch (IOException e) {
            PropHuntMod.LOGGER.error("Failed to save map: " + mapName, e);
            return false;
        }
    }

    /**
     * Get a map by name
     */
    public static GameMapData getMap(String mapName) {
        return loadedMaps.get(mapName.toLowerCase());
    }

    /**
     * Check if map exists
     */
    public static boolean mapExists(String mapName) {
        return loadedMaps.containsKey(mapName.toLowerCase());
    }

    /**
     * Get all available maps
     */
    public static Collection<GameMapData> getAllMaps() {
        return loadedMaps.values();
    }

    /**
     * Get list of map names
     */
    public static List<String> getMapNames() {
        return new ArrayList<>(loadedMaps.keySet());
    }

    /**
     * Delete a map
     */
    public static boolean deleteMap(String mapName) {
        if (!loadedMaps.containsKey(mapName.toLowerCase())) {
            return false;
        }

        loadedMaps.remove(mapName.toLowerCase());

        try {
            File mapFile = new File(MAPS_DIRECTORY, mapName.toLowerCase() + ".dat");
            if (mapFile.exists()) {
                mapFile.delete();
            }
            PropHuntMod.LOGGER.info("Deleted map: " + mapName);
            return true;
        } catch (Exception e) {
            PropHuntMod.LOGGER.error("Failed to delete map: " + mapName, e);
            return false;
        }
    }

    /**
     * Load all maps from disk
     */
    private static void loadAllMaps() {
        File[] mapFiles = MAPS_DIRECTORY.listFiles((dir, name) -> name.endsWith(".dat"));

        if (mapFiles != null) {
            for (File mapFile : mapFiles) {
                try {
                    Path mapPath = mapFile.toPath();
                    // Use a high limit for NBT reading (256MB should be enough)
                    CompoundTag tag = NbtIo.readCompressed(mapPath, new NbtAccounter(256L * 1024L * 1024L, 512));
                    GameMapData mapData = GameMapData.deserializeFromNBT(tag);
                    loadedMaps.put(mapData.name.toLowerCase(), mapData);
                    PropHuntMod.LOGGER.info("Loaded map: " + mapData.name);
                } catch (IOException e) {
                    PropHuntMod.LOGGER.error("Failed to load map from " + mapFile.getName(), e);
                }
            }
        }

        if (loadedMaps.isEmpty()) {
            PropHuntMod.LOGGER.info("No saved maps found");
        }
    }

    /**
     * Reload all maps from disk
     */
    public static void reloadMaps() {
        loadedMaps.clear();
        loadAllMaps();
    }

    /**
     * Set spawn point for a map (general spawn)
     */
    public static boolean setSpawnPoint(String mapName, int x, int y, int z) {
        GameMapData mapData = getMap(mapName);
        if (mapData == null) {
            return false;
        }

        mapData.spawnX = x;
        mapData.spawnY = y;
        mapData.spawnZ = z;

        // Save updated map
        try {
            Path mapPath = new File(MAPS_DIRECTORY, mapName.toLowerCase() + ".dat").toPath();
            CompoundTag tag = mapData.serializeToNBT();
            NbtIo.writeCompressed(tag, mapPath);
            PropHuntMod.LOGGER.info("Updated spawn for map: " + mapName);
            return true;
        } catch (IOException e) {
            PropHuntMod.LOGGER.error("Failed to update spawn for map: " + mapName, e);
            return false;
        }
    }

    /**
     * Set hunter team spawn point for a map
     */
    public static boolean setHunterSpawn(String mapName, int x, int y, int z) {
        GameMapData mapData = getMap(mapName);
        if (mapData == null) {
            return false;
        }

        mapData.hunterSpawnX = x;
        mapData.hunterSpawnY = y;
        mapData.hunterSpawnZ = z;

        // Save updated map
        try {
            Path mapPath = new File(MAPS_DIRECTORY, mapName.toLowerCase() + ".dat").toPath();
            CompoundTag tag = mapData.serializeToNBT();
            NbtIo.writeCompressed(tag, mapPath);
            PropHuntMod.LOGGER.info("Updated hunter spawn for map: " + mapName);
            return true;
        } catch (IOException e) {
            PropHuntMod.LOGGER.error("Failed to update hunter spawn for map: " + mapName, e);
            return false;
        }
    }

    /**
     * Set props team spawn point for a map
     */
    public static boolean setPropSpawn(String mapName, int x, int y, int z) {
        GameMapData mapData = getMap(mapName);
        if (mapData == null) {
            return false;
        }

        mapData.propSpawnX = x;
        mapData.propSpawnY = y;
        mapData.propSpawnZ = z;

        // Save updated map
        try {
            Path mapPath = new File(MAPS_DIRECTORY, mapName.toLowerCase() + ".dat").toPath();
            CompoundTag tag = mapData.serializeToNBT();
            NbtIo.writeCompressed(tag, mapPath);
            PropHuntMod.LOGGER.info("Updated prop spawn for map: " + mapName);
            return true;
        } catch (IOException e) {
            PropHuntMod.LOGGER.error("Failed to update prop spawn for map: " + mapName, e);
            return false;
        }
    }

    /**
     * Add a block to the map's prop blocks list (without NBT)
     */
    public static boolean addBlockToMap(String mapName, BlockState blockState) {
        return addBlockToMap(mapName, blockState, null);
    }

    /**
     * Add a block to the map's prop blocks list with optional NBT data
     */
    public static boolean addBlockToMap(String mapName, BlockState blockState, CompoundTag nbtData) {
        GameMapData mapData = getMap(mapName);
        if (mapData == null) {
            return false;
        }

        mapData.propBlocks.add(new PropBlockData(blockState, nbtData));

        // Save updated map
        try {
            Path mapPath = new File(MAPS_DIRECTORY, mapName.toLowerCase() + ".dat").toPath();
            CompoundTag tag = mapData.serializeToNBT();
            NbtIo.writeCompressed(tag, mapPath);
            PropHuntMod.LOGGER.info("Added block to map: " + mapName + (nbtData != null ? " (with NBT)" : ""));
            return true;
        } catch (IOException e) {
            PropHuntMod.LOGGER.error("Failed to add block to map: " + mapName, e);
            return false;
        }
    }
}
