package dev.loaderbridge.fabric.api.item.mixin;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.item.v1.FabricComponentMapBuilder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DataComponentMap.Builder.class)
public abstract class ComponentMapBuilderMixin implements FabricComponentMapBuilder {
    @Shadow @Final private Reference2ObjectMap<DataComponentType<?>, Object> map;
    @Shadow public abstract <T> DataComponentMap.Builder set(DataComponentType<T> type, T value);

    @Override @SuppressWarnings("unchecked")
    public <T> T getOrCreate(DataComponentType<T> type, Supplier<@NotNull T> fallback) {
        if (!map.containsKey(type)) {
            T value = Objects.requireNonNull(fallback.get(),
                    "Cannot insert null values to component map builder");
            set(type, value);
        }
        return (T) map.get(type);
    }

    @Override
    public <T> List<T> getOrEmpty(DataComponentType<List<T>> type) {
        List<T> existing = new ArrayList<>(getOrCreate(type, Collections::emptyList));
        set(type, existing);
        return existing;
    }
}
