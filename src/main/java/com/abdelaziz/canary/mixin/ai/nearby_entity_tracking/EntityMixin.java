package com.abdelaziz.canary.mixin.ai.nearby_entity_tracking;

import com.abdelaziz.canary.common.entity.nearby_tracker.NearbyEntityListener;
import com.abdelaziz.canary.common.entity.nearby_tracker.NearbyEntityListenerMulti;
import com.abdelaziz.canary.common.entity.nearby_tracker.NearbyEntityListenerProvider;
import com.abdelaziz.canary.common.entity.nearby_tracker.NearbyEntityTracker;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Extends the base living entity class to provide a {@link NearbyEntityListenerMulti} which will handle the
 * child {@link NearbyEntityListenerProvider}s of AI tasks attached to this entity.
 */
@Mixin(Entity.class)
public class EntityMixin implements NearbyEntityListenerProvider {
    private NearbyEntityListenerMulti tracker = null;

    @Override
    @Nullable
    public NearbyEntityListenerMulti getListener() {
        return this.tracker;
    }

    @Override
    public void addListener(NearbyEntityTracker listener) {
        if (this.tracker == null) {
            this.tracker = new NearbyEntityListenerMulti();
        }

        this.tracker.addListener(listener);
    }
}
