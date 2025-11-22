package com.yourname.prophunt.game;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public class BlockAliasManager {
    private static final Map<String, BlockAliasData> blockAliases = new HashMap<>();

    /**
     * Stores both BlockState and NBT data for a block
     */
    public static class BlockAliasData {
        public final BlockState blockState;
        public final CompoundTag nbtTag;

        public BlockAliasData(BlockState blockState, CompoundTag nbtTag) {
            this.blockState = blockState;
            this.nbtTag = nbtTag;
        }

        public BlockAliasData(BlockState blockState) {
            this(blockState, null);
        }

        public boolean hasNBT() {
            return nbtTag != null && !nbtTag.isEmpty();
        }

        public String getDescription() {
            String desc = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
            if (hasNBT()) {
                desc += " " + nbtTag.toString();
            }
            return desc;
        }
    }

    public static void addAlias(String alias, BlockState blockState) {
        addAlias(alias, blockState, null);
    }

    public static void addAlias(String alias, BlockState blockState, CompoundTag nbtTag) {
        blockAliases.put(alias.toLowerCase(), new BlockAliasData(blockState, nbtTag));
    }

    public static BlockAliasData getBlockData(String alias) {
        return blockAliases.get(alias.toLowerCase());
    }

    public static BlockState getBlock(String alias) {
        BlockAliasData data = getBlockData(alias);
        return data != null ? data.blockState : null;
    }

    public static CompoundTag getNBT(String alias) {
        BlockAliasData data = getBlockData(alias);
        return data != null ? data.nbtTag : null;
    }

    public static boolean hasAlias(String alias) {
        return blockAliases.containsKey(alias.toLowerCase());
    }

    public static void removeAlias(String alias) {
        blockAliases.remove(alias.toLowerCase());
    }

    public static Map<String, BlockAliasData> getAllAliases() {
        return new HashMap<>(blockAliases);
    }

    public static void clear() {
        blockAliases.clear();
    }
}
