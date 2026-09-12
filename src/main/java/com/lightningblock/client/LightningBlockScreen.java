package com.lightningblock.client;

import com.lightningblock.block.entity.LightningBlockEntity;
import com.lightningblock.menu.LightningBlockMenu;
import com.lightningblock.network.FilterUpdatePacket;
import com.lightningblock.network.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class LightningBlockScreen extends AbstractContainerScreen<LightningBlockMenu> {

    private static final int GUI_WIDTH = 230;
    private static final int GUI_HEIGHT = 220;

    private EditBox filterBox;

    public LightningBlockScreen(LightningBlockMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.filterBox = new EditBox(this.font,
                this.leftPos + 85, this.topPos + 32,
                135, 18,
                Component.translatable("container.lightningblock.filter"));
        this.filterBox.setMaxLength(256);
        this.filterBox.setValue(menu.getBlockEntity().getFilter());
        this.addWidget(this.filterBox);
        this.setInitialFocus(this.filterBox);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String value = this.filterBox.getValue();
        super.resize(minecraft, width, height);
        this.filterBox.setValue(value);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        // Sync filter from BE when the user is not actively editing
        if (!this.filterBox.isFocused()) {
            String current = menu.getBlockEntity().getFilter();
            if (!current.equals(this.filterBox.getValue())) {
                this.filterBox.setValue(current);
            }
        }
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        this.renderTooltip(graphics, mouseX, mouseY);
        this.filterBox.render(graphics, mouseX, mouseY, partial);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        // Dark panel background
        graphics.fill(this.leftPos, this.topPos,
                this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF000000);
        graphics.fill(this.leftPos + 1, this.topPos + 1,
                this.leftPos + this.imageWidth - 1, this.topPos + this.imageHeight - 1, 0xFF2A2A3A);
        // Slot column highlight
        graphics.fill(this.leftPos + 35, this.topPos + 27,
                this.leftPos + 63, this.topPos + 130, 0xFF1A1A2A);
        // Divider line between config area and player inventory
        graphics.fill(this.leftPos + 10, this.topPos + 132,
                this.leftPos + this.imageWidth - 10, this.topPos + 133, 0xFF444466);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 10, 6, 0xFFFFFF, false);

        // Slot labels
        graphics.drawString(this.font, "Gold / Star", 10, 36, 0xCCCCCC, false);
        graphics.drawString(this.font, "Emerald", 10, 74, 0xCCCCCC, false);
        graphics.drawString(this.font, "Diamond", 10, 112, 0xCCCCCC, false);

        // Filter label
        graphics.drawString(this.font, "Target Filter:", 85, 18, 0xAAAAFF, false);

        // Config readout
        int rem = menu.getRemaining();
        String remStr = rem == -1 ? "INF" : String.valueOf(rem);
        graphics.drawString(this.font, "Remaining: " + remStr, 85, 58, 0xFFD700, false);

        int freq = menu.getFreq();
        String freqStr = freq <= 0 ? "INF (never)" : String.format("%.1fs (%dt)", freq / 20.0, freq);
        graphics.drawString(this.font, "Freq: " + freqStr, 85, 76, 0x55FF55, false);

        graphics.drawString(this.font, "Area side: " + menu.getAreaSide() + " blocks",
                85, 94, 0x55FFFF, false);

        graphics.drawString(this.font, "Insert blocks to configure.", 85, 114, 0x999999, false);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // ESC is the ONLY key that closes the GUI — save filter first
        if (key == 256) {
            saveFilter();
            assert this.minecraft != null;
            this.minecraft.player.closeContainer();
            return true;
        }
        // Let the filter box handle editing keys (arrows, backspace, delete, etc.)
        if (this.filterBox.keyPressed(key, scan, mods)) {
            return true;
        }
        // Swallow every other key so game keybinds (e = inventory, etc.)
        // never close this screen or steal focus.  charTyped still fires
        // afterwards and feeds printable characters into the box.
        return true;
    }

    private void saveFilter() {
        LightningBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            ModMessages.sendToServer(new FilterUpdatePacket(be.getBlockPos(), this.filterBox.getValue()));
        }
    }

    @Override
    public void removed() {
        // Fallback: also save if the screen is removed by any other path
        saveFilter();
        super.removed();
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (this.filterBox.charTyped(c, mods)) return true;
        return super.charTyped(c, mods);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.filterBox.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
