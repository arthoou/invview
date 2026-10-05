package com.arthou.invview.command;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class OfflinePlayerAccess {
    private OfflinePlayerAccess() {
    }

    public static ServerPlayer loadPlayer(MinecraftServer server, GameProfile profile) {
        ServerPlayer player = new ServerPlayer(server, server.overworld(), profile);
        Path path = getPlayerFile(server, profile.getId().toString());
        if (Files.exists(path)) {
            try {
                CompoundTag tag = NbtIo.readCompressed(path.toFile());
                if (tag != null) {
                    player.load(tag);
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
            CompoundTag tag = player.saveWithoutId(new CompoundTag());
            Path tempFile = Files.createTempFile(playerDataDir, player.getStringUUID() + "-", ".dat");
            NbtIo.writeCompressed(tag, tempFile.toFile());

            Path current = getPlayerFile(server, player.getStringUUID());
            Path backup = playerDataDir.resolve(player.getStringUUID() + ".dat_old");
            if (Files.exists(current)) {
                Files.copy(current, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            replace(tempFile, current);
        } catch (IOException ignored) {
        }
    }

    private static void replace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path getPlayerFile(MinecraftServer server, String uuid) {
        return server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(uuid + ".dat");
    }
}
