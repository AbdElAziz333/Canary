package com.abdelaziz.canary.common.util.collections;

import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import java.util.Collection;
import java.util.Set;

public class SetFactory {
    public static <T> Set<T> createFastRefBasedCopy(Collection<T> in) {
        // This threshold is not based on anything but gut feeling, if anyone has actual profiling data it should be
        // adjusted
        if (in.size() < 5) {
            return new ReferenceArraySet<>(in);
        } else {
            return new ReferenceOpenHashSet<>(in);
        }
    }
}