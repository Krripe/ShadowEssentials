package org.krripe.shadowessentials.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.krripe.shadowessentials.modules.spawn.SpawnManager;
import org.krripe.shadowessentials.modules.spawn.SpawnLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerRespawnMixin {

    @Inject(method = "respawnPlayer", at = @At("TAIL"))
    private void shadowessentials$teleportToCustomSpawn(ServerPlayerEntity player,
                                                        boolean alive,
                                                        Entity.RemovalReason removalReason,
                                                        CallbackInfoReturnable<ServerPlayerEntity> cir) {
        SpawnLocation spawn = SpawnManager.INSTANCE.getSpawnLocation();
        ServerPlayerEntity respawned = cir.getReturnValue();

        if (spawn != null && respawned != null && respawned.getServer() != null) {
            Identifier id = Identifier.tryParse(spawn.getWorld());
            if (id == null) return;

            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, id);
            ServerWorld targetWorld = Objects.requireNonNull(respawned.getServer()).getWorld(worldKey);
            if (targetWorld != null) {
                respawned.teleport(
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