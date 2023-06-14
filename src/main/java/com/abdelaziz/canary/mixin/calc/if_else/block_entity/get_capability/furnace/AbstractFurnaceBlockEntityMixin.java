package com.abdelaziz.canary.mixin.calc.if_else.block_entity.get_capability.furnace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(value = AbstractFurnaceBlockEntity.class, priority = 999)
public abstract class AbstractFurnaceBlockEntityMixin extends BaseContainerBlockEntity {

    @Shadow
    LazyOptional<? extends IItemHandler>[] handlers;

    protected AbstractFurnaceBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
        super(blockEntityType, pos, state);
    }

    /**
     * @reason replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite (remap = false)
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing) {
        if (!this.remove && facing != null && capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            switch(facing) {
                case UP -> {
                    return handlers[0].cast();
                }
                case DOWN -> {
                    return handlers[1].cast();
                }
                default -> {
                    return handlers[2].cast();
                }
            }
        }
        return super.getCapability(capability, facing);
    }
}
