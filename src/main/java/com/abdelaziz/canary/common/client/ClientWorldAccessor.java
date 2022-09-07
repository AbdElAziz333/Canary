package com.abdelaziz.canary.common.client;

import net.minecraft.client.world.ClientEntityManager;
import net.minecraft.entity.Entity;

public interface ClientWorldAccessor {
    ClientEntityManager<Entity> getEntityManager();
}
