package com.abdelaziz.canary.mixin.util.entity_section_position;

import com.abdelaziz.canary.common.entity.PositionedEntityTrackingSection;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntitySectionStorage.class)
public class EntitySectionStorageMixin<T extends EntityAccess> {
    @Inject(method = "createSection(J)Lnet/minecraft/world/level/entity/EntitySection;", at = @At("RETURN"))
    private void rememberPos(long sectionPos, CallbackInfoReturnable<EntitySection<T>> cir) {
        ((PositionedEntityTrackingSection) cir.getReturnValue()).setPos(sectionPos);
    }
}
