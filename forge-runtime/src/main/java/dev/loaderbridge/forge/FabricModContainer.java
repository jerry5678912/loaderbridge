package dev.loaderbridge.forge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.loaderbridge.fabric.runtime.BridgeFabricLoader;
import dev.loaderbridge.fabric.runtime.BridgeModContainer;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;

public final class FabricModContainer extends ModContainer {
    private final List<Object> modInstances = new ArrayList<>();
    private final AtomicBoolean clientEntrypointsInvoked = new AtomicBoolean();
    private final AtomicBoolean serverEntrypointsInvoked = new AtomicBoolean();

    public FabricModContainer(IModInfo info, ModFileScanData scanData, ModuleLayer gameLayer) {
        super(info);
        this.contextExtension = () -> null;
        Path metadataPath = info.getOwningFile().getFile().findResource("fabric.mod.json");
        Path root = metadataPath.getParent();
        BridgeFabricLoader.getInstance().registerMod(BridgeModContainer.create(info.getModId(),
                info.getVersion().toString(), info.getDisplayName(), List.of(), root));
        invokeEntrypoints(metadataPath, "main", ModInitializer.class,
                initializer -> initializer.onInitialize());
    }

    private <T> void invokeEntrypoints(Path metadataPath, String key, Class<T> contract,
            EntrypointInvoker<T> invoker) {
        try (Reader reader = Files.newBufferedReader(metadataPath)) {
            JsonObject metadata = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject entrypoints = metadata.getAsJsonObject("entrypoints");
            if (entrypoints == null || !entrypoints.has(key)) {
                return;
            }
            JsonElement selected = entrypoints.get(key);
            Iterable<JsonElement> declarations = selected.isJsonArray() ? selected.getAsJsonArray()
                    : List.of(selected);
            for (JsonElement declaration : declarations) {
                String className = declaration.isJsonPrimitive() ? declaration.getAsString()
                        : declaration.getAsJsonObject().get("value").getAsString();
                if (className.contains("::")) {
                    throw new IllegalStateException("LB-ENTRY-002: member entrypoints are not implemented: "
                            + className);
                }
                Object instance = Class.forName(className, true, Thread.currentThread().getContextClassLoader())
                        .getDeclaredConstructor().newInstance();
                if (!contract.isInstance(instance)) {
                    throw new IllegalStateException("LB-ENTRY-003: " + key
                            + " entrypoint does not implement " + contract.getName() + ": " + className);
                }
                modInstances.add(instance);
                BridgeFabricLoader.getInstance().registerEntrypoint(key, instance);
                T typed = contract.cast(instance);
                invoker.invoke(typed);
            }
        } catch (IOException | ReflectiveOperationException exception) {
            throw new IllegalStateException("LB-ENTRY-001: failed to initialize " + modId, exception);
        }
    }

    @Override
    protected <T extends Event & IModBusEvent> void acceptEvent(T event) {
        String eventName = event.getClass().getName();
        Path metadataPath = modInfo.getOwningFile().getFile().findResource("fabric.mod.json");
        if (eventName.equals("net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent")
                && serverEntrypointsInvoked.compareAndSet(false, true)) {
            invokeEntrypoints(metadataPath, "server", DedicatedServerModInitializer.class,
                    initializer -> initializer.onInitializeServer());
        } else if (eventName.equals("net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent")
                && clientEntrypointsInvoked.compareAndSet(false, true)) {
            invokeEntrypoints(metadataPath, "client", ClientModInitializer.class,
                    initializer -> initializer.onInitializeClient());
        }
    }

    @Override
    public boolean matches(Object mod) {
        return modInstances.contains(mod);
    }

    @Override
    public Object getMod() {
        return modInstances.isEmpty() ? this : modInstances.getFirst();
    }

    @FunctionalInterface
    private interface EntrypointInvoker<T> {
        void invoke(T entrypoint);
    }
}
