package com.abdelaziz.canary.mixin.block.flatten_states;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This patch safely avoids excessive overhead in some hot methods by caching some constant values in the BlockState
 * itself, excluding dynamic dispatch and the pointer dereferences.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Shadow
    protected abstract BlockState asState();

    @Shadow
    public abstract Block getBlock();

    /**
     * The fluid state is constant for any given block state, so it can be safely cached. This notably improves performance
     * when scanning for fluid blocks.
     */
    private FluidState fluidStateCache = null;

    /**
     * Indicates whether the current block state can be ticked. Since this value is always the same for any given block state
     * and random block ticking is a frequent process during chunk ticking, in theory this is a very good change.
     */
    private boolean isTickable;

    /**
     * We can't use the ctor as a BlockState will be constructed *before* a Block has fully initialized.
     */
    @Inject(method = "initCache()V", at = @At("HEAD"))
    private void init(CallbackInfo ci) {
        //noinspection deprecation
        this.fluidStateCache = this.getBlock().getFluidState(this.asState());
        this.isTickable = this.getBlock().isRandomlyTicking(this.asState());
    }

    /**
     * @reason Use cached property
     * @author JellySquid
     */
    @Overwrite
    public FluidState getFluidState() {
        if (this.fluidStateCache == null) {
            //noinspection deprecation
            this.fluidStateCache = this.getBlock().getFluidState(this.asState());
        }

        return this.fluidStateCache;
    }

    /**
     * @reason Use cached property
     * @author Maity
     */
    @Overwrite
    public boolean isRandomlyTicking() {
        return this.isTickable;
    }
}
