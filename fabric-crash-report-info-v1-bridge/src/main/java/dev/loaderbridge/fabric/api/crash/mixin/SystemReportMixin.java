package dev.loaderbridge.fabric.api.crash.mixin;

import dev.loaderbridge.fabric.api.crash.FabricModReportAccess;
import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SystemReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SystemReport.class)
public abstract class SystemReportMixin {
    @Shadow public abstract void setDetail(String name, Supplier<String> value);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void loaderbridge$addFabricMods(CallbackInfo callback) {
        setDetail("Fabric Mods", () -> FabricModReportAccess.format(
                FabricLoader.getInstance().getAllMods()));
    }
}
