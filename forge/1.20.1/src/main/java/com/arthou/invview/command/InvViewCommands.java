package com.arthou.invview.command;

import com.arthou.invview.menu.InventoryViewCompat;
import com.arthou.invview.menu.TargetExtraInventoryMenu;
import com.arthou.invview.menu.TargetInventoryMenu;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class InvViewCommands {
    private static final SimpleCommandExceptionType NO_TARGET =
        new SimpleCommandExceptionType(Component.literal("Player not found."));

    private InvViewCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("view")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("inv")
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                    .executes(context -> openInventory(context)))
                .then(Commands.literal("modify")
                    .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .executes(context -> openInventory(context)))))
            .then(Commands.literal("curios")
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                    .executes(context -> openCurios(context))))
            .then(Commands.literal("cosmetic")
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                    .executes(context -> openCosmetic(context))))
            .then(Commands.literal("echest")
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                    .executes(context -> openEnderChest(context)))
                .then(Commands.literal("modify")
                    .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .executes(context -> openEnderChest(context))))));
    }

    private static int openInventory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer viewer = context.getSource().getPlayerOrException();
        LoadedTarget loadedTarget = loadTarget(context);
        ItemStack[] contents = copyInventoryList(loadedTarget.player().getInventory().items, 36);
        ItemStack[] armor = copyInventoryList(loadedTarget.player().getInventory().armor, 4);
        ItemStack[] offhand = copyInventoryList(loadedTarget.player().getInventory().offhand, 1);

        viewer.openMenu(new SimpleMenuProvider((containerId, playerInventory, player) ->
            TargetInventoryMenu.forPlayerInventory(containerId, playerInventory, loadedTarget, true, contents, armor, offhand),
            Component.literal(loadedTarget.name() + " Inventory*")));
        return Command.SINGLE_SUCCESS;
    }

    private static int openCurios(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer viewer = context.getSource().getPlayerOrException();
        LoadedTarget loadedTarget = loadTarget(context);
        InventoryViewCompat.CompatState compat = InventoryViewCompat.capture(loadedTarget.player());
        if (compat.rightSlots().isEmpty()) {
            context.getSource().sendFailure(Component.literal("This player has no Curios slots available."));
            return 0;
        }

        viewer.openMenu(new SimpleMenuProvider((containerId, playerInventory, player) ->
            new TargetExtraInventoryMenu(containerId, playerInventory, compat.rightSlots(), compat.saveHook(), true),
            Component.literal(loadedTarget.name() + " Curios*")));
        return Command.SINGLE_SUCCESS;
    }

    private static int openCosmetic(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer viewer = context.getSource().getPlayerOrException();
        LoadedTarget loadedTarget = loadTarget(context);
        InventoryViewCompat.CompatState compat = InventoryViewCompat.capture(loadedTarget.player());
        if (compat.leftSlots().isEmpty()) {
            context.getSource().sendFailure(Component.literal("This player has no Cosmetic Armor slots available."));
            return 0;
        }

        viewer.openMenu(new SimpleMenuProvider((containerId, playerInventory, player) ->
            new TargetExtraInventoryMenu(containerId, playerInventory, compat.leftSlots(), compat.saveHook(), true),
            Component.literal(loadedTarget.name() + " Cosmetic Armor*")));
        return Command.SINGLE_SUCCESS;
    }

    private static int openEnderChest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer viewer = context.getSource().getPlayerOrException();
        LoadedTarget loadedTarget = loadTarget(context);
        Container enderChest = loadedTarget.player().getEnderChestInventory();
        ItemStack[] contents = copyContainer(enderChest);
        MenuType<?> menuType = TargetInventoryMenu.getMenuTypeForSize(contents.length);

        viewer.openMenu(new SimpleMenuProvider((containerId, playerInventory, player) ->
            TargetInventoryMenu.forGenericContainer(containerId, playerInventory, loadedTarget, true, contents, menuType, true),
            Component.literal(loadedTarget.name() + " Ender Chest*")));
        return Command.SINGLE_SUCCESS;
    }

    private static LoadedTarget loadTarget(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        GameProfile entry = GameProfileArgument.getGameProfiles(context, "target").iterator().next();
        if (entry.getId() == null) {
            throw NO_TARGET.create();
        }

        MinecraftServer server = context.getSource().getServer();
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(entry.getId());
        if (onlinePlayer != null) {
            return new LoadedTarget(entry.getName(), onlinePlayer, true, server);
        }

        GameProfile profile = new GameProfile(entry.getId(), entry.getName());
        ServerPlayer offlinePlayer = OfflinePlayerAccess.loadPlayer(server, profile);
        return new LoadedTarget(entry.getName(), offlinePlayer, false, server);
    }

    private static ItemStack[] copyContainer(Container container) {
        ItemStack[] contents = new ItemStack[container.getContainerSize()];
        for (int i = 0; i < contents.length; i++) {
            contents[i] = copy(container.getItem(i));
        }
        return contents;
    }

    private static ItemStack[] copyInventoryList(net.minecraft.core.NonNullList<ItemStack> source, int size) {
        ItemStack[] contents = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            contents[i] = i < source.size() ? copy(source.get(i)) : ItemStack.EMPTY;
        }
        return contents;
    }

    private static ItemStack copy(ItemStack stack) {
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    public record LoadedTarget(String name, ServerPlayer player, boolean online, MinecraftServer server) {
    }
}
