package com.abdelaziz.canary.common.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.TransientEntitySectionManager;

public interface ClientWorldAccessor {
    TransientEntitySectionManager<Entity> getEntityManager();
}
