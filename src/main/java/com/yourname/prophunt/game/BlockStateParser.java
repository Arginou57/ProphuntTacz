package com.yourname.prophunt.game;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.CompoundTag;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

/**
 * Parses block strings with optional NBT data
 * Syntax: "blockid" or "blockid{nbt}" or "namespace:blockid{nbt}"
 */
public class BlockStateParser {
    private static final SimpleCommandExceptionType INVALID_NBT = new SimpleCommandExceptionType(() -> "Invalid NBT data");
    private static final SimpleCommandExceptionType UNKNOWN_BLOCK = new SimpleCommandExceptionType(() -> "Unknown block");

    /**
     * Parse a block string that may include NBT data
     * Examples:
     * - "oak_planks"
     * - "minecraft:oak_planks"
     * - "oak_planks{waterlogged:1b}"
     * - "custommod:custom_block{data:1b,color:\"red\"}"
     */
    public static ParsedBlockState parse(String input) throws CommandSyntaxException {
        // Find the opening brace for NBT data
        int nbtStart = input.indexOf('{');

        String blockId;
        CompoundTag nbtTag = null;

        if (nbtStart >= 0) {
            // Has NBT data
            blockId = input.substring(0, nbtStart).trim();
            String nbtString = input.substring(nbtStart).trim();

            try {
                nbtTag = TagParser.parseTag(nbtString);
                if (!(nbtTag instanceof CompoundTag)) {
                    throw new IllegalArgumentException("NBT data must be a compound tag");
                }
            } catch (Exception e) {
                throw INVALID_NBT.create();
            }
        } else {
            blockId = input.trim();
        }

        // Parse the block ID
        ResourceLocation blockResourceId = ResourceLocation.tryParse(blockId);
        if (blockResourceId == null) {
            blockResourceId = ResourceLocation.withDefaultNamespace(blockId);
        }

        Block block = BuiltInRegistries.BLOCK.get(blockResourceId);
        if (block == null) {
            throw UNKNOWN_BLOCK.create();
        }

        BlockState blockState = block.defaultBlockState();

        return new ParsedBlockState(blockState, nbtTag);
    }

    /**
     * Result of parsing a block string
     */
    public static class ParsedBlockState {
        public final BlockState blockState;
        public final CompoundTag nbtTag;

        public ParsedBlockState(BlockState blockState, CompoundTag nbtTag) {
            this.blockState = blockState;
            this.nbtTag = nbtTag;
        }

        public boolean hasNBT() {
            return nbtTag != null && !nbtTag.isEmpty();
        }

        public String getBlockName() {
            return BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
        }

        @Override
        public String toString() {
            if (hasNBT()) {
                return getBlockName() + nbtTag;
            }
            return getBlockName();
        }
    }
}
