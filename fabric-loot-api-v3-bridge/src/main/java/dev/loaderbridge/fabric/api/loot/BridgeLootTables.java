package dev.loaderbridge.fabric.api.loot;

import com.google.gson.JsonElement;
import com.mojang.serialization.Lifecycle;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.loot.v3.FabricLootTableBuilder;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runtime state for one vanilla reload. All state is cleared when ALL_LOADED fires. */
public final class BridgeLootTables {
    private static final Logger LOGGER = LoggerFactory.getLogger("LoaderBridge/LootApiV3");
    private static final RegistrationInfo REGISTRATION =
            new RegistrationInfo(Optional.empty(), Lifecycle.experimental());
    private static final Map<RegistryOps<JsonElement>, HolderLookup.Provider> LOOKUPS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ResourceLocation, LootTableSource> SOURCES = new ConcurrentHashMap<>();
    private static final AtomicReference<ResourceManager> RESOURCE_MANAGER = new AtomicReference<>();

    private BridgeLootTables() {}

    public static void beginReload(ResourceManager manager) {
        RESOURCE_MANAGER.set(manager);
        SOURCES.clear();
        String directory = Registries.elementsDirPath(Registries.LOOT_TABLE);
        FileToIdConverter converter = FileToIdConverter.json(directory);
        converter.listMatchingResources(manager).forEach((file, resource) ->
                SOURCES.put(converter.fileToId(file), determineSource(resource)));
    }

    public static void associate(RegistryOps<JsonElement> ops, HolderLookup.Provider provider) {
        LOOKUPS.put(ops, provider);
    }

    @SuppressWarnings("unchecked")
    public static <T> boolean parseAndRegister(LootDataType<T> dataType,
            RegistryOps<JsonElement> ops, WritableRegistry<T> registry,
            ResourceLocation id, JsonElement json) {
        if (dataType != LootDataType.TABLE) return false;
        Optional<T> decoded = dataType.deserialize(id, ops, json);
        decoded.ifPresent(value -> {
            LootTable modified = modify(id, (LootTable) value, ops);
            registry.register(ResourceKey.create(dataType.registryKey(), id), (T) modified, REGISTRATION);
        });
        return true;
    }

    static LootTable modify(ResourceLocation id, LootTable original, RegistryOps<JsonElement> ops) {
        if (original == LootTable.EMPTY) return original;
        HolderLookup.Provider provider = LOOKUPS.get(ops);
        if (provider == null) {
            throw new IllegalStateException("LB-FAPI-LOOT-001: missing registry lookup for " + id);
        }
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, id);
        LootTableSource source = SOURCES.getOrDefault(id, LootTableSource.DATA_PACK);
        LootTable table = LootTableEvents.REPLACE.invoker()
                .replaceLootTable(key, original, source, provider);
        if (table == null) {
            table = original;
        } else {
            source = LootTableSource.REPLACED;
        }
        LootTable.Builder builder = FabricLootTableBuilder.copyOf(table);
        LootTableEvents.MODIFY.invoker().modifyLootTable(key, builder, source, provider);
        return builder.build();
    }

    public static void finishReload(LayeredRegistryAccess<RegistryLayer> layers) {
        ResourceManager manager = RESOURCE_MANAGER.getAndSet(null);
        if (manager == null) {
            LOGGER.error("LB-FAPI-LOOT-002: loot reload completed without a resource manager");
            return;
        }
        Registry<LootTable> registry = layers.compositeAccess().registryOrThrow(Registries.LOOT_TABLE);
        LootTableEvents.ALL_LOADED.invoker().onLootTablesLoaded(manager, registry);
        LOOKUPS.clear();
        SOURCES.clear();
    }

    static LootTableSource determineSource(Resource resource) {
        String packId = resource.sourcePackId();
        if ("vanilla".equals(packId)) return LootTableSource.VANILLA;
        if ("mod_resources".equals(packId) || packId.startsWith("mod:")) return LootTableSource.MOD;
        return LootTableSource.DATA_PACK;
    }
}
