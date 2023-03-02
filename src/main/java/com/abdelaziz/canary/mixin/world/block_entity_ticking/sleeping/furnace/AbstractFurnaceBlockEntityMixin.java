package com.abdelaziz.canary.mixin.world.block_entity_ticking.sleeping.furnace;

import com.abdelaziz.canary.common.block.entity.SleepingBlockEntity;
import com.abdelaziz.canary.mixin.world.block_entity_ticking.sleeping.RebindableTickingBlockEntityWrapperAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin extends BlockEntity implements SleepingBlockEntity {

    @Shadow
    protected abstract boolean isLit();

    @Shadow
    int cookingProgress;
    private RebindableTickingBlockEntityWrapperAccessor tickWrapper = null;
    private TickingBlockEntity sleepingTicker = null;

    public AbstractFurnaceBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public RebindableTickingBlockEntityWrapperAccessor getTickWrapper() {
        return tickWrapper;
    }

    @Override
    public void setTickWrapper(RebindableTickingBlockEntityWrapperAccessor tickWrapper) {
        this.tickWrapper = tickWrapper;
        this.setSleepingTicker(null);
    }

    @Override
    public TickingBlockEntity getSleepingTicker() {
        return sleepingTicker;
    }

    @Override
    public void setSleepingTicker(TickingBlockEntity sleepingTicker) {
        this.sleepingTicker = sleepingTicker;
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void checkSleep(Level world, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        ((AbstractFurnaceBlockEntityMixin) (Object) blockEntity).checkSleep(state);
    }

    private void checkSleep(BlockState state) {
        if (!this.isLit() && this.cookingProgress == 0 && (state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER)) && this.level != null) {
            this.startSleeping();
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void wakeUpAfterFromTag(CallbackInfo ci) {
        if (this.isSleeping() && this.level != null && !this.level.isClientSide) {
            this.wakeUpNow();
        }
    }

    @Intrinsic
    @Override
    public void setChanged() {
        super.setChanged();
    }

    @SuppressWarnings({"MixinAnnotationTarget", "UnresolvedMixinReference"})
    @Inject(method = "setChanged()V", at = @At("RETURN"))
    private void wakeOnMarkDirty(CallbackInfo ci) {
        if (this.isSleeping() && this.level != null && !this.level.isClientSide) {
            this.wakeUpNow();
        }
    }
}
