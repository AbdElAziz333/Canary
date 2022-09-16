package com.abdelaziz.canary.mixin.ai.poi;

import com.abdelaziz.canary.common.world.interests.RegionBasedStorageColumn;
import com.google.common.collect.AbstractIterator;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import com.abdelaziz.canary.common.util.Pos;
import com.abdelaziz.canary.common.util.collections.ListeningLong2ObjectOpenHashMap;
import com.abdelaziz.canary.common.world.interests.RegionBasedStorageSectionExtended;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.storage.SerializingRegionBasedStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType") // We don't get a choice, this is Minecraft's doing!
@Mixin(SerializingRegionBasedStorage.class)
public abstract class SerializingRegionBasedStorageMixin<R> implements RegionBasedStorageSectionExtended<R> {
    @Shadow
    @Final
    protected HeightLimitView world;
    @Mutable
    @Shadow
    @Final
    private Long2ObjectMap<Optional<R>> loadedElements;

    @Shadow
    protected abstract Optional<R> get(long pos);

    @Shadow
    protected abstract void loadDataAt(ChunkPos pos);

    private Long2ObjectOpenHashMap<RegionBasedStorageColumn> columns;

    @SuppressWarnings("rawtypes")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Path path, Function codecFactory, Function factory, DataFixer dataFixer, DataFixTypes dataFixTypes, boolean dsync, DynamicRegistryManager dynamicRegistryManager, HeightLimitView world, CallbackInfo ci) {
        this.columns = new Long2ObjectOpenHashMap<>();
        this.loadedElements = new ListeningLong2ObjectOpenHashMap<>(this::onEntryAdded, this::onEntryRemoved);
    }

    private static long getChunkFromSection(long section) {
        int x = ChunkSectionPos.unpackX(section);
        int z = ChunkSectionPos.unpackZ(section);
        return ChunkPos.toLong(x, z);
    }

    private static boolean isSectionValid(int y) {
        return y >= 0 && y < RegionBasedStorageColumn.SECTIONS_IN_CHUNK;
    }

    private void onEntryRemoved(long key, Optional<R> value) {
        // NO-OP... vanilla never removes anything, leaking entries.
        // We might want to fix this.
        int y = ChunkSectionPos.unpackY(key);

        if (!isSectionValid(y)) {
            return;
        }

        long pos = getChunkFromSection(key);
        RegionBasedStorageColumn flags = this.columns.get(pos);

        if (flags != null && flags.clear(y)) {
            this.columns.remove(pos);
        }
    }

    private void onEntryAdded(long key, Optional<R> value) {
        int y = Pos.SectionYIndex.fromSectionCoord(this.world, ChunkSectionPos.unpackY(key));

        // We only care about items belonging to a valid sub-chunk
        //if (y < 0 || y >= Pos.SectionYIndex.getNumYSections(this.world)) {
        if (!isSectionValid(y)) {
            return;
        }

        int x = ChunkSectionPos.unpackX(key);
        int z = ChunkSectionPos.unpackZ(key);

        long pos = ChunkPos.toLong(x, z);

        RegionBasedStorageColumn flags = this.columns.get(pos);

        if (flags == null) {
            this.columns.put(pos, flags = new RegionBasedStorageColumn()); //Pos.SectionYIndex.getNumYSections(this.world)
        }

        flags.set(y, value.isPresent());
    }

    @Override
    public Stream<R> getWithinChunkColumn(int chunkX, int chunkZ) { //getNonEmptyPOISections
        RegionBasedStorageColumn sectionsWithPOI = this.getNonEmptyPOISections(chunkX, chunkZ);

        // No items are present in this column
        if (sectionsWithPOI.noSectionsPresent()) {
            return Stream.empty();
        }

        List<R> list = new ArrayList<>();
        int minYSection = Pos.SectionYCoord.getMinYSection(this.world);
        for (int chunkYIndex = sectionsWithPOI.nextNonEmptySection(0); chunkYIndex != -1; chunkYIndex = sectionsWithPOI.nextNonEmptySection(chunkYIndex + 1)) {
            int chunkY = chunkYIndex + minYSection;
            //noinspection SimplifyOptionalCallChains
            R r = this.loadedElements.get(ChunkSectionPos.asLong(chunkX, chunkY, chunkZ)).orElse(null);
            if (r != null) {
                list.add(r);
            }
        }

        return list.stream();
    }

    @Override
    public Iterable<R> getInChunkColumn(int chunkX, int chunkZ) {
        RegionBasedStorageColumn sectionsWithPOI = this.getNonEmptyPOISections(chunkX, chunkZ);

        // No items are present in this column
        if (sectionsWithPOI.noSectionsPresent()) {
            return Collections::emptyIterator;
        }

        Long2ObjectMap<Optional<R>> loadedElements = this.loadedElements;
        HeightLimitView world = this.world;

        return () -> new AbstractIterator<>() {
            private int nextBit = sectionsWithPOI.nextNonEmptySection(0);


            @Override
            protected R computeNext() {
                // If the next bit is <0, that means that no remaining set bits exist
                while (this.nextBit >= 0) {
                    Optional<R> next = loadedElements.get(ChunkSectionPos.asLong(chunkX, Pos.SectionYCoord.fromSectionIndex(world, this.nextBit), chunkZ));

                    // Find and advance to the next set bit
                    this.nextBit = sectionsWithPOI.nextNonEmptySection(this.nextBit + 1);

                    if (next.isPresent()) {
                        return next.get();
                    }
                }

                return this.endOfData();
            }
        };
    }

    private RegionBasedStorageColumn getNonEmptyPOISections(int chunkX, int chunkZ) {
        long pos = ChunkPos.toLong(chunkX, chunkZ);

        RegionBasedStorageColumn flags = this.getNonEmptySections(pos, false);

        if (flags != null) {
            return flags;
        }

        this.loadDataAt(new ChunkPos(pos));

        return this.getNonEmptySections(pos, true);
    }

    private RegionBasedStorageColumn getNonEmptySections(long pos, boolean required) {
        RegionBasedStorageColumn set = this.columns.get(pos);

        if (set == null && required) {
            throw new NullPointerException("No data is present for column: " + new ChunkPos(pos));
        }

        return set;
    }
}