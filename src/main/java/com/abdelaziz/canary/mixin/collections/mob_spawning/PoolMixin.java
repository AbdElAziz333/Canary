package com.abdelaziz.canary.mixin.collections.mob_spawning;

import com.google.common.collect.ImmutableList;
import com.abdelaziz.canary.common.util.collections.HashedReferenceList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

@Mixin(WeightedRandomList.class)
public class PoolMixin<E extends WeightedEntry> {

    @Mutable
    @Shadow
    @Final
    private ImmutableList<E> items;
    //Need a separate variable due to items being type ImmutableList
    private List<E> entryHashList;

    @Inject(method = "<init>(Ljava/util/List;)V", at = @At("RETURN"))
    private void init(List<? extends E> items, CallbackInfo ci) {
        //We are using reference equality here, because all vanilla implementations of Weighted use reference equality
        this.entryHashList = this.items.size() > 4 ? this.items : Collections.unmodifiableList(new HashedReferenceList<>(this.items));
    }

    /**
     * @author 2No2Name
     * @reason return a collection with a faster contains() call
     */
    @Overwrite
    public List<E> unwrap() {
        return this.entryHashList;
    }
}
