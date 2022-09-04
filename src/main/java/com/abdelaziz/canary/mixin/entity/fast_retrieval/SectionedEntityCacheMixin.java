package com.abdelaziz.canary.mixin.entity.fast_retrieval;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.SectionedEntityCache;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.function.Consumer;

@Mixin(SectionedEntityCache.class)
public abstract class SectionedEntityCacheMixin<T extends EntityLike> {
    @Shadow
    @Nullable
    public abstract EntityTrackingSection<T> findTrackingSection(long sectionPos);

    /**
     * @author 2No2Name
     * @reason avoid iterating through LongAVLTreeSet, possibly iterating over hundreds of irrelevant longs to save up to 8 hash set gets
     */
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(
            method = "forEachInBox",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/util/math/ChunkSectionPos;getSectionCoord(D)I",
                    ordinal = 5
            ),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    public void forEachInBox(Box box, Consumer<EntityTrackingSection<T>> action, CallbackInfo ci, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (maxX >= minX + 4 || maxZ >= minZ + 4) {
            return; // Vanilla is likely more optimized when shooting entities with TNT cannons over huge distances.
            // Choosing a cutoff of 4 chunk size, as it becomes more likely that these entity sections do not exist when
            // they are far away from the shot entity (player despawn range, position maybe not on the ground, etc)
        }
        ci.cancel();

        // Vanilla order of the AVL long set is sorting by ascending long value. The x, y, z positions are packed into
        // a long with the x position's lowest 22 bits placed at the MSB.
        // Therefore the long is negative iff the 22th bit of the x position is set, which happens iff the x position
        // is negative. A positive x position will never have its 22th bit set, as these big coordinates are far outside
        // the world. y and z positions are treated as unsigned when sorting by ascending long value, as their sign bits
        // are placed somewhere inside the packed long

        for (int x = minX; x <= maxX; x++) {
            for (int z = Math.max(minZ, 0); z <= maxZ; z++) {
                this.forEachInColumn(x, minY, maxY, z, action);
            }

            int bound = Math.min(-1, maxZ);
            for (int z = minZ; z <= bound; z++) {
                this.forEachInColumn(x, minY, maxY, z, action);
            }
        }
    }

    private void forEachInColumn(int x, int minY, int maxY, int z, Consumer<EntityTrackingSection<T>> action) {
        //y from negative to positive, but y is treated as unsigned
        for (int y = Math.max(minY, 0); y <= maxY; y++) {
            this.consumeSection(ChunkSectionPos.asLong(x, y, z), action);
        }
        int bound = Math.min(-1, maxY);
        for (int y = minY; y <= bound; y++) {
            this.consumeSection(ChunkSectionPos.asLong(x, y, z), action);
        }
    }

    private void consumeSection(long pos, Consumer<EntityTrackingSection<T>> action) {
        EntityTrackingSection<T> section = this.findTrackingSection(pos);
        if (section != null && 0 != section.size() && section.getStatus().shouldTrack()) {
            action.accept(section);
        }
    }
}
