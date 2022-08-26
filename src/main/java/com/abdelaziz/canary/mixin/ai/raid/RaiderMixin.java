package com.abdelaziz.canary.mixin.ai.raid;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

@Mixin(Raider.class)
public class RaiderMixin {
    // The call to Raider#getLeaderBannerInstance() is very expensive, so cache it and re-use it during AI ticking
    private static final ItemStack CACHED_OMINOUS_BANNER = Raid.getLeaderBannerInstance();

    @Mutable
    @Shadow
    @Final //static final Predicate<ItemEntity> ALLOWED_ITEMS; - OBTAINABLE_OMINOUS_BANNER_PREDICATE;
    static Predicate<ItemEntity> ALLOWED_ITEMS;

    static {
        ALLOWED_ITEMS = (itemEntity) -> !itemEntity.hasPickUpDelay() && itemEntity.isAlive() && ItemStack.matches(itemEntity.getItem(), CACHED_OMINOUS_BANNER);
    }

    @Redirect(
            method = "onDeath(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;getLeaderBannerInstance()Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack getOminousBanner() {
        return CACHED_OMINOUS_BANNER;
    }
}
