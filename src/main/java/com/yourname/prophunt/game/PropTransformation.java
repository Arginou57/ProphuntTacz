package com.yourname.prophunt.game;

import com.yourname.prophunt.config.PropHuntConfig;
import com.yourname.prophunt.network.FreezeSyncPayload;
import com.yourname.prophunt.sounds.ModSounds;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PropTransformation {
    private BlockState blockState;
    private CompoundTag blockNBT; // Store NBT data for modded blocks
    private final ServerPlayer player;
    private boolean isTransformed;
    private Vec3 lastPosition;
    private Display.BlockDisplay displayEntity; // Entité BlockDisplay (mode mobile)
    private boolean isFrozen; // Si le joueur est figé (shift)

    // Freeze cooldown system
    private int freezeTimer = 0; // Temps depuis le début du freeze
    private int freezeCooldown = 0; // Cooldown avant de pouvoir re-freeze
    private int maxCooldown = 0; // Maximum cooldown pour cette session (pour la barre)

    private GameState gameState = GameState.LOBBY;  // Track game state for sound logic

    public PropTransformation(ServerPlayer player) {
        this.player = player;
        this.isTransformed = false;
        this.blockState = Blocks.BARREL.defaultBlockState();
    }

    public void setGameState(GameState state) {
        this.gameState = state;
    }

    public void transformIntoBlock(BlockState state) {
        if (state == null || state.isAir()) {
            player.sendSystemMessage(Component.literal("§cCannot transform into that block!"));
            return;
        }

        // Remove old display
        removeDisplay();

        this.blockState = state;
        this.isTransformed = true;
        this.lastPosition = player.position();
        this.isFrozen = false;

        // Create floating entity by default (mobile mode)
        createDisplayEntity();

        // Hide player's nametag during transformation
        player.setCustomNameVisible(false);

        // Notify player
        String blockName = blockState.getBlock().getName().getString();
        player.sendSystemMessage(Component.literal("§aTransformed into: §e" + blockName));
        player.sendSystemMessage(Component.literal("§7Hold Shift to freeze as a solid block!"));
        player.sendSystemMessage(Component.literal("§c⚠ Warning: You'll make noise every 30 seconds!"));

        // Apply visual effects
        applyTransformationEffects();
    }

    /**
     * Transform into a block with NBT data (for modded blocks with properties)
     */
    public void transformIntoBlockWithNBT(BlockState state, CompoundTag nbtData) {
        if (state == null || state.isAir()) {
            player.sendSystemMessage(Component.literal("§cCannot transform into that block!"));
            return;
        }

        // Remove old display
        removeDisplay();

        this.blockState = state;
        this.blockNBT = nbtData;
        this.isTransformed = true;
        this.lastPosition = player.position();
        this.isFrozen = false;

        // Create floating entity by default (mobile mode)
        createDisplayEntity();

        // Hide player's nametag during transformation
        player.setCustomNameVisible(false);

        // Notify player
        String blockName = blockState.getBlock().getName().getString();
        player.sendSystemMessage(Component.literal("§aTransformed into: §e" + blockName));
        player.sendSystemMessage(Component.literal("§7Hold Shift to freeze as a solid block!"));
        player.sendSystemMessage(Component.literal("§c⚠ Warning: You'll make noise every 30 seconds!"));

        // Apply visual effects
        applyTransformationEffects();
    }

    public void transformIntoNearbyBlock() {
        BlockPos playerPos = player.blockPosition();
        BlockState foundState = null;

        // Search for a suitable block nearby
        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    BlockState state = player.level().getBlockState(checkPos);

                    if (isValidPropBlock(state)) {
                        foundState = state;
                        break;
                    }
                }
                if (foundState != null) break;
            }
            if (foundState != null) break;
        }

        if (foundState != null) {
            transformIntoBlock(foundState);
        } else {
            // Default to barrel if no suitable block found
            transformIntoBlock(Blocks.BARREL.defaultBlockState());
            player.sendSystemMessage(Component.literal("§eNo suitable blocks nearby, transformed into barrel!"));
        }
    }

    public void revert() {
        if (!isTransformed) {
            return;
        }

        this.isTransformed = false;
        this.isFrozen = false;
        this.freezeTimer = 0;
        this.freezeCooldown = 0;
        this.maxCooldown = 0;

        removeDisplay();
        removeTransformationEffects();

        // Restore player's nametag visibility
        player.setCustomNameVisible(true);

        // Reset XP bar
        player.giveExperienceLevels(-player.experienceLevel);
        player.experienceProgress = 0.0f;

        player.sendSystemMessage(Component.literal("§eReverted to normal form!"));
    }

    private void applyTransformationEffects() {
        // Set player to adventure mode
        player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);

        // Apply invisibility effect without potion particles
        player.addEffect(new MobEffectInstance(
            MobEffects.INVISIBILITY,
            Integer.MAX_VALUE, // Infinite duration
            0,
            true, // hideParticles: hide potion effect particles
            false
        ));

        // Apply Speed 1 effect
        player.addEffect(new MobEffectInstance(
            MobEffects.MOVEMENT_SPEED,
            Integer.MAX_VALUE, // Infinite duration
            0, // Speed 1 (level 0 = Speed I)
            true, // hideParticles
            false
        ));
    }

    private void removeTransformationEffects() {
        player.removeEffect(MobEffects.INVISIBILITY);
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    }

    private boolean isValidPropBlock(BlockState state) {
        if (state.isAir()) {
            return false;
        }

        Block block = state.getBlock();

        // Exclude certain blocks
        return block != Blocks.BEDROCK &&
               block != Blocks.BARRIER &&
               block != Blocks.COMMAND_BLOCK &&
               block != Blocks.CHAIN_COMMAND_BLOCK &&
               block != Blocks.REPEATING_COMMAND_BLOCK &&
               block != Blocks.END_PORTAL &&
               block != Blocks.END_PORTAL_FRAME &&
               block != Blocks.NETHER_PORTAL;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public boolean isTransformed() {
        return isTransformed;
    }

    public AABB getTransformedBoundingBox() {
        if (!isTransformed) {
            return player.getBoundingBox();
        }

        // Create a smaller bounding box based on the block
        Vec3 pos = player.position();
        double size = 0.5;
        return new AABB(
            pos.x - size, pos.y, pos.z - size,
            pos.x + size, pos.y + size * 2, pos.z + size
        );
    }

    private void createDisplayEntity() {
        if (blockState == null || blockState.isAir()) {
            return;
        }

        // Remove old entity if exists
        removeDisplayEntity();

        // Create a BlockDisplay centered on the player
        Vec3 pos = player.position();
        displayEntity = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, player.level());
        displayEntity.setPos(pos.x, pos.y, pos.z);

        // Set entity properties
        displayEntity.setInvulnerable(true);
        displayEntity.setNoGravity(true);

        // Add to world FIRST
        player.level().addFreshEntity(displayEntity);

        // Now set ALL properties via NBT (this is the only reliable way)
        CompoundTag entityData = new CompoundTag();
        displayEntity.saveWithoutId(entityData);

        // Set block state
        CompoundTag blockStateNBT = NbtUtils.writeBlockState(blockState);

        // Merge NBT data if available (for player heads, etc.)
        if (blockNBT != null) {
            for (String key : blockNBT.getAllKeys()) {
                blockStateNBT.put(key, blockNBT.get(key));
            }
            System.out.println("[PropTransformation] Merged NBT data: " + blockNBT);
        }

        entityData.put("block_state", blockStateNBT);

        // Set display properties using NBT
        entityData.putByte("billboard", (byte) 2); // FIXED = 2
        entityData.putFloat("shadow_radius", 0.0f);
        entityData.putFloat("shadow_strength", 1.0f);
        entityData.putFloat("view_range", 1.0f);
        entityData.putFloat("width", 0.0f);
        entityData.putFloat("height", 0.0f);
        entityData.putInt("interpolation_duration", 0);
        entityData.putInt("teleport_duration", 0);

        // Set transformation via NBT
        CompoundTag transformation = new CompoundTag();

        // left_rotation [0.0f, 0.0f, 0.0f, 1.0f]
        ListTag leftRot = new ListTag();
        leftRot.add(FloatTag.valueOf(0.0f));
        leftRot.add(FloatTag.valueOf(0.0f));
        leftRot.add(FloatTag.valueOf(0.0f));
        leftRot.add(FloatTag.valueOf(1.0f));
        transformation.put("left_rotation", leftRot);

        // translation [0.0d, 0.0d, 0.0d]
        ListTag trans = new ListTag();
        trans.add(DoubleTag.valueOf(0.0d));
        trans.add(DoubleTag.valueOf(0.0d));
        trans.add(DoubleTag.valueOf(0.0d));
        transformation.put("translation", trans);

        // scale [1.0f, 1.0f, 1.0f]
        ListTag scaleTag = new ListTag();
        scaleTag.add(FloatTag.valueOf(1.0f));
        scaleTag.add(FloatTag.valueOf(1.0f));
        scaleTag.add(FloatTag.valueOf(1.0f));
        transformation.put("scale", scaleTag);

        // right_rotation [0.0f, 0.0f, 0.0f, 1.0f]
        ListTag rightRot = new ListTag();
        rightRot.add(FloatTag.valueOf(0.0f));
        rightRot.add(FloatTag.valueOf(0.0f));
        rightRot.add(FloatTag.valueOf(0.0f));
        rightRot.add(FloatTag.valueOf(1.0f));
        transformation.put("right_rotation", rightRot);

        entityData.put("transformation", transformation);

        // Load all NBT data into the entity
        displayEntity.load(entityData);

        System.out.println("[PropTransformation] ✓ Created BlockDisplay for: " + blockState.getBlock().getName().getString());
        System.out.println("[PropTransformation] Block State: " + blockStateNBT);
        System.out.println("[PropTransformation] Position: " + pos);
    }

    private void removeDisplayEntity() {
        if (displayEntity != null && !displayEntity.isRemoved()) {
            displayEntity.discard();
            displayEntity = null;
        }
    }

    private void removeDisplay() {
        removeDisplayEntity();
    }

    public void update() {
        if (isTransformed) {
            // Reapply invisibility and speed effects to ensure they stay active
            if (!player.hasEffect(MobEffects.INVISIBILITY)) {
                player.addEffect(new MobEffectInstance(
                    MobEffects.INVISIBILITY,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false,
                    false
                ));
            }
            if (!player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    Integer.MAX_VALUE,
                    0, // Speed I
                    false,
                    false,
                    false
                ));
            }

            // Check if player is sneaking
            boolean wasFrozen = isFrozen;
            boolean isSneaking = player.isCrouching();

            // Handle freeze cooldown
            if (freezeCooldown > 0) {
                // Player is in cooldown, cannot freeze
                freezeCooldown--;
                isFrozen = false;

                // Remove slowness effect when not frozen
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

                // Send freeze sync to client
                float cooldownProgress = (float) freezeCooldown / PropHuntConfig.getFreezeCooldownTicks();
                int secondsRemaining = freezeCooldown / 20;
                PacketDistributor.sendToPlayer(player, new FreezeSyncPayload(1.0f - cooldownProgress, false, true, secondsRemaining));

            } else if (isSneaking && freezeTimer < PropHuntConfig.getMaxFreezeTimeTicks()) {
                // Player can freeze
                isFrozen = true;
                freezeTimer++;

                // Apply slowness effect so HUD can detect frozen state
                if (!player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        40, // 2 seconds, refreshed each tick
                        255, // Max slowness
                        false,
                        false,
                        false
                    ));
                }

                // Send freeze sync to client
                float freezeProgress = 1.0f - ((float) freezeTimer / PropHuntConfig.getMaxFreezeTimeTicks());
                int secondsRemaining = (int) (freezeProgress * PropHuntConfig.COMMON.freezeMaxTime.get());
                PacketDistributor.sendToPlayer(player, new FreezeSyncPayload(freezeProgress, true, false, secondsRemaining));

            } else if (isSneaking && freezeTimer >= PropHuntConfig.getMaxFreezeTimeTicks()) {
                // Freeze time exhausted, start cooldown
                isFrozen = false;
                freezeCooldown = PropHuntConfig.getFreezeCooldownTicks();
                freezeTimer = 0;

                // Remove slowness effect
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

                player.sendSystemMessage(Component.literal("§c§lFreeze exhausted! Recharging..."));

            } else {
                // Not sneaking, slowly regenerate freeze time
                isFrozen = false;
                if (freezeTimer > 0) {
                    freezeTimer = Math.max(0, freezeTimer - PropHuntConfig.getFreezeRegenRate()); // Regenerate based on config
                }

                // Remove slowness effect when not frozen
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

                // Send freeze sync to client
                float freezeProgress = 1.0f - ((float) freezeTimer / PropHuntConfig.getMaxFreezeTimeTicks());
                int secondsRemaining = (int) (freezeProgress * PropHuntConfig.COMMON.freezeMaxTime.get());
                PacketDistributor.sendToPlayer(player, new FreezeSyncPayload(freezeProgress, false, false, secondsRemaining));
            }

            // Player just unfroze: offset slightly to avoid being inside the block
            if (!isFrozen && wasFrozen) {
                Vec3 pos = player.position();
                player.teleportTo(pos.x + 0.2, pos.y, pos.z + 0.2);
            }

            if (isFrozen && !wasFrozen) {
                // Player just started sneaking: teleport to center of block
                BlockPos blockPos = player.blockPosition();
                double centerX = blockPos.getX() + 0.5;
                double centerY = blockPos.getY();
                double centerZ = blockPos.getZ() + 0.5;

                player.teleportTo(centerX, centerY, centerZ);

                // Teleport display entity centered on player with offset
                if (displayEntity != null && !displayEntity.isRemoved()) {
                    displayEntity.setPos(centerX - 0.5, centerY, centerZ - 0.5);
                }

            } else if (isFrozen) {
                // Player is still sneaking: keep both at center position and prevent movement
                BlockPos blockPos = player.blockPosition();
                double centerX = blockPos.getX() + 0.5;
                double centerY = blockPos.getY();
                double centerZ = blockPos.getZ() + 0.5;

                // Force player to stay at center position
                player.setPos(centerX, centerY, centerZ);
                player.setDeltaMovement(0, player.getDeltaMovement().y, 0);

                // Keep display entity centered on player with offset
                if (displayEntity != null && !displayEntity.isRemoved()) {
                    displayEntity.setPos(centerX - 0.5, centerY, centerZ - 0.5);
                }

            } else {
                // Player is moving: display entity follows player smoothly with offset
                Vec3 playerPos = player.position();
                if (displayEntity != null && !displayEntity.isRemoved()) {
                    displayEntity.setPos(playerPos.x - 0.5, playerPos.y, playerPos.z - 0.5);
                } else {
                    // Entity was lost, recreate it
                    createDisplayEntity();
                }
            }

            lastPosition = player.position();
        }
    }

}
