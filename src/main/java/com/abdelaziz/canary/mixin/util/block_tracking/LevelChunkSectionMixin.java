package com.abdelaziz.canary.mixin.util.block_tracking;

import com.abdelaziz.canary.common.block.BlockCountingSection;
import com.abdelaziz.canary.common.block.BlockStateFlagHolder;
import com.abdelaziz.canary.common.block.BlockStateFlags;
import com.abdelaziz.canary.common.block.TrackedBlockStatePredicate;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Keep track of how many blocks that meet certain criteria are in this chunk section.
 * E.g. if no over-sized blocks are there, collision code can skip a few blocks.
 *
 * @author 2No2Name
 */
@Mixin(LevelChunkSection.class)
public abstract class LevelChunkSectionMixin implements BlockCountingSection {

    @Shadow
    @Final
    private PalettedContainer<BlockState> states;
    @Unique
    private short[] countsByFlag = null;
    private CompletableFuture<short[]> countsByFlagFuture;

    @Override
    public boolean anyMatch(TrackedBlockStatePredicate trackedBlockStatePredicate, boolean fallback) {
        if (this.countsByFlag == null) {
            if (!tryInitializeCountsByFlag()) {
                return fallback;
            }
        }
        return this.countsByFlag[trackedBlockStatePredicate.getIndex()] != (short) 0;
    }

    private static short[] calculateCanaryCounts(PalettedContainer<BlockState> states) {
        short[] countsByFlag = new short[BlockStateFlags.NUM_FLAGS];
        states.count((BlockState state, int count) -> addToFlagCount(countsByFlag, state, count));
        return countsByFlag;
    }

    /**
     * Compute the block state counts using a future using a thread pool to avoid lagging the rendering thread.
     * Before modifying the block data, we join the future or discard it.
     *
     * @return Whether the block counts short array is initialized.
     */
    private boolean tryInitializeCountsByFlag() {
        Future<short[]> countsByFlagFuture = this.countsByFlagFuture;
        if (countsByFlagFuture != null && countsByFlagFuture.isDone()) {
            try {
                this.countsByFlag = countsByFlagFuture.get();
                return true;
            } catch (InterruptedException | ExecutionException | CancellationException e) {
                this.countsByFlagFuture = null;
            }
        }

        if (this.countsByFlagFuture == null) {
            PalettedContainer<BlockState> states = this.states;
            this.countsByFlagFuture = CompletableFuture.supplyAsync(() -> calculateCanaryCounts(states));
        }
        return false;
    }

    @Redirect(
            method = "recalcBlockCounts()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/PalettedContainer;count(Lnet/minecraft/world/level/chunk/PalettedContainer$CountConsumer;)V"
            )
    )
    private void initFlagCounters(PalettedContainer<BlockState> palettedContainer, PalettedContainer.CountConsumer<BlockState> consumer) {
        palettedContainer.count((state, count) -> {
            consumer.accept(state, count);
            addToFlagCount(this.countsByFlag, state, count);
        });
    }

    private static void addToFlagCount(short[] countsByFlag, BlockState state, int change) {
        int flags = ((BlockStateFlagHolder) state).getAllFlags();
        int i;
        while ((i = Integer.numberOfTrailingZeros(flags)) < 32) {
            //either count up by one (prevFlag not set) or down by one (prevFlag set)
            countsByFlag[i] += change;
            flags &= ~(1 << i);
        }
    }

    @Inject(method = "recalcBlockCounts()V", at = @At("HEAD"))
    private void createFlagCounters(CallbackInfo ci) {
        this.countsByFlag = new short[BlockStateFlags.NUM_FLAGS];
    }

    @Inject(
            method = "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(value = "HEAD")
    )
    private void joinFuture(int x, int y, int z, BlockState state, boolean lock, CallbackInfoReturnable<BlockState> cir) {
        if (this.countsByFlagFuture != null) {
            this.countsByFlag = this.countsByFlagFuture.join();
            this.countsByFlagFuture = null;
        }
    }

    @Inject(
            method = "read",
            at = @At(value = "HEAD")
    )
    private void resetData(FriendlyByteBuf buf, CallbackInfo ci) {
        this.countsByFlag = null;
        this.countsByFlagFuture = null;
    }


    @Inject(
            method = "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void updateFlagCounters(int x, int y, int z, BlockState newState, boolean lock, CallbackInfoReturnable<BlockState> cir, BlockState oldState) {
        short[] countsByFlag = this.countsByFlag;
        if (countsByFlag == null) {
            return;
        }
        int prevFlags = ((BlockStateFlagHolder) oldState).getAllFlags();
        int flags = ((BlockStateFlagHolder) newState).getAllFlags();

        //no need to update indices that did not change
        int flagsXOR = prevFlags ^ flags;
        int i;
        while ((i = Integer.numberOfTrailingZeros(flagsXOR)) < 32) {
            //either count up by one (prevFlag not set) or down by one (prevFlag set)
            countsByFlag[i] += 1 - (((prevFlags >>> i) & 1) << 1);
            flagsXOR &= ~(1 << i);
        }
    }
}
