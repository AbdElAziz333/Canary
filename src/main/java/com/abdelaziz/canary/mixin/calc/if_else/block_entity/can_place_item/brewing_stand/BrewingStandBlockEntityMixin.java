package com.abdelaziz.canary.mixin.calc.if_else.block_entity.can_place_item.brewing_stand;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {
    @Shadow public abstract ItemStack getItem(int p_58985_);

    /**
     * @reason replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public boolean canPlaceItem(int i1, ItemStack itemStack) {
        switch (i1) {
            case 3 -> {
                return net.minecraftforge.common.brewing.BrewingRecipeRegistry.isValidIngredient(itemStack);
            }
            case 4 -> {
                return itemStack.is(Items.BLAZE_POWDER);
            }
            default -> {
                return net.minecraftforge.common.brewing.BrewingRecipeRegistry.isValidInput(itemStack) && this.getItem(i1).isEmpty();
            }
        }
    }
}
