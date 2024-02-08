package com.abdelaziz.canary.mixin.world.inline_block_access;

import com.abdelaziz.canary.common.util.constants.BlockConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Level.class)
public abstract class LevelMixin implements LevelHeightAccessor {
    @Shadow
    public abstract LevelChunk getChunk(int i, int j);

    /**
     * @reason Reduce method size to help the JVM inline, Avoid excess height limit checks
     * @author 2No2Name
     */
    @Overwrite
    public BlockState getBlockState(BlockPos pos) {
        LevelChunk worldChunk = this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        LevelChunkSection[] sections = worldChunk.getSections();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        int chunkY = this.getSectionIndex(y);
        if (chunkY < 0 || chunkY >= sections.length) {
            return BlockConstants.VOID_DEFAULT_BLOCKSTATE; //outside world
        }

        LevelChunkSection section = sections[chunkY];
        if (section == null || section.hasOnlyAir()) {
            return BlockConstants.DEFAULT_BLOCKSTATE;
        }
        return section.getBlockState(x & 15, y & 15, z & 15);
        //This code path is slower than with the extra world height limit check. Tradeoff in favor of the default path.
    }
}