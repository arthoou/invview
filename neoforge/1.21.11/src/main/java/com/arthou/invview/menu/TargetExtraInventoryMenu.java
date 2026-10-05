package com.arthou.invview.menu;

import com.arthou.invview.command.InvViewCommands;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class TargetExtraInventoryMenu extends AbstractContainerMenu {
    private final InventoryViewCompat.AccessorContainer extraContainer;
    private final Runnable saveHook;
    private final boolean mutable;
    private final int rows;

    public TargetExtraInventoryMenu(int containerId, Inventory playerInventory, List<InventoryViewCompat.SlotAccessor> accessors, Runnable saveHook, boolean mutable) {
        super(TargetInventoryMenu.getMenuTypeForSize(Math.max(9, ((accessors.size() + 8) / 9) * 9)), containerId);
        this.extraContainer = new InventoryViewCompat.AccessorContainer(accessors, this::broadcastChanges);
        this.saveHook = saveHook;
        this.mutable = mutable;
        this.rows = Math.max(1, (accessors.size() + 8) / 9);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9;
                int x = 8 + col * 18;
                int y = 18 + row * 18;
                if (slotIndex < accessors.size()) {
                    addSlot(mutable ? new Slot(extraContainer, slotIndex, x, y) : new ReadOnlySlot(extraContainer, slotIndex, x, y));
                } else {
                    addSlot(new ReadOnlySlot(new net.minecraft.world.SimpleContainer(rows * 9), slotIndex, x, y));
                }
            }
        }

        int offset = (rows - 4) * 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + offset));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 161 + offset));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saveHook.run();
    }
}
