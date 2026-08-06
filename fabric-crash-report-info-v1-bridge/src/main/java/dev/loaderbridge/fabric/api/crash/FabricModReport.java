package dev.loaderbridge.fabric.api.crash;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import net.fabricmc.loader.api.ModContainer;

final class FabricModReport {
    private FabricModReport() {}

    static String format(Collection<ModContainer> containers) {
        ArrayList<ModContainer> roots = new ArrayList<>();
        for (ModContainer container : containers) {
            if (container.getContainingMod().isEmpty()) roots.add(container);
        }
        StringBuilder report = new StringBuilder();
        append(report, 2, roots);
        return report.toString();
    }

    private static void append(StringBuilder report, int depth, ArrayList<ModContainer> mods) {
        mods.sort(Comparator.comparing(mod -> mod.getMetadata().getId()));
        for (ModContainer mod : mods) {
            report.append('\n').append("\t".repeat(depth))
                    .append(mod.getMetadata().getId()).append(": ")
                    .append(mod.getMetadata().getName()).append(' ')
                    .append(mod.getMetadata().getVersion().getFriendlyString());
            if (!mod.getContainedMods().isEmpty()) {
                append(report, depth + 1, new ArrayList<>(mod.getContainedMods()));
            }
        }
    }
}
