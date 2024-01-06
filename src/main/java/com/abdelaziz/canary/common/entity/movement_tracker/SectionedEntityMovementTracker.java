package com.abdelaziz.canary.common.entity.movement_tracker;

import com.abdelaziz.canary.common.util.tuples.WorldSectionBox;
import com.abdelaziz.canary.mixin.util.accessors.PersistentEntitySectionManagerAccessor;
import com.abdelaziz.canary.mixin.util.accessors.ServerLevelAccessor;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;

import java.util.ArrayList;

public abstract class SectionedEntityMovementTracker<E extends EntityAccess, S> {
    final WorldSectionBox trackedWorldSections;
    final Class<S> clazz;
    private final int trackedClass;
    ArrayList<EntitySection<E>> sortedSections;
    boolean[] sectionVisible;
    private int timesRegistered;
    private final ArrayList<EntityMovementTrackerSection> sectionsNotListeningTo;

    private long maxChangeTime;

    private ReferenceOpenHashSet<SectionedEntityMovementListener> nearbyEntityMovementListeners;

    public SectionedEntityMovementTracker(WorldSectionBox interactionChunks, Class<S> clazz) {
        this.clazz = clazz;
        this.trackedWorldSections = interactionChunks;
        this.trackedClass = MovementTrackerHelper.MOVEMENT_NOTIFYING_ENTITY_CLASSES.indexOf(clazz);
        assert this.trackedClass != -1;
        this.nearbyEntityMovementListeners = null;
        this.sectionsNotListeningTo = new ArrayList<>();
    }

    @Override
    public int hashCode() {
        return HashCommon.mix(this.trackedWorldSections.hashCode()) ^ HashCommon.mix(this.trackedClass) ^ this.getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass() == this.getClass() &&
                this.clazz == ((SectionedEntityMovementTracker<?, ?>) obj).clazz &&
                this.trackedWorldSections.equals(((SectionedEntityMovementTracker<?, ?>) obj).trackedWorldSections);
    }

    /**
     * Method to quickly check whether any relevant entities moved inside the relevant entity sections after
     * the last interaction attempt.
     *
     * @param lastCheckedTime time of the last interaction attempt
     * @return whether any relevant entity moved in the tracked area
     */
    public boolean isUnchangedSince(long lastCheckedTime) {
        if (lastCheckedTime <= this.maxChangeTime) {
            return false;
        }
        if (!this.sectionsNotListeningTo.isEmpty()) {
            this.setChanged(this.listenToAllSectionsAndGetMaxChangeTime());
            return lastCheckedTime > this.maxChangeTime;
        }
        return true;
    }

    private long listenToAllSectionsAndGetMaxChangeTime() {
        long maxChangeTime = Long.MIN_VALUE;
        ArrayList<EntityMovementTrackerSection> notListeningTo = this.sectionsNotListeningTo;
        for (int i = notListeningTo.size() - 1; i >= 0; i--) {
            EntityMovementTrackerSection entityTrackerSection = notListeningTo.remove(i);
            entityTrackerSection.listenToMovementOnce(this, this.trackedClass);
            maxChangeTime = Math.max(maxChangeTime, entityTrackerSection.getChangeTime(this.trackedClass));
        }
        return maxChangeTime;
    }

    public void register(ServerLevel world) {
        assert world == this.trackedWorldSections.world();

        if (this.timesRegistered == 0) {
            //noinspection unchecked
            EntitySectionStorage<E> cache = ((PersistentEntitySectionManagerAccessor<E>) ((ServerLevelAccessor) world).getEntityManager()).getSectionStorage();

            WorldSectionBox trackedSections = this.trackedWorldSections;
            int size = trackedSections.numSections();
            assert size > 0;
            this.sortedSections = new ArrayList<>(size);
            this.sectionVisible = new boolean[size];

            //vanilla iteration order in SectionedEntityCache is xzy
            //WorldSectionBox upper coordinates are exclusive
            for (int x = trackedSections.chunkX1(); x < trackedSections.chunkX2(); x++) {
                for (int z = trackedSections.chunkZ1(); z < trackedSections.chunkZ2(); z++) {
                    for (int y = trackedSections.chunkY1(); y < trackedSections.chunkY2(); y++) {
                        EntitySection<E> section = cache.getOrCreateSection(SectionPos.asLong(x, y, z));
                        EntityMovementTrackerSection sectionAccess = (EntityMovementTrackerSection) section;
                        this.sortedSections.add(section);
                        sectionAccess.addListener(this);
                    }
                }
            }
            this.setChanged(world.getGameTime());
        }

        this.timesRegistered++;
    }

    public void unRegister(ServerLevel world) {
        assert world == this.trackedWorldSections.world();
        if (--this.timesRegistered > 0) {
            return;
        }
        assert this.timesRegistered == 0;
        //noinspection unchecked
        EntitySectionStorage<E> cache = ((PersistentEntitySectionManagerAccessor<E>) ((ServerLevelAccessor) world).getEntityManager()).getSectionStorage();
        MovementTrackerCache storage = (MovementTrackerCache) cache;
        storage.remove(this);

        ArrayList<EntitySection<E>> sections = this.sortedSections;
        for (int i = sections.size() - 1; i >= 0; i--) {
            EntitySection<E> section = sections.get(i);
            EntityMovementTrackerSection sectionAccess = (EntityMovementTrackerSection) section;
            sectionAccess.removeListener(cache, this);
            if (!this.sectionsNotListeningTo.remove(section)) {
                ((EntityMovementTrackerSection) section).removeListenToMovementOnce(this, this.trackedClass);
            }
        }
        this.setChanged(world.getGameTime());
    }

    /**
     * Register an entity section to this listener, so this listener can look for changes in the section.
     */
    public void onSectionEnteredRange(EntityMovementTrackerSection section) {
        this.setChanged(this.trackedWorldSections.world().getGameTime());
        //noinspection SuspiciousMethodCalls
        int sectionIndex = this.sortedSections.lastIndexOf(section);
        this.sectionVisible[sectionIndex] = true;

        this.sectionsNotListeningTo.add(section);
    }

    public void onSectionLeftRange(EntityMovementTrackerSection section) {
        this.setChanged(this.trackedWorldSections.world().getGameTime());
        //noinspection SuspiciousMethodCalls
        int sectionIndex = this.sortedSections.lastIndexOf(section);

        this.sectionVisible[sectionIndex] = false;

        if (!this.sectionsNotListeningTo.remove(section)) {
            section.removeListenToMovementOnce(this, this.trackedClass);
        }
    }

    /**
     * Method that marks that new entities might have appeared or moved in the tracked chunk sections.
     */
    private void setChanged(long atTime) {
        if (atTime > this.maxChangeTime) {
            this.maxChangeTime = atTime;
        }
    }

    public void listenToEntityMovementOnce(SectionedEntityMovementListener listener) {
        if (this.nearbyEntityMovementListeners == null) {
            this.nearbyEntityMovementListeners = new ReferenceOpenHashSet<>();
        }
        this.nearbyEntityMovementListeners.add(listener);

        if (!this.sectionsNotListeningTo.isEmpty()) {
            this.setChanged(this.listenToAllSectionsAndGetMaxChangeTime());
        }

    }

    public void emitEntityMovement(int classMask, EntityMovementTrackerSection section) {
        if ((classMask & (1 << this.trackedClass)) != 0) {
            ReferenceOpenHashSet<SectionedEntityMovementListener> listeners = this.nearbyEntityMovementListeners;
            if (listeners != null) {
                for (SectionedEntityMovementListener listener : listeners) {
                    listener.handleEntityMovement(this.clazz);
                }
                listeners.clear();
            }
            this.sectionsNotListeningTo.add(section);
        }
    }
}
