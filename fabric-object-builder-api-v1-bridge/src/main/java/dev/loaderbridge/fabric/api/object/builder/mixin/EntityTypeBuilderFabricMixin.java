package dev.loaderbridge.fabric.api.object.builder.mixin;

import dev.loaderbridge.fabric.api.object.builder.EntityTypeBuilderExtension;
import dev.loaderbridge.fabric.api.object.builder.EntityTypeExtensionBridge;
import java.util.Objects;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Injects Fabric's modern builder interface and deferred living/mob hooks. */
@Mixin(EntityType.Builder.class)
public abstract class EntityTypeBuilderFabricMixin<T extends Entity>
        implements FabricEntityType.Builder<T>, EntityTypeBuilderExtension<T> {
    @Shadow
    public abstract EntityType<T> build(String id);

    @Unique
    private EntityTypeExtensionBridge.BuildHook<T> loaderbridge$buildHook;

    @Override
    @SuppressWarnings("unchecked")
    public EntityType.Builder<T> alwaysUpdateVelocity(boolean alwaysUpdateVelocity) {
        EntityType.Builder<T> builder = (EntityType.Builder<T>) (Object) this;
        builder.setShouldReceiveVelocityUpdates(alwaysUpdateVelocity);
        return builder;
    }

    @Override
    public EntityType<T> build() {
        return build(null);
    }

    @Override
    public void loaderbridge$setBuildHook(EntityTypeExtensionBridge.BuildHook<T> hook) {
        loaderbridge$buildHook = Objects.requireNonNull(hook, "Build hook cannot be null");
    }

    @Inject(method = "build", at = @At("RETURN"))
    private void loaderbridge$applyBuildHook(String id,
            CallbackInfoReturnable<EntityType<T>> callback) {
        if (loaderbridge$buildHook != null) {
            loaderbridge$buildHook.onBuild(callback.getReturnValue());
        }
    }
}
