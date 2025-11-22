package com.yourname.prophunt.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages frozen prop blocks that appear in the world
 * When a prop freezes, a real block is placed and can be destroyed by hunters
 */
public class FrozenPropBlockManager {
    /**
     * Maps frozen block positions to their owner prop UUID
     * BlockPos -> UUID of prop player
     */
    private static final Map<BlockPos, UUID> frozenBlocks = new HashMap<>();

    /**
     * Maps prop UUID to their frozen block position
     */
    private static final Map<UUID, BlockPos> propFrozenPositions = new HashMap<>();

    /**
     * Place a frozen block for a prop
     * The block is placed at the prop's feet position
     */
    public static void placeFrozenBlock(UUID propId, BlockPos pos, BlockState blockState, ServerLevel level) {
        // Remove old frozen block if exists
        removeFrozenBlock(propId, level);

        // Use the position's Y at player feet, and adjust X/Z for block centering
        // This centers the block on the player
        BlockPos placementPos = pos;

        // Place the new block in the world
        BlockState currentState = level.getBlockState(placementPos);
        if (currentState.isAir() || currentState.canBeReplaced()) {
            level.setBlock(placementPos, blockState, 2); // Flag 2 = client update
        }

        // Register the block
        frozenBlocks.put(placementPos, propId);
        propFrozenPositions.put(propId, placementPos);
    }

    /**
     * Remove a frozen block when prop unfreezes or dies
     */
    public static void removeFrozenBlock(UUID propId, ServerLevel level) {
        BlockPos pos = propFrozenPositions.get(propId);
        if (pos != null) {
            // Destroy the block
            level.destroyBlock(pos, false); // Don't drop items
            frozenBlocks.remove(pos);
            propFrozenPositions.remove(propId);
        }
    }

    /**
     * Check if a broken block was a frozen prop block
     * Returns the UUID of the prop if it was, null otherwise
     */
    public static UUID checkFrozenBlockBreak(BlockPos pos) {
        return frozenBlocks.remove(pos);
    }

    /**
     * Get the prop UUID for a frozen block position
     */
    public static UUID getFrozenBlockProp(BlockPos pos) {
        return frozenBlocks.get(pos);
    }

    /**
     * Check if a position has a frozen block
     */
    public static boolean isFrozenBlock(BlockPos pos) {
        return frozenBlocks.containsKey(pos);
    }

    /**
     * Get frozen block position for a prop
     */
    public static BlockPos getFrozenBlockPosition(UUID propId) {
        return propFrozenPositions.get(propId);
    }

    /**
     * Clear all frozen blocks (when game ends)
     */
    public static void clear(ServerLevel level) {
        for (BlockPos pos : frozenBlocks.keySet()) {
            level.destroyBlock(pos, false);
        }
        frozenBlocks.clear();
        propFrozenPositions.clear();
    }
}
