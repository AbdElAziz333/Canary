package com.abdelaziz.canary.mixin.calc.if_else.player_slot;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static net.minecraft.world.entity.EquipmentSlot.Type.ARMOR;

@Mixin(value = Player.class, priority = 999)
public abstract class PlayerMixin extends LivingEntity {

    @Shadow public abstract boolean isSwimming();

    @Shadow @Final private Inventory inventory;

    protected PlayerMixin(EntityType<? extends LivingEntity> livingEntity, Level level) {
        super(livingEntity, level);
    }

    /**
     * @reason replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public ItemStack getItemBySlot(EquipmentSlot equipmentSlot) {
        switch(equipmentSlot) {
            case MAINHAND: {
                return this.inventory.getSelected();
            }
            case OFFHAND: {
                return this.inventory.offhand.get(0);
            }
            default: {
                return equipmentSlot.getType() == ARMOR ? this.inventory.armor.get(equipmentSlot.getIndex()) : ItemStack.EMPTY;
            }
        }
    }

    /**
     * @reason Replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public void setItemSlot(EquipmentSlot equipmentSlot, ItemStack itemStack) {
        this.verifyEquippedItem(itemStack);

        switch(equipmentSlot) {
            case MAINHAND: {
                this.onEquipItem(equipmentSlot, this.inventory.items.set(this.inventory.selected, itemStack), itemStack);
                break;
            }
            case OFFHAND : {
                this.onEquipItem(equipmentSlot, this.inventory.offhand.set(0, itemStack), itemStack);
                break;
            }
            default : {
                this.onEquipItem(equipmentSlot, this.inventory.armor.set(equipmentSlot.getIndex(), itemStack), itemStack);
                break;
            }
        }
    }
}
