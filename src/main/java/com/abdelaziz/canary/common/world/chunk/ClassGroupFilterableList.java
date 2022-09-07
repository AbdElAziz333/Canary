package com.abdelaziz.canary.common.world.chunk;

import com.abdelaziz.canary.common.entity.EntityClassGroup;

import java.util.Collection;

public interface ClassGroupFilterableList<T> {
    Collection<T> getAllOfGroupType(EntityClassGroup type);

}