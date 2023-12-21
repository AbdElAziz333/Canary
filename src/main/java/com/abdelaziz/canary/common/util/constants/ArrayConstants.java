package com.abdelaziz.canary.common.util.constants;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Pre-initialized constants to avoid unnecessary allocations.
 */
public interface ArrayConstants {
    List EMPTY_LIST = Collections.emptyList();
    Iterator EMPTY_ITERATOR = Collections.emptyIterator();
    List IMMUTABLE_EMPTY_LIST = List.of();

    int[] EMPTY_ARRAY = new int[0];
    int[] ZERO_ARRAY = new int[]{0};
}

