package com.abdelaziz.canary.mixin.block.hopper;

import com.abdelaziz.canary.api.inventory.CanaryCooldownReceivingInventory;
import com.abdelaziz.canary.api.inventory.CanaryInventory;
import com.abdelaziz.canary.common.block.entity.SleepingBlockEntity;
import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeListener;
import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import com.abdelaziz.canary.common.block.entity.inventory_comparator_tracking.ComparatorTracker;
import com.abdelaziz.canary.common.entity.movement_tracker.SectionedEntityMovementListener;
import com.abdelaziz.canary.common.entity.movement_tracker.SectionedInventoryEntityMovementTracker;
import com.abdelaziz.canary.common.entity.movement_tracker.SectionedItemEntityMovementTracker;
import com.abdelaziz.canary.common.hopper.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static net.minecraft.world.level.block.entity.HopperBlockEntity.getItemsAtAndAbove;

@Mixin(value = HopperBlockEntity.class, priority = 950)
public abstract class HopperBlockEntityMixin extends BlockEntity implements Hopper, UpdateReceiver, CanaryInventory, InventoryChangeListener, SectionedEntityMovementListener {

    public HopperBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    //The currently used block inventories
    @Nullable
    private Container insertBlockInventory, extractBlockInventory;

    //item pickup bounding boxes in order. The last box in the array is the box that encompasses all of the others
    private AABB[] collectItemEntityBoxes;

    @Shadow
    private long tickedGameTime;
    private long collectItemEntityAttemptTime;

    private long myModCountAtLastInsert, myModCountAtLastExtract, myModCountAtLastItemCollect;

    private HopperCachingState.BlockInventory insertionMode = HopperCachingState.BlockInventory.UNKNOWN;
    private HopperCachingState.BlockInventory extractionMode = HopperCachingState.BlockInventory.UNKNOWN;
    private SectionedInventoryEntityMovementTracker<Container> extractInventoryEntityTracker;

    //The currently used inventories (optimized type, if not present, skip optimizations)
    @Nullable
    private CanaryInventory insertInventory, extractInventory;

    //@Shadow
    //private static native boolean ejectItems(Level level, BlockPos pos, BlockState state, HopperBlockEntity inventory);

    @Nullable //Null iff corresp. CanaryInventory field is null
    private CanaryStackList insertStackList, extractStackList;

    //Mod count used to avoid transfer attempts that are known to fail (no change since last attempt)
    private long insertStackListModCount, extractStackListModCount;

    private SectionedItemEntityMovementTracker<ItemEntity> collectItemEntityTracker;

    private boolean collectItemEntityTrackerWasEmpty;
    private boolean shouldCheckSleep;

    private AABB extractInventoryEntityBox;
    private long extractInventoryEntityFailedSearchTime;

    private AABB insertInventoryEntityBox;
    private long insertInventoryEntityFailedSearchTime;


    private SectionedInventoryEntityMovementTracker<Container> insertInventoryEntityTracker;

    @Shadow
    @Nullable
    private static native Container getSourceContainer(Level world, Hopper hopper);

    @Shadow
    private static native boolean canTakeItemFromContainer(Container inv, ItemStack stack, int slot, Direction facing);

    /**
     * Effectively overwrites {@link HopperBlockEntity#insert(World, BlockPos, BlockState, Inventory)} (only usage redirect)
     * [VanillaCopy] general hopper insert logic, modified for optimizations
     *
     * @reason Adding the inventory caching into the static method using mixins seems to be unfeasible without temporarily storing state in static fields.
     */
    @SuppressWarnings("JavadocReference")
    @Inject(
            cancellable = true,
            method = "ejectItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;)Z",
            at = @At(
                    value = "INVOKE", shift = At.Shift.BEFORE,
                    target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;isFullContainer(Lnet/minecraft/world/Container;Lnet/minecraft/core/Direction;)Z"
            ), locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void canaryInsert(Level world, BlockPos pos, BlockState hopperState, HopperBlockEntity hopper, CallbackInfoReturnable<Boolean> cir, Container insertInventory, Direction direction) {
        if (insertInventory == null || !(hopper instanceof HopperBlockEntity) || hopper instanceof WorldlyContainer) {
            //call the vanilla code to allow other mods inject features
            //e.g. carpet mod allows hoppers to insert items into wool blocks
            return;
        }

        HopperBlockEntityMixin hopperBlockEntity = (HopperBlockEntityMixin) HopperBlockEntity.getContainerAt(world, pos);
        CanaryStackList hopperStackList = InventoryHelper.getCanaryStackList(hopperBlockEntity);

        if (hopperBlockEntity.insertInventory == insertInventory && hopperStackList.getModCount() == hopperBlockEntity.myModCountAtLastInsert) {
            if (hopperBlockEntity.insertStackList != null && hopperBlockEntity.insertStackList.getModCount() == hopperBlockEntity.insertStackListModCount) {
                //ComparatorUpdatePattern.NO_UPDATE.apply(hopperBlockEntity, hopperStackList); //commented because it's a noop, Hoppers do not send useless comparator updates
                cir.setReturnValue(false);
                return;
            }
        }

        boolean insertInventoryWasEmptyHopperNotDisabled = insertInventory instanceof HopperBlockEntityMixin &&
                !((HopperBlockEntityMixin) insertInventory).isOnCustomCooldown() && hopperBlockEntity.insertStackList != null &&
                hopperBlockEntity.insertStackList.getOccupiedSlots() == 0;

        boolean insertInventoryHandlesModdedCooldown =
                ((CanaryCooldownReceivingInventory) insertInventory).canReceiveTransferCooldown() &&
                        hopperBlockEntity.insertStackList != null ?
                        hopperBlockEntity.insertStackList.getOccupiedSlots() == 0 :
                        insertInventory.isEmpty();

        //noinspection ConstantConditions
        if (!(hopperBlockEntity.insertInventory == insertInventory && hopperBlockEntity.insertStackList.getFullSlots() == hopperBlockEntity.insertStackList.size())) {
            Direction fromDirection = hopperState.getValue(HopperBlock.FACING).getOpposite();
            int size = hopperStackList.size();
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < size; ++i) {
                ItemStack transferStack = hopperStackList.get(i);
                if (!transferStack.isEmpty()) {
                    boolean transferSuccess = HopperHelper.tryMoveSingleItem(insertInventory, transferStack, fromDirection);
                    if (transferSuccess) {
                        if (insertInventoryWasEmptyHopperNotDisabled) {
                            HopperBlockEntityMixin receivingHopper = (HopperBlockEntityMixin) insertInventory;
                            int k = 8;
                            if (receivingHopper.tickedGameTime >= hopperBlockEntity.tickedGameTime) {
                                k = 7;
                            }
                            receivingHopper.setCooldown(k);
                        }

                        if (insertInventoryHandlesModdedCooldown) {
                            ((CanaryCooldownReceivingInventory) insertInventory).setTransferCooldown(hopperBlockEntity.tickedGameTime);
                        }

                        insertInventory.setChanged();
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }
        }

        hopperBlockEntity.myModCountAtLastInsert = hopperStackList.getModCount();
        if (hopperBlockEntity.insertStackList != null) {
            hopperBlockEntity.insertStackListModCount = hopperBlockEntity.insertStackList.getModCount();
        }

        cir.setReturnValue(false);
    }

    /**
     * Inject to replace the extract method with an optimized but equivalent replacement.
     * Uses the vanilla method as fallback for non-optimized Inventories.
     *
     * @param to   Hopper or Hopper Minecart that is extracting
     */
    @Inject(method = "suckInItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/Hopper;)Z", at = @At(value = "FIELD", target = "Lnet/minecraft/core/Direction;DOWN:Lnet/minecraft/core/Direction;", shift = At.Shift.AFTER), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    private static void canaryExtract(Level world, Hopper to, CallbackInfoReturnable<Boolean> cir) {
        Container from = getSourceContainer(world, to);

        if (!(to instanceof HopperBlockEntityMixin hopperBlockEntity)) {
            return; //optimizations not implemented for hopper minecarts
        }
        if (from != hopperBlockEntity.extractInventory || hopperBlockEntity.extractStackList == null) {
            return; //from inventory is not an optimized inventory, vanilla fallback
        }

        CanaryStackList hopperStackList = InventoryHelper.getCanaryStackList(hopperBlockEntity);
        CanaryStackList fromStackList = hopperBlockEntity.extractStackList;

        if (hopperStackList.getModCount() == hopperBlockEntity.myModCountAtLastExtract) {
            if (fromStackList.getModCount() == hopperBlockEntity.extractStackListModCount) {
                if (!(from instanceof ComparatorTracker comparatorTracker) || comparatorTracker.hasAnyComparatorNearby()) {
                    //noinspection CollectionAddedToSelf
                    fromStackList.runComparatorUpdatePatternOnFailedExtract(fromStackList, from);
                }
                cir.setReturnValue(false);
                return;
            }
        }

        int[] availableSlots = from instanceof WorldlyContainer ? ((WorldlyContainer) from).getSlotsForFace(Direction.DOWN) : null;
        int fromSize = availableSlots != null ? availableSlots.length : from.getContainerSize();
        for (int i = 0; i < fromSize; i++) {
            int fromSlot = availableSlots != null ? availableSlots[i] : i;
            ItemStack itemStack = fromStackList.get(fromSlot);
            if (!itemStack.isEmpty() && canTakeItemFromContainer(from, itemStack, fromSlot, Direction.DOWN)) {
                //calling removeStack is necessary due to its side effects (setChanged in LootableContainerBlockEntity)
                ItemStack takenItem = from.removeItem(fromSlot, 1);
                assert !takenItem.isEmpty();
                boolean transferSuccess = HopperHelper.tryMoveSingleItem(to, takenItem, null);
                if (transferSuccess) {
                    to.setChanged();
                    from.setChanged();
                    cir.setReturnValue(true);
                    return;
                }
                //put the item back similar to vanilla
                ItemStack restoredStack = fromStackList.get(fromSlot);
                if (restoredStack.isEmpty()) {
                    restoredStack = takenItem;
                } else {
                    restoredStack.grow(1);
                }
                //calling setItem is necessary due to its side effects (setChanged in LootableContainerBlockEntity)
                from.setItem(fromSlot, restoredStack);
            }
        }

        hopperBlockEntity.myModCountAtLastExtract = hopperStackList.getModCount();
        if (fromStackList != null) {
            hopperBlockEntity.extractStackListModCount = fromStackList.getModCount();
        }

        cir.setReturnValue(false);
    }

    @Redirect(
            method = "tryMoveItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;Ljava/util/function/BooleanSupplier;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;inventoryFull()Z"
            )
    )
    private static boolean canaryHopperIsFull(HopperBlockEntity hopperBlockEntity) {
        //noinspection ConstantConditions
        CanaryStackList canaryStackList = InventoryHelper.getCanaryStackList((HopperBlockEntityMixin) (Object) hopperBlockEntity);
        return canaryStackList.getFullSlots() == canaryStackList.size();
    }

    @Redirect(
            method = "tryMoveItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;Ljava/util/function/BooleanSupplier;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;isEmpty()Z"
            )
    )
    private static boolean canaryHopperIsEmpty(HopperBlockEntity hopperBlockEntity) {
        //noinspection ConstantConditions
        CanaryStackList canaryStackList = InventoryHelper.getCanaryStackList((HopperBlockEntityMixin) (Object) hopperBlockEntity);
        return canaryStackList.getOccupiedSlots() == 0;
    }

    @Override
    public void invalidateCacheOnNeighborUpdate(boolean fromAbove) {
        //Clear the block inventory cache (composter inventories and no inventory present) on block update / observer update
        if (fromAbove) {
            if (this.extractionMode == HopperCachingState.BlockInventory.NO_BLOCK_INVENTORY || this.extractionMode == HopperCachingState.BlockInventory.BLOCK_STATE) {
                this.invalidateBlockExtractionData();
            }
        } else {
            if (this.insertionMode == HopperCachingState.BlockInventory.NO_BLOCK_INVENTORY || this.insertionMode == HopperCachingState.BlockInventory.BLOCK_STATE) {
                this.invalidateBlockInsertionData();
            }
        }
    }

    @Override
    public void invalidateCacheOnNeighborUpdate(Direction fromDirection) {
        boolean fromAbove = fromDirection == Direction.UP;
        if (fromAbove || this.getBlockState().getValue(HopperBlock.FACING) == fromDirection) {
            this.invalidateCacheOnNeighborUpdate(fromAbove);
        }
    }

    @Redirect(method = "ejectItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;getAttachedContainer(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/Container;"))
    private static Container nullify(Level world, BlockPos pos, BlockState state) {
        return null;
    }

    /*@ModifyVariable(
            method = "ejectItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;)Z",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;getAttachedContainer(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/Container;"
            ),
            ordinal = 1
    )
    private static Container getCanaryOutputInventory(HopperBlockEntity container, Level level, BlockPos pos, BlockState state, HopperBlockEntity hopper) {
        HopperBlockEntityMixin hopperBlockEntity = (HopperBlockEntityMixin) hopper.getContainerAt(level, pos);
        return hopperBlockEntity.getInsertInventory(level, state);
    } */

    @Redirect(method = "suckInItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/Hopper;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;getItemsAtAndAbove(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/Hopper;)Ljava/util/List;"))
    private static List<ItemEntity> canaryGetInputItemEntities(Level world, Hopper hopper) {
        if (!(hopper instanceof HopperBlockEntityMixin hopperBlockEntity)) {
            return getItemsAtAndAbove(world, hopper); //optimizations not implemented for hopper minecarts
        }

        if (hopperBlockEntity.collectItemEntityTracker == null) {
            hopperBlockEntity.initCollectItemEntityTracker();
        }

        long modCount = InventoryHelper.getCanaryStackList(hopperBlockEntity).getModCount();

        if ((hopperBlockEntity.collectItemEntityTrackerWasEmpty || hopperBlockEntity.myModCountAtLastItemCollect == modCount) &&
                hopperBlockEntity.collectItemEntityTracker.isUnchangedSince(hopperBlockEntity.collectItemEntityAttemptTime)) {
            hopperBlockEntity.collectItemEntityAttemptTime = hopperBlockEntity.tickedGameTime;
            return Collections.emptyList();
        }

        hopperBlockEntity.myModCountAtLastItemCollect = modCount;
        hopperBlockEntity.shouldCheckSleep = false;

        List<ItemEntity> itemEntities = hopperBlockEntity.collectItemEntityTracker.getEntities(hopperBlockEntity.collectItemEntityBoxes);
        hopperBlockEntity.collectItemEntityAttemptTime = hopperBlockEntity.tickedGameTime;
        hopperBlockEntity.collectItemEntityTrackerWasEmpty = itemEntities.isEmpty();
        //set unchanged so that if this extract fails and there is no other change to hoppers or items, extracting
        // items can be skipped.
        return itemEntities;
    }

    /**
     * Makes this hopper remember the given inventory.
     *
     * @param insertInventory Block inventory / Blockentity inventory to be remembered
     */
    private void cacheInsertBlockInventory(Container insertInventory) {
        assert !(insertInventory instanceof Entity);

        if (insertInventory instanceof CanaryInventory optimizedInventory) {
            this.cacheInsertCanaryInventory(optimizedInventory);
        } else {
            this.insertInventory = null;
            this.insertStackList = null;
            this.insertStackListModCount = 0;
        }

        if (insertInventory instanceof BlockEntity || insertInventory instanceof CompoundContainer) {
            this.insertBlockInventory = insertInventory;
            if (insertInventory instanceof InventoryChangeTracker) {
                this.insertionMode = HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY;
                ((InventoryChangeTracker) insertInventory).listenForMajorInventoryChanges(this);
            } else {
                this.insertionMode = HopperCachingState.BlockInventory.BLOCK_ENTITY;
            }
        } else {
            if (insertInventory == null) {
                this.insertBlockInventory = null;
                this.insertionMode = HopperCachingState.BlockInventory.NO_BLOCK_INVENTORY;
            } else {
                this.insertBlockInventory = insertInventory;
                this.insertionMode = HopperCachingState.BlockInventory.BLOCK_STATE;
            }
        }
    }

    private void cacheInsertCanaryInventory(CanaryInventory optimizedInventory) {
        CanaryStackList insertInventoryStackList = InventoryHelper.getCanaryStackList(optimizedInventory);
        this.insertInventory = optimizedInventory;
        this.insertStackList = insertInventoryStackList;
        this.insertStackListModCount = insertInventoryStackList.getModCount() - 1;
    }

    private void cacheExtractCanaryInventory(CanaryInventory optimizedInventory) {
        CanaryStackList extractInventoryStackList = InventoryHelper.getCanaryStackList(optimizedInventory);
        this.insertInventory = optimizedInventory;
        this.insertStackList = extractInventoryStackList;
        this.insertStackListModCount = extractInventoryStackList.getModCount() - 1;
    }

    /**
     * @author 2No2Name
     * @reason avoid stream code
     */
    @Overwrite
    private static boolean isEmptyContainer(Container inv, Direction side) {
        int[] availableSlots = inv instanceof WorldlyContainer ? ((WorldlyContainer) inv).getSlotsForFace(side) : null;
        int fromSize = availableSlots != null ? availableSlots.length : inv.getContainerSize();
        for (int i = 0; i < fromSize; i++) {
            int slot = availableSlots != null ? availableSlots[i] : i;
            if (!inv.getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Redirect(method = "suckInItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/Hopper;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;getSourceContainer(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/Hopper;)Lnet/minecraft/world/Container;"))
    private static Container getExtractInventory(Level world, Hopper hopper) {
        if (!(hopper instanceof HopperBlockEntityMixin hopperBlockEntity)) {
            return getSourceContainer(world, hopper); //Hopper Minecarts do not cache Inventories
        }

        Container blockInventory = hopperBlockEntity.getExtractBlockInventory(world);

        if (blockInventory != null) {
            return blockInventory;
        }

        if (hopperBlockEntity.extractInventoryEntityTracker == null) {
            hopperBlockEntity.initExtractInventoryTracker(world);
        }

        if (hopperBlockEntity.extractInventoryEntityTracker.isUnchangedSince(hopperBlockEntity.extractInventoryEntityFailedSearchTime)) {
            hopperBlockEntity.extractInventoryEntityFailedSearchTime = hopperBlockEntity.tickedGameTime;
            return null;
        }

        hopperBlockEntity.extractInventoryEntityFailedSearchTime = Long.MIN_VALUE;
        hopperBlockEntity.shouldCheckSleep = false;

        List<Container> inventoryEntities = hopperBlockEntity.extractInventoryEntityTracker.getEntities(hopperBlockEntity.extractInventoryEntityBox);
        if (inventoryEntities.isEmpty()) {
            hopperBlockEntity.extractInventoryEntityFailedSearchTime = hopperBlockEntity.tickedGameTime;
            //only set unchanged when no entity present. this allows shortcutting this case
            //shortcutting the entity present case requires checking its change counter
            return null;
        }

        Container inventory = inventoryEntities.get(world.random.nextInt(inventoryEntities.size()));
        if (inventory instanceof CanaryInventory optimizedInventory) {
            CanaryStackList extractInventoryStackList = InventoryHelper.getCanaryStackList(optimizedInventory);
            if (inventory != hopperBlockEntity.extractInventory || hopperBlockEntity.extractStackList != extractInventoryStackList) {
                //not caching the inventory (NO_BLOCK_INVENTORY prevents it)
                //make change counting on the entity inventory possible, without caching it as block inventory
                hopperBlockEntity.cacheExtractCanaryInventory(optimizedInventory);
            }
        }
        return inventory;
    }

    @Shadow
    public abstract boolean isOnCustomCooldown();

    @Shadow
    public abstract void setCooldown(int cooldown);

    @Shadow
    protected abstract boolean isOnCooldown();

    public Container getExtractBlockInventory(Level world) {
        Container blockInventory = this.extractBlockInventory;
        if (this.extractionMode == HopperCachingState.BlockInventory.NO_BLOCK_INVENTORY) {
            return null;
        } else if (this.extractionMode == HopperCachingState.BlockInventory.BLOCK_STATE) {
            return blockInventory;
        } else if (this.extractionMode == HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY) {
            return blockInventory;
        } else if (this.extractionMode == HopperCachingState.BlockInventory.BLOCK_ENTITY) {
            BlockEntity blockEntity = (BlockEntity) Objects.requireNonNull(blockInventory);
            //Movable Block Entity compatibility - position comparison
            BlockPos pos = blockEntity.getBlockPos();
            BlockPos thisPos = this.getBlockPos();
            if (!(blockEntity).isRemoved() &&
                    pos.getX() == thisPos.getX() &&
                    pos.getY() == thisPos.getY() + 1 &&
                    pos.getZ() == thisPos.getZ()) {
                CanaryInventory optimizedInventory;
                if ((optimizedInventory = this.extractInventory) != null) {
                    CanaryStackList insertInventoryStackList = InventoryHelper.getCanaryStackList(optimizedInventory);
                    //This check is necessary as sometimes the stacklist is silently replaced (e.g. command making furnace read inventory from nbt)
                    if (insertInventoryStackList == this.extractStackList) {
                        return optimizedInventory;
                    } else {
                        this.invalidateBlockExtractionData();
                    }
                } else {
                    return blockInventory;
                }
            }
        }

        //No Cached Inventory: Get like vanilla and cache
        blockInventory = HopperHelper.vanillaGetBlockInventory(world, this.getBlockPos().above());
        blockInventory = HopperHelper.replaceDoubleInventory(blockInventory);
        this.cacheExtractBlockInventory(blockInventory);
        return blockInventory;
    }

    public Container getInsertBlockInventory(Level world, BlockState hopperState) {
        Container blockInventory = this.insertBlockInventory;
        if (this.insertionMode == HopperCachingState.BlockInventory.NO_BLOCK_INVENTORY) {
            return null;
        } else if (this.insertionMode == HopperCachingState.BlockInventory.BLOCK_STATE) {
            return blockInventory;
        } else if (this.insertionMode == HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY) {
            return blockInventory;
        } else if (this.insertionMode == HopperCachingState.BlockInventory.BLOCK_ENTITY) {
            BlockEntity blockEntity = (BlockEntity) Objects.requireNonNull(blockInventory);
            //Movable Block Entity compatibility - position comparison
            BlockPos pos = blockEntity.getBlockPos();
            Direction direction = hopperState.getValue(HopperBlock.FACING);
            BlockPos transferPos = this.getBlockPos().relative(direction);
            if (!(blockEntity).isRemoved() &&
                    pos.equals(transferPos)) {
                CanaryInventory optimizedInventory;
                if ((optimizedInventory = this.insertInventory) != null) {
                    CanaryStackList insertInventoryStackList = InventoryHelper.getCanaryStackList(optimizedInventory);
                    //This check is necessary as sometimes the stacklist is silently replaced (e.g. command making furnace read inventory from nbt)
                    if (insertInventoryStackList == this.insertStackList) {
                        return optimizedInventory;
                    } else {
                        this.invalidateBlockInsertionData();
                    }
                } else {
                    return blockInventory;
                }
            }
        }

        //No Cached Inventory: Get like vanilla and cache
        Direction direction = hopperState.getValue(HopperBlock.FACING);
        blockInventory = HopperHelper.vanillaGetBlockInventory(world, this.getBlockPos().relative(direction));
        blockInventory = HopperHelper.replaceDoubleInventory(blockInventory);
        this.cacheInsertBlockInventory(blockInventory);
        return blockInventory;
    }

    public Container getInsertInventory(Level world, BlockState hopperState) {
        Container blockInventory = this.getInsertBlockInventory(world, hopperState);
        if (blockInventory != null) {
            return blockInventory;
        }

        if (this.insertInventoryEntityTracker == null) {
            this.initInsertInventoryTracker(world, hopperState);
        }
        if (this.insertInventoryEntityTracker.isUnchangedSince(this.insertInventoryEntityFailedSearchTime)) {
            this.insertInventoryEntityFailedSearchTime = this.tickedGameTime;
            return null;
        }

        this.insertInventoryEntityFailedSearchTime = Long.MIN_VALUE;
        this.shouldCheckSleep = false;

        List<Container> inventoryEntities = this.insertInventoryEntityTracker.getEntities(this.insertInventoryEntityBox);
        if (inventoryEntities.isEmpty()) {
            this.insertInventoryEntityFailedSearchTime = this.tickedGameTime;
            //Remember failed entity search timestamp. This allows shortcutting if no entity movement happens.
            return null;
        }

        Container inventory = inventoryEntities.get(world.random.nextInt(inventoryEntities.size()));
        if (inventory instanceof CanaryInventory optimizedInventory) {
            CanaryStackList insertInventoryStackList = InventoryHelper.getCanaryStackList(optimizedInventory);
            if (inventory != this.insertInventory || this.insertStackList != insertInventoryStackList) {
                this.cacheInsertCanaryInventory(optimizedInventory);
            }
        }
        return inventory;
    }

    //Entity tracker initialization:

    private void initCollectItemEntityTracker() {
        assert this.level instanceof ServerLevel;
        List<AABB> list = new ArrayList<>();
        AABB encompassingBox = null;
        for (AABB box : HopperHelper.getHopperPickupVolumeBoxes(this)) {
            AABB offsetBox = box.move(this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ());
            list.add(offsetBox);
            if (encompassingBox == null) {
                encompassingBox = offsetBox;
            } else {
                encompassingBox = encompassingBox.minmax(offsetBox);
            }
        }
        list.add(encompassingBox);
        this.collectItemEntityBoxes = list.toArray(new AABB[0]);
        this.collectItemEntityTracker =
                SectionedItemEntityMovementTracker.registerAt(
                        (ServerLevel) this.level,
                        encompassingBox,
                        ItemEntity.class
                );
        this.collectItemEntityAttemptTime = Long.MIN_VALUE;
    }

    /**
     * Makes this hopper remember the given inventory.
     *
     * @param extractInventory Block inventory / Blockentity inventory to be remembered
     */
    private void cacheExtractBlockInventory(Container extractInventory) {
        assert !(extractInventory instanceof Entity);

        if (extractInventory instanceof CanaryInventory optimizedInventory) {
            this.cacheExtractCanaryInventory(optimizedInventory);
        } else {
            this.extractInventory = null;
            this.extractStackList = null;
            this.extractStackListModCount = 0;
        }

        if (extractInventory instanceof BlockEntity || extractInventory instanceof CompoundContainer) {
            this.extractBlockInventory = extractInventory;
            if (extractInventory instanceof InventoryChangeTracker) {
                this.extractionMode = HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY;
                ((InventoryChangeTracker) extractInventory).listenForMajorInventoryChanges(this);
            }
        }
    }

    private void initInsertInventoryTracker(Level world, BlockState hopperState) {
        assert world instanceof ServerLevel;
        Direction direction = hopperState.getValue(HopperBlock.FACING);
        BlockPos pos = this.getBlockPos().relative(direction);
        this.insertInventoryEntityBox = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        this.insertInventoryEntityTracker =
                SectionedInventoryEntityMovementTracker.registerAt(
                        (ServerLevel) this.level,
                        this.insertInventoryEntityBox,
                        Container.class
                );
        this.insertInventoryEntityFailedSearchTime = Long.MIN_VALUE;
    }

    private void initExtractInventoryTracker(Level world) {
        assert world instanceof ServerLevel;
        BlockPos pos = this.getBlockPos().relative(Direction.UP);
        this.extractInventoryEntityBox = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        this.extractInventoryEntityTracker =
                SectionedInventoryEntityMovementTracker.registerAt(
                        (ServerLevel) this.level,
                        this.extractInventoryEntityBox,
                        Container.class
                );
        this.extractInventoryEntityFailedSearchTime = Long.MIN_VALUE;
    }

    //Cached data invalidation:

    @SuppressWarnings("deprecation")
    @Intrinsic
    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
    }

    @SuppressWarnings({"MixinAnnotationTarget", "UnresolvedMixinReference"})
    @Inject(method = "setBlockState(Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("HEAD"))
    private void invalidateOnSetCachedState(BlockState state, CallbackInfo ci) {
        if (this.level != null && !this.level.isClientSide() && state.getValue(HopperBlock.FACING) != this.getBlockState().getValue(HopperBlock.FACING)) {
            this.invalidateCachedData();
        }
    }

    private void invalidateCachedData() {
        this.shouldCheckSleep = false;
        this.invalidateInsertionData();
        this.invalidateExtractionData();
    }

    private void invalidateInsertionData() {
        if (this.level instanceof ServerLevel serverWorld) {
            if (this.insertInventoryEntityTracker != null) {
                this.insertInventoryEntityTracker.unRegister(serverWorld);
                this.insertInventoryEntityTracker = null;
                this.insertInventoryEntityBox = null;
                this.insertInventoryEntityFailedSearchTime = 0L;
            }
        }

        if (this.insertionMode == HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY) {
            assert this.insertBlockInventory != null;
            ((InventoryChangeTracker) this.insertBlockInventory).stopListenForMajorInventoryChanges(this);
        }

        this.invalidateBlockInsertionData();
    }

    private void invalidateBlockInsertionData() {
        this.insertionMode = HopperCachingState.BlockInventory.UNKNOWN;
        this.insertBlockInventory = null;
        this.insertInventory = null;
        this.insertStackList = null;
        this.insertStackListModCount = 0;

        if (this instanceof SleepingBlockEntity sleepingBlockEntity) {
            sleepingBlockEntity.wakeUpNow();
        }
    }

    private void invalidateExtractionData() {
        if (this.level instanceof ServerLevel serverWorld) {
            if (this.extractInventoryEntityTracker != null) {
                this.extractInventoryEntityTracker.unRegister(serverWorld);
                this.extractInventoryEntityTracker = null;
                this.extractInventoryEntityBox = null;
                this.extractInventoryEntityFailedSearchTime = 0L;
            }
            if (this.collectItemEntityTracker != null) {
                this.collectItemEntityTracker.unRegister(serverWorld);
                this.collectItemEntityTracker = null;
                this.collectItemEntityBoxes = null;
                this.collectItemEntityTrackerWasEmpty = false;
            }
        }
        if (this.extractionMode == HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY) {
            assert this.extractBlockInventory != null;
            ((InventoryChangeTracker) this.extractBlockInventory).stopListenForMajorInventoryChanges(this);
        }
        this.invalidateBlockExtractionData();
    }

    private void invalidateBlockExtractionData() {
        if (this.extractionMode == HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY) {
            assert this.extractBlockInventory != null;
            ((InventoryChangeTracker) this.extractBlockInventory).stopListenForMajorInventoryChanges(this);
        }

        this.extractionMode = HopperCachingState.BlockInventory.UNKNOWN;
        this.extractBlockInventory = null;
        this.extractInventory = null;
        this.extractStackList = null;
        this.extractStackListModCount = 0;

        if (this instanceof SleepingBlockEntity sleepingBlockEntity) {
            sleepingBlockEntity.wakeUpNow();
        }
    }

    @Inject(
            method = "pushItemsTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;tryMoveItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;Ljava/util/function/BooleanSupplier;)Z",
                    shift = At.Shift.AFTER
            )
    )
    private static void checkSleepingConditions(Level world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, CallbackInfo ci) {
        ((HopperBlockEntityMixin) (Object) blockEntity).checkSleepingConditions();
    }

    private void checkSleepingConditions() {
        if (this.isOnCooldown()) {
            return;
        }
        if (this instanceof SleepingBlockEntity thisSleepingBlockEntity) {
            if (thisSleepingBlockEntity.isSleeping()) {
                return;
            }
            if (!this.shouldCheckSleep) {
                this.shouldCheckSleep = true;
                return;
            }
            if (this instanceof InventoryChangeTracker thisTracker) {
                boolean listenToExtractTracker = false;
                boolean listenToInsertTracker = false;
                boolean listenToExtractEntities = false;
                boolean listenToInsertEntities = false;

                CanaryStackList thisStackList = InventoryHelper.getCanaryStackList(this);

                if (this.extractionMode != HopperCachingState.BlockInventory.BLOCK_STATE && thisStackList.getFullSlots() != thisStackList.size()) {
                    if (this.extractionMode == HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY) {
                        Container blockInventory = this.extractBlockInventory;
                        if (this.extractStackList != null &&
                                blockInventory instanceof InventoryChangeTracker) {
                            if (!this.extractStackList.maybeSendsComparatorUpdatesOnFailedExtract() || (blockInventory instanceof ComparatorTracker comparatorTracker && !comparatorTracker.hasAnyComparatorNearby())) {
                                listenToExtractTracker = true;
                            } else {
                                return;
                            }
                        } else {
                            return;
                        }
                    } else if (this.extractionMode == HopperCachingState.BlockInventory.NO_BLOCK_INVENTORY) {
                        listenToExtractEntities = true;
                    } else {
                        return;
                    }
                }

                if (this.insertionMode != HopperCachingState.BlockInventory.BLOCK_STATE && 0 < thisStackList.getOccupiedSlots()) {
                    if (this.insertionMode == HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY) {
                        Container blockInventory = this.insertBlockInventory;

                        if (this.insertStackList != null && blockInventory instanceof InventoryChangeTracker) {
                            listenToInsertTracker = true;
                        } else {
                            return;
                        }
                    } else if (this.insertionMode == HopperCachingState.BlockInventory.NO_BLOCK_INVENTORY) {
                        listenToInsertEntities = true;
                    } else {
                        return;
                    }
                }

                if (listenToExtractTracker) {
                    ((InventoryChangeTracker) this.extractBlockInventory).listenForContentChangesOnce(this.extractStackList, this);
                }

                if (listenToInsertTracker) {
                    ((InventoryChangeTracker) this.insertBlockInventory).listenForContentChangesOnce(this.insertStackList, this);
                }

                if (listenToInsertEntities) {
                    if (this.insertInventoryEntityTracker == null) {
                        this.initInsertInventoryTracker(this.level, this.getBlockState());
                    }
                    this.insertInventoryEntityTracker.listenToEntityMovementOnce(this);
                }
                if (listenToExtractEntities) {
                    if (this.extractInventoryEntityTracker == null) {
                        this.initExtractInventoryTracker(this.level);
                    }
                    this.extractInventoryEntityTracker.listenToEntityMovementOnce(this);
                    if (this.collectItemEntityTracker == null) {
                        this.initCollectItemEntityTracker();
                    }
                    this.collectItemEntityTracker.listenToEntityMovementOnce(this);
                }
                thisTracker.listenForContentChangesOnce(thisStackList, this);
                thisSleepingBlockEntity.startSleeping();
            }
        }
    }

    @Override
    public void handleInventoryContentModified(Container inventory) {
        if (this instanceof SleepingBlockEntity sleepingBlockEntity) {
            sleepingBlockEntity.wakeUpNow();
        }
    }

    @Override
    public void handleInventoryRemoved(Container inventory) {
        if (this instanceof SleepingBlockEntity sleepingBlockEntity) {
            sleepingBlockEntity.wakeUpNow();
        }
        if (inventory == this.insertBlockInventory) {
            this.invalidateBlockInsertionData();
        }
        if (inventory == this.extractBlockInventory) {
            this.invalidateBlockExtractionData();
        }
        if (inventory == this) {
            this.invalidateCachedData();
        }
    }

    @Override
    public boolean handleComparatorAdded(Container inventory) {
        if (inventory == this.extractBlockInventory && this instanceof SleepingBlockEntity sleepingBlockEntity) {
            sleepingBlockEntity.wakeUpNow();
            return true;
        }
        return false;
    }

    @Override
    public void handleEntityMovement(Class<?> category) {
        if (this instanceof SleepingBlockEntity sleepingBlockEntity) {
            sleepingBlockEntity.wakeUpNow();
        }
    }
}