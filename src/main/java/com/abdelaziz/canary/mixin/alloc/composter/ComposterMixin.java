package com.abdelaziz.canary.mixin.alloc.composter;

import com.abdelaziz.canary.common.util.ArrayConstants;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

public class ComposterMixin {

    //@Mixin(targets = "net.minecraft.block.ComposterBlock$ComposterInventory")
    @Mixin(targets = "net.minecraft.world.level.block.ComposterBlock$InputContainer")
    static abstract class ComposterBlockComposterInventoryMixin implements WorldlyContainer {
        /**
         * @author 2No2Name
         * @reason avoid allocation
         */
        @Overwrite
        public int[] getAvailableSlots(Direction side) {
            return side == Direction.UP ? ArrayConstants.ZERO : ArrayConstants.EMPTY;
        }
    }

    //@Mixin(targets = "net.minecraft.block.ComposterBlock$DummyInventory")
    @Mixin(targets = "net.minecraft.world.level.block.ComposterBlock$EmptyContainer")
    static abstract class ComposterBlockDummyInventoryMixin implements WorldlyContainer {
        /**
         * @author 2No2Name
         * @reason avoid allocation
         */
        @Overwrite
        public int[] getAvailableSlots(Direction side) {
            return ArrayConstants.EMPTY;
        }
    }

    //@Mixin(targets = "net.minecraft.block.ComposterBlock$FullComposterInventory")
    @Mixin(targets = "net.minecraft.world.level.block.ComposterBlock$OutputContainer")
    static abstract class ComposterBlockFullComposterInventoryMixin implements WorldlyContainer {
        /**
         * @author 2No2Name
         * @reason avoid allocation
         */
        @Overwrite
        public int[] getAvailableSlots(Direction side) {
            return side == Direction.DOWN ? ArrayConstants.ZERO : ArrayConstants.EMPTY;
        }
    }
}
