package com.yourname.prophunt.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI pour la sélection de props
 * NOTE: Cette classe nécessite un événement client-side pour être affichée
 * et des packets réseau pour communiquer la sélection au serveur
 */
public class PropSelectionScreen extends Screen {

    private final List<Block> availableBlocks = new ArrayList<>();
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public PropSelectionScreen() {
        super(Component.literal("Select Prop"));
        initializeBlocks();
    }

    private void initializeBlocks() {
        // Blocs communs pour la transformation
        availableBlocks.add(Blocks.OAK_PLANKS);
        availableBlocks.add(Blocks.STONE);
        availableBlocks.add(Blocks.COBBLESTONE);
        availableBlocks.add(Blocks.BARREL);
        availableBlocks.add(Blocks.CHEST);
        availableBlocks.add(Blocks.CRAFTING_TABLE);
        availableBlocks.add(Blocks.FURNACE);
        availableBlocks.add(Blocks.HAY_BLOCK);
        availableBlocks.add(Blocks.BOOKSHELF);
        availableBlocks.add(Blocks.BRICKS);
        availableBlocks.add(Blocks.GRASS_BLOCK);
        availableBlocks.add(Blocks.DIRT);
        availableBlocks.add(Blocks.OAK_LEAVES);
        availableBlocks.add(Blocks.GLASS);
        availableBlocks.add(Blocks.SAND);
        availableBlocks.add(Blocks.GRAVEL);
        availableBlocks.add(Blocks.CLAY);
        availableBlocks.add(Blocks.PUMPKIN);
        availableBlocks.add(Blocks.MELON);
        availableBlocks.add(Blocks.CACTUS);
    }

    @Override
    protected void init() {
        super.init();
        // Add buttons or widgets here if needed
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Title
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // Instructions
        Component instructions = Component.literal("Click a block to transform");
        graphics.drawCenteredString(this.font, instructions, this.width / 2, 40, 0xAAAAAA);

        // Draw block grid
        int startX = this.width / 2 - 180;
        int startY = 60;
        int columns = 5;
        int spacing = 72;

        for (int i = scrollOffset; i < availableBlocks.size() && i < scrollOffset + 15; i++) {
            int row = (i - scrollOffset) / columns;
            int col = (i - scrollOffset) % columns;

            int x = startX + col * spacing;
            int y = startY + row * spacing;

            // Draw selection highlight
            if (i == selectedIndex) {
                graphics.fill(x - 2, y - 2, x + 68, y + 68, 0x88FFFF00);
            }

            // Draw background
            graphics.fill(x, y, x + 64, y + 64, 0x88000000);

            // Draw block name
            Block block = availableBlocks.get(i);
            String blockName = block.getName().getString();
            if (blockName.length() > 12) {
                blockName = blockName.substring(0, 12) + "...";
            }

            graphics.drawString(this.font, blockName, x + 2, y + 52, 0xFFFFFF);

            // Note: Rendering the actual block would require ItemStack rendering
            // This is simplified for the example
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int startX = this.width / 2 - 180;
        int startY = 60;
        int columns = 5;
        int spacing = 72;

        for (int i = scrollOffset; i < availableBlocks.size() && i < scrollOffset + 15; i++) {
            int row = (i - scrollOffset) / columns;
            int col = (i - scrollOffset) % columns;

            int x = startX + col * spacing;
            int y = startY + row * spacing;

            if (mouseX >= x && mouseX <= x + 64 && mouseY >= y && mouseY <= y + 64) {
                selectedIndex = i;
                onBlockSelected(availableBlocks.get(i));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, availableBlocks.size() - 15);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)scrollY));
        return true;
    }

    private void onBlockSelected(Block block) {
        // Here you would send a packet to the server to transform
        // Example: NetworkHandler.sendTransformPacket(block);

        // For now, just close the screen
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
