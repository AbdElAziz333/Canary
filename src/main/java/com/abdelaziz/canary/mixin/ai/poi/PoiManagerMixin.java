package com.abdelaziz.canary.mixin.ai.poi;

import com.abdelaziz.canary.common.util.Distances;
import com.abdelaziz.canary.common.world.interests.PointOfInterestSetExtended;
import com.abdelaziz.canary.common.world.interests.PointOfInterestStorageExtended;
import com.abdelaziz.canary.common.world.interests.RegionBasedStorageSectionExtended;
import com.abdelaziz.canary.common.world.interests.iterator.NearbyPointOfInterestStream;
import com.abdelaziz.canary.common.world.interests.iterator.SinglePointOfInterestTypeFilter;
import com.abdelaziz.canary.common.world.interests.iterator.SphereChunkOrderedPoiSetSpliterator;
import com.abdelaziz.canary.common.world.interests.types.PoiTypeHelper;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Mixin(PoiManager.class)
public abstract class PoiManagerMixin extends SectionStorage<PoiSection>
        implements PointOfInterestStorageExtended {

    public PoiManagerMixin(Path path, Function<Runnable, Codec<PoiSection>> codecFactory, Function<Runnable, PoiSection> factory, DataFixer dataFixer, DataFixTypes dataFixTypes, boolean dsync, LevelHeightAccessor world) {
        super(path, codecFactory, factory, dataFixer, dataFixTypes, dsync, world);
    }

    /**
     * @reason Avoid Stream API
     * @author Jellysquid
     */
    @Overwrite
    public void checkConsistencyWithBlocks(ChunkPos chunkPos_1, LevelChunkSection section) {
        SectionPos sectionPos = SectionPos.of(chunkPos_1, section.bottomBlockY() >> 4);

        PoiSection set = this.get(sectionPos.asLong()).orElse(null);

        if (set != null) {
            set.refresh(consumer -> {
                if (PoiTypeHelper.shouldScan(section)) {
                    this.updateFromSection(section, sectionPos, consumer);
                }
            });
        } else {
            if (PoiTypeHelper.shouldScan(section)) {
                set = this.getOrCreate(sectionPos.asLong());

                this.updateFromSection(section, sectionPos, set::add);
            }
        }
    }


    /**
     * @reason Retrieve all points of interest in one operation
     * @author JellySquid
     */
    @VisibleForDebug
    @Debug
    @SuppressWarnings("unchecked")
    @Overwrite
    public Stream<PoiRecord> getInChunk(Predicate<PoiType> predicate, ChunkPos pos,
                                        PoiManager.Occupancy status) {
        return ((RegionBasedStorageSectionExtended<PoiSection>) this)
                .getWithinChunkColumn(pos.x, pos.z)
                .flatMap(set -> set.getRecords(predicate, status));
    }

    /**
     * Gets a random POI that matches the requirements. Uses spherical radius.
     *
     * @reason Retrieve all points of interest in one operation, avoid stream code
     * @author JellySquid
     */
    @Overwrite
    public Optional<BlockPos> getRandom(Predicate<PoiType> typePredicate, Predicate<BlockPos> posPredicate,
                                        PoiManager.Occupancy status, BlockPos pos, int radius,
                                        Random rand) {
        ArrayList<PoiRecord> list = this.withinSphereChunkSectionSorted(typePredicate, pos, radius, status);

        for (int i = list.size() - 1; i >= 0; i--) {
            //shuffle by swapping randomly
            PoiRecord currentPOI = list.set(rand.nextInt(i + 1), list.get(i));
            list.set(i, currentPOI); //Move to the end of the unconsumed part of the list

            //consume while shuffling, abort shuffling when result found
            if (posPredicate.test(currentPOI.getPos())) {
                return Optional.of(currentPOI.getPos());
            }
        }

        return Optional.empty();
    }

    /**
     * Gets the closest POI that matches the requirements.
     *
     * @reason Avoid stream-heavy code, use a faster iterator and callback-based approach
     * @author 2No2Name
     */
    @Overwrite
    public Optional<BlockPos> findClosest(Predicate<PoiType> predicate, BlockPos pos, int radius,
                                          PoiManager.Occupancy status) {
        return this.findClosest(predicate, null, pos, radius, status);
    }

    /**
     * Gets the closest POI that matches the requirements.
     * If there are several closest POIs, negative chunk coordinate first (sort by x, then z, then y)
     *
     * @reason Avoid stream-heavy code, use a faster iterator and callback-based approach
     * @author JellySquid, 2No2Name
     */
    @Overwrite
    public Optional<BlockPos> findClosest(Predicate<PoiType> predicate,
                                          Predicate<BlockPos> posPredicate, BlockPos pos, int radius,
                                          PoiManager.Occupancy status) {
        Stream<PoiRecord> pointOfInterestStream = this.streamOutwards(pos, radius, status, true, false, predicate, posPredicate == null ? null : poi -> posPredicate.test(poi.getPos()));
        return pointOfInterestStream.map(PoiRecord::getPos).findFirst();
    }

    /**
     * Get number of matching POIs in sphere
     *
     * @reason Avoid stream-heavy code, use a faster iterator and callback-based approach
     * @author JellySquid
     */
    @Overwrite
    public long getCountInRange(Predicate<PoiType> predicate, BlockPos pos, int radius,
                                PoiManager.Occupancy status) {
        return this.withinSphereChunkSectionSorted(predicate, pos, radius, status).size();
    }

    /**
     * Get all POI in sphere around origin with given radius. Order is vanilla order
     * Vanilla order (might be undefined, but pratically):
     * Chunk section order: Negative X first, if equal, negative Z first, if equal, negative Y first.
     * Within the chunk section: Whatever the internal order is (we are not modifying that)
     *
     * @author JellySquid
     * @reason Avoid stream-heavy code, use faster filtering and fetches
     */
    @Overwrite
    public Stream<PoiRecord> getInRange(Predicate<PoiType> predicate, BlockPos sphereOrigin, int radius,
                                        PoiManager.Occupancy status) {
        return this.withinSphereChunkSectionSortedStream(predicate, sphereOrigin, radius, status);
    }

    @Override
    public Optional<PoiRecord> findNearestForPortalLogic(BlockPos origin, int radius, PoiType type,
                                                         PoiManager.Occupancy status,
                                                         Predicate<PoiRecord> afterSortPredicate, WorldBorder worldBorder) {
        // Order of the POI:
        // return closest accepted POI (L2 distance). If several exist:
        // return the one with most negative Y. If several exist:
        // return the one with most negative X. If several exist:
        // return the one with most negative Z. If several exist: Be confused about two POIs being in the same location.

        boolean worldBorderIsFarAway = worldBorder == null || worldBorder.getDistanceToBorder(origin.getX(), origin.getZ()) > radius + 3;
        Predicate<PoiRecord> poiPredicateAfterSorting;
        if (worldBorderIsFarAway) {
            poiPredicateAfterSorting = afterSortPredicate;
        } else {
            poiPredicateAfterSorting = poi -> worldBorder.isWithinBounds(poi.getPos()) && afterSortPredicate.test(poi);
        }
        return this.streamOutwards(origin, radius, status, true, true, new SinglePointOfInterestTypeFilter(type), poiPredicateAfterSorting).findFirst();
    }

    private Stream<PoiRecord> withinSphereChunkSectionSortedStream(Predicate<PoiType> predicate, BlockPos origin,
                                                                   int radius, PoiManager.Occupancy status) {
        double radiusSq = radius * radius;


        // noinspection unchecked
        RegionBasedStorageSectionExtended<PoiSection> storage = (RegionBasedStorageSectionExtended<PoiSection>) this;


        Stream<Stream<PoiSection>> stream = StreamSupport.stream(new SphereChunkOrderedPoiSetSpliterator(radius, origin, storage), false);

        return stream.flatMap((Stream<PoiSection> setStream) -> setStream.flatMap(
                (PoiSection set) -> set.getRecords(predicate, status)
                        .filter(point -> Distances.isWithinCircleRadius(origin, radiusSq, point.getPos()))
        ));
    }

    private ArrayList<PoiRecord> withinSphereChunkSectionSorted(Predicate<PoiType> predicate, BlockPos origin,
                                                                int radius, PoiManager.Occupancy status) {
        double radiusSq = radius * radius;

        int minChunkX = origin.getX() - radius - 1 >> 4;
        int minChunkZ = origin.getZ() - radius - 1 >> 4;

        int maxChunkX = origin.getX() + radius + 1 >> 4;
        int maxChunkZ = origin.getZ() + radius + 1 >> 4;

        // noinspection unchecked
        RegionBasedStorageSectionExtended<PoiSection> storage = (RegionBasedStorageSectionExtended<PoiSection>) this;

        ArrayList<PoiRecord> points = new ArrayList<>();
        Consumer<PoiRecord> collector = point -> {
            if (Distances.isWithinCircleRadius(origin, radiusSq, point.getPos())) {
                points.add(point);
            }
        };

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                for (PoiSection set : storage.getInChunkColumn(x, z)) {
                    ((PointOfInterestSetExtended) set).collectMatchingPoints(predicate, status, collector);
                }
            }
        }

        return points;
    }

    private Stream<PoiRecord> streamOutwards(BlockPos origin, int radius,
                                             PoiManager.Occupancy status,
                                             @SuppressWarnings("SameParameterValue") boolean useSquareDistanceLimit,
                                             boolean preferNegativeY,
                                             Predicate<PoiType> typePredicate,
                                             @Nullable Predicate<PoiRecord> afterSortingPredicate) {
        // noinspection unchecked
        RegionBasedStorageSectionExtended<PoiSection> storage = (RegionBasedStorageSectionExtended<PoiSection>) this;

        return StreamSupport.stream(new NearbyPointOfInterestStream(typePredicate, status, useSquareDistanceLimit, preferNegativeY, afterSortingPredicate, origin, radius, storage), false);
    }

    @Shadow
    protected abstract void updateFromSection(LevelChunkSection section, SectionPos sectionPos, BiConsumer<BlockPos, PoiType> entryConsumer);
}
