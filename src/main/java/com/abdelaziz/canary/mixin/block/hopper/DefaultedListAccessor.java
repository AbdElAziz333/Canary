package com.abdelaziz.canary.mixin.block.hopper;

import net.minecraft.core.NonNullList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(NonNullList.class)
public interface DefaultedListAccessor<T> {
    @Accessor("delegate")
    List<T> getDelegate();
}
