package com.abdelaziz.canary.common;

import com.abdelaziz.canary.common.config.CanaryConfig;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(Canary.MODID)
public class Canary {
    public static final String MODID = "canary";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static CanaryConfig CONFIG;

    public Canary() {
        MinecraftForge.EVENT_BUS.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        if (CONFIG == null) {
            throw new IllegalStateException("The mixin plugin did not initialize the config! Did it not load?");
        }
    }
}