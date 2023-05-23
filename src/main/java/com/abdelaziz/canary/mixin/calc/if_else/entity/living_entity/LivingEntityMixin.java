package com.abdelaziz.canary.mixin.calc.if_else.entity.living_entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow public abstract ItemStack getItemBySlot(EquipmentSlot p_21127_);

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * @reason replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public ItemStack getItemInHand(InteractionHand hand) {
        switch(hand) {
            case MAIN_HAND -> {
                return this.getItemBySlot(EquipmentSlot.MAINHAND);
            }
            case OFF_HAND -> {
                return this.getItemBySlot(EquipmentSlot.OFFHAND);
            }
            default -> throw new IllegalArgumentException("Invalid hand " + hand);
        }
    }
}