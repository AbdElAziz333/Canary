package com.abdelaziz.canary.mixin.block.flatten_states;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.Stream;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin extends StateHolder<Block, BlockState> {
    @Shadow
    public abstract Block getBlock();

    Stream<TagKey<Block>> tags;

    protected BlockStateBaseMixin(Block block, ImmutableMap<Property<?>, Comparable<?>> immutableMap, MapCodec<BlockState> mapCodec) {
        super(block, immutableMap, mapCodec);
    }

    @Deprecated
    @Inject(
            method = "initCache",
            at = @At(
                    value = "RETURN"
            )
    )
    private void initialize(CallbackInfo ci) {
        tags = this.getBlock().builtInRegistryHolder().tags();
    }

    /**
     * @reason cache allocations
     * @author AbdElAziz
     * */
    @Overwrite
    public Stream<TagKey<Block>> getTags() {
        return tags;
    }
}