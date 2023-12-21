package com.abdelaziz.canary.mixin.util.block_tracking;

import com.abdelaziz.canary.common.block.BlockStateFlagHolder;
import com.abdelaziz.canary.common.block.BlockStateFlags;
import com.abdelaziz.canary.common.block.TrackedBlockStatePredicate;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateBaseMixin implements BlockStateFlagHolder {
    private int flags;

    @Inject(method = "initCache", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        this.initFlags();
    }

    private void initFlags() {
        TrackedBlockStatePredicate.FULLY_INITIALIZED.set(true);

        int flags = 0;

        for (int i = 0; i < BlockStateFlags.FLAGS.length; i++) {
            if (BlockStateFlags.FLAGS[i].test((BlockState) (Object) this)) {
                flags |= 1 << i;
            }
        }

        this.flags = flags;
    }

    @Override
    public int getAllFlags() {
        return this.flags;
    }
}
