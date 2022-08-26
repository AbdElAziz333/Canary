package com.abdelaziz.canary.mixin.alloc.blockstate;

import com.google.common.collect.Table;
import com.abdelaziz.canary.common.state.FastImmutableTable;
import com.abdelaziz.canary.common.state.StatePropertyTableCache;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(StateHolder.class)
public class StateHolderMixin<O, S> {
    @Shadow
    private Table<Property<?>, Comparable<?>, S> neighbours;

    @Shadow
    @Final
    protected O owner;

    @Inject(method = "populateNeighbours", at = @At("RETURN"))
    private void populateNeighbours(Map<Map<Property<?>, Comparable<?>>, S> states, CallbackInfo ci) {
        this.neighbours = new FastImmutableTable<>(this.neighbours, StatePropertyTableCache.getTableCache(this.owner));
    }

}
