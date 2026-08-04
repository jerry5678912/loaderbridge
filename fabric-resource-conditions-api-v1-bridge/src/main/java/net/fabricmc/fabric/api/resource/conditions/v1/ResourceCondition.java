/*
 * Copyright (c) FabricMC
 * SPDX-License-Identifier: Apache-2.0
 */
package net.fabricmc.fabric.api.resource.conditions.v1;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.HolderLookup;

/** A predicate attached to a JSON resource through {@code fabric:load_conditions}. */
public interface ResourceCondition {
    Codec<ResourceCondition> CODEC = ResourceConditionType.TYPE_CODEC.dispatch(
            "condition", ResourceCondition::getType, ResourceConditionType::codec);
    Codec<List<ResourceCondition>> LIST_CODEC = CODEC.listOf();
    Codec<ResourceCondition> CONDITION_CODEC = Codec.withAlternative(
            CODEC,
            LIST_CODEC,
            conditions -> ResourceConditions.and(conditions.toArray(ResourceCondition[]::new)));

    ResourceConditionType<?> getType();

    boolean test(HolderLookup.Provider registryLookup);
}
