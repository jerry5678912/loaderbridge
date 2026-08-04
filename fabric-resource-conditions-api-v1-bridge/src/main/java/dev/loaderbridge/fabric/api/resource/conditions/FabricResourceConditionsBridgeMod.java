package dev.loaderbridge.fabric.api.resource.conditions;

import net.minecraftforge.fml.common.Mod;

@Mod("loaderbridge_fabric_resource_conditions_api_v1")
public final class FabricResourceConditionsBridgeMod {
    public FabricResourceConditionsBridgeMod() {
        BridgeResourceConditions.registerDefaults();
    }
}
