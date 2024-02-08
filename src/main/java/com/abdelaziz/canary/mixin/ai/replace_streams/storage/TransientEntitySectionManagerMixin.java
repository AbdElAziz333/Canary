package com.abdelaziz.canary.mixin.ai.replace_streams.storage;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TransientEntitySectionManager.class)
public class TransientEntitySectionManagerMixin<T extends EntityAccess> {
    @Shadow @Final
    private LongSet tickingChunks;

    @Shadow @Final
    EntitySectionStorage<T> sectionStorage;

    @Shadow @Final
    LevelCallback<T> callbacks;

    /**
     * @reason Avoid streams.
     * @author AbdElAziz
     * */
    @Overwrite
    public void startTicking(ChunkPos chunkPos) {
        long i = chunkPos.toLong();
        this.tickingChunks.add(i);

        for (EntitySection<T> section : this.sectionStorage.getExistingSectionsInChunk(i).toList()) {
            Visibility visibility = section.updateChunkStatus(Visibility.TICKING);

            if (!visibility.isTicking()) {
                for (T entity : section.getEntities().toList()) {
                    if (!entity.isAlwaysTicking()) {
                        this.callbacks.onTickingStart(entity);
                    }
                }
            }
        }
    }

    /**
     * @reason Avoid streams.
     * @author AbdElAziz
     * */
    @Overwrite
    public void stopTicking(ChunkPos chunkPos) {
        long i = chunkPos.toLong();
        this.tickingChunks.remove(i);

        for (EntitySection<T> section : this.sectionStorage.getExistingSectionsInChunk(i).toList()) {
            Visibility visibility = section.updateChunkStatus(Visibility.TRACKED);

            if (visibility.isTicking()) {
                for (T entity : section.getEntities().toList()) {
                    if (!entity.isAlwaysTicking()) {
                        this.callbacks.onTickingEnd(entity);
                    }
                }
            }
        }
    }
}