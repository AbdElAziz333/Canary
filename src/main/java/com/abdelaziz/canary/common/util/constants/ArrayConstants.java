package com.abdelaziz.canary.common.util.constants;

import java.util.*;

public interface ArrayConstants {
    List EMPTY_LIST = Collections.emptyList();
    Iterator EMPTY_ITERATOR = Collections.emptyIterator();
    List IMMUTABLE_EMPTY_LIST = List.of();

    int[] EMPTY_ARRAY = new int[0];
    int[] ZERO_ARRAY = new int[]{0};
}
