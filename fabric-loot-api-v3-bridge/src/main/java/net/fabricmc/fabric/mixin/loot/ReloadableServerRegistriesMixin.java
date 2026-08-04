package net.fabricmc.fabric.mixin.loot;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import dev.loaderbridge.fabric.api.loot.BridgeLootTables;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ReloadableServerRegistries.class)
abstract class ReloadableServerRegistriesMixin {
    @Inject(method = "reload", at = @At("HEAD"))
    private static void loaderbridge$begin(LayeredRegistryAccess<RegistryLayer> layers,
            ResourceManager manager, Executor executor,
            CallbackInfoReturnable<CompletableFuture<LayeredRegistryAccess<RegistryLayer>>> cir) {
        BridgeLootTables.beginReload(manager);
    }

    @Redirect(method = "reload", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/ReloadableServerRegistries$EmptyTagLookupWrapper;createSerializationContext(Lcom/mojang/serialization/DynamicOps;)Lnet/minecraft/resources/RegistryOps;"))
    private static RegistryOps<JsonElement> loaderbridge$captureLookup(
            @Coerce Object wrapper, DynamicOps<JsonElement> delegate) {
        HolderLookup.Provider provider = (HolderLookup.Provider) wrapper;
        RegistryOps<JsonElement> ops = provider.createSerializationContext(delegate);
        BridgeLootTables.associate(ops, provider);
        return ops;
    }

    @Inject(method = "lambda$scheduleElementParse$3", at = @At("HEAD"), cancellable = true)
    private static <T> void loaderbridge$modifyBeforeRegistration(LootDataType<T> dataType,
            RegistryOps<JsonElement> ops, WritableRegistry<T> registry,
            ResourceLocation id, JsonElement json, CallbackInfo ci) {
        if (BridgeLootTables.parseAndRegister(dataType, ops, registry, id, json)) ci.cancel();
    }

    @Inject(method = "createUpdatedRegistries", at = @At("RETURN"))
    private static void loaderbridge$allLoaded(LayeredRegistryAccess<RegistryLayer> layers,
            List<WritableRegistry<?>> registries,
            CallbackInfoReturnable<LayeredRegistryAccess<RegistryLayer>> cir) {
        BridgeLootTables.finishReload(cir.getReturnValue());
    }
}
