package com.abdelaziz.canary.mixin.util.inventory_change_listening;

import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeEmitter;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin extends LootableContainerBlockEntity implements InventoryChangeEmitter {
    protected ChestBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setCachedState(BlockState state) {
        super.setCachedState(state);
        //Handle switching double / single chest state
        this.emitRemoved();
    }
}
