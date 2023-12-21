package com.abdelaziz.canary.mixin.world.block_entity_ticking.sleeping.campfire;

import com.abdelaziz.canary.common.block.entity.SleepingBlockEntity;
import com.abdelaziz.canary.mixin.world.block_entity_ticking.sleeping.RebindableTickingBlockEntityWrapperAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CampfireBlockEntity.class)
public class CampfireBlockEntityMixin extends BlockEntity implements SleepingBlockEntity {

    private RebindableTickingBlockEntityWrapperAccessor tickWrapper = null;
    private TickingBlockEntity sleepingTicker = null;

    public CampfireBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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


    @Inject(
            method = "placeFood",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;")
    )
    private void wakeUpOnAddItem(Entity p_238285_, ItemStack p_238286_, int p_238287_, CallbackInfoReturnable<Boolean> cir) {
        this.wakeUpNow();
    }

    @Inject(
            method = "load",
            at = @At(value = "RETURN")
    )
    private void wakeUpOnReadNbt(CompoundTag nbt, CallbackInfo ci) {
        this.wakeUpNow();
    }

    @Inject(
            method = "cooldownTick",
            at = @At("RETURN"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void trySleepUnlit(Level world, BlockPos pos, BlockState state, CampfireBlockEntity campfire, CallbackInfo ci, boolean hadProgress) {
        if (!hadProgress) {
            CampfireBlockEntityMixin self = (CampfireBlockEntityMixin) (Object) campfire;
            self.startSleeping();
        }
    }

    @Inject(
            method = "cookTick",
            at = @At("RETURN" ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void trySleepLit(Level world, BlockPos pos, BlockState state, CampfireBlockEntity campfire, CallbackInfo ci, boolean hadProgress) {
        if (!hadProgress) {
            CampfireBlockEntityMixin self = (CampfireBlockEntityMixin) (Object) campfire;
            self.startSleeping();
        }
    }
}
