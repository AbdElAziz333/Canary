package com.abdelaziz.canary.mixin.entity.skip_equipment_change_check;

import com.abdelaziz.canary.common.entity.EquipmentEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public class MobEntityMixin implements EquipmentEntity.EquipmentTrackingEntity, EquipmentEntity {
    @Inject(
            method = "readCustomDataFromNbt(Lnet/minecraft/nbt/NbtCompound;)V",
            at = @At("RETURN")
    )
    private void trackEquipChange(NbtCompound nbt, CallbackInfo ci) {
        this.canaryOnEquipmentChanged();
    }

    @Inject(
            method = "equipStack(Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/item/ItemStack;)V",
            at = @At("RETURN")
    )
    private void trackEquipChange(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        this.canaryOnEquipmentChanged();
    }
}
