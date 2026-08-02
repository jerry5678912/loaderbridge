package dev.loaderbridge.forge.transform;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import java.util.List;
import java.util.Set;

public final class FabricBridgeTransformationService implements ITransformationService {
    @Override
    public String name() {
        return "fabricbridge";
    }

    @Override
    public void initialize(IEnvironment environment) {
        // Access-widener transformers will be registered here before game classes load.
    }

    @Override
    public void onLoad(IEnvironment environment, Set<String> otherServices) {
        // No incompatible peer transformation service is known at scaffold stage.
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<ITransformer> transformers() {
        return List.of();
    }
}
