package com.abdelaziz.canary.client.world;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.TransientEntitySectionManager;

public interface ClientWorldAccessor {
    TransientEntitySectionManager<Entity> getEntityManager();
}
