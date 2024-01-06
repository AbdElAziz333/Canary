package com.abdelaziz.canary.mixin;

import com.abdelaziz.canary.common.Canary;
import com.abdelaziz.canary.common.config.CanaryConfig;
import com.abdelaziz.canary.common.config.Option;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.File;
import java.util.List;
import java.util.Set;

public class CanaryMixinPlugin implements IMixinConfigPlugin {
    private static final String MIXIN_PACKAGE_ROOT = "com.abdelaziz.canary.mixin.";

    private final Logger logger = LogManager.getLogger("Canary");

    private CanaryConfig config;

    @Override
    public void onLoad(String mixinPackage) {
        try {
            this.config = CanaryConfig.load(new File("./config/canary.properties"));
        } catch (Exception e) {
            throw new RuntimeException("Could not load configuration file for Canary", e);
        }

        this.logger.info("Loaded configuration file for Canary: {} options available, {} override(s) found",
                this.config.getOptionCount(), this.config.getOptionOverrideCount());

        Canary.CONFIG = this.config;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        //Ferritecore adds a better optimization for neighbor table, disable the StateMixin when ferritecore is installed
        if (mixinClassName.startsWith(MIXIN_PACKAGE_ROOT + "alloc.blockstate") && (FMLLoader.getLoadingModList().getModFileById("ferritecore") != null)) {
            return false;
        }

        //For now disable this until running some tests
        if (mixinClassName.startsWith(MIXIN_PACKAGE_ROOT + "shapes.lazy_shape_context") && (FMLLoader.getLoadingModList().getModFileById("the_bumblezone") != null)) {
            return false;
        }

        if (mixinClassName.startsWith(MIXIN_PACKAGE_ROOT + "block.hopper") && (FMLLoader.getLoadingModList().getModFileById("easyvillagers")) != null) {
            return false;
        }

        if ((mixinClassName.startsWith(MIXIN_PACKAGE_ROOT + "shapes") || (mixinClassName.startsWith(MIXIN_PACKAGE_ROOT + "math.sine_lut")) || (mixinClassName.startsWith(MIXIN_PACKAGE_ROOT + "alloc.block_state"))) && !LoadingModList.get().getErrors().isEmpty()) {
            return false;
        }

        if (!mixinClassName.startsWith(MIXIN_PACKAGE_ROOT)) {
            this.logger.error("Expected mixin '{}' to start with package root '{}', treating as foreign and " +
                    "disabling!", mixinClassName, MIXIN_PACKAGE_ROOT);

            return false;
        }

        String mixin = mixinClassName.substring(MIXIN_PACKAGE_ROOT.length());
        Option option = this.config.getEffectiveOptionForMixin(mixin);

        if (option == null) {
            this.logger.error("No rules matched mixin '{}', treating as foreign and disabling!", mixin);

            return false;
        }

        if (option.isOverridden()) {
            String source = "[unknown]";

            if (option.isUserDefined()) {
                source = "user configuration";
            } else if (option.isModDefined()) {
                source = "mods [" + String.join(", ", option.getDefiningMods()) + "]";
            }

            if (option.isEnabled()) {
                this.logger.warn("Force-enabling mixin '{}' as rule '{}' (added by {}) enables it", mixin,
                        option.getName(), source);
            } else {
                this.logger.warn("Force-disabling mixin '{}' as rule '{}' (added by {}) disables it and children", mixin,
                        option.getName(), source);
            }
        }

        return option.isEnabled();
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}