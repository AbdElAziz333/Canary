package com.abdelaziz.canary.common.world.interests;

import java.util.BitSet;
import java.util.stream.IntStream;

public class RegionBasedStorageColumn {
    public static final int SECTIONS_IN_CHUNK = 16;
    // Sections in this column mapping to a non-empty non-null optional
    private final BitSet nonEmptySections = new BitSet(SECTIONS_IN_CHUNK);
    // Sections in this column present in the map (empty or non-empty optional)
    private final BitSet sectionsInMap = new BitSet(SECTIONS_IN_CHUNK);

    public boolean noSectionsPresent() {
        return this.nonEmptySections.isEmpty();
    }

    public boolean clear(int section) {
        this.nonEmptySections.clear(section);
        this.sectionsInMap.clear(section);
        return this.sectionsInMap.isEmpty();
    }

    public void set(int section, boolean present) {
        this.nonEmptySections.set(section, present);
        this.sectionsInMap.set(section);
    }

    public IntStream nonEmptySections() {
        return this.nonEmptySections.stream();
    }

    public int nextNonEmptySection(int last) {
        return this.nonEmptySections.nextSetBit(last);
    }
}