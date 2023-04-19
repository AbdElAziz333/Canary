package com.abdelaziz.canary.common.compat;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;

public class WorldEditCompat {
    public static final boolean WORLD_EDIT_PRESENT = ModList.get().isLoaded("worldedit");
}
