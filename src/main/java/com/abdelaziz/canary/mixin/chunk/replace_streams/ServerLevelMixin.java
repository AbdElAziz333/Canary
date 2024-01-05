package com.abdelaziz.canary.mixin.chunk.replace_streams;

import com.abdelaziz.canary.common.util.constants.BlockConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

import static net.minecraft.server.level.ServerLevel.END_SPAWN_POINT;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Shadow @Final
    private SleepStatus sleepStatus;

    @Shadow @Final
    List<ServerPlayer> players;

    /**
     * @reason Avoid streams.
     * @author AbdElAziz
     * */
    @Overwrite
    private void wakeUpAllPlayers() {
        this.sleepStatus.removeAllSleepers();

        for (ServerPlayer player : this.players) {
            if (player.isSleeping()) {
                player.stopSleepInBed(false, false);
            }
        }
    }

    /**
     * @reason Avoid streams.
     * @author AbdElAziz
     * */
    @Overwrite
    public static void makeObsidianPlatform(ServerLevel level) {
        BlockPos blockpos = END_SPAWN_POINT;
        int i = blockpos.getX();
        int j = blockpos.getY() - 2;
        int k = blockpos.getZ();

        for (BlockPos pos : BlockPos.betweenClosed(i - 2, j + 1, k - 2, i + 2, j + 3, k + 2)) {
            level.setBlockAndUpdate(pos, BlockConstants.DEFAULT_BLOCKSTATE);
        }

        for (BlockPos pos : BlockPos.betweenClosed(i - 2, j, k - 2, i + 2, j, k + 2)) {
            level.setBlockAndUpdate(pos, BlockConstants.OBSIDIAN_BLOCK_STATE);
        }
    }
}
