package net.fabricmc.fabric.api.transfer.v1.item;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Internal immutable implementation of the public item-variant contract. */
final class BridgeItemVariant implements ItemVariant {
    private final Item item;
    private final DataComponentPatch components;
    private final int hashCode;

    BridgeItemVariant(Item item, DataComponentPatch components) {
        this.item = Objects.requireNonNull(item, "Item may not be null.");
        this.components = Objects.requireNonNull(components, "Components may not be null.");
        this.hashCode = Objects.hash(item, components);
    }

    @Override
    public boolean isBlank() {
        return item == Items.AIR;
    }

    @Override
    public Item getObject() {
        return item;
    }

    @Override
    public DataComponentPatch getComponents() {
        return components;
    }

    @Override
    public DataComponentMap getComponentMap() {
        return PatchedDataComponentMap.fromPatch(item.components(), components);
    }

    @Override
    public ItemVariant withComponentChanges(DataComponentPatch changes) {
        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        copy(components, builder);
        copy(changes, builder);
        return new BridgeItemVariant(item, builder.build());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void copy(DataComponentPatch patch, DataComponentPatch.Builder builder) {
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
            DataComponentType type = entry.getKey();
            if (entry.getValue().isPresent()) {
                builder.set(type, entry.getValue().get());
            } else {
                builder.remove(type);
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof BridgeItemVariant variant
                && item == variant.item && components.equals(variant.components);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "ItemVariant{item=" + item + ", components=" + components + '}';
    }
}
