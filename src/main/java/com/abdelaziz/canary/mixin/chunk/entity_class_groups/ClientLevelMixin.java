package com.abdelaziz.canary.mixin.chunk.entity_class_groups;

import com.abdelaziz.canary.client.world.ClientWorldAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientLevel.class)
public class ClientLevelMixin implements ClientWorldAccessor {
    @Shadow
    @Final
    private TransientEntitySectionManager<Entity> entityStorage;

    @Override
    public TransientEntitySectionManager<Entity> getEntityManager() {
        return this.entityStorage;
    }
}

