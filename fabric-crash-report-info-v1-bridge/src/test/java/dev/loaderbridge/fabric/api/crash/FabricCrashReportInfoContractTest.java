package dev.loaderbridge.fabric.api.crash;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModOrigin;
import org.junit.jupiter.api.Test;

class FabricCrashReportInfoContractTest {
    @Test
    void advertisesPinnedMetadataOnlyContract() {
        var descriptor = new FabricCrashReportInfoBridgeProvider().descriptor();

        assertThat(descriptor.contractVersion())
                .isEqualTo("fabric-crash-report-info-v1:0.2.29");
        assertThat(descriptor.providedModVersions())
                .containsEntry("fabric-crash-report-info-v1", "0.2.29+0af3f5a719");
        assertThat(descriptor.providedClasses()).isEmpty();
    }

    @Test
    void reportsSortedRootsAndNestedMods() throws Exception {
        ModContainer child = container("child", "Child", "2.0.0", null, List.of());
        ModContainer alpha = container("alpha", "Alpha", "1.0.0", null, List.of(child));
        ModContainer childWithParent = container("child", "Child", "2.0.0", alpha, List.of());
        alpha = container("alpha", "Alpha", "1.0.0", null, List.of(childWithParent));
        ModContainer zeta = container("zeta", "Zeta", "3.0.0", null, List.of());

        assertThat(FabricModReport.format(List.of(zeta, childWithParent, alpha)))
                .isEqualTo("\n\t\talpha: Alpha 1.0.0"
                        + "\n\t\t\tchild: Child 2.0.0"
                        + "\n\t\tzeta: Zeta 3.0.0");
    }

    @SuppressWarnings("deprecation")
    private static ModContainer container(String id, String name, String version,
            ModContainer parent, Collection<ModContainer> children) throws Exception {
        ModMetadata metadata = new ModMetadata() {
            @Override public String getId() { return id; }
            @Override public Collection<String> getProvides() { return List.of(); }
            @Override public Version getVersion() {
                try {
                    return Version.parse(version);
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }
            @Override public String getName() { return name; }
        };
        return new ModContainer() {
            @Override public ModMetadata getMetadata() { return metadata; }
            @Override public List<Path> getRootPaths() { return List.of(); }
            @Override public ModOrigin getOrigin() { throw new UnsupportedOperationException(); }
            @Override public Optional<ModContainer> getContainingMod() {
                return Optional.ofNullable(parent);
            }
            @Override public Collection<ModContainer> getContainedMods() { return children; }
            @Override public Path getRootPath() { throw new UnsupportedOperationException(); }
            @Override public Path getPath(String file) { throw new UnsupportedOperationException(); }
        };
    }
}
