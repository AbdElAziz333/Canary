package com.abdelaziz.canary.mixin.alloc.blockstate;

import com.google.common.collect.Table;
import com.abdelaziz.canary.common.state.FastImmutableTable;
import com.abdelaziz.canary.common.state.StatePropertyTableCache;
import net.minecraft.state.State;
import net.minecraft.state.property.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(State.class)
public class StateMixin<O, S> {
    @Shadow
    private Table<Property<?>, Comparable<?>, S> withTable;

    @Shadow
    @Final
    protected O owner;

    @Inject(method = "createWithTable", at = @At("RETURN"))
    private void postCreateWithTable(Map<Map<Property<?>, Comparable<?>>, S> states, CallbackInfo ci) {
        this.withTable = new FastImmutableTable<>(this.withTable, StatePropertyTableCache.getTableCache(this.owner));
    }

}
