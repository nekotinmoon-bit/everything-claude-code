package com.cuteferrets.menu;

import com.cuteferrets.entity.FerretEntity;
import com.cuteferrets.entity.FerretMode;
import com.cuteferrets.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The ferret's menu: its 3x3 pouch plus mode buttons. Buttons are sent with the vanilla
 * "container button click" packet, so no custom networking is needed.
 */
public class FerretMenu extends AbstractContainerMenu {
    public static final int BUTTON_GIVE_LOOT = 100;
    public static final int BUTTON_DANCE = 101;

    public static final int WIDTH = 220;
    public static final int HEIGHT = 204;
    public static final int POUCH_X = 66;
    public static final int POUCH_Y = 20;
    public static final int PLAYER_INV_X = (WIDTH - 162) / 2 + 1;
    public static final int PLAYER_INV_Y = 122;

    private final Container pouch;
    @Nullable
    private final FerretEntity ferret;

    /** Client-side constructor. */
    public FerretMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, findFerret(playerInventory.player, buf.readVarInt()));
    }

    /** Server-side constructor. */
    public FerretMenu(int id, Inventory playerInventory, @Nullable FerretEntity ferret) {
        super(ModMenus.FERRET.get(), id);
        this.ferret = ferret;
        this.pouch = ferret != null ? ferret.getInventory() : new SimpleContainer(FerretEntity.INVENTORY_SIZE);
        pouch.startOpen(playerInventory.player);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(pouch, col + row * 3, POUCH_X + col * 18, POUCH_Y + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * 18, PLAYER_INV_Y + 58));
        }
    }

    @Nullable
    private static FerretEntity findFerret(Player player, int entityId) {
        Entity entity = player.level().getEntity(entityId);
        return entity instanceof FerretEntity ferret ? ferret : null;
    }

    @Nullable
    public FerretEntity getFerret() {
        return ferret;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (ferret == null || !ferret.isOwnedBy(player)) {
            return false;
        }
        if (id >= 0 && id < FerretMode.values().length) {
            ferret.setMode(FerretMode.byId(id));
            return true;
        }
        if (id == BUTTON_GIVE_LOOT) {
            ferret.giveLootTo(player);
            return true;
        }
        if (id == BUTTON_DANCE) {
            ferret.startDance();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return ferret != null && ferret.isAlive() && ferret.isOwnedBy(player) && ferret.distanceToSqr(player) < 8 * 8;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        pouch.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int pouchEnd = FerretEntity.INVENTORY_SIZE;
            if (index < pouchEnd) {
                if (!moveItemStackTo(stack, pouchEnd, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, pouchEnd, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }
}
