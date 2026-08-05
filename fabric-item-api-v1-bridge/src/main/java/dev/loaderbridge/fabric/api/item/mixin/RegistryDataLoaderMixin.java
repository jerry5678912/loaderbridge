package dev.loaderbridge.fabric.api.item.mixin;

import com.google.gson.JsonElement;
import com.mojang.serialization.Decoder;
import java.util.Optional;
import dev.loaderbridge.fabric.api.item.EnchantingBridge;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RegistryDataLoader.class)
public abstract class RegistryDataLoaderMixin {
    @Redirect(method = "loadElementFromResource",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/WritableRegistry;register("
                            + "Lnet/minecraft/resources/ResourceKey;Ljava/lang/Object;"
                            + "Lnet/minecraft/core/RegistrationInfo;)"
                            + "Lnet/minecraft/core/Holder$Reference;"))
    @SuppressWarnings("unchecked")
    private static <E> Holder.Reference<E> loaderbridge$modifyEnchantment(
            WritableRegistry<E> registry, ResourceKey<E> key, E value,
            RegistrationInfo registrationInfo, WritableRegistry<E> ignoredRegistry,
            Decoder<Optional<E>> ignoredDecoder, RegistryOps<JsonElement> ignoredOps,
            ResourceKey<E> ignoredKey, Resource resource,
            RegistrationInfo ignoredRegistrationInfo) {
        E replacement = value;
        if (value instanceof Enchantment enchantment) {
            replacement = (E) EnchantingBridge.modify(
                    (ResourceKey<Enchantment>) (ResourceKey<?>) key,
                    enchantment, EnchantingBridge.source(resource));
        }
        return registry.register(key, replacement, registrationInfo);
    }
}
