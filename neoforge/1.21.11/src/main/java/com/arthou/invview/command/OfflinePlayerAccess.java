package com.arthou.invview.command;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class OfflinePlayerAccess {
    private OfflinePlayerAccess() {
    }

    public static ServerPlayer loadPlayer(MinecraftServer server, GameProfile profile) {
        ServerPlayer player = new ServerPlayer(server, server.overworld(), profile, ClientInformation.createDefault());
        Path path = getPlayerFile(server, profile.id().toString());
        if (Files.exists(path)) {
            try {
                CompoundTag tag = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
                if (tag != null) {
                    player.load(TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), tag));
                }
            } catch (IOException ignored) {
            }
        }
        return player;
    }

    public static void savePlayer(MinecraftServer server, ServerPlayer player) {
        Path playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        try {
            Files.createDirectories(playerDataDir);
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, server.registryAccess());
            player.saveWithoutId(output);
            CompoundTag tag = output.buildResult();
            Path tempFile = Files.createTempFile(playerDataDir, player.getStringUUID() + "-", ".dat");
            NbtIo.writeCompressed(tag, tempFile);
            Path current = getPlayerFile(server, player.getStringUUID());
            Path backup = playerDataDir.resolve(player.getStringUUID() + ".dat_old");
            if (Files.exists(current)) {
                Files.copy(current, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tempFile, current, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    private static Path getPlayerFile(MinecraftServer server, String uuid) {
        return server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(uuid + ".dat");
    }
}
