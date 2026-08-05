package net.fabricmc.fabric.api.transfer.v1.fluid;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

final class BridgeFluidVariant implements FluidVariant {
    static FluidVariant of(Fluid fluid, DataComponentPatch components) {
        Objects.requireNonNull(fluid, "Fluid may not be null.");
        Objects.requireNonNull(components, "Components may not be null.");
        if (fluid != Fluids.EMPTY && !fluid.isSource(fluid.defaultFluidState())) {
            if (fluid instanceof FlowingFluid flowing) fluid = flowing.getSource();
            else throw new IllegalArgumentException(
                    "Cannot convert flowing fluid " + BuiltInRegistries.FLUID.getKey(fluid)
                            + " into a still fluid.");
        }
        return new BridgeFluidVariant(fluid, components);
    }

    private final Fluid fluid;
    private final DataComponentPatch components;
    private final DataComponentMap componentMap;
    private final int hashCode;

    private BridgeFluidVariant(Fluid fluid, DataComponentPatch components) {
        this.fluid = fluid;
        this.components = components;
        this.componentMap = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, components);
        this.hashCode = Objects.hash(fluid, components);
    }

    @Override public boolean isBlank() { return fluid == Fluids.EMPTY; }
    @Override public Fluid getObject() { return fluid; }
    @Override public DataComponentPatch getComponents() { return components; }
    @Override public DataComponentMap getComponentMap() { return componentMap; }

    @Override public FluidVariant withComponentChanges(DataComponentPatch changes) {
        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        copy(components, builder);
        copy(changes, builder);
        return of(fluid, builder.build());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void copy(DataComponentPatch patch, DataComponentPatch.Builder builder) {
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
            DataComponentType type = entry.getKey();
            if (entry.getValue().isPresent()) builder.set(type, entry.getValue().get());
            else builder.remove(type);
        }
    }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof BridgeFluidVariant variant
                && hashCode == variant.hashCode && fluid == variant.fluid
                && components.equals(variant.components);
    }
    @Override public int hashCode() { return hashCode; }
    @Override public String toString() {
        return "FluidVariant{fluid=" + fluid + ", components=" + components + '}';
    }
}
