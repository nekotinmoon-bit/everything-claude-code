package com.cuteferrets.client;

import com.cuteferrets.entity.FerretEntity;
import com.cuteferrets.entity.FerretMode;
import com.cuteferrets.menu.FerretMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/** Ferret menu: live 3D preview, affection meter, the pouch, and buttons to pick what the ferret does. */
public class FerretScreen extends AbstractContainerScreen<FerretMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;

    private final List<Button> modeButtons = new ArrayList<>();

    public FerretScreen(FerretMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = FerretMenu.WIDTH;
        this.imageHeight = FerretMenu.HEIGHT;
        this.inventoryLabelX = FerretMenu.PLAYER_INV_X - 1;
        this.inventoryLabelY = FerretMenu.PLAYER_INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        modeButtons.clear();
        int x = leftPos + 130;
        for (FerretMode mode : FerretMode.values()) {
            int y = topPos + 18 + mode.ordinal() * 16;
            Button button = Button.builder(mode.displayName(), b -> press(mode.ordinal()))
                    .bounds(x, y, 82, 14)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                            Component.translatable("mode.cuteferrets." + mode.name().toLowerCase() + ".tooltip")))
                    .build();
            modeButtons.add(addRenderableWidget(button));
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.cuteferrets.give_loot"), b -> press(FerretMenu.BUTTON_GIVE_LOOT))
                .bounds(leftPos + 8, topPos + 96, 84, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cuteferrets.dance"), b -> press(FerretMenu.BUTTON_DANCE))
                .bounds(leftPos + 94, topPos + 96, 32, 14).build());
        updateButtons();
    }

    private void press(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void updateButtons() {
        FerretEntity ferret = menu.getFerret();
        FerretMode current = ferret != null ? ferret.getMode() : null;
        for (int i = 0; i < modeButtons.size(); i++) {
            modeButtons.get(i).active = current == null || current.ordinal() != i;
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        // panel with a bevelled border
        g.fill(x, y, x + imageWidth, y + imageHeight, SHADOW);
        g.fill(x, y, x + imageWidth - 1, y + imageHeight - 1, LIGHT);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL);

        // preview window
        sunkenBox(g, x + 8, y + 18, 52, 60, 0xFF2B2B3A);

        // slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                slot(g, x + FerretMenu.POUCH_X - 1 + col * 18, y + FerretMenu.POUCH_Y - 1 + row * 18);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slot(g, x + FerretMenu.PLAYER_INV_X - 1 + col * 18, y + FerretMenu.PLAYER_INV_Y - 1 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            slot(g, x + FerretMenu.PLAYER_INV_X - 1 + col * 18, y + FerretMenu.PLAYER_INV_Y + 57);
        }

        FerretEntity ferret = menu.getFerret();
        if (ferret != null) {
            // affection bar
            int barX = x + 8;
            int barY = y + 86;
            int barW = 118;
            sunkenBox(g, barX, barY, barW, 8, 0xFF3A2A2A);
            int filled = (barW - 2) * ferret.getAffection() / FerretEntity.MAX_AFFECTION;
            g.fill(barX + 1, barY + 1, barX + 1 + filled, barY + 7, 0xFFFF6FA8);
            g.fill(barX + 1, barY + 1, barX + 1 + filled, barY + 3, 0xFFFFA8CB);

            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 9, y + 19, x + 59, y + 77, 30, 0.1F,
                    mouseX, mouseY, ferret);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        FerretEntity ferret = menu.getFerret();
        if (ferret == null) {
            return;
        }
        Component love = Component.translatable("gui.cuteferrets.affection", ferret.getAffection(), FerretEntity.MAX_AFFECTION);
        g.drawString(this.font, love, 130 + 41 - this.font.width(love) / 2, 18 + 5 * 16 + 2, 0xFFB03060, false);
        if (ferret.hasTreatBuff()) {
            Component buff = Component.translatable("gui.cuteferrets.treat_buff");
            g.drawString(this.font, buff, 130 + 41 - this.font.width(buff) / 2, 18 + 5 * 16 + 12, 0xFF2F7F2F, false);
        }
        int hp = (int) Math.ceil(ferret.getHealth());
        Component health = Component.translatable("gui.cuteferrets.health", hp, (int) ferret.getMaxHealth());
        g.drawString(this.font, health, 66, 76, 0xFF404040, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        super.renderTooltip(g, mouseX, mouseY);
        int barX = leftPos + 8;
        int barY = topPos + 86;
        FerretEntity ferret = menu.getFerret();
        if (ferret != null && mouseX >= barX && mouseX < barX + 118 && mouseY >= barY && mouseY < barY + 8) {
            g.renderTooltip(this.font, Component.translatable("gui.cuteferrets.affection.tooltip"), mouseX, mouseY);
        }
    }

    private static void slot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, LIGHT);
        g.fill(x, y, x + 17, y + 17, SLOT_DARK);
        g.fill(x + 1, y + 1, x + 17, y + 17, SLOT_BG);
    }

    private static void sunkenBox(GuiGraphics g, int x, int y, int w, int h, int inner) {
        g.fill(x, y, x + w, y + h, LIGHT);
        g.fill(x, y, x + w - 1, y + h - 1, SLOT_DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, inner);
    }
}
