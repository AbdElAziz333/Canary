package com.abdelaziz.canary.mixin.gen.cached_generator_settings;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoiseBasedChunkGenerator.class)
public class NoiseBasedChunkGeneratorMixin {

    @Shadow
    @Final
    protected Holder<NoiseGeneratorSettings> settings;
    private int cachedSeaLevel;

    /**
     * Use cached sea level instead of retrieving from the registry every time.
     * This method is called for every block in the chunk so this will save a lot of registry lookups.
     *
     * @author SuperCoder79
     * @reason avoid registry lookup
     */
    @Overwrite
    public int getSeaLevel() {
        return this.cachedSeaLevel;
    }

    /**
     * Initialize the cache early in the ctor to avoid potential future problems with uninitialized usages
     */
    @SuppressWarnings("rawtypes")
    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/Holder;value()Ljava/lang/Object;",
                    shift = At.Shift.BEFORE
            )
    )
    private void hookConstructor(Registry structureSetRegistry, Registry noiseRegistry, BiomeSource populationSource, Holder registryEntry, CallbackInfo ci) {
        this.cachedSeaLevel = this.settings.value().seaLevel();
    }
}
