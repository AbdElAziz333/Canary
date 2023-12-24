package com.abdelaziz.canary.mixin.ai.poi.fast_portals;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

@Mixin(PoiManager.class)
public abstract class PoiManagerMixin extends SectionStorage<PoiSection> {
    @Shadow
    @Final
    private LongSet loadedChunks;

    private final LongSet preloadedCenterChunks = new LongOpenHashSet();

    private int preloadRadius = 0;

    public PoiManagerMixin(Path path, Function<Runnable, Codec<PoiSection>> codecFactory, Function<Runnable, PoiSection> factory, DataFixer dataFixer, DataFixTypes dataFixTypes, boolean dsync, LevelHeightAccessor world) {
        super(path, codecFactory, factory, dataFixer, dataFixTypes, dsync, world);
    }

    /**
     * @author Crec0, 2No2Name
     * @reason Streams in this method cause unnecessary lag. Simply rewriting this to not use streams, we gain
     * considerable performance. Noticeable when large amount of entities are traveling through nether portals.
     * Furthermore, caching whether all surrounding chunks are loaded is more efficient than caching the state
     * of single chunks only.
     */
    @Overwrite
    public void ensureLoadedAndValid(LevelReader worldView, BlockPos pos, int radius) {
        if (this.preloadRadius != radius) {
            //Usually there is only one preload radius per PointOfInterestStorage. Just in case another mod adjusts it dynamically, we avoid
            //assuming its value.
            preloadedCenterChunks.clear();
            preloadRadius = radius;
        }

        long chunkPos = ChunkPos.asLong(pos);

        if (this.preloadedCenterChunks.contains(chunkPos)) {
            return;
        }

        //TODO: needs to be tchecked
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());

        int chunkRadius = Math.floorDiv(radius, 16);
        int maxHeight = this.levelHeightAccessor.getMaxSection() - 1;
        int minHeight = this.levelHeightAccessor.getMinSection();

        for (int x = chunkX - chunkRadius, xMax = chunkX + chunkRadius; x <= xMax; x++) {
            for (int z = chunkZ - chunkRadius, zMax = chunkZ + chunkRadius; z <= zMax; z++) {
                preloadChunkIfAnySubChunkContainsPOI(worldView, x, z, minHeight, maxHeight);
            }
        }

        this.preloadedCenterChunks.add(chunkPos);
    }

    private void preloadChunkIfAnySubChunkContainsPOI(LevelReader worldView, int x, int z, int minSubChunk, int maxSubChunk) {
        ChunkPos chunkPos = new ChunkPos(x, z);
        long longChunkPos = chunkPos.toLong();

        if (this.loadedChunks.contains(longChunkPos)) return;

        for (int y = minSubChunk; y <= maxSubChunk; y++) {
            Optional<PoiSection> section = this.getOrLoad(SectionPos.asLong(x, y, z));
            if (section.isPresent()) {
                boolean result = section.get().isValid();
                if (result) {
                    if (this.loadedChunks.add(longChunkPos)) {
                        worldView.getChunk(x, z, ChunkStatus.EMPTY);
                    }
                    break;
                }
            }
        }
    }
}
