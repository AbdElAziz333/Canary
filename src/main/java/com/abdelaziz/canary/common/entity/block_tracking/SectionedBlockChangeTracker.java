package com.abdelaziz.canary.common.entity.block_tracking;

import com.abdelaziz.canary.common.block.BlockListeningSection;
import com.abdelaziz.canary.common.block.ListeningBlockStatePredicate;
import com.abdelaziz.canary.common.util.Pos;
import com.abdelaziz.canary.common.util.deduplication.CanaryInternerWrapper;
import com.abdelaziz.canary.common.util.tuples.WorldSectionBox;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Objects;

public class SectionedBlockChangeTracker {
    public final WorldSectionBox trackedWorldSections;
    public final ListeningBlockStatePredicate blockGroup;

    private long maxChangeTime;

    private int timesRegistered;
    //Some sections may not exist / be unloaded. We have to be aware of those. //TODO Invalidation when sections / chunks unload (but the entity does not (?), not sure whether this is possible)
    boolean isListeningToAll = false;
    private ArrayList<SectionPos> sectionsNotListeningTo = null;
    private ArrayList<BlockListeningSection> sectionsUnsubscribed = null;

    public SectionedBlockChangeTracker(WorldSectionBox trackedWorldSections, ListeningBlockStatePredicate blockGroup) {
        this.trackedWorldSections = trackedWorldSections;
        this.blockGroup = blockGroup;

        this.maxChangeTime = 0;
    }

    public boolean matchesMovedBox(AABB box) {
        return this.trackedWorldSections.matchesRelevantBlocksBox(box);
    }

    public static SectionedBlockChangeTracker registerAt(Level world, AABB entityBoundingBox, ListeningBlockStatePredicate blockGroup) {
        WorldSectionBox worldSectionBox = WorldSectionBox.relevantExpandedBlocksBox(world, entityBoundingBox);
        SectionedBlockChangeTracker tracker = new SectionedBlockChangeTracker(worldSectionBox, blockGroup);
        //noinspection unchecked
        tracker = ((CanaryInternerWrapper<SectionedBlockChangeTracker>)world).getCanonical(tracker);

        tracker.register();
        return tracker;
    }

    long getWorldTime() {
        return this.trackedWorldSections.world().getGameTime();
    }

    public void register() {
        if (this.timesRegistered == 0) {
            WorldSectionBox trackedSections = this.trackedWorldSections;
            for (int x = trackedSections.chunkX1(); x < trackedSections.chunkX2(); x++) {
                for (int z = trackedSections.chunkZ1(); z < trackedSections.chunkZ2(); z++) {
                    ChunkAccess chunk = trackedSections.world().getChunk(x, z, ChunkStatus.FULL, false);
                    LevelChunkSection[] sectionArray = chunk == null ? null : chunk.getSections();
                    for (int y = trackedSections.chunkY1(); y < trackedSections.chunkY2(); y++) {
                        if (Pos.SectionYCoord.getMinYSection(trackedSections.world()) > y || Pos.SectionYCoord.getMaxYSectionExclusive(trackedSections.world()) <= y) {
                            continue;
                        }
                        if (sectionArray == null) {
                            if (this.sectionsNotListeningTo == null) {
                                this.sectionsNotListeningTo = new ArrayList<>();
                            }
                            this.sectionsNotListeningTo.add(SectionPos.of(x, y, z));
                            continue;
                        }
                        LevelChunkSection section = sectionArray[Pos.SectionYIndex.fromSectionCoord(trackedSections.world(), y)];

                        BlockListeningSection blockListeningSection = (BlockListeningSection) section;
                        blockListeningSection.addToCallback(this.blockGroup, this);
                    }
                }
            }
            this.setChanged(this.getWorldTime());
        }
        this.timesRegistered++;
    }

    public void unregister() {
        if (--this.timesRegistered > 0) {
            return;
        }
        WorldSectionBox trackedSections = this.trackedWorldSections;
        Level world = trackedSections.world();
        for (int x = trackedSections.chunkX1(); x < trackedSections.chunkX2(); x++) {
            for (int z = trackedSections.chunkZ1(); z < trackedSections.chunkZ2(); z++) {
                ChunkAccess chunk = world.getChunk(x, z, ChunkStatus.FULL, false);
                LevelChunkSection[] sectionArray = chunk == null ? null : chunk.getSections();
                for (int y = trackedSections.chunkY1(); y < trackedSections.chunkY2(); y++) {

                    if (sectionArray == null) {
                        continue;
                    }
                    if (Pos.SectionYCoord.getMinYSection(world) > y || Pos.SectionYCoord.getMaxYSectionExclusive(world) <= y) {
                        continue;
                    }
                    LevelChunkSection section = sectionArray[Pos.SectionYIndex.fromSectionCoord(world, y)];

                    BlockListeningSection blockListeningSection = (BlockListeningSection) section;
                    blockListeningSection.removeFromCallback(this.blockGroup, this);
                }
            }
        }
        this.sectionsNotListeningTo = null;
        //noinspection unchecked
        ((CanaryInternerWrapper<SectionedBlockChangeTracker>)world).deleteCanonical(this);
    }

    public void listenToAllSections() {
        boolean changed = false;
        ArrayList<SectionPos> notListeningTo = this.sectionsNotListeningTo;
        if (notListeningTo != null) {
            for (int i = notListeningTo.size() - 1; i >= 0; i--) {
                changed = true;
                SectionPos chunkSectionPos = notListeningTo.get(i);
                ChunkAccess chunk = this.trackedWorldSections.world().getChunk(chunkSectionPos.getX(), chunkSectionPos.getZ(), ChunkStatus.FULL, false);
                if (chunk != null) {
                    notListeningTo.remove(i);
                } else {
                    //Chunk not loaded, cannot listen to all sections.
                    return;
                }
                LevelChunkSection section = chunk.getSections()[Pos.SectionYIndex.fromSectionCoord(this.trackedWorldSections.world(), chunkSectionPos.getY())];
                BlockListeningSection blockListeningSection = (BlockListeningSection) section;
                blockListeningSection.addToCallback(this.blockGroup, this);
            }
        }
        if (this.sectionsUnsubscribed != null) {
            ArrayList<BlockListeningSection> unsubscribed = this.sectionsUnsubscribed;
            for (int i = unsubscribed.size() - 1; i >= 0; i--) {
                changed = true;
                BlockListeningSection blockListeningSection = unsubscribed.remove(i);
                blockListeningSection.addToCallback(this.blockGroup, this);
            }
        }
        this.isListeningToAll = true;
        if (changed) {
            this.setChanged(this.getWorldTime());
        }
    }

    public void setChanged(BlockListeningSection section) {
        if (this.sectionsUnsubscribed == null) {
            this.sectionsUnsubscribed = new ArrayList<>();
        }
        this.sectionsUnsubscribed.add(section);
        this.setChanged(this.getWorldTime());
        this.isListeningToAll = false;
    }

    public void setChanged(long atTime) {
        if (atTime > this.maxChangeTime) {
            this.maxChangeTime = atTime;
        }
    }

    /**
     * Method to quickly check whether any relevant blocks changed inside the relevant chunk sections after
     * the last test.
     *
     * @param lastCheckedTime time of the last interaction attempt
     * @return whether any relevant entity moved in the tracked area
     */
    public boolean isUnchangedSince(long lastCheckedTime) {
        if (lastCheckedTime <= this.maxChangeTime) {
            return false;
        }
        if (!this.isListeningToAll) {
            this.listenToAllSections();
            return this.isListeningToAll && lastCheckedTime > this.maxChangeTime;
        }
        return true;
    }

    //Do not modify, used for deduplication of instances
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SectionedBlockChangeTracker) obj;
        return Objects.equals(this.trackedWorldSections, that.trackedWorldSections) &&
                Objects.equals(this.blockGroup, that.blockGroup);
    }
    //Do not modify, used for deduplication of instances
    @Override
    public int hashCode() {
        return this.getClass().hashCode() ^ this.trackedWorldSections.hashCode() ^ this.blockGroup.hashCode();
    }
}