package com.arthou.invview.menu;

import com.arthou.invview.command.InvViewCommands;
import com.arthou.invview.command.OfflinePlayerAccess;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class TargetInventoryMenu extends AbstractContainerMenu {
    private final Container targetContainer;
    private final InvViewCommands.LoadedTarget loadedTarget;
    private final boolean mutable;
    private final boolean enderChest;
    private final boolean temporaryContainer;
    private final int rows;

    private TargetInventoryMenu(
        int containerId,
        Inventory playerInventory,
        MenuType<?> menuType,
        Container targetContainer,
        InvViewCommands.LoadedTarget loadedTarget,
        boolean mutable,
        boolean enderChest,
        boolean temporaryContainer
    ) {
        super(menuType, containerId);
        this.targetContainer = targetContainer;
        this.loadedTarget = loadedTarget;
        this.mutable = mutable;
        this.enderChest = enderChest;
        this.temporaryContainer = temporaryContainer;
        this.rows = Math.max(1, targetContainer.getContainerSize() / 9);

        int offset = (this.rows - 4) * 18;
        for (int row = 0; row < this.rows; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9;
                int x = 8 + col * 18;
                int y = 18 + row * 18;
                addSlot(mutable ? new Slot(targetContainer, slotIndex, x, y) : new ReadOnlySlot(targetContainer, slotIndex, x, y));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + offset));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 161 + offset));
        }
    }

    public static TargetInventoryMenu forPlayerInventory(
        int containerId,
        Inventory playerInventory,
        InvViewCommands.LoadedTarget loadedTarget,
        boolean mutable,
        ItemStack[] contents,
        ItemStack[] armor,
        ItemStack[] offhand
    ) {
        if (loadedTarget.online()) {
            return new TargetInventoryMenu(
                containerId,
                playerInventory,
                MenuType.GENERIC_9x5,
                new LivePlayerInventoryContainer(loadedTarget.player()),
                loadedTarget,
                mutable,
                false,
                false
            );
        }

        SimpleContainer container = new SimpleContainer(45);
        for (int i = 0; i < contents.length && i < 36; i++) {
            container.setItem(i, copy(contents[i]));
        }
        for (int i = 0; i < armor.length && i < 4; i++) {
            container.setItem(36 + i, copy(armor[i]));
        }
        if (offhand.length > 0) {
            container.setItem(40, copy(offhand[0]));
        }
        return new TargetInventoryMenu(containerId, playerInventory, MenuType.GENERIC_9x5, container, loadedTarget, mutable, false, true);
    }

    public static TargetInventoryMenu forGenericContainer(
        int containerId,
        Inventory playerInventory,
        InvViewCommands.LoadedTarget loadedTarget,
        boolean mutable,
        ItemStack[] contents,
        MenuType<?> menuType,
        boolean enderChest
    ) {
        if (loadedTarget.online() && enderChest) {
            return new TargetInventoryMenu(
                containerId,
                playerInventory,
                menuType,
                loadedTarget.player().getEnderChestInventory(),
                loadedTarget,
                mutable,
                true,
                false
            );
        }

        int rows = Math.max(1, Math.min(6, (contents.length + 8) / 9));
        SimpleContainer container = new SimpleContainer(rows * 9);
        for (int i = 0; i < contents.length && i < container.getContainerSize(); i++) {
            container.setItem(i, copy(contents[i]));
        }
        return new TargetInventoryMenu(
            containerId,
            playerInventory,
            menuType,
            container,
            loadedTarget,
            mutable,
            enderChest,
            true
        );
    }

    public static MenuType<?> getMenuTypeForSize(int size) {
        int rows = Math.max(1, Math.min(6, (size + 8) / 9));
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
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
        if (!mutable || !temporaryContainer) {
            return;
        }

        if (enderChest) {
            for (int i = 0; i < targetContainer.getContainerSize(); i++) {
                loadedTarget.player().getEnderChestInventory().setItem(i, copy(targetContainer.getItem(i)));
            }
        } else {
            for (int i = 0; i < 36; i++) {
                loadedTarget.player().getInventory().items.set(i, copy(targetContainer.getItem(i)));
            }
            for (int i = 0; i < 4; i++) {
                loadedTarget.player().getInventory().armor.set(i, copy(targetContainer.getItem(36 + i)));
            }
            loadedTarget.player().getInventory().offhand.set(0, copy(targetContainer.getItem(40)));
        }

        if (!loadedTarget.online()) {
            OfflinePlayerAccess.savePlayer(loadedTarget.server(), loadedTarget.player());
        }
    }

    private static ItemStack copy(ItemStack stack) {
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }
}
