/*
 * Copyright (c) FabricMC
 * SPDX-License-Identifier: Apache-2.0
 */
package net.fabricmc.fabric.api.resource.conditions.v1;

import dev.loaderbridge.fabric.api.resource.conditions.BridgeResourceConditions;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlag;

/** Fabric-compatible registry and factories for resource conditions. */
public final class ResourceConditions {
    private static final Map<ResourceLocation, ResourceConditionType<?>> REGISTERED_CONDITIONS =
            new ConcurrentHashMap<>();

    public static final String CONDITIONS_KEY = "fabric:load_conditions";
    public static final String OVERLAYS_KEY = "fabric:overlays";

    static {
        BridgeResourceConditions.registerDefaults();
    }

    private ResourceConditions() {}

    public static void register(ResourceConditionType<?> condition) {
        Objects.requireNonNull(condition, "Condition may not be null.");
        if (REGISTERED_CONDITIONS.putIfAbsent(condition.id(), condition) != null) {
            throw new IllegalArgumentException(
                    "Duplicate resource condition registered with id " + condition.id());
        }
    }

    public static ResourceConditionType<?> getConditionType(ResourceLocation id) {
        return REGISTERED_CONDITIONS.get(id);
    }

    public static ResourceCondition alwaysTrue() {
        return BridgeResourceConditions.alwaysTrue();
    }

    public static ResourceCondition not(ResourceCondition condition) {
        return BridgeResourceConditions.not(condition);
    }

    public static ResourceCondition and(ResourceCondition... conditions) {
        return BridgeResourceConditions.and(conditions);
    }

    public static ResourceCondition or(ResourceCondition... conditions) {
        return BridgeResourceConditions.or(conditions);
    }

    public static ResourceCondition allModsLoaded(String... modIds) {
        return BridgeResourceConditions.allModsLoaded(modIds);
    }

    public static ResourceCondition anyModsLoaded(String... modIds) {
        return BridgeResourceConditions.anyModsLoaded(modIds);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> ResourceCondition tagsPopulated(TagKey<T>... tags) {
        return BridgeResourceConditions.tagsPopulated(tags);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> ResourceCondition tagsPopulated(
            ResourceKey<? extends Registry<T>> registry, TagKey<T>... tags) {
        return BridgeResourceConditions.tagsPopulated(registry, tags);
    }

    public static ResourceCondition featuresEnabled(ResourceLocation... features) {
        return BridgeResourceConditions.featuresEnabled(features);
    }

    public static ResourceCondition featuresEnabled(FeatureFlag... features) {
        return BridgeResourceConditions.featuresEnabled(features);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> ResourceCondition registryContains(ResourceKey<T>... entries) {
        return BridgeResourceConditions.registryContains(entries);
    }

    public static <T> ResourceCondition registryContains(
            ResourceKey<? extends Registry<T>> registry, ResourceLocation... entries) {
        return BridgeResourceConditions.registryContains(registry, entries);
    }
}
