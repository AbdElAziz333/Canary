package com.abdelaziz.canary.mixin.alloc.composter;

import com.abdelaziz.canary.common.util.constants.ArrayConstants;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

public class ComposterMixin {

    @Mixin(targets = "net.minecraft.world.level.block.ComposterBlock$InputContainer")
    static abstract class ComposterBlockInputContainerMixin implements WorldlyContainer {
        /**
         * @author 2No2Name
         * @reason avoid allocation
         */
        @Overwrite
        public int[] getSlotsForFace(Direction side) {
            return side == Direction.UP ? ArrayConstants.ZERO_ARRAY : ArrayConstants.EMPTY_ARRAY;
        }
    }

    @Mixin(targets = "net.minecraft.world.level.block.ComposterBlock$EmptyContainer")
    static abstract class ComposterBlockEmptyContainerMixin implements WorldlyContainer {
        /**
         * @author 2No2Name
         * @reason avoid allocation
         */
        @Overwrite
        public int[] getSlotsForFace(Direction side) {
            return ArrayConstants.EMPTY_ARRAY;
        }
    }

    @Mixin(targets = "net.minecraft.world.level.block.ComposterBlock$OutputContainer")
    static abstract class ComposterBlockOutputContainerMixin implements WorldlyContainer {
        /**
         * @author 2No2Name
         * @reason avoid allocation
         */
        @Overwrite
        public int[] getSlotsForFace(Direction side) {
            return side == Direction.DOWN ? ArrayConstants.ZERO_ARRAY : ArrayConstants.EMPTY_ARRAY;
        }
    }
}