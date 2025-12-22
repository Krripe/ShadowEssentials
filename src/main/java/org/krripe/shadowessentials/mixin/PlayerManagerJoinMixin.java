package org.krripe.shadowessentials.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import org.krripe.shadowessentials.modules.spawn.SpawnManager;
import org.krripe.shadowessentials.modules.spawn.SpawnLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerJoinMixin {

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void shadowessentials$onFirstJoin(ClientConnection connection,
                                              ServerPlayerEntity player,
                                              ConnectedClientData clientData,
                                              CallbackInfo ci) {
        if (player.getServer() == null) return;

        String uuid = player.getUuidAsString();
        Path playerDataDir = Objects.requireNonNull(player.getServer()).getSavePath(WorldSavePath.PLAYERDATA);
        File playerDataFile = playerDataDir.resolve(uuid + ".dat").toFile();

        if (!playerDataFile.exists()) {
            SpawnLocation spawn = SpawnManager.INSTANCE.getSpawnLocation();
            if (spawn != null) {
                Identifier id = Identifier.tryParse(spawn.getWorld());
                if (id == null) return;

                RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, id);
                ServerWorld targetWorld = Objects.requireNonNull(player.getServer()).getWorld(worldKey);
                if (targetWorld != null) {
                    player.teleport(
                            targetWorld,
                            spawn.getX(),
                            spawn.getY(),
                            spawn.getZ(),
                            spawn.getYaw(),
                            spawn.getPitch()
                    );
                }
            }
        }
    }
}