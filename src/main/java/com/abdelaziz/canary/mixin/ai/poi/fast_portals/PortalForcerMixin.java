package com.abdelaziz.canary.mixin.ai.poi.fast_portals;

import com.abdelaziz.canary.common.util.POIRegistryEntries;
import com.abdelaziz.canary.common.world.interests.PointOfInterestStorageExtended;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(PortalForcer.class)
public class PortalForcerMixin {
    @Shadow
    @Final
    protected ServerLevel level;

    /**
     * @author JellySquid
     * @reason Use optimized search for nearby points, avoid slow filtering, check for valid locations first
     * [VanillaCopy] everything but the Optional<PointOfInterest> lookup
     */
    @Overwrite
    public Optional<BlockUtil.FoundRectangle> findPortalAround(BlockPos centerPos, boolean dstIsNether, WorldBorder worldBorder) {
        int searchRadius = dstIsNether ? 16 : 128;

        PoiManager poiStorage = this.level.getPoiManager();
        poiStorage.ensureLoadedAndValid(this.level, centerPos, searchRadius);

        Optional<PoiRecord> ret = ((PointOfInterestStorageExtended) poiStorage).findNearestForPortalLogic(centerPos, searchRadius,
                POIRegistryEntries.NETHER_PORTAL_ENTRY, PoiManager.Occupancy.ANY,
                (poi) -> this.level.getBlockState(poi.getPos()).hasProperty(BlockStateProperties.HORIZONTAL_AXIS),
                worldBorder
        );

        return ret.map(poi -> {
            BlockPos blockPos = poi.getPos();
            this.level.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(blockPos), 3, blockPos);
            BlockState blockState = this.level.getBlockState(blockPos);
            return BlockUtil.getLargestRectangleAround(blockPos, blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS), 21, Direction.Axis.Y, 21, pos -> this.level.getBlockState(pos) == blockState);
        });
    }
}