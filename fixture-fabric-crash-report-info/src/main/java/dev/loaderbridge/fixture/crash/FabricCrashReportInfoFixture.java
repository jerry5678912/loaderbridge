package dev.loaderbridge.fixture.crash;

import net.fabricmc.api.ModInitializer;
import net.minecraft.SystemReport;

/** Proves the bridge contributes translated Fabric metadata to Minecraft system details. */
public final class FabricCrashReportInfoFixture implements ModInitializer {
    @Override public void onInitialize() {
        String report = new SystemReport().toLineSeparatedString();
        String expected = "loaderbridge_crash_report_fixture: LoaderBridge Fabric Crash Report Fixture 1.0.0";
        if (!report.contains("Fabric Mods") || !report.contains(expected)) {
            throw new IllegalStateException("LOADERBRIDGE_FABRIC_CRASH_REPORT_INFO_FAILED: " + report);
        }
        System.out.println("LOADERBRIDGE_FABRIC_CRASH_REPORT_INFO_READY mod=" + expected);
    }
}
