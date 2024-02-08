package com.abdelaziz.canary.mixin.ai.replace_streams.storage;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Mixin(PersistentEntitySectionManager.class)
public abstract class PersistentEntitySectionManagerMixin<T extends EntityAccess> {
    @Shadow @Final
    private Long2ObjectMap<Visibility> chunkVisibility;

    @Shadow @Final
    private LongSet chunksToUnload;

    @Shadow @Final
    EntitySectionStorage<T> sectionStorage;

    @Shadow @Final
    private Long2ObjectMap<PersistentEntitySectionManager.ChunkLoadStatus> chunkLoadStatuses;

    @Shadow @Final
    private Queue<ChunkEntities<T>> loadingInbox;

    @Shadow
    abstract void stopTicking(T entity);

    @Shadow
    abstract void stopTracking(T entity);

    @Shadow
    abstract void startTracking(T entity);

    @Shadow
    abstract void startTicking(T entity);

    @Shadow
    protected abstract boolean storeChunkSections(long chunkPos, Consumer<T> consumer);

    @Shadow
    protected abstract void unloadEntity(EntityAccess entityAccess);

    @Shadow
    protected abstract boolean addEntity(T entityType, boolean callback);

    @Shadow
    protected abstract void ensureChunkQueuedForLoad(long chunkPos);

    /**
     * @reason Avoid streams.
     * @author AbdElAziz
     * */
    @Overwrite
    public void addLegacyChunkEntities(Stream<T> stream) {
        for (T entity : stream.toList()) {
            this.addEntity(entity, true);

            if (entity instanceof Entity entity1) {
                entity1.onAddedToWorld();
            }
        }
    }

    /**
     * @reason Avoid streams.
     * @author AbdElAziz
     * */
    @Overwrite
    public void addWorldGenChunkEntities(Stream<T> stream) {
        for (T entity : stream.toList()) {
            this.addEntity(entity, false);

            if (entity instanceof Entity entity1) {
                entity1.onAddedToWorld();
            }
        }
    }

    /**
     * @reason Avoid streams.
     * @author AbdElAziz
     * */
    @Overwrite
    public void updateChunkStatus(ChunkPos chunkPos, Visibility defaultVisibility) {
        long i = chunkPos.toLong();

        if (defaultVisibility == Visibility.HIDDEN) {
            this.chunkVisibility.remove(i);
            this.chunksToUnload.add(i);
        } else {
            this.chunkVisibility.put(i, defaultVisibility);
            this.chunksToUnload.remove(i);
            this.ensureChunkQueuedForLoad(i);
        }

        List<EntitySection<T>> list = this.sectionStorage.getExistingSectionsInChunk(i).toList();

        for (EntitySection<T> section : list) {
            Visibility visibility = section.updateChunkStatus(defaultVisibility);
            boolean flag = visibility.isAccessible();
            boolean flag1 = defaultVisibility.isAccessible();
            boolean flag2 = visibility.isTicking();
            boolean flag3 = defaultVisibility.isTicking();

            if (flag2 && !flag3) {
                for (T entity : section.getEntities().toList()) {
                    if (!entity.isAlwaysTicking()) {
                        this.stopTicking(entity);
                    }
                }
            }

            if (flag && !flag1) {
                for (T entity : section.getEntities().toList()) {
                    if (!entity.isAlwaysTicking()) {
                        this.stopTracking(entity);
                    }
                }
            } else if (!flag && flag1) {
                for (T entity : section.getEntities().toList()) {
                    if (!entity.isAlwaysTicking()) {
                        this.startTracking(entity);
                    }
                }
            }

            if (!flag2 && flag3) {
                for (T entity : section.getEntities().toList()) {
                    if (!entity.isAlwaysTicking()) {
                        this.startTicking(entity);
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
    private boolean processChunkUnload(long chunkPos) {
        boolean flag = this.storeChunkSections(chunkPos, (type) -> {
            for (EntityAccess entity : type.getPassengersAndSelf().toList()) {
                this.unloadEntity(entity);
            }
        });
        if (!flag) {
            return false;
        } else {
            this.chunkLoadStatuses.remove(chunkPos);
            return true;
        }
    }

    /**
     * @reason Avoid streams.
     * @author AbdElAziz
     * */
    @Overwrite
    private void processPendingLoads() {
        ChunkEntities<T> chunkEntities;
        while((chunkEntities = this.loadingInbox.poll()) != null) {
            for (T entity : chunkEntities.getEntities().toList()) {
                this.addEntity(entity, true);

                if (entity instanceof Entity entity1) {
                    entity1.onAddedToWorld();
                }
            }

            this.chunkLoadStatuses.put(chunkEntities.getPos().toLong(), PersistentEntitySectionManager.ChunkLoadStatus.LOADED);
        }

    }
}
