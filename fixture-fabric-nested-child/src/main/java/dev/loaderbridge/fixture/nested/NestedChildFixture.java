package dev.loaderbridge.fixture.nested;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;

public final class NestedChildFixture implements ModInitializer {
    @Override
    public void onInitialize() {
        var loader = FabricLoader.getInstance();
        var child = loader.getModContainer("loaderbridge_nested_child").orElseThrow();
        var parent = loader.getModContainer("loaderbridge_fixture").orElseThrow();
        var parentByAlias = loader.getModContainer("loaderbridge_fixture_api_alias").orElseThrow();
        if (child.getContainingMod().orElseThrow() != parent
                || parentByAlias != parent
                || !parent.getContainedMods().contains(child)
                || child.getOrigin().getKind() != ModOrigin.Kind.NESTED
                || !child.getOrigin().getParentModId().equals("loaderbridge_fixture")
                || !child.getOrigin().getParentSubLocation()
                        .equals("META-INF/jars/loaderbridge-nested-child.jar")) {
            throw new IllegalStateException("nested Fabric mod containment contract failed");
        }
        System.out.println("LOADERBRIDGE_FIXTURE_ALIAS_RESOLUTION_READY");
        System.out.println("LOADERBRIDGE_FIXTURE_NESTED_CHILD_READY");
    }
}
