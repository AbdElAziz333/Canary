package com.abdelaziz.canary.common.entity.pushable;

import com.abdelaziz.canary.common.entity.EntityClassGroup;
import com.abdelaziz.canary.common.reflection.ReflectionUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

public class PushableEntityClassGroup {

    /**
     * Contains Entity Classes that use {@link LivingEntity#isPushable()} ()} to determine their pushability state
     * and use {@link LivingEntity#isClimbing()} to determine their climbing state and are never spectators (no players).
     * <p>
     * LivingEntity, but not Players and not Subclasses with different pushability calculations
     */
    public static final EntityClassGroup CACHABLE_UNPUSHABILITY;

    /**
     * Entities that might be pushable or are always pushable.
     * <p>
     * Pushable subclasses of ArmorStandEntity and BatEntity, Minecarts, Boats, LivingEntity. Not EnderDragonEntity
     */
    public static final EntityClassGroup MAYBE_PUSHABLE;

    static {
        String remapped_isClimbing = ObfuscationReflectionHelper.findMethod(Entity.class, "m_6147_", new Class[]{Entity.class}).getName();
        String remapped_isPushable = ObfuscationReflectionHelper.findMethod(Entity.class, "m_6094_", new Class[]{Entity.class}).getName();
                CACHABLE_UNPUSHABILITY = new EntityClassGroup(
                (Class<?> entityClass) -> {
                    if (LivingEntity.class.isAssignableFrom(entityClass) && !Player.class.isAssignableFrom(entityClass)) {
                        if (!ReflectionUtil.hasMethodOverride(entityClass, LivingEntity.class, true, remapped_isPushable)) {
                            if (!ReflectionUtil.hasMethodOverride(entityClass, LivingEntity.class, true, remapped_isClimbing)) {
                                return true;
                            }
                        }
                    }
                    return false;
                });
        MAYBE_PUSHABLE = new EntityClassGroup(
                (Class<?> entityClass) -> {
                    if (ReflectionUtil.hasMethodOverride(entityClass, Entity.class, true, remapped_isPushable)) {
                        if (EnderDragon.class.isAssignableFrom(entityClass)) {
                            return false;
                        }
                        if (ArmorStand.class.isAssignableFrom(entityClass)) {
                            return ReflectionUtil.hasMethodOverride(entityClass, ArmorStand.class, true, remapped_isPushable);
                        }
                        if (Bat.class.isAssignableFrom(entityClass)) {
                            return ReflectionUtil.hasMethodOverride(entityClass, Bat.class, true, remapped_isPushable);
                        }
                        return true;
                    }
                    if (Player.class.isAssignableFrom(entityClass)) {
                        return true;
                    }
                    return false;
                });
    }
}
