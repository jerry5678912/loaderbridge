package net.fabricmc.fabric.api.object.builder.v1.villager;

import java.util.Objects;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.biome.Biome;

/** Deprecated Fabric villager-type helpers retained for binary compatibility. */
@Deprecated
public final class VillagerTypeHelper {
    private VillagerTypeHelper() {}

    public static VillagerType register(ResourceLocation id) {
        Objects.requireNonNull(id, "Id of villager type cannot be null");
        return Registry.register(BuiltInRegistries.VILLAGER_TYPE, id,
                new VillagerType(id.toString()));
    }

    public static void addVillagerTypeToBiome(ResourceKey<Biome> biomeKey,
            VillagerType villagerType) {
        Objects.requireNonNull(biomeKey, "Biome registry key cannot be null");
        Objects.requireNonNull(villagerType, "Villager type cannot be null");
        VillagerType.registerBiomeType(biomeKey, villagerType);
    }
}
