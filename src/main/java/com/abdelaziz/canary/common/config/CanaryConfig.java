package com.abdelaziz.canary.common.config;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Documentation of these options: https://github.com/AbdElAziz333/Canary/wiki/Configuration-File
 */
@SuppressWarnings("CanBeFinal")
public class CanaryConfig {
    private static final Logger LOGGER = LogManager.getLogger("CanaryConfig");

    private static final String JSON_KEY_CANARY_OPTIONS = "canary:options";

    private final Map<String, Option> options = new HashMap<>();
    private final Set<Option> optionsWithDependencies = new ObjectLinkedOpenHashSet<>();

    private CanaryConfig() {
        // Defines the default rules which can be configured by the user or other mods.
        // You must manually add a rule for any new mixins not covered by an existing package rule.

        this.addMixinRule("ai", true);
        this.addMixinRule("ai.nearby_entity_tracking", false);
        this.addMixinRule("ai.nearby_entity_tracking.goals", true);
        this.addMixinRule("ai.pathing", true);
        this.addMixinRule("ai.poi", true);
        this.addMixinRule("ai.poi.fast_portals", true);
        this.addMixinRule("ai.poi.tasks", true);
        this.addMixinRule("ai.raid", true);
        this.addMixinRule("ai.sensor", true);
        this.addMixinRule("ai.sensor.secondary_poi", true);
        this.addMixinRule("ai.task", true);
        this.addMixinRule("ai.task.launch", true);
        this.addMixinRule("ai.task.memory_change_counting", true);
        this.addMixinRule("ai.task.replace_streams", true);

        this.addMixinRule("alloc", true);
        this.addMixinRule("alloc.blockstate", true);
        this.addMixinRule("alloc.chunk_random", true);
        this.addMixinRule("alloc.chunk_ticking", true);
        this.addMixinRule("alloc.composter", true);
        this.addMixinRule("alloc.deep_passengers", true);
        this.addMixinRule("alloc.entity_tracker", true);
        this.addMixinRule("alloc.enum_values", true);
        this.addMixinRule("alloc.explosion_behavior", true);
        this.addMixinRule("alloc.nbt", true);

        this.addMixinRule("block", true);
        this.addMixinRule("block.flatten_states", true);
        this.addMixinRule("block.hopper", true);
        this.addMixinRule("block.moving_block_shapes", true);
        this.addMixinRule("block.redstone_wire", true);

        this.addMixinRule("cached_hashcode", true);

        this.addMixinRule("chunk", true);
        this.addMixinRule("chunk.block_counting", true);
        this.addMixinRule("chunk.entity_class_groups", true);
        this.addMixinRule("chunk.no_locking", true);
        this.addMixinRule("chunk.no_validation", true);
        this.addMixinRule("chunk.palette", true);
        this.addMixinRule("chunk.serialization", true);

        this.addMixinRule("collections", true);
        this.addMixinRule("collections.attributes", true);
        this.addMixinRule("collections.block_entity_tickers", true);
        this.addMixinRule("collections.brain", true);
        this.addMixinRule("collections.entity_by_type", true);
        this.addMixinRule("collections.entity_filtering", true);
        this.addMixinRule("collections.entity_ticking", true);
        this.addMixinRule("collections.gamerules", true);
        this.addMixinRule("collections.goals", true);
        this.addMixinRule("collections.mob_spawning", true);

        this.addMixinRule("entity", true);
        this.addMixinRule("entity.collisions", true);
        this.addMixinRule("entity.collisions.intersection", true);
        this.addMixinRule("entity.collisions.movement", true);
        this.addMixinRule("entity.collisions.suffocation", true);
        this.addMixinRule("entity.collisions.unpushable_cramming", true);
        this.addMixinRule("entity.data_tracker", true);
        this.addMixinRule("entity.data_tracker.no_locks", true);
        this.addMixinRule("entity.data_tracker.use_arrays", true);
        this.addMixinRule("entity.fast_elytra_check", true);
        this.addMixinRule("entity.fast_hand_swing", true);
        this.addMixinRule("entity.fast_powder_snow_check", true);
        this.addMixinRule("entity.fast_retrieval", true);
        this.addMixinRule("entity.inactive_navigations", true);
        this.addMixinRule("entity.replace_entitytype_predicates", true);
        this.addMixinRule("entity.skip_equipment_change_check", true);
        this.addMixinRule("entity.skip_fire_check", true);

        this.addMixinRule("gen", true);
        this.addMixinRule("gen.cached_generator_settings", true);
        this.addMixinRule("gen.chunk_region", true);

        this.addMixinRule("item", true);

        this.addMixinRule("math", true);
        this.addMixinRule("math.fast_blockpos", true);
        this.addMixinRule("math.fast_util", true);
        this.addMixinRule("math.sine_lut", true);

        this.addMixinRule("profiler", true);

        this.addMixinRule("shapes", true);
        this.addMixinRule("shapes.blockstate_cache", true);
        this.addMixinRule("shapes.lazy_shape_context", true);
        this.addMixinRule("shapes.optimized_matching", true);
        this.addMixinRule("shapes.precompute_shape_arrays", true);
        this.addMixinRule("shapes.shape_merging", true);
        this.addMixinRule("shapes.specialized_shapes", true);

        this.addMixinRule("util", true);
        this.addMixinRule("util.entity_movement_tracking", true);
        this.addMixinRule("util.entity_section_position", true);
        this.addMixinRule("util.world_border_listener", true);
        this.addMixinRule("util.inventory_change_listening", true);
        this.addMixinRule("util.inventory_comparator_tracking", true);

        this.addMixinRule("world", true);
        this.addMixinRule("world.block_entity_retrieval", true);
        this.addMixinRule("world.block_entity_ticking", true);
        this.addMixinRule("world.block_entity_ticking.sleeping", true);
        this.addMixinRule("world.block_entity_ticking.sleeping.brewing_stand", true);
        this.addMixinRule("world.block_entity_ticking.sleeping.campfire", true);
        this.addMixinRule("world.block_entity_ticking.sleeping.furnace", true);
        this.addMixinRule("world.block_entity_ticking.sleeping.hopper", true);
        this.addMixinRule("world.block_entity_ticking.sleeping.shulker_box", true);
        this.addMixinRule("world.block_entity_ticking.support_cache", false); //have to check whether the cached state bugfix fixes any detectable vanilla bugs first
        this.addMixinRule("world.block_entity_ticking.world_border", true);
        this.addMixinRule("world.chunk_access", true);
        this.addMixinRule("world.chunk_tickets", true);
        this.addMixinRule("world.chunk_ticking", true);
        this.addMixinRule("world.combined_heightmap_update", true);
        this.addMixinRule("world.explosions", true);
        this.addMixinRule("world.inline_block_access", true);
        this.addMixinRule("world.inline_height", true);
        this.addMixinRule("world.player_chunk_tick", true);
        this.addMixinRule("world.tick_scheduler", true);

        this.addRuleDependency("ai.nearby_entity_tracking", "util", true);
        this.addRuleDependency("ai.nearby_entity_tracking", "util.entity_section_position", true);
        this.addRuleDependency("block.hopper", "util", true);
        this.addRuleDependency("block.hopper", "util.entity_movement_tracking", true);
        this.addRuleDependency("block.hopper", "world", true);
        this.addRuleDependency("block.hopper", "world.block_entity_retrieval", true);
        this.addRuleDependency("block.hopper", "util.inventory_change_listening", true);

        this.addRuleDependency("util.inventory_comparator_tracking", "world.block_entity_retrieval", true);

        this.addRuleDependency("util.entity_movement_tracking", "util.entity_section_position", true);

        this.addRuleDependency("entity.collisions.unpushable_cramming", "chunk.entity_class_groups", true);

        this.addRuleDependency("world.block_entity_ticking.world_border", "util.world_border_listener", true);

    }

    /**
     * Loads the configuration file from the specified location. If it does not exist, a new configuration file will be
     * created. The file on disk will then be updated to include any new options.
     */
    public static CanaryConfig load(File file) {
        CanaryConfig config = new CanaryConfig();

        if (file.exists()) {
            Properties props = new Properties();

            try (FileInputStream fin = new FileInputStream(file)) {
                props.load(fin);
            } catch (IOException e) {
                throw new RuntimeException("Could not load config file", e);
            }

            config.readProperties(props);
        } else {
            try {
                writeDefaultConfig(file);
            } catch (IOException e) {
                LOGGER.warn("Could not write default configuration file", e);
            }
        }

        config.applyModOverrides();

        // Check dependencies several times, because one iteration may disable a rule required by another rule
        // This terminates because each additional iteration will disable one or more rules, and there is only a finite number of rules
        //noinspection StatementWithEmptyBody
        while (config.applyDependencies()) {
            //noinspection UnnecessarySemicolon
            ;
        }

        return config;
    }

    /**
     * Defines a dependency between two registered mixin rules. If a dependency is not satisfied, the mixin will
     * be disabled.
     *
     * @param rule          the mixin rule that requires another rule to be set to a given value
     * @param dependency    the mixin rule the given rule depends on
     * @param requiredValue the required value of the dependency
     */
    @SuppressWarnings("SameParameterValue")
    private void addRuleDependency(String rule, String dependency, boolean requiredValue) {
        String ruleOptionName = getMixinRuleName(rule);
        Option option = this.options.get(ruleOptionName);
        if (option == null) {
            LOGGER.error("Option {} for dependency '{} depends on {}={}' not found. Skipping.", rule, rule, dependency, requiredValue);
            return;
        }
        String dependencyOptionName = getMixinRuleName(dependency);
        Option dependencyOption = this.options.get(dependencyOptionName);
        if (dependencyOption == null) {
            LOGGER.error("Option {} for dependency '{} depends on {}={}' not found. Skipping.", dependency, rule, dependency, requiredValue);
            return;
        }
        option.addDependency(dependencyOption, requiredValue);
        this.optionsWithDependencies.add(option);
    }


    /**
     * Defines a Mixin rule which can be configured by users and other mods.
     *
     * @param mixin   The name of the mixin package which will be controlled by this rule
     * @param enabled True if the rule will be enabled by default, otherwise false
     * @throws IllegalStateException If a rule with that name already exists
     */
    private void addMixinRule(String mixin, boolean enabled) {
        String name = getMixinRuleName(mixin);

        if (this.options.putIfAbsent(name, new Option(name, enabled, false)) != null) {
            throw new IllegalStateException("Mixin rule already defined: " + mixin);
        }
    }

    private void readProperties(Properties props) {
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();

            Option option = this.options.get(key);

            if (option == null) {
                LOGGER.warn("No configuration key exists with name '{}', ignoring", key);
                continue;
            }

            boolean enabled;

            if (value.equalsIgnoreCase("true")) {
                enabled = true;
            } else if (value.equalsIgnoreCase("false")) {
                enabled = false;
            } else {
                LOGGER.warn("Invalid value '{}' encountered for configuration key '{}', ignoring", value, key);
                continue;
            }

            option.setEnabled(enabled, true);
        }
    }

    private static void writeDefaultConfig(File file) throws IOException {
        File dir = file.getParentFile();

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Could not create parent directories");
            }
        } else if (!dir.isDirectory()) {
            throw new IOException("The parent file is not a directory");
        }

        try (Writer writer = new FileWriter(file)) {
            writer.write("# This is the configuration file for Canary.\n");
            writer.write("# This file exists for debugging purposes and should not be configured otherwise.\n");
            writer.write("# Before configuring anything, take a backup of the worlds that will be opened.\n");
            writer.write("#\n");
            writer.write("# You can find information on editing this file and all the available options here:\n");
            writer.write("# https://github.com/AbdElAziz333/Canary/wiki/Configuration-File\n");
            writer.write("#\n");
            writer.write("# By default, this file will be empty except for this notice.\n");
        }
    }

    // Ported from MaxNeedsSnacks/roadrunner

    private void applyModOverrides() {
        for (ModInfo mod : LoadingModList.get().getMods()) {
            String modid = mod.getModId();
            Path path = mod.getOwningFile().getFile().findResource("canary.overrides.properties");
            if (Files.exists(path)) {
                Properties props = new Properties();

                try (InputStream stream = Files.newInputStream(path)) {
                    props.load(stream);
                } catch (IOException e) {
                    LOGGER.warn("Could not load overrides file for mod '{}', ignoring", modid);
                    continue;
                }

                for (Map.Entry<Object, Object> entry : props.entrySet()) {
                    applyModOverride(modid, entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }
    }

    /**
     * Returns the effective option for the specified class name. This traverses the package path of the given mixin
     * and checks each root for configuration rules. If a configuration rule disables a package, all mixins located in
     * that package and its children will be disabled. The effective option is that of the highest-priority rule, either
     * a enable rule at the end of the chain or a disable rule at the earliest point in the chain.
     *
     * @return Null if no options matched the given mixin name, otherwise the effective option for this Mixin
     */
    public Option getEffectiveOptionForMixin(String mixinClassName) {
        int lastSplit = 0;
        int nextSplit;

        Option rule = null;

        while ((nextSplit = mixinClassName.indexOf('.', lastSplit)) != -1) {
            String key = getMixinRuleName(mixinClassName.substring(0, nextSplit));

            Option candidate = this.options.get(key);

            if (candidate != null) {
                rule = candidate;

                if (!rule.isEnabled()) {
                    return rule;
                }
            }

            lastSplit = nextSplit + 1;
        }

        return rule;
    }

    /**
     * Tests all dependencies and disables options when their dependencies are not met.
     */
    private boolean applyDependencies() {
        boolean changed = false;
        for (Option optionWithDependency : this.optionsWithDependencies) {

            changed |= optionWithDependency.disableIfDependenciesNotMet(LOGGER, this);
        }
        return changed;
    }

    private void applyModOverride(String modid, String name, String value) {
        Option option = this.options.get(name);

        if (option == null) {
            LOGGER.warn("Mod '{}' attempted to override option '{}', which doesn't exist, ignoring", modid, name);
            return;
        }

        boolean enabled = Boolean.parseBoolean(value);

        if (!value.equals(Boolean.toString(enabled))) {
            LOGGER.warn("Mod '{}' attempted to override option '{}' with an invalid value, ignoring", modid, name);
            return;
        }

        // disabling the option takes precedence over enabling
        if (!enabled && option.isEnabled()) {
            option.clearModsDefiningValue();
        }

        if (!enabled || option.isEnabled() || option.getDefiningMods().isEmpty()) {
            option.addModOverride(enabled, modid);
        }
    }

    private static String getMixinRuleName(String name) {
        return "mixin." + name;
    }

    public int getOptionCount() {
        return this.options.size();
    }

    public int getOptionOverrideCount() {
        return (int) this.options.values()
                .stream()
                .filter(Option::isOverridden)
                .count();
    }

    public Option getParent(Option option) {
        String optionName = option.getName();
        int split;

        if ((split = optionName.lastIndexOf('.')) != -1) {
            String key = optionName.substring(0, split);
            return this.options.get(key);

        }
        return null;
    }
}