package com.abdelaziz.canary.mixin.alloc.empty_iterator;

import com.abdelaziz.canary.common.util.constants.ArrayConstants;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.ChunkRenderTypeSet;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Iterator;
import java.util.List;

@Mixin(ChunkRenderTypeSet.None.class)
public class ChunkRenderTypeSetMixin {

    /**
     * @reason avoid allocations
     * @author AbdElAziz
     * */
    @NotNull
    @Overwrite (remap = false)
    public Iterator<RenderType> iterator() {
        return ArrayConstants.EMPTY_ITERATOR;
    }

    /**
     * @reason avoid allocations
     * @author AbdElAziz
     * */
    @Overwrite (remap = false)
    public List<RenderType> asList() {
        return ArrayConstants.IMMUTABLE_EMPTY_LIST;
    }
}
