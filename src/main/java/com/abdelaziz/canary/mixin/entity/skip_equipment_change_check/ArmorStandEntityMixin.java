package com.abdelaziz.canary.mixin.entity.skip_equipment_change_check;

import com.abdelaziz.canary.common.entity.EquipmentEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorStand.class)
public class ArmorStandEntityMixin implements EquipmentEntity.EquipmentTrackingEntity, EquipmentEntity {
    @Inject(
            method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("RETURN")
    )
    private void trackEquipChange(CompoundTag nbt, CallbackInfo ci) {
        this.canaryOnEquipmentChanged();
    }

    @Inject(
            method = "setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("RETURN")
    )
    private void trackEquipChange(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        this.canaryOnEquipmentChanged();
    }

    @Inject(
            method = "brokenByAnything(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At("RETURN")
    )
    private void trackEquipChange(DamageSource damageSource, CallbackInfo ci) {
        this.canaryOnEquipmentChanged();
    }
}
