package com.abdelaziz.canary.common.compat;

import net.minecraftforge.fml.ModList;

public class WorldEditCompat {
    public static final boolean WORLD_EDIT_PRESENT = ModList.get().isLoaded("worldedit");
}
