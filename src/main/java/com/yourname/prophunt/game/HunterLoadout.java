package com.yourname.prophunt.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * Manages the weapons and ammunition given to hunters at the start of the game.
 * Stores complete ItemStacks with NBT data.
 */
public class HunterLoadout {
    private static HunterLoadout instance;

    private final List<ItemStack> weapons = new ArrayList<>();
    private final List<ItemStack> ammo = new ArrayList<>();

    private HunterLoadout() {}

    public static HunterLoadout getInstance() {
        if (instance == null) {
            instance = new HunterLoadout();
        }
        return instance;
    }

    /**
     * Add a weapon from player's hand
     */
    public void addWeapon(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return;
        }
        weapons.add(itemStack.copy());
    }

    /**
     * Add ammunition from player's hand
     */
    public void addAmmo(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return;
        }
        ammo.add(itemStack.copy());
    }

    /**
     * Equip a player with all configured weapons and ammo
     */
    public void equipHunter(Player player) {
        // Equip weapons - first one goes to main hand, rest to inventory
        boolean firstWeapon = true;
        for (ItemStack weapon : weapons) {
            ItemStack stack = weapon.copy();

            if (firstWeapon && player.getMainHandItem().isEmpty()) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
                firstWeapon = false;
            } else {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
        }

        // Equip ammo to inventory
        for (ItemStack ammoItem : ammo) {
            ItemStack stack = ammoItem.copy();
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    /**
     * Give ammo back to a player (after a kill)
     */
    public void giveAmmo(Player player) {
        for (ItemStack ammoItem : ammo) {
            ItemStack stack = ammoItem.copy();
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    /**
     * Clear all weapons and ammo from the loadout
     */
    public void clear() {
        weapons.clear();
        ammo.clear();
    }

    /**
     * Get number of weapons configured
     */
    public int getWeaponCount() {
        return weapons.size();
    }

    /**
     * Get number of ammo types configured
     */
    public int getAmmoCount() {
        return ammo.size();
    }

    /**
     * Get a string representation of the loadout
     */
    public String getLoadoutInfo() {
        if (weapons.isEmpty() && ammo.isEmpty()) {
            return "No items configured";
        }

        StringBuilder sb = new StringBuilder();

        if (!weapons.isEmpty()) {
            sb.append("§6=== WEAPONS ===\n");
            int index = 1;
            for (ItemStack stack : weapons) {
                sb.append("§e").append(index).append(". ").append(stack.getHoverName().getString())
                    .append(" x").append(stack.getCount()).append("\n");
                index++;
            }
        }

        if (!ammo.isEmpty()) {
            sb.append("§6=== AMMUNITION ===\n");
            int index = 1;
            for (ItemStack stack : ammo) {
                sb.append("§e").append(index).append(". ").append(stack.getHoverName().getString())
                    .append(" x").append(stack.getCount()).append("\n");
                index++;
            }
        }

        return sb.toString();
    }
}
