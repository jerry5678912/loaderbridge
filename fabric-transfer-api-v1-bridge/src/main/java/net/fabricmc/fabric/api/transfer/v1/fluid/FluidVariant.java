package net.fabricmc.fabric.api.transfer.v1.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Immutable association of a still fluid and data-component changes. */
public interface FluidVariant extends TransferVariant<Fluid> {
    Codec<FluidVariant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.FLUID.holderByNameCodec().fieldOf("fluid")
                    .forGetter(FluidVariant::getRegistryEntry),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                    .forGetter(FluidVariant::getComponents))
            .apply(instance, (holder, components) -> of(holder.value(), components)));
    StreamCodec<RegistryFriendlyByteBuf, FluidVariant> PACKET_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderRegistry(Registries.FLUID), FluidVariant::getRegistryEntry,
            DataComponentPatch.STREAM_CODEC, FluidVariant::getComponents,
            (holder, components) -> of(holder.value(), components));

    static FluidVariant blank() { return of(Fluids.EMPTY); }
    static FluidVariant of(Fluid fluid) { return of(fluid, DataComponentPatch.EMPTY); }
    static FluidVariant of(Fluid fluid, DataComponentPatch components) {
        return BridgeFluidVariant.of(fluid, components);
    }

    default Fluid getFluid() { return getObject(); }
    @SuppressWarnings("deprecation")
    default Holder<Fluid> getRegistryEntry() { return getFluid().builtInRegistryHolder(); }
    @Override FluidVariant withComponentChanges(DataComponentPatch changes);
}
