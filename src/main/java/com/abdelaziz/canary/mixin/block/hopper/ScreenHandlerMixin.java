package com.abdelaziz.canary.mixin.block.hopper;

import com.abdelaziz.canary.api.inventory.CanaryInventory;
import com.abdelaziz.canary.common.hopper.InventoryHelper;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.inventory.AbstractContainerMenu.class)
public abstract class ScreenHandlerMixin {
    @Inject(method = "getRedstoneSignalFromContainer(Lnet/minecraft/world/Container;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;getContainerSize()I", shift = At.Shift.BEFORE, ordinal = 0), cancellable = true)
    private static void getFastOutputStrength(Container inventory, CallbackInfoReturnable<Integer> cir) {
        if (inventory instanceof CanaryInventory optimizedInventory) {
            cir.setReturnValue(InventoryHelper.getCanaryStackList(optimizedInventory).getSignalStrength(inventory));
        }
    }
}
