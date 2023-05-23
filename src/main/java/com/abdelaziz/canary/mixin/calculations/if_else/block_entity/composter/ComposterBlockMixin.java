package com.abdelaziz.canary.mixin.calculations.if_else.block_entity.composter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ComposterBlock.class)
public class ComposterBlockMixin {
    @Shadow @Final public static IntegerProperty LEVEL;

    /**
     * @reason replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState p_51981_, boolean p_51982_) {
        switch(state.getValue(LEVEL)) {
            case 7 : {
                level.scheduleTick(pos, state.getBlock(), 20);
            }
        }
    }
}
