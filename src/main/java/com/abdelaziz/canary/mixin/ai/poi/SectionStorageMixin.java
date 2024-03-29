package com.abdelaziz.canary.mixin.ai.poi;

import com.abdelaziz.canary.common.util.Pos;
import com.abdelaziz.canary.common.util.collections.ListeningLong2ObjectOpenHashMap;
import com.abdelaziz.canary.common.world.interests.RegionBasedStorageSectionExtended;
import com.google.common.collect.AbstractIterator;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.storage.SectionStorage;
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
@Mixin(SectionStorage.class)
public abstract class SectionStorageMixin<R> implements RegionBasedStorageSectionExtended<R> {
    private Long2ObjectOpenHashMap<BitSet> columns;

    @Shadow
    @Final
    protected LevelHeightAccessor levelHeightAccessor;

    @Mutable
    @Shadow
    @Final
    private Long2ObjectMap<Optional<R>> storage;

    @Shadow
    protected abstract void readColumn(ChunkPos pos);

    @SuppressWarnings("rawtypes")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Path p_223509_, Function p_223510_, Function p_223511_, DataFixer p_223512_, DataFixTypes p_223513_, boolean p_223514_, RegistryAccess p_223515_, LevelHeightAccessor p_223516_, CallbackInfo ci) {
        this.columns = new Long2ObjectOpenHashMap<>();
        this.storage = new ListeningLong2ObjectOpenHashMap<>(this::onEntryAdded, this::onEntryRemoved);
    }

    private void onEntryRemoved(long key, Optional<R> value) {
        int y = Pos.SectionYIndex.fromSectionCoord(this.levelHeightAccessor, SectionPos.y(key));

        // We only care about items belonging to a valid sub-chunk
        if (y < 0 || y >= Pos.SectionYIndex.getNumYSections(this.levelHeightAccessor)) {
            return;
        }

        int x = SectionPos.x(key);
        int z = SectionPos.z(key);

        long pos = ChunkPos.asLong(x, z);
        BitSet flags = this.columns.get(pos);

        if (flags != null) {
            flags.clear(y);
            if (flags.isEmpty()) {
                this.columns.remove(pos);
            }
        }
    }

    private void onEntryAdded(long key, Optional<R> value) {
        int y = Pos.SectionYIndex.fromSectionCoord(this.levelHeightAccessor, SectionPos.y(key));

        // We only care about items belonging to a valid sub-chunk
        if (y < 0 || y >= Pos.SectionYIndex.getNumYSections(this.levelHeightAccessor)) {
            return;
        }

        int x = SectionPos.x(key);
        int z = SectionPos.z(key);

        long pos = ChunkPos.asLong(x, z);

        BitSet flags = this.columns.get(pos);

        if (flags == null) {
            this.columns.put(pos, flags = new BitSet(Pos.SectionYIndex.getNumYSections(this.levelHeightAccessor)));
        }

        flags.set(y, value.isPresent());
    }

    @Override
    public Stream<R> getWithinChunkColumn(int chunkX, int chunkZ) {
        BitSet sectionsWithPOI = this.getNonEmptyPOISections(chunkX, chunkZ);

        // No items are present in this column
        if (sectionsWithPOI.isEmpty()) {
            return Stream.empty();
        }

        List<R> list = new ArrayList<>();
        int minYSection = Pos.SectionYCoord.getMinYSection(this.levelHeightAccessor);
        for (int chunkYIndex = sectionsWithPOI.nextSetBit(0); chunkYIndex != -1; chunkYIndex = sectionsWithPOI.nextSetBit(chunkYIndex + 1)) {
            int chunkY = chunkYIndex + minYSection;
            //noinspection SimplifyOptionalCallChains
            R r = this.storage.get(SectionPos.asLong(chunkX, chunkY, chunkZ)).orElse(null);
            if (r != null) {
                list.add(r);
            }
        }

        return list.stream();
    }

    @Override
    public Iterable<R> getInChunkColumn(int chunkX, int chunkZ) {
        BitSet sectionsWithPOI = this.getNonEmptyPOISections(chunkX, chunkZ);

        // No items are present in this column
        if (sectionsWithPOI.isEmpty()) {
            return Collections::emptyIterator;
        }

        Long2ObjectMap<Optional<R>> storage = this.storage;
        LevelHeightAccessor world = this.levelHeightAccessor;

        return () -> new AbstractIterator<>() {
            private int nextBit = sectionsWithPOI.nextSetBit(0);


            @Override
            protected R computeNext() {
                // If the next bit is <0, that means that no remaining set bits exist
                while (this.nextBit >= 0) {
                    Optional<R> next = storage.get(SectionPos.asLong(chunkX, Pos.SectionYCoord.fromSectionIndex(world, this.nextBit), chunkZ));

                    // Find and advance to the next set bit
                    this.nextBit = sectionsWithPOI.nextSetBit(this.nextBit + 1);

                    if (next.isPresent()) {
                        return next.get();
                    }
                }

                return this.endOfData();
            }
        };
    }

    private BitSet getNonEmptyPOISections(int chunkX, int chunkZ) {
        long pos = ChunkPos.asLong(chunkX, chunkZ);

        BitSet flags = this.getNonEmptySections(pos, false);

        if (flags != null) {
            return flags;
        }

        this.readColumn(new ChunkPos(pos));

        return this.getNonEmptySections(pos, true);
    }

    private BitSet getNonEmptySections(long pos, boolean required) {
        BitSet set = this.columns.get(pos);

        if (set == null && required) {
            throw new NullPointerException("No data is present for column: " + new ChunkPos(pos));
        }

        return set;
    }
}