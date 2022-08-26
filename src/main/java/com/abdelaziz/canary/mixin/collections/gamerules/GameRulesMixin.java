package com.abdelaziz.canary.mixin.collections.gamerules;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(GameRules.class)
public class GameRulesMixin {
    @Mutable
    @Shadow
    @Final
    private Map<GameRules.Key<?>, GameRules.Value<?>> GAME_RULE_TYPES;

    @Inject(
            method = "<init>()V",
            at = @At("RETURN")
    )
    private void reinitializeMap(CallbackInfo ci) {
        this.GAME_RULE_TYPES = new Object2ObjectOpenHashMap<>(this.GAME_RULE_TYPES);
    }

    @Inject(
            method = "<init>(Ljava/util/Map;)V",
            at = @At("RETURN")
    )
    private void reinitializeMap(Map<?, ?> GAME_RULE_TYPES, CallbackInfo ci) {
        this.GAME_RULE_TYPES = new Object2ObjectOpenHashMap<>(this.GAME_RULE_TYPES);
    }
}
