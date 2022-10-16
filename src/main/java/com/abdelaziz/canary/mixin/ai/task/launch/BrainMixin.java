package com.abdelaziz.canary.mixin.ai.task.launch;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import com.abdelaziz.canary.common.util.collections.MaskedList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;
import java.util.function.Supplier;

@Mixin(Brain.class)
public class BrainMixin<E extends LivingEntity> {

    @Shadow
    @Final
    private Map<Integer, Map<Activity, Set<Behavior<? super E>>>> availableBehaviorsByPriority;

    @Shadow
    @Final
    private Set<Activity> activeActivities;

    private ArrayList<Behavior<? super E>> possibleTasks;
    private MaskedList<Behavior<? super E>> runningTasks;

    private void onTasksChanged() {
        this.runningTasks = null;
        this.onPossibleActivitiesChanged();
    }

    private void onPossibleActivitiesChanged() {
        this.possibleTasks = null;
    }

    private void initPossibleTasks() {
        this.possibleTasks = new ArrayList<>();
        for (Map<Activity, Set<Behavior<? super E>>> map : this.availableBehaviorsByPriority.values()) {
            for (Map.Entry<Activity, Set<Behavior<? super E>>> entry : map.entrySet()) {
                Activity activity = entry.getKey();
                if (!this.activeActivities.contains(activity)) {
                    continue;
                }
                Set<Behavior<? super E>> set = entry.getValue();
                for (Behavior<? super E> task : set) {
                    //noinspection UseBulkOperation
                    this.possibleTasks.add(task);
                }
            }
        }
    }

    private ArrayList<Behavior<? super E>> getPossibleTasks() {
        if (this.possibleTasks == null) {
            this.initPossibleTasks();
        }
        return this.possibleTasks;
    }

    private MaskedList<Behavior<? super E>> getCurrentlyRunningTasks() {
        if (this.runningTasks == null) {
            this.initCurrentlyRunningTasks();
        }
        return this.runningTasks;
    }

    private void initCurrentlyRunningTasks() {
        MaskedList<Behavior<? super E>> list = new MaskedList<>(new ObjectArrayList<>(), false);

        for (Map<Activity, Set<Behavior<? super E>>> map : this.availableBehaviorsByPriority.values()) {
            for (Set<Behavior<? super E>> set : map.values()) {
                for (Behavior<? super E> task : set) {
                    list.addOrSet(task, task.getStatus() == Behavior.Status.RUNNING);
                }
            }
        }
        this.runningTasks = list;
    }

    /**
     * @author 2No2Name
     * @reason use optimized cached collection
     */
    @Overwrite
    private void startEachNonRunningBehavior(ServerLevel world, E entity) {
        long startTime = world.getGameTime();
        for (Behavior<? super E> task : this.getPossibleTasks()) {
            if (task.getStatus() == Behavior.Status.STOPPED) {
                task.tryStart(world, entity, startTime);
            }
        }
    }

    /**
     * @author 2No2Name
     * @reason use optimized cached collection
     */
    @Overwrite
    @Deprecated
    @Debug
    public List<Behavior<? super E>> getRunningBehaviors() {
        return this.getCurrentlyRunningTasks();
    }


    @Inject(
            method = "<init>(Ljava/util/Collection;Ljava/util/Collection;Lcom/google/common/collect/ImmutableList;Ljava/util/function/Supplier;)V",
            at = @At("RETURN")
    )
    private void reinitializeBrainCollections(Collection<?> memories, Collection<?> sensors, ImmutableList<?> memoryEntries, Supplier<?> codecSupplier, CallbackInfo ci) {
        this.onTasksChanged();
    }

    @Inject(
            method = "addActivityAndRemoveMemoriesWhenStopped(Lnet/minecraft/world/entity/schedule/Activity;Lcom/google/common/collect/ImmutableList;Ljava/util/Set;Ljava/util/Set;)V",
            at = @At("RETURN")
    )
    private void reinitializeTasksSorted(Activity activity, ImmutableList<? extends Pair<Integer, ? extends Behavior<?>>> indexedTasks, Set<Pair<MemoryModuleType<?>, MemoryStatus>> requiredMemories, Set<MemoryModuleType<?>> forgettingMemories, CallbackInfo ci) {
        this.onTasksChanged();
    }

    @Inject(
            method = "removeAllBehaviors()V",
            at = @At("RETURN")
    )
    private void reinitializeTasksSorted(CallbackInfo ci) {
        this.onTasksChanged();
    }

    @Inject(
            method = "setActiveActivity(Lnet/minecraft/world/entity/schedule/Activity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Set;add(Ljava/lang/Object;)Z",
                    shift = At.Shift.AFTER
            )
    )
    private void onPossibleActivitiesChanged(Activity except, CallbackInfo ci) {
        this.onPossibleActivitiesChanged();
    }


    @Inject(
            method = "stopAll",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/behavior/Behavior;doStop(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;J)V"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void removeStoppedTask(ServerLevel world, E entity, CallbackInfo ci, long l, Iterator<?> it, Behavior<? super E> task) {
        if (this.runningTasks != null) {
            this.runningTasks.setVisible(task, false);
        }
    }

    @Inject(
            method = "tickEachRunningBehavior",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/behavior/Behavior;tickOrStop(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;J)V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void removeTaskIfStopped(ServerLevel world, E entity, CallbackInfo ci, long l, Iterator<?> it, Behavior<? super E> task) {
        if (this.runningTasks != null && task.getStatus() != Behavior.Status.RUNNING) {
            this.runningTasks.setVisible(task, false);
        }
    }

    @ModifyVariable(
            method = "startEachNonRunningBehavior",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/behavior/Behavior;tryStart(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;J)Z",
                    shift = At.Shift.AFTER
            )
    )
    private Behavior<? super E> addStartedTasks(Behavior<? super E> task) {
        if (this.runningTasks != null && task.getStatus() == Behavior.Status.RUNNING) {
            this.runningTasks.setVisible(task, true);
        }
        return task;
    }
}