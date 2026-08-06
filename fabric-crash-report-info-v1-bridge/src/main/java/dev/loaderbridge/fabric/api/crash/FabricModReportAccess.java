package dev.loaderbridge.fabric.api.crash;

import java.util.Collection;
import net.fabricmc.loader.api.ModContainer;

/** Internal cross-package entry used by the Mixin without exposing a Fabric API class. */
public final class FabricModReportAccess {
    private FabricModReportAccess() {}

    public static String format(Collection<ModContainer> containers) {
        return FabricModReport.format(containers);
    }
}
