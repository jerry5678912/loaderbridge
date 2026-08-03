package net.fabricmc.fabric.api.gamerule.v1;

import dev.loaderbridge.fabric.api.gamerule.CustomCategoryRegistry;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;

public final class CustomGameRuleCategory {
    private final ResourceLocation id;
    private final Component name;

    public CustomGameRuleCategory(ResourceLocation id, Component name) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
    }
    public ResourceLocation getId() { return id; }
    public Component getName() { return name; }
    @Override public boolean equals(Object value) {
        return value instanceof CustomGameRuleCategory category && id.equals(category.id);
    }
    @Override public int hashCode() { return id.hashCode(); }
    public static <T extends GameRules.Value<T>> Optional<CustomGameRuleCategory> getCategory(
            GameRules.Key<T> key) {
        return CustomCategoryRegistry.get(key);
    }
}
