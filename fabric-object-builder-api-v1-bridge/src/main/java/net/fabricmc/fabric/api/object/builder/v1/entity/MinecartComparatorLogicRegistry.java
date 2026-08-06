package net.fabricmc.fabric.api.object.builder.v1.entity;

import com.mojang.logging.LogUtils;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.slf4j.Logger;

/** Identity-keyed registry for Fabric minecart comparator callbacks. */
public final class MinecartComparatorLogicRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<EntityType<?>, MinecartComparatorLogic<?>> LOGICS =
            new IdentityHashMap<>();

    private MinecartComparatorLogicRegistry() {}

    @SuppressWarnings("unchecked")
    public static MinecartComparatorLogic<AbstractMinecart> getCustomComparatorLogic(
            EntityType<?> type) {
        return (MinecartComparatorLogic<AbstractMinecart>) LOGICS.get(type);
    }

    public static <T extends AbstractMinecart> void register(
            EntityType<T> type, MinecartComparatorLogic<? super T> logic) {
        Objects.requireNonNull(type, "Entity type cannot be null");
        Objects.requireNonNull(logic, "Logic cannot be null");
        if (LOGICS.put(type, logic) != null) {
            LOGGER.warn("Overriding existing minecart comparator logic for entity type {}",
                    BuiltInRegistries.ENTITY_TYPE.getKey(type));
        }
    }
}
