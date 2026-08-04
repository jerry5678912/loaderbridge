/*
 * Copyright (c) FabricMC
 * SPDX-License-Identifier: Apache-2.0
 */
package net.fabricmc.fabric.api.resource.conditions.v1;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** The ID and codec for one resource-condition implementation. */
public interface ResourceConditionType<T extends ResourceCondition> {
    Codec<ResourceConditionType<?>> TYPE_CODEC = ResourceLocation.CODEC.comapFlatMap(
            id -> {
                ResourceConditionType<?> type = ResourceConditions.getConditionType(id);
                return type == null
                        ? DataResult.error(() -> "Unknown resource condition key: " + id)
                        : DataResult.success(type);
            },
            ResourceConditionType::id);

    ResourceLocation id();

    MapCodec<T> codec();

    static <T extends ResourceCondition> ResourceConditionType<T> create(
            ResourceLocation id, MapCodec<T> codec) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(codec, "codec cannot be null");
        return new ResourceConditionType<>() {
            @Override
            public ResourceLocation id() {
                return id;
            }

            @Override
            public MapCodec<T> codec() {
                return codec;
            }

            @Override
            public String toString() {
                return id.toString();
            }
        };
    }
}
