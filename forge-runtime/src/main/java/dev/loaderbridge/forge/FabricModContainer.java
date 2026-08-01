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
import net.fabricmc.api.ModInitializer;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;

public final class FabricModContainer extends ModContainer {
    private final List<Object> modInstances = new ArrayList<>();

    public FabricModContainer(IModInfo info, ModFileScanData scanData, ModuleLayer gameLayer) {
        super(info);
        this.contextExtension = () -> null;
        Path metadataPath = info.getOwningFile().getFile().findResource("fabric.mod.json");
        Path root = metadataPath.getParent();
        BridgeFabricLoader.getInstance().registerMod(BridgeModContainer.create(info.getModId(),
                info.getVersion().toString(), info.getDisplayName(), List.of(), root));
        invokeMainEntrypoints(metadataPath);
    }

    private void invokeMainEntrypoints(Path metadataPath) {
        try (Reader reader = Files.newBufferedReader(metadataPath)) {
            JsonObject metadata = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject entrypoints = metadata.getAsJsonObject("entrypoints");
            if (entrypoints == null || !entrypoints.has("main")) {
                return;
            }
            JsonElement main = entrypoints.get("main");
            Iterable<JsonElement> declarations = main.isJsonArray() ? main.getAsJsonArray() : List.of(main);
            for (JsonElement declaration : declarations) {
                String className = declaration.isJsonPrimitive() ? declaration.getAsString()
                        : declaration.getAsJsonObject().get("value").getAsString();
                if (className.contains("::")) {
                    throw new IllegalStateException("LB-ENTRY-002: member entrypoints are not implemented: "
                            + className);
                }
                Object instance = Class.forName(className, true, Thread.currentThread().getContextClassLoader())
                        .getDeclaredConstructor().newInstance();
                if (!(instance instanceof ModInitializer initializer)) {
                    throw new IllegalStateException("LB-ENTRY-003: main entrypoint does not implement ModInitializer: "
                            + className);
                }
                modInstances.add(instance);
                BridgeFabricLoader.getInstance().registerEntrypoint("main", instance);
                initializer.onInitialize();
            }
        } catch (IOException | ReflectiveOperationException exception) {
            throw new IllegalStateException("LB-ENTRY-001: failed to initialize " + modId, exception);
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
}
