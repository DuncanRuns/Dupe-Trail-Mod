package xyz.duncanruns.dupetrail.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin
        extends PlayerEntity
        implements ScreenHandlerListener {

    @Shadow
    @Final
    private List<Integer> removedEntities;
    @Shadow
    @Final
    public MinecraftServer server;
    private final Queue<BlockPos> dupeQueue = new LinkedList<>();
    private BlockPos lastDupePos = null;

    public ServerPlayerEntityMixin(World world, BlockPos blockPos, GameProfile gameProfile) {
        super(world, blockPos, gameProfile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickMixin(CallbackInfo info) {
        BlockPos newDupePos = getBlockPos();
        if (!newDupePos.equals(lastDupePos)) {
            lastDupePos = newDupePos;
        } else {
            return;
        }

        dupeQueue.add(newDupePos);

        if (!(dupeQueue.size() > 10)) {
            return;
        }


        BlockPos toDupe = dupeQueue.poll();

        final int height = 2;

        for (int i = 0; i < height; i++) {
            world.setBlockState(toDupe, world.getBlockState(toDupe.down()));
            toDupe = toDupe.up();
        }
    }

    @Inject(method = {"changeDimension", "dimensionChanged"}, at = @At("HEAD"))
    private void onDimChange(CallbackInfoReturnable<Entity> info) {
        dupeQueue.clear();
    }

    @Shadow
    public abstract ServerWorld getServerWorld();

}
