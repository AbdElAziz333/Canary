package com.abdelaziz.canary.mixin.block.hopper;

import com.abdelaziz.canary.api.inventory.CanaryInventory;
import com.abdelaziz.canary.common.hopper.*;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.minecraft.block.entity.HopperBlockEntity.getInputItemEntities;


@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin extends BlockEntity implements Hopper, UpdateReceiver, CanaryInventory {

    //The currently used block inventories
    @Nullable
    private Inventory insertBlockInventory, extractBlockInventory;
    //Block Entity removal count used for Inventory Cache invalidation
    //Compatible with adding the same BlockEntity to the world after removing it (Movable BlockEntities)
    private int insertBlockEntityRemovedCount, extractBlockEntityRemovedCount;
    @Shadow
    private long lastTickTime;
    //The currently used inventories (optimized type, if not present, skip optimizations)
    @Nullable
    private CanaryInventory insertInventory, extractInventory;
    private long myModCountAtLastInsert, myModCountAtLastExtract, myModCountAtLastItemCollect;
    private HopperCachingState.BlockInventory insertionMode = HopperCachingState.BlockInventory.UNKNOWN;
    private HopperCachingState.BlockInventory extractionMode = HopperCachingState.BlockInventory.UNKNOWN;
    @Nullable //Null iff corresp. CanaryInventory field is null
    private CanaryStackList insertStackList, extractStackList;
    //item pickup bounding boxes in order. The last box in the array is the box that encompasses all of the others
    private Box[] collectItemEntityBoxes;
    private SectionedInventoryEntityMovementTracker<Inventory> extractInventoryEntityTracker;
    private Box extractInventoryEntityBox;
    //Mod count used to avoid transfer attempts that are known to fail (no change since last attempt)
    private long insertStackListModCount, extractStackListModCount;
    private SectionedItemEntityMovementTracker<ItemEntity> collectItemEntityTracker;
    private boolean collectItemEntityTrackerWasEmpty;
    private SectionedInventoryEntityMovementTracker<Inventory> insertInventoryEntityTracker;
    private long collectItemEntityAttemptTime;
    private Box insertInventoryEntityBox;
    private long extractInventoryEntityFailedSearchTime;
    private long insertInventoryEntityFailedSearchTime;

    public HopperBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow
    @Nullable
    private static native Inventory getInputInventory(World world, Hopper hopper);

    @Shadow
    private static native boolean ejectItems(World world, BlockPos pos, BlockState state, HopperBlockEntity inventory);

    @Shadow
    private static native boolean canExtract(Inventory inv, ItemStack stack, int slot, Direction facing);

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
            return null;
        }
        hopperBlockEntity.extractInventoryEntityFailedSearchTime = Long.MIN_VALUE;

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
                //not caching the inventory (hopperBlockEntity.extractBlockInventory == USE_ENTITY_INVENTORY prevents it)
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
            if (hopperBlockEntity.insertStackList.getModCount() == hopperBlockEntity.insertStackListModCount) {
//                ComparatorUpdatePattern.NO_UPDATE.apply(hopperBlockEntity, hopperStackList); //commented because it's a noop, Hoppers do not send useless comparator updates
                return false;
            }
        }

        boolean insertInventoryWasEmptyHopperNotDisabled = insertInventory instanceof HopperBlockEntityMixin && !((HopperBlockEntityMixin) insertInventory).isDisabled() && hopperBlockEntity.insertStackList.getOccupiedSlots() == 0;
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
        if (from != hopperBlockEntity.extractInventory) {
            return; //from inventory is not an optimized inventory, vanilla fallback
        }

        CanaryStackList hopperStackList = InventoryHelper.getCanaryStackList(hopperBlockEntity);
        CanaryStackList fromStackList = hopperBlockEntity.extractStackList;

        if (hopperStackList.getModCount() == hopperBlockEntity.myModCountAtLastExtract) {
            if (fromStackList.getModCount() == hopperBlockEntity.extractStackListModCount) {
                //noinspection CollectionAddedToSelf
                fromStackList.runComparatorUpdatePatternOnFailedExtract(fromStackList, from);
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
            return Collections.emptyList();
        }
        hopperBlockEntity.myModCountAtLastItemCollect = modCount;

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

    @Shadow
    protected abstract boolean isDisabled();

    @Shadow
    protected abstract void setTransferCooldown(int cooldown);

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

    /**
     * Makes this hopper remember the given inventory.
     *
     * @param insertInventory Block inventory / Blockentity inventory to be remembered
     */
    private void cacheInsertBlockInventory(Inventory insertInventory) {
        assert !(insertInventory instanceof Entity);
        if (insertInventory instanceof BlockEntity || insertInventory instanceof DoubleInventory) {
            this.insertBlockInventory = insertInventory;
            this.insertionMode = HopperCachingState.BlockInventory.BLOCK_ENTITY;
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
            if (this.insertionMode == HopperCachingState.BlockInventory.BLOCK_ENTITY) {
                this.insertBlockEntityRemovedCount = ((RemovalCounter) insertInventory).getRemovedCountCanary();
            } else {
                this.insertBlockEntityRemovedCount = 0;
            }
        }
    }

    private void cacheInsertCanaryInventory(CanaryInventory optimizedInventory) {
        this.insertInventory = optimizedInventory;
        CanaryStackList insertInventoryStackList = InventoryHelper.getCanaryStackList(optimizedInventory);
        this.insertStackList = insertInventoryStackList;
        this.insertStackListModCount = insertInventoryStackList.getModCount() - 1;
        this.insertBlockEntityRemovedCount = optimizedInventory.getRemovedCountCanary();
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
            this.extractionMode = HopperCachingState.BlockInventory.BLOCK_ENTITY;
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
            this.extractBlockEntityRemovedCount = optimizedInventory.getRemovedCountCanary();
        } else {
            this.extractInventory = null;
            this.extractStackList = null;
            this.extractStackListModCount = 0;
            if (this.extractionMode == HopperCachingState.BlockInventory.BLOCK_ENTITY) {
                this.extractBlockEntityRemovedCount = ((RemovalCounter) extractInventory).getRemovedCountCanary();
            } else {
                this.extractBlockEntityRemovedCount = 0;
            }
        }
    }

    public Inventory getExtractBlockInventory(World world) {
        Inventory blockInventory = this.extractBlockInventory;
        if (this.extractionMode == HopperCachingState.BlockInventory.NO_BLOCK_INVENTORY) {
            return null;
        } else if (this.extractionMode == HopperCachingState.BlockInventory.BLOCK_STATE) {
            return blockInventory;
        } else if (this.extractionMode == HopperCachingState.BlockInventory.BLOCK_ENTITY) {
            if (((RemovalCounter) blockInventory).getRemovedCountCanary() == this.extractBlockEntityRemovedCount) {
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
        } else if (this.insertionMode == HopperCachingState.BlockInventory.BLOCK_ENTITY) {
            if (((RemovalCounter) blockInventory).getRemovedCountCanary() == this.insertBlockEntityRemovedCount) {
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


    public Inventory getInsertInventory(World world, BlockState hopperState) {
        Inventory blockInventory = this.getInsertBlockInventory(world, hopperState);
        if (blockInventory != null) {
            return blockInventory;
        }

        if (this.insertInventoryEntityTracker == null) {
            this.initInsertInventoryTracker(world, hopperState);
        }
        if (this.insertInventoryEntityTracker.isUnchangedSince(this.insertInventoryEntityFailedSearchTime)) {
            return null;
        }
        this.insertInventoryEntityFailedSearchTime = Long.MIN_VALUE;

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

    //Entity tracker initialization:

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

    //Cached data invalidation:

    @SuppressWarnings("deprecation")
    @Override
    public void setCachedState(BlockState state) {
        BlockState cachedState = this.getCachedState();
        super.setCachedState(state);
        if (state.get(HopperBlock.FACING) != cachedState.get(HopperBlock.FACING)) {
            this.invalidateCachedData();
        }
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        this.invalidateCachedData();
    }

    private void invalidateCachedData() {
        this.invalidateInsertionData();
        this.invalidateExtractionData();
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

    private void invalidateBlockInsertionData() {
        this.insertionMode = HopperCachingState.BlockInventory.UNKNOWN;
        this.insertBlockInventory = null;
        this.insertInventory = null;
        this.insertBlockEntityRemovedCount = 0;
        this.insertStackList = null;
        this.insertStackListModCount = 0;

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

    private void invalidateBlockExtractionData() {
        this.extractionMode = HopperCachingState.BlockInventory.UNKNOWN;
        this.extractBlockInventory = null;
        this.extractInventory = null;
        this.extractBlockEntityRemovedCount = 0;
        this.extractStackList = null;
        this.extractStackListModCount = 0;
    }
}
