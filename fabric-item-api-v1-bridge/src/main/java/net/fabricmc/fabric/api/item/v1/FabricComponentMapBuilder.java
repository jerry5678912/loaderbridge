package net.fabricmc.fabric.api.item.v1;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import org.jetbrains.annotations.NotNull;

public interface FabricComponentMapBuilder {
    default <T> T getOrCreate(DataComponentType<T> type, Supplier<@NotNull T> fallback) {
        throw new AssertionError("Implemented in Mixin");
    }

    default <T> T getOrDefault(DataComponentType<T> type, @NotNull T defaultValue) {
        Objects.requireNonNull(defaultValue, "Cannot insert null values to component map builder");
        return getOrCreate(type, () -> defaultValue);
    }

    default <T> List<T> getOrEmpty(DataComponentType<List<T>> type) {
        throw new AssertionError("Implemented in Mixin");
    }
}
