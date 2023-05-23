package com.abdelaziz.canary.mixin.alloc.empty_iterator;

import com.abdelaziz.canary.common.util.constants.ArrayConstants;
import com.google.common.collect.Iterators;
import net.minecraft.util.ClassInstanceMultiMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;

@Mixin(ClassInstanceMultiMap.class)
public abstract class ClassInstanceMultiMapMixin<T> extends AbstractCollection<T> {

    @Shadow @Final private List<T> allInstances;

    /**
     * @reason avoid allocations
     * @author AbdElAziz
     * */
    @Overwrite
    public Iterator<T> iterator() {
        return (this.allInstances.isEmpty() ? ArrayConstants.EMPTY_ITERATOR
                : Iterators.unmodifiableIterator(this.allInstances.iterator()));
    }
}
