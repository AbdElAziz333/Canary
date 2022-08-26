package com.abdelaziz.canary.mixin.block.hopper;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompoundContainer.class)
public interface DoubleInventoryAccessor {

    @Accessor("first")
    Container getFirst();

    @Accessor("second")
    Container getSecond();
}
