package com.abdelaziz.canary.mixin.ai.raid;

import com.abdelaziz.canary.common.util.constants.RaidConstants;
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
    @Mutable
    @Shadow
    @Final
    static Predicate<ItemEntity> ALLOWED_ITEMS;

    static {
        ALLOWED_ITEMS = (itemEntity) -> !itemEntity.hasPickUpDelay() && itemEntity.isAlive() && ItemStack.matches(itemEntity.getItem(), RaidConstants.CACHED_OMINOUS_BANNER);
    }

    @Redirect(
            method = {
                    "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
                    "pickUpItem"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;getLeaderBannerInstance()Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack getOminousBanner() {
        return RaidConstants.CACHED_OMINOUS_BANNER;
    }
}
