package com.abdelaziz.canary.mixin.world.biome_manager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@OnlyIn(Dist.CLIENT)
@Mixin(BiomeManager.class)
public class BiomeManagerMixin {
    /**
     * Optimized getBiome call: Reduce the number of calls to the mess of
     * {@link net.minecraft.util.LinearCongruentialGenerator#next(long, long)} which is pretty heavy on performance.
     * <p>
     * We are able to do this by skipping around 370 of 512 possible calls to getBiome() by predicting the outcome
     * before doing the seed mixing. This seems to be around 25% - 75% faster depending on the use case.
     * We can predict much faster than the seed mixing.
     *
     * @author FX - PR0CESS
     */

    @Shadow
    @Final
    private BiomeManager.NoiseBiomeSource noiseBiomeSource;

    @Shadow
    @Final
    private long biomeZoomSeed;

    @Shadow
    private static double method_38108(long l) {
        return 0;
    }

    private static final double maxOffset = 0.4500000001D;


    @Inject(
            method = "getBiome",
            at = @At("HEAD"),
            cancellable = true
    )
    public void optimizedGetBiome(BlockPos pos, CallbackInfoReturnable<Holder<Biome>> cir) {
        int xMinus2 = pos.getX() - 2;
        int yMinus2 = pos.getY() - 2;
        int zMinus2 = pos.getZ() - 2;
        int x = xMinus2 >> 2; // BlockPos to BiomePos
        int y = yMinus2 >> 2;
        int z = zMinus2 >> 2;
        double quartX = (double) (xMinus2 & 3) / 4.0D; // quartLocal divided by 4
        double quartY = (double) (yMinus2 & 3) / 4.0D; // 0/4, 1/4, 2/4, 3/4
        double quartZ = (double) (zMinus2 & 3) / 4.0D; // [0, 0.25, 0.5, 0.75]
        int smallestX = 0;
        double smallestDist = Double.POSITIVE_INFINITY;
        for (int biomeX = 0; biomeX < 8; ++biomeX) {
            boolean everyOtherQuad = (biomeX & 4) == 0; // 1 1 1 1 0 0 0 0
            boolean everyOtherPair = (biomeX & 2) == 0; // 1 1 0 0 1 1 0 0
            boolean everyOther = (biomeX & 1) == 0; // 1 0 1 0 1 0 1 0
            double quartXX = everyOtherQuad ? quartX : quartX - 1.0D; //[-1.0, -0.75, -0.5, -0.25, 0.0, 0.25, 0.5, 0.75]
            double quartYY = everyOtherPair ? quartY : quartY - 1.0D;
            double quartZZ = everyOther ? quartZ : quartZ - 1.0D;

            //This code block is new
            double maxQuartYY = 0.0D, maxQuartZZ = 0.0D;
            if (biomeX != 0) {
                maxQuartYY = Mth.square(Math.max(quartYY + maxOffset, Math.abs(quartYY - maxOffset)));
                maxQuartZZ = Mth.square(Math.max(quartZZ + maxOffset, Math.abs(quartZZ - maxOffset)));
                double maxQuartXX = Mth.square(Math.max(quartXX + maxOffset, Math.abs(quartXX - maxOffset)));
                if (smallestDist < maxQuartXX + maxQuartYY + maxQuartZZ) {
                    continue;
                }
            }

            int xx = everyOtherQuad ? x : x + 1;
            int yy = everyOtherPair ? y : y + 1;
            int zz = everyOther ? z : z + 1;

            //I transferred the code from method_38106 to here, so I could call continue halfway through
            long seed = LinearCongruentialGenerator.next(this.biomeZoomSeed, xx);
            seed = LinearCongruentialGenerator.next(seed, yy);
            seed = LinearCongruentialGenerator.next(seed, zz);
            seed = LinearCongruentialGenerator.next(seed, xx);
            seed = LinearCongruentialGenerator.next(seed, yy);
            seed = LinearCongruentialGenerator.next(seed, zz);
            double offsetX = method_38108(seed);
            double sqrX = Mth.square(quartXX + offsetX);
            if (biomeX != 0 && smallestDist < sqrX + maxQuartYY + maxQuartZZ) continue; // skip the rest of the loop
            seed = LinearCongruentialGenerator.next(seed, this.biomeZoomSeed);
            double offsetY = method_38108(seed);
            double sqrY = Mth.square(quartYY + offsetY);
            if (biomeX != 0 && smallestDist < sqrX + sqrY + maxQuartZZ) continue; // skip the rest of the loop
            seed = LinearCongruentialGenerator.next(seed, this.biomeZoomSeed);
            double offsetZ = method_38108(seed);
            double biomeDist = sqrX + sqrY + Mth.square(quartZZ + offsetZ);

            if (smallestDist > biomeDist) {
                smallestX = biomeX;
                smallestDist = biomeDist;
            }
        }

        //Back to the orignal code
        int biomeX = (smallestX & 4) == 0 ? x : x + 1;
        int biomeY = (smallestX & 2) == 0 ? y : y + 1;
        int biomeZ = (smallestX & 1) == 0 ? z : z + 1;
        cir.setReturnValue(this.noiseBiomeSource.getNoiseBiome(biomeX, biomeY, biomeZ));
    }
}