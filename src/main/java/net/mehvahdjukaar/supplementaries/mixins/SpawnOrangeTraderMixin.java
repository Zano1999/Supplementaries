package net.mehvahdjukaar.supplementaries.mixins;

import net.mehvahdjukaar.supplementaries.entities.OrangeMerchantEntity;
import net.mehvahdjukaar.supplementaries.setup.Registry;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.village.PointOfInterestType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.WanderingTraderSpawner;
import net.minecraft.world.storage.IServerWorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.Random;

@Mixin(WanderingTraderSpawner.class)
public abstract class SpawnOrangeTraderMixin {

    @Final
    @Shadow
    private Random random;
    @Final
    @Shadow
    private IServerWorldInfo serverLevelData;

    @Inject(method = "spawn", at = @At("RETURN"), cancellable = true)
    public void spawn(ServerWorld world, CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue()){
            //doesn't set cir to true, so it doesn't interfere with wandering trader spawn
            PlayerEntity playerentity = world.getRandomPlayer();
            //1/10 chance here already
            if (playerentity != null && this.random.nextInt(10) == 0) {

                BlockPos blockpos = playerentity.blockPosition();

                //17.5 % max on hard ->1.75% (wandering trader maxes at 7.5%)
                if (this.calculateNormalizeDifficulty(world, blockpos) > random.nextFloat()*100) {

                    PointOfInterestManager pointofinterestmanager = world.getPoiManager();
                    Optional<BlockPos> optional = pointofinterestmanager.find(PointOfInterestType.MEETING.getPredicate(), (p_221241_0_) -> true, blockpos, 48, PointOfInterestManager.Status.ANY);
                    BlockPos targetPos = optional.orElse(blockpos);
                    BlockPos spawnPos = this.findSpawnPositionNear(world, targetPos, 48);
                    if (spawnPos != null && this.hasEnoughSpace(world, spawnPos)) {
                        if (!world.getBiomeName(spawnPos).equals(Optional.of(Biomes.THE_VOID))) {

                            OrangeMerchantEntity trader = Registry.ORANGE_TRADER.get().spawn(world, null, null, null, spawnPos, SpawnReason.EVENT, false, false);
                            if (trader != null) {
                                this.serverLevelData.setWanderingTraderId(trader.getUUID());
                                trader.setDespawnDelay(25000);
                                trader.setWanderTarget(targetPos);
                                trader.restrictTo(targetPos, 16);
                            }
                        }
                    }
                }
            }
        }

    }

    private float calculateNormalizeDifficulty(ServerWorld world, BlockPos pos){
        long i = 0L;
        float f = 0.0F;
        if (world.hasChunkAt(pos)) {
            f = world.getMoonBrightness();
            i = world.getChunkAt(pos).getInhabitedTime();
        }
        //goes from 1.5 to 4 on normal
        float diff = (new DifficultyInstance(Difficulty.NORMAL, world.getDayTime(), i, f)).getEffectiveDifficulty();
        diff -= 1.5;
        //from 0 to 10
        diff *= 4;
        float scale;
        switch (world.getDifficulty()){
            default:
            case PEACEFUL:
                scale = 1;
                break;
            case EASY:
                scale = 1.25f;
                break;
            case NORMAL:
                scale = 1.5f;
                break;
            case HARD:
                scale = 1.75f;
                break;
        }

        diff *= scale;

        return diff;
    }

    @Shadow
    protected abstract boolean hasEnoughSpace(IBlockReader reader, BlockPos pos);

    @Shadow
    protected abstract BlockPos findSpawnPositionNear(IWorldReader world, BlockPos blockpos1, int i);
}