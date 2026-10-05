package com.arthou.invview.command;

import com.arthou.invview.menu.TargetInventoryMenu;
import com.arthou.invview.menu.TargetExtraInventoryMenu;
import com.arthou.invview.menu.InventoryViewCompat;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class InvViewCommands {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.FEET,
        EquipmentSlot.LEGS,
        EquipmentSlot.CHEST,
        EquipmentSlot.HEAD
    };

    private InvViewCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("view")
            .requires(source -> hasCommandLevel(source, 2))
            .then(Commands.literal("inv")
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                    .executes(context -> openInventory(context, true)))
                .then(Commands.literal("modify")
                    .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .executes(context -> openInventory(context, true)))))
            .then(Commands.literal("curios")
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                    .executes(context -> openCurios(context, true))))
            .then(Commands.literal("cosmetic")
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                    .executes(context -> openCosmetic(context, true))))
            .then(Commands.literal("echest")
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                    .executes(context -> openEnderChest(context, true)))
                .then(Commands.literal("modify")
                    .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .executes(context -> openEnderChest(context, true))))));
    }

    private static int openInventory(CommandContext<CommandSourceStack> context, boolean mutable) throws CommandSyntaxException {
        ServerPlayer viewer = context.getSource().getPlayerOrException();
        LoadedTarget loadedTarget = loadTarget(context);
        String title = mutable ? loadedTarget.name() + " Inventory*" : loadedTarget.name() + " Inventory";
        ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i < contents.length; i++) {
            contents[i] = loadedTarget.player().getInventory().getItem(i).copy();
        }
        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < armor.length; i++) {
            armor[i] = loadedTarget.player().getItemBySlot(ARMOR_SLOTS[i]).copy();
        }
        ItemStack[] offhand = {loadedTarget.player().getItemBySlot(EquipmentSlot.OFFHAND).copy()};

        viewer.openMenu(new SimpleMenuProvider((containerId, playerInventory, player) ->
            TargetInventoryMenu.forPlayerInventory(containerId, playerInventory, loadedTarget, mutable, contents, armor, offhand),
            Component.literal(title)));
        return Command.SINGLE_SUCCESS;
    }

    private static int openEnderChest(CommandContext<CommandSourceStack> context, boolean mutable) throws CommandSyntaxException {
        ServerPlayer viewer = context.getSource().getPlayerOrException();
        LoadedTarget loadedTarget = loadTarget(context);
        ItemStack[] contents = loadedTarget.player().getEnderChestInventory().getItems().toArray(ItemStack[]::new);
        MenuType<?> menuType = TargetInventoryMenu.getMenuTypeForSize(contents.length);
        String title = mutable ? loadedTarget.name() + " Ender Chest*" : loadedTarget.name() + " Ender Chest";

        viewer.openMenu(new SimpleMenuProvider((containerId, playerInventory, player) ->
            TargetInventoryMenu.forGenericContainer(containerId, playerInventory, loadedTarget, mutable, contents, menuType, true),
            Component.literal(title)));
        return Command.SINGLE_SUCCESS;
    }

    private static int openCurios(CommandContext<CommandSourceStack> context, boolean mutable) throws CommandSyntaxException {
        ServerPlayer viewer = context.getSource().getPlayerOrException();
        LoadedTarget loadedTarget = loadTarget(context);
        InventoryViewCompat.CompatState compat = InventoryViewCompat.capture(loadedTarget.player());
        if (compat.rightSlots().isEmpty()) {
            context.getSource().sendFailure(Component.literal("This player has no Curios slots available."));
            return 0;
        }
        viewer.openMenu(new SimpleMenuProvider((containerId, playerInventory, player) ->
            new TargetExtraInventoryMenu(containerId, playerInventory, compat.rightSlots(), compat.saveHook(), mutable),
            Component.literal(loadedTarget.name() + " Curios")));
        return Command.SINGLE_SUCCESS;
    }

    private static int openCosmetic(CommandContext<CommandSourceStack> context, boolean mutable) throws CommandSyntaxException {
        ServerPlayer viewer = context.getSource().getPlayerOrException();
        LoadedTarget loadedTarget = loadTarget(context);
        InventoryViewCompat.CompatState compat = InventoryViewCompat.capture(loadedTarget.player());
        if (compat.leftSlots().isEmpty()) {
            context.getSource().sendFailure(Component.literal("This player has no Cosmetic Armor slots available."));
            return 0;
        }
        viewer.openMenu(new SimpleMenuProvider((containerId, playerInventory, player) ->
            new TargetExtraInventoryMenu(containerId, playerInventory, compat.leftSlots(), compat.saveHook(), mutable),
            Component.literal(loadedTarget.name() + " Cosmetic Armor")));
        return Command.SINGLE_SUCCESS;
    }

    private static LoadedTarget loadTarget(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        NameAndId entry = GameProfileArgument.getGameProfiles(context, "target").iterator().next();
        MinecraftServer server = context.getSource().getServer();
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(entry.id());
        if (onlinePlayer != null) {
            return new LoadedTarget(entry.name(), onlinePlayer, true);
        }

        GameProfile profile = new GameProfile(entry.id(), entry.name());
        ServerPlayer offlinePlayer = OfflinePlayerAccess.loadPlayer(server, profile);
        return new LoadedTarget(entry.name(), offlinePlayer, false);
    }

    private static boolean hasCommandLevel(CommandSourceStack source, int level) {
        return source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(level)));
    }

    public record LoadedTarget(String name, ServerPlayer player, boolean online) {
    }
}
