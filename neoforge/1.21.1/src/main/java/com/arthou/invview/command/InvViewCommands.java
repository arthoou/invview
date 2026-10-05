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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class InvViewCommands {
    private InvViewCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("view")
            .requires(source -> source.hasPermission(2))
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
        ItemStack[] contents = loadedTarget.player().getInventory().items.toArray(ItemStack[]::new);
        ItemStack[] armor = loadedTarget.player().getInventory().armor.toArray(ItemStack[]::new);
        ItemStack[] offhand = loadedTarget.player().getInventory().offhand.toArray(ItemStack[]::new);

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
        GameProfile entry = GameProfileArgument.getGameProfiles(context, "target").iterator().next();
        MinecraftServer server = context.getSource().getServer();
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(entry.getId());
        if (onlinePlayer != null) {
            return new LoadedTarget(entry.getName(), onlinePlayer, true);
        }

        GameProfile profile = new GameProfile(entry.getId(), entry.getName());
        ServerPlayer offlinePlayer = OfflinePlayerAccess.loadPlayer(server, profile);
        return new LoadedTarget(entry.getName(), offlinePlayer, false);
    }

    public record LoadedTarget(String name, ServerPlayer player, boolean online) {
    }
}
