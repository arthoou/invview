package com.arthou.invview.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class LivePlayerInventoryContainer implements Container {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.FEET,
        EquipmentSlot.LEGS,
        EquipmentSlot.CHEST,
        EquipmentSlot.HEAD
    };

    private final Player target;

    public LivePlayerInventoryContainer(Player target) {
        this.target = target;
    }

    @Override
    public int getContainerSize() {
        return 45;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 36) {
            return target.getInventory().getItem(slot);
        }
        if (slot < 40) {
            return target.getItemBySlot(ARMOR_SLOTS[slot - 36]);
        }
        if (slot == 40) {
            return target.getItemBySlot(EquipmentSlot.OFFHAND);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        ItemStack result = stack.split(amount);
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 36) {
            target.getInventory().setItem(slot, stack);
        } else if (slot < 40) {
            target.setItemSlot(ARMOR_SLOTS[slot - 36], stack);
        } else if (slot == 40) {
            target.setItemSlot(EquipmentSlot.OFFHAND, stack);
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        target.getInventory().setChanged();
        if (target instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.broadcastChanges();
            serverPlayer.containerMenu.broadcastChanges();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < 36; i++) {
            target.getInventory().setItem(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < 4; i++) {
            target.setItemSlot(ARMOR_SLOTS[i], ItemStack.EMPTY);
        }
        target.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        setChanged();
    }
}
