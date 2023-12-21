package com.abdelaziz.canary.mixin.block.hopper;

import com.abdelaziz.canary.api.inventory.CanaryInventory;
import com.abdelaziz.canary.common.hopper.RemovalCounter;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CompoundContainer.class)
public abstract class CompoundContainerMixin implements RemovalCounter {
    @Shadow
    @Final
    private Container container1;

    @Shadow
    @Final
    private Container container2;

    @Override
    public int getRemovedCountCanary() {
        return ((CanaryInventory) this.container1).getRemovedCountCanary() +
                ((CanaryInventory) this.container2).getRemovedCountCanary();
    }
}