package com.abdelaziz.canary.mixin.block.hopper;

import com.abdelaziz.canary.api.inventory.CanaryInventory;
import com.abdelaziz.canary.common.hopper.*;
import com.abdelaziz.canary.common.block.entity.SleepingBlockEntity;
import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeListener;
import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import com.abdelaziz.canary.common.block.entity.inventory_comparator_tracking.ComparatorTracker;
import com.abdelaziz.canary.common.entity.tracker.nearby.NearbyEntityMovementListener;
import com.abdelaziz.canary.common.entity.tracker.nearby.SectionedInventoryEntityMovementTracker;
import com.abdelaziz.canary.common.entity.tracker.nearby.SectionedItemEntityMovementTracker;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
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

import static net.minecraft.block.entity.HopperBlockEntity.getInputItemEntities;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin extends BlockEntity implements Hopper, UpdateReceiver, CanaryInventory, InventoryChangeListener, NearbyEntityMovementListener {

    public HopperBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    //The currently used block inventories
    @Nullable
    private Inventory insertBlockInventory, extractBlockInventory;
    //item pickup bounding boxes in order. The last box in the array is the box that encompasses all of the others
    private Box[] collectItemEntityBoxes;

    @Shadow
    protected abstract boolean isDisabled();

    @Shadow
    private long lastTickTime;
    private SectionedInventoryEntityMovementTracker<Inventory> extractInventoryEntityTracker;

    private long myModCountAtLastInsert, myModCountAtLastExtract, myModCountAtLastItemCollect;

    private HopperCachingState.BlockInventory insertionMode = HopperCachingState.BlockInventory.UNKNOWN;
    private HopperCachingState.BlockInventory extractionMode = HopperCachingState.BlockInventory.UNKNOWN;
    private Box extractInventoryEntityBox;

    //The currently used inventories (optimized type, if not present, skip optimizations)
    @Nullable
    private CanaryInventory insertInventory, extractInventory;
    @Nullable //Null iff corresp. CanaryInventory field is null
    private CanaryStackList insertStackList, extractStackList;
    //Mod count used to avoid transfer attempts that are known to fail (no change since last attempt)
    private long insertStackListModCount, extractStackListModCount;

    private SectionedItemEntityMovementTracker<ItemEntity> collectItemEntityTracker;
    private boolean collectItemEntityTrackerWasEmpty;
    private SectionedInventoryEntityMovementTracker<Inventory> insertInventoryEntityTracker;
    private long collectItemEntityAttemptTime;
    private Box insertInventoryEntityBox;

    @Shadow
    @Nullable
    private static native Inventory getInputInventory(World world, Hopper hopper);

    private long extractInventoryEntityFailedSearchTime;

    @Shadow
    private static native boolean ejectItems(World world, BlockPos pos, BlockState state, HopperBlockEntity inventory);

    @Shadow
    private static native boolean canExtract(Inventory inv, ItemStack stack, int slot, Direction facing);

    private long insertInventoryEntityFailedSearchTime;

    private boolean shouldCheckSleep;

    @Redirect(method = "extract(Lnet/minecraft/world/World;Lnet/minecraft/block/entity/Hopper;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/HopperBlockEntity;getInputInventory(Lnet/minecraft/world/World;Lnet/minecraft/block/entity/Hopper;)Lnet/minecraft/inventory/Inventory;"))
    private static Inventory getExtractInventory(World world, Hopper hopper) {
        if (!(hopper instanceof HopperBlockEntityMixin hopperBlockEntity)) {
            return getInputInventory(world, hopper); //Hopper Minecarts do not cache Inventories
        }

        Inventory blockInventory = hopperBlockEntity.getExtractBlockInventory(world);
        if (blockInventory != null) {
            return blockInventory;
        }

        if (hopperBlockEntity.extractInventoryEntityTracker == null) {
            hopperBlockEntity.initExtractInventoryTracker(world);
        }
        if (hopperBlockEntity.extractInventoryEntityTracker.isUnchangedSince(hopperBlockEntity.extractInventoryEntityFailedSearchTime)) {
            hopperBlockEntity.extractInventoryEntityFailedSearchTime = hopperBlockEntity.lastTickTime;
            return null;
        }
        hopperBlockEntity.extractInventoryEntityFailedSearchTime = Long.MIN_VALUE;
        hopperBlockEntity.shouldCheckSleep = false;

        List<Inventory> inventoryEntities = hopperBlockEntity.extractInventoryEntityTracker.getEntities(hopperBlockEntity.extractInventoryEntityBox);
        if (inventoryEntities.isEmpty()) {
            hopperBlockEntity.extractInventoryEntityFailedSearchTime = hopperBlockEntity.lastTickTime;
            //only set unchanged when no entity present. this allows shortcutting this case
            //shortcutting the entity present case requires checking its change counter
            return null;
        }
        Inventory inventory = inventoryEntities.get(world.random.nextInt(inventoryEntities.size()));
        if (inventory instanceof CanaryInventory optimizedInventory) {
            CanaryStackList extractInventoryStackList = InventoryHelper.getCanaryStackList(optimizedInventory);
            if (inventory != hopperBlockEntity.extractInventory || hopperBlockEntity.extractStackList != extractInventoryStackList) {
                //not caching the inventory (NO_BLOCK_INVENTORY prevents it)
                //make change counting on the entity inventory possible, without caching it as block inventory
                hopperBlockEntity.extractInventory = optimizedInventory;
                hopperBlockEntity.extractStackList = extractInventoryStackList;
                hopperBlockEntity.extractStackListModCount = hopperBlockEntity.extractStackList.getModCount() - 1;
            }
        }
        return inventory;
    }

    /**
     * Effectively overwrites {@link HopperBlockEntity#insert(World, BlockPos, BlockState, Inventory)} (only usage redirect)
     * [VanillaCopy] general hopper insert logic, modified for optimizations
     *
     * @reason Adding the inventory caching into the static method using mixins seems to be unfeasible without temporarily storing state in static fields.
     */
    @SuppressWarnings("JavadocReference")
    @Redirect(method = "insertAndExtract(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/HopperBlockEntity;Ljava/util/function/BooleanSupplier;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/HopperBlockEntity;ejectItems(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/HopperBlockEntity;)Z"))
    private static boolean canaryInsert(World world, BlockPos pos, BlockState hopperState, HopperBlockEntity hopper) {
        HopperBlockEntityMixin hopperBlockEntity = (HopperBlockEntityMixin) hopper.getInventoryAt(world, pos);
        Inventory insertInventory = hopperBlockEntity.getInsertInventory(world, hopperState);
        if (insertInventory == null) {
            //call the vanilla code, but with target inventory nullify (mixin above) to allow other mods inject features
            //e.g. carpet mod allows hoppers to insert items into wool blocks
            return ejectItems(world, pos, hopperState, hopper);
        }

        CanaryStackList hopperStackList = InventoryHelper.getCanaryStackList(hopperBlockEntity);
        if (hopperBlockEntity.insertInventory == insertInventory && hopperStackList.getModCount() == hopperBlockEntity.myModCountAtLastInsert) {
            if (hopperBlockEntity.insertStackList != null && hopperBlockEntity.insertStackList.getModCount() == hopperBlockEntity.insertStackListModCount) {
//                ComparatorUpdatePattern.NO_UPDATE.apply(hopperBlockEntity, hopperStackList); //commented because it's a noop, Hoppers do not send useless comparator updates
                return false;
            }
        }

        boolean insertInventoryWasEmptyHopperNotDisabled = insertInventory instanceof HopperBlockEntityMixin && !((HopperBlockEntityMixin) insertInventory).isDisabled() && hopperBlockEntity.insertStackList != null && hopperBlockEntity.insertStackList.getOccupiedSlots() == 0;
        //noinspection ConstantConditions
        if (!(hopperBlockEntity.insertInventory == insertInventory && hopperBlockEntity.insertStackList.getFullSlots() == hopperBlockEntity.insertStackList.size())) {
            Direction fromDirection = hopperState.get(HopperBlock.FACING).getOpposite();
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
                            if (receivingHopper.lastTickTime >= hopperBlockEntity.lastTickTime) {
                                k = 7;
                            }
                            receivingHopper.setTransferCooldown(k);
                        }
                        insertInventory.markDirty();
                        return true;
                    }
                }
            }
        }
        hopperBlockEntity.myModCountAtLastInsert = hopperStackList.getModCount();
        if (hopperBlockEntity.insertStackList != null) {
            hopperBlockEntity.insertStackListModCount = hopperBlockEntity.insertStackList.getModCount();
        }
        return false;
    }

    /**
     * Inject to replace the extract method with an optimized but equivalent replacement.
     * Uses the vanilla method as fallback for non-optimized Inventories.
     *
     * @param to   Hopper or Hopper Minecart that is extracting
     * @param from Inventory the hopper is extracting from
     */
    @Inject(method = "extract(Lnet/minecraft/world/World;Lnet/minecraft/block/entity/Hopper;)Z", at = @At(value = "FIELD", target = "Lnet/minecraft/util/math/Direction;DOWN:Lnet/minecraft/util/math/Direction;", shift = At.Shift.AFTER), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    private static void canaryExtract(World world, Hopper to, CallbackInfoReturnable<Boolean> cir) {
        Inventory from = getInputInventory(world, to);

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

        int[] availableSlots = from instanceof SidedInventory ? ((SidedInventory) from).getAvailableSlots(Direction.DOWN) : null;
        int fromSize = availableSlots != null ? availableSlots.length : from.size();
        for (int i = 0; i < fromSize; i++) {
            int fromSlot = availableSlots != null ? availableSlots[i] : i;
            ItemStack itemStack = fromStackList.get(fromSlot);
            if (!itemStack.isEmpty() && canExtract(from, itemStack, fromSlot, Direction.DOWN)) {
                //calling removeStack is necessary due to its side effects (markDirty in LootableContainerBlockEntity)
                ItemStack takenItem = from.removeStack(fromSlot, 1);
                assert !takenItem.isEmpty();
                boolean transferSuccess = HopperHelper.tryMoveSingleItem(to, takenItem, null);
                if (transferSuccess) {
                    to.markDirty();
                    from.markDirty();
                    cir.setReturnValue(true);
                    return;
                }
                //put the item back similar to vanilla
                ItemStack restoredStack = fromStackList.get(fromSlot);
                if (restoredStack.isEmpty()) {
                    restoredStack = takenItem;
                } else {
                    restoredStack.increment(1);
                }
                //calling setStack is necessary due to its side effects (markDirty in LootableContainerBlockEntity)
                from.setStack(fromSlot, restoredStack);
            }
        }
        hopperBlockEntity.myModCountAtLastExtract = hopperStackList.getModCount();
        if (fromStackList != null) {
            hopperBlockEntity.extractStackListModCount = fromStackList.getModCount();
        }
        cir.setReturnValue(false);
    }

    @Redirect(
            method = "insertAndExtract(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/HopperBlockEntity;Ljava/util/function/BooleanSupplier;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/entity/HopperBlockEntity;isFull()Z"
            )
    )
    private static boolean canaryHopperIsFull(HopperBlockEntity hopperBlockEntity) {
        //noinspection ConstantConditions
        CanaryStackList canaryStackList = InventoryHelper.getCanaryStackList((HopperBlockEntityMixin) (Object) hopperBlockEntity);
        return canaryStackList.getFullSlots() == canaryStackList.size();
    }

    @Redirect(
            method = "insertAndExtract(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/HopperBlockEntity;Ljava/util/function/BooleanSupplier;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/entity/HopperBlockEntity;isEmpty()Z"
            )
    )
    private static boolean canaryHopperIsEmpty(HopperBlockEntity hopperBlockEntity) {
        //noinspection ConstantConditions
        CanaryStackList canaryStackList = InventoryHelper.getCanaryStackList((HopperBlockEntityMixin) (Object) hopperBlockEntity);
        return canaryStackList.getOccupiedSlots() == 0;
    }

    @Shadow
    protected abstract void setTransferCooldown(int cooldown);

    @Shadow
    protected abstract boolean isFull();

    @Shadow
    protected abstract boolean needsCooldown();

    @Override
    public void onNeighborUpdate(boolean above) {
        //Clear the block inventory cache (composter inventories and no inventory present) on block update / observer update
        if (above) {
            if (this.extractionMode == HopperCachingState.BlockInventory.NO_BLOCK_INVENTORY || this.extractionMode == HopperCachingState.BlockInventory.BLOCK_STATE) {
                this.invalidateBlockExtractionData();
            }
        } else {
            if (this.insertionMode == HopperCachingState.BlockInventory.NO_BLOCK_INVENTORY || this.insertionMode == HopperCachingState.BlockInventory.BLOCK_STATE) {
                this.invalidateBlockInsertionData();
            }
        }
    }


    @Redirect(method = "ejectItems(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/HopperBlockEntity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/HopperBlockEntity;getOutputInventory(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/inventory/Inventory;"))
    private static Inventory nullify(World world, BlockPos pos, BlockState state) {
        return null;
    }

    @Redirect(method = "extract(Lnet/minecraft/world/World;Lnet/minecraft/block/entity/Hopper;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/HopperBlockEntity;getInputItemEntities(Lnet/minecraft/world/World;Lnet/minecraft/block/entity/Hopper;)Ljava/util/List;"))
    private static List<ItemEntity> canaryGetInputItemEntities(World world, Hopper hopper) {
        if (!(hopper instanceof HopperBlockEntityMixin hopperBlockEntity)) {
            return getInputItemEntities(world, hopper); //optimizations not implemented for hopper minecarts
        }

        if (hopperBlockEntity.collectItemEntityTracker == null) {
            hopperBlockEntity.initCollectItemEntityTracker();
        }
        long modCount = InventoryHelper.getCanaryStackList(hopperBlockEntity).getModCount();
        if ((hopperBlockEntity.collectItemEntityTrackerWasEmpty || hopperBlockEntity.myModCountAtLastItemCollect == modCount) &&
                hopperBlockEntity.collectItemEntityTracker.isUnchangedSince(hopperBlockEntity.collectItemEntityAttemptTime)) {
            hopperBlockEntity.collectItemEntityAttemptTime = hopperBlockEntity.lastTickTime;
            return Collections.emptyList();
        }
        hopperBlockEntity.myModCountAtLastItemCollect = modCount;
        hopperBlockEntity.shouldCheckSleep = false;

        List<ItemEntity> itemEntities = hopperBlockEntity.collectItemEntityTracker.getEntities(hopperBlockEntity.collectItemEntityBoxes);
        hopperBlockEntity.collectItemEntityAttemptTime = hopperBlockEntity.lastTickTime;
        hopperBlockEntity.collectItemEntityTrackerWasEmpty = itemEntities.isEmpty();
        //set unchanged so that if this extract fails and there is no other change to hoppers or items, extracting
        // items can be skipped.
        return itemEntities;
    }

    /**
     * @author 2No2Name
     * @reason avoid stream code
     */
    @Overwrite
    private static boolean isInventoryEmpty(Inventory inv, Direction side) {
        int[] availableSlots = inv instanceof SidedInventory ? ((SidedInventory) inv).getAvailableSlots(side) : null;
        int fromSize = availableSlots != null ? availableSlots.length : inv.size();
        for (int i = 0; i < fromSize; i++) {
            int slot = availableSlots != null ? availableSlots[i] : i;
            if (!inv.getStack(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Inject(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/entity/HopperBlockEntity;insertAndExtract(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/HopperBlockEntity;Ljava/util/function/BooleanSupplier;)Z",
                    shift = At.Shift.AFTER
            )
    )
    private static void checkSleepingConditions(World world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, CallbackInfo ci) {
        ((HopperBlockEntityMixin) (Object) blockEntity).checkSleepingConditions();
    }

    private void cacheInsertCanaryInventory(CanaryInventory optimizedInventory) {
        this.insertInventory = optimizedInventory;
        CanaryStackList insertInventoryStackList = InventoryHelper.getCanaryStackList(optimizedInventory);
        this.insertStackList = insertInventoryStackList;
        this.insertStackListModCount = insertInventoryStackList.getModCount() - 1;
    }

    /**
     * Makes this hopper remember the given inventory.
     *
     * @param insertInventory Block inventory / Blockentity inventory to be remembered
     */
    private void cacheInsertBlockInventory(Inventory insertInventory) {
        assert !(insertInventory instanceof Entity);
        if (insertInventory instanceof BlockEntity || insertInventory instanceof DoubleInventory) {
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

        if (insertInventory instanceof CanaryInventory optimizedInventory) {
            this.cacheInsertCanaryInventory(optimizedInventory);
        } else {
            this.insertInventory = null;
            this.insertStackList = null;
            this.insertStackListModCount = 0;
        }
    }

    /**
     * Makes this hopper remember the given inventory.
     *
     * @param extractInventory Block inventory / Blockentity inventory to be remembered
     */
    private void cacheExtractBlockInventory(Inventory extractInventory) {
        assert !(extractInventory instanceof Entity);
        if (extractInventory instanceof BlockEntity || extractInventory instanceof DoubleInventory) {
            this.extractBlockInventory = extractInventory;
            if (extractInventory instanceof InventoryChangeTracker) {
                this.extractionMode = HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY;
                ((InventoryChangeTracker) extractInventory).listenForMajorInventoryChanges(this);
            } else {
                this.extractionMode = HopperCachingState.BlockInventory.BLOCK_ENTITY;
            }
        } else {
            if (extractInventory == null) {
                this.extractBlockInventory = null;
                this.extractionMode = HopperCachingState.BlockInventory.NO_BLOCK_INVENTORY;
            } else {
                this.extractBlockInventory = extractInventory;
                this.extractionMode = HopperCachingState.BlockInventory.BLOCK_STATE;
            }
        }

        if (extractInventory instanceof CanaryInventory optimizedInventory) {
            this.extractInventory = optimizedInventory;
            CanaryStackList extractInventoryStackList = InventoryHelper.getCanaryStackList(optimizedInventory);
            this.extractStackList = extractInventoryStackList;
            this.extractStackListModCount = extractInventoryStackList.getModCount() - 1;
        } else {
            this.extractInventory = null;
            this.extractStackList = null;
            this.extractStackListModCount = 0;
        }
    }

    public Inventory getExtractBlockInventory(World world) {
        Inventory blockInventory = this.extractBlockInventory;
        if (this.extractionMode == HopperCachingState.BlockInventory.NO_BLOCK_INVENTORY) {
            return null;
        } else if (this.extractionMode == HopperCachingState.BlockInventory.BLOCK_STATE) {
            return blockInventory;
        } else if (this.extractionMode == HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY) {
            return blockInventory;
        } else if (this.extractionMode == HopperCachingState.BlockInventory.BLOCK_ENTITY) {
            BlockEntity blockEntity = (BlockEntity) Objects.requireNonNull(blockInventory);
            //Movable Block Entity compatibility - position comparison
            BlockPos pos = blockEntity.getPos();
            BlockPos thisPos = this.getPos();
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
        blockInventory = HopperHelper.vanillaGetBlockInventory(world, this.getPos().up());
        this.cacheExtractBlockInventory(blockInventory);
        return blockInventory;
    }

    public Inventory getInsertBlockInventory(World world, BlockState hopperState) {
        Inventory blockInventory = this.insertBlockInventory;
        if (this.insertionMode == HopperCachingState.BlockInventory.NO_BLOCK_INVENTORY) {
            return null;
        } else if (this.insertionMode == HopperCachingState.BlockInventory.BLOCK_STATE) {
            return blockInventory;
        } else if (this.insertionMode == HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY) {
            return blockInventory;
        } else if (this.insertionMode == HopperCachingState.BlockInventory.BLOCK_ENTITY) {
            BlockEntity blockEntity = (BlockEntity) Objects.requireNonNull(blockInventory);
            //Movable Block Entity compatibility - position comparison
            BlockPos pos = blockEntity.getPos();
            Direction direction = hopperState.get(HopperBlock.FACING);
            BlockPos transferPos = this.getPos().offset(direction);
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
        Direction direction = hopperState.get(HopperBlock.FACING);
        blockInventory = HopperHelper.vanillaGetBlockInventory(world, this.getPos().offset(direction));
        this.cacheInsertBlockInventory(blockInventory);
        return blockInventory;
    }

    //Entity tracker initialization:

    public Inventory getInsertInventory(World world, BlockState hopperState) {
        Inventory blockInventory = this.getInsertBlockInventory(world, hopperState);
        if (blockInventory != null) {
            return blockInventory;
        }

        if (this.insertInventoryEntityTracker == null) {
            this.initInsertInventoryTracker(world, hopperState);
        }
        if (this.insertInventoryEntityTracker.isUnchangedSince(this.insertInventoryEntityFailedSearchTime)) {
            this.insertInventoryEntityFailedSearchTime = this.lastTickTime;
            return null;
        }
        this.insertInventoryEntityFailedSearchTime = Long.MIN_VALUE;
        this.shouldCheckSleep = false;

        List<Inventory> inventoryEntities = this.insertInventoryEntityTracker.getEntities(this.insertInventoryEntityBox);
        if (inventoryEntities.isEmpty()) {
            this.insertInventoryEntityFailedSearchTime = this.lastTickTime;
            //Remember failed entity search timestamp. This allows shortcutting if no entity movement happens.
            return null;
        }
        Inventory inventory = inventoryEntities.get(world.random.nextInt(inventoryEntities.size()));
        if (inventory instanceof CanaryInventory optimizedInventory) {
            CanaryStackList insertInventoryStackList = InventoryHelper.getCanaryStackList(optimizedInventory);
            if (inventory != this.insertInventory || this.insertStackList != insertInventoryStackList) {
                this.cacheInsertCanaryInventory(optimizedInventory);
            }
        }

        return inventory;
    }

    private void initCollectItemEntityTracker() {
        assert this.world instanceof ServerWorld;
        List<Box> list = new ArrayList<>();
        Box encompassingBox = null;
        for (Box box : HopperHelper.getHopperPickupVolumeBoxes(this)) {
            Box offsetBox = box.offset(this.pos.getX(), this.pos.getY(), this.pos.getZ());
            list.add(offsetBox);
            if (encompassingBox == null) {
                encompassingBox = offsetBox;
            } else {
                encompassingBox = encompassingBox.union(offsetBox);
            }
        }
        list.add(encompassingBox);
        this.collectItemEntityBoxes = list.toArray(new Box[0]);
        this.collectItemEntityTracker =
                SectionedItemEntityMovementTracker.registerAt(
                        (ServerWorld) this.world,
                        encompassingBox,
                        ItemEntity.class
                );
        this.collectItemEntityAttemptTime = Long.MIN_VALUE;
    }

    private void initExtractInventoryTracker(World world) {
        assert world instanceof ServerWorld;
        BlockPos pos = this.pos.offset(Direction.UP);
        this.extractInventoryEntityBox = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        this.extractInventoryEntityTracker =
                SectionedInventoryEntityMovementTracker.registerAt(
                        (ServerWorld) this.world,
                        this.extractInventoryEntityBox,
                        Inventory.class
                );
        this.extractInventoryEntityFailedSearchTime = Long.MIN_VALUE;
    }

    //Cached data invalidation:

    private void initInsertInventoryTracker(World world, BlockState hopperState) {
        assert world instanceof ServerWorld;
        Direction direction = hopperState.get(HopperBlock.FACING);
        BlockPos pos = this.pos.offset(direction);
        this.insertInventoryEntityBox = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        this.insertInventoryEntityTracker =
                SectionedInventoryEntityMovementTracker.registerAt(
                        (ServerWorld) this.world,
                        this.insertInventoryEntityBox,
                        Inventory.class
                );
        this.insertInventoryEntityFailedSearchTime = Long.MIN_VALUE;
    }

    private void invalidateCachedData() {
        this.shouldCheckSleep = false;
        this.invalidateInsertionData();
        this.invalidateExtractionData();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setCachedState(BlockState state) {
        BlockState cachedState = this.getCachedState();
        super.setCachedState(state);
        if (state.get(HopperBlock.FACING) != cachedState.get(HopperBlock.FACING)) {
            this.invalidateCachedData();
        }
    }

    private void invalidateBlockInsertionData() {
        if (this.insertionMode == HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY) {
            assert this.insertBlockInventory != null;
            ((InventoryChangeTracker) this.insertBlockInventory).stopListenForMajorInventoryChanges(this);
        }
        this.insertionMode = HopperCachingState.BlockInventory.UNKNOWN;
        this.insertBlockInventory = null;
        this.insertInventory = null;
        this.insertStackList = null;
        this.insertStackListModCount = 0;

        if (this instanceof SleepingBlockEntity sleepingBlockEntity) {
            sleepingBlockEntity.wakeUpNow();
        }
    }

    private void invalidateInsertionData() {
        if (this.world instanceof ServerWorld serverWorld) {
            if (this.insertInventoryEntityTracker != null) {
                this.insertInventoryEntityTracker.unRegister(serverWorld);
                this.insertInventoryEntityTracker = null;
                this.insertInventoryEntityBox = null;
                this.insertInventoryEntityFailedSearchTime = 0L;
            }
        }
        this.invalidateBlockInsertionData();
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

    private void invalidateExtractionData() {
        if (this.world instanceof ServerWorld serverWorld) {
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
        this.invalidateBlockExtractionData();
    }

    private void checkSleepingConditions() {
        if (this.needsCooldown()) {
            return;
        }
        if (!this.shouldCheckSleep) {
            this.shouldCheckSleep = true;
            return;
        }
        //TODO check sleeping conditions less often, otherwise this might be quite expensive
        if (this instanceof SleepingBlockEntity thisSleepingBlockEntity) {
            if (this instanceof InventoryChangeTracker thisTracker) {
                boolean listenToExtractTracker = false;
                boolean listenToInsertTracker = false;
                boolean listenToExtractEntities = false;
                boolean listenToInsertEntities = false;

                CanaryStackList thisStackList = InventoryHelper.getCanaryStackList(this);

                if (this.extractionMode != HopperCachingState.BlockInventory.BLOCK_STATE && thisStackList.getFullSlots() != thisStackList.size()) {
                    if (this.extractionMode == HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY) {
                        Inventory blockInventory = this.extractBlockInventory;
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
                        Inventory blockInventory = this.insertBlockInventory;
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
                        this.initInsertInventoryTracker(this.world, this.getCachedState());
                    }
                    this.insertInventoryEntityTracker.listenToEntityMovementOnce(this);
                }
                if (listenToExtractEntities) {
                    if (this.extractInventoryEntityTracker == null) {
                        this.initExtractInventoryTracker(this.world);
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
    public void handleStackListReplaced(Inventory inventory) {
        if (this instanceof SleepingBlockEntity) {
            ((SleepingBlockEntity) this).wakeUpNow();
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
    public void handleInventoryContentModified(Inventory inventory) {
        if (this instanceof SleepingBlockEntity sleepingBlockEntity) {
            sleepingBlockEntity.wakeUpNow();
        }
    }

    @Override
    public void handleInventoryRemoved(Inventory inventory) {
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
    public void handleComparatorAdded(Inventory inventory) {
        if (inventory == this.extractBlockInventory && this instanceof SleepingBlockEntity sleepingBlockEntity) {
            sleepingBlockEntity.wakeUpNow();
        }
    }

    @Override
    public void handleEntityMovement(Class<?> category) {
        if (this instanceof SleepingBlockEntity sleepingBlockEntity) {
            sleepingBlockEntity.wakeUpNow();
        }
    }
}
