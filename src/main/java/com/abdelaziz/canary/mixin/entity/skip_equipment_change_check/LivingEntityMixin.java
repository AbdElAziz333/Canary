package com.abdelaziz.canary.mixin.entity.skip_equipment_change_check;

import com.abdelaziz.canary.common.entity.EquipmentEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements EquipmentEntity {

    private boolean equipmentChanged = true;

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    public void lithiumOnEquipmentChanged() {
        this.equipmentChanged = true;
    }

    @Inject(
            method = "getEquipmentChanges()Ljava/util/Map;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void skipSentEquipmentComparison(CallbackInfoReturnable<@Nullable Map<EquipmentSlot, ItemStack>> cir) {
        if (!this.equipmentChanged) {
            cir.setReturnValue(null);
        }
    }

    @Inject(
            method = "sendEquipmentChanges()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;checkHandStackSwap(Ljava/util/Map;)V"
            )
    )
    private void resetEquipmentChanged(CallbackInfo ci) {
        //Not implemented for player entities and modded entities, fallback to never skipping inventory comparison
        if (this instanceof EquipmentTrackingEntity) {
            this.equipmentChanged = false;
        }
    }

    @Inject(
            method = "eatFood(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN")
    )
    private void trackEatingEquipmentChange(Level world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        this.lithiumOnEquipmentChanged();
    }
}
