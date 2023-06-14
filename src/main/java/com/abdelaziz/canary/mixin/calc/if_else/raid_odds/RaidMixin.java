package com.abdelaziz.canary.mixin.calc.if_else.raid_odds;

import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Raid.class, priority = 999)
public abstract class RaidMixin {
    @Shadow public abstract int getBadOmenLevel();

    /**
     * @reason replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public float getEnchantOdds() {
        int i = this.getBadOmenLevel();
        switch(i) {
            case 2 -> {
                return 0.1F;
            }
            case 3 -> {
                return 0.25F;
            }
            case 4 -> {
                return 0.5F;
            }
            case 5 -> {
                return 0.75F;
            }
            default -> {
                return 0.0F;
            }
        }
    }
}
