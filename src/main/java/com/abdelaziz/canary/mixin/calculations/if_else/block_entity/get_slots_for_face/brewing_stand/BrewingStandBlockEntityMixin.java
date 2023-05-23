package com.abdelaziz.canary.mixin.calculations.if_else.block_entity.get_slots_for_face.brewing_stand;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BrewingStandBlockEntity.class)
public class BrewingStandBlockEntityMixin {

    @Shadow
    @Final
    private static int[] SLOTS_FOR_DOWN;

    @Shadow @Final private static int[] SLOTS_FOR_UP;

    @Shadow @Final private static int[] SLOTS_FOR_SIDES;

    /**
     * @reason replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public int[] getSlotsForFace(Direction direction) {
        switch(direction) {
            case DOWN -> {
                return SLOTS_FOR_DOWN;
            }
            case UP -> {
                return SLOTS_FOR_UP;
            }
            default -> {
                return SLOTS_FOR_SIDES;
            }
        }
    }
}
