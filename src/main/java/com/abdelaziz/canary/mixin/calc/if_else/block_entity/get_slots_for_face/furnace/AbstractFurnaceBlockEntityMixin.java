package com.abdelaziz.canary.mixin.calc.if_else.block_entity.get_slots_for_face.furnace;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AbstractFurnaceBlockEntity.class, priority = 999)
public class AbstractFurnaceBlockEntityMixin {

    @Shadow @Final private static int[] SLOTS_FOR_DOWN;

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
