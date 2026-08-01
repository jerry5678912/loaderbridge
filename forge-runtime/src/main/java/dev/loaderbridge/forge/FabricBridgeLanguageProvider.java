package dev.loaderbridge.forge;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraftforge.forgespi.language.ILifecycleEvent;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.IModLanguageProvider;
import net.minecraftforge.forgespi.language.ModFileScanData;

public final class FabricBridgeLanguageProvider implements IModLanguageProvider {
    @Override
    public String name() {
        return "fabricbridge";
    }

    @Override
    public Consumer<ModFileScanData> getFileVisitor() {
        return scanResult -> {
            Map<String, FabricTarget> targets = scanResult.getIModInfoData().stream()
                    .flatMap(file -> file.getMods().stream())
                    .map(IModInfo::getModId)
                    .map(FabricTarget::new)
                    .collect(Collectors.toMap(FabricTarget::modId, Function.identity(), (first, ignored) -> first));
            scanResult.addLanguageLoader(targets);
        };
    }

    @Override
    public <R extends ILifecycleEvent<R>> void consumeLifecycleEvent(Supplier<R> consumeEvent) {
        // Sided entrypoint dispatch is deliberately deferred until Forge lifecycle integration is complete.
    }

    private record FabricTarget(String modId) implements IModLanguageLoader {
        @Override
        @SuppressWarnings("unchecked")
        public <T> T loadMod(IModInfo info, ModFileScanData scanData, ModuleLayer layer) {
            try {
                Class<?> type = Class.forName("dev.loaderbridge.forge.FabricModContainer", true,
                        Thread.currentThread().getContextClassLoader());
                Constructor<?> constructor = type.getConstructor(IModInfo.class, ModFileScanData.class,
                        ModuleLayer.class);
                return (T) constructor.newInstance(info, scanData, layer);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("LB-RUNTIME-001: could not create Fabric container for " + modId,
                        exception);
            }
        }
    }
}
