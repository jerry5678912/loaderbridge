package dev.loaderbridge.fabric.remap;

import java.io.IOException;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerRemapper;
import net.fabricmc.accesswidener.AccessWidenerWriter;
import org.objectweb.asm.commons.Remapper;

/** Validates and translates Fabric access-widener resources into the runtime namespace. */
final class AccessWidenerResourceTransformer {
    byte[] transform(byte[] input, TinyMappingIndex mappings) throws IOException {
        try {
            AccessWidenerReader.Header header = AccessWidenerReader.readHeader(input);
            if (header.getNamespace().equals("intermediary")) {
                if (mappings == null) {
                    throw new IOException("LB-AW-002: intermediary access widener requires mappings");
                }
                AccessWidenerWriter writer = new AccessWidenerWriter(header.getVersion());
                new AccessWidenerReader(new AccessWidenerRemapper(writer,
                        new MappingRemapper(mappings), "intermediary", "official")).read(input);
                return writer.write();
            }
            if (header.getNamespace().equals("named")) {
                AccessWidenerWriter writer = new AccessWidenerWriter(header.getVersion());
                new AccessWidenerReader(new AccessWidenerRemapper(writer,
                        new Remapper() {}, "named", "official")).read(input);
                return writer.write();
            }
            if (!header.getNamespace().equals("official")) {
                throw new IOException("LB-AW-002: unsupported access-widener namespace: "
                        + header.getNamespace());
            }
            new AccessWidenerReader(new net.fabricmc.accesswidener.AccessWidener()).read(input);
            return input.clone();
        } catch (net.fabricmc.accesswidener.AccessWidenerFormatException
                | IllegalArgumentException exception) {
            throw new IOException("LB-AW-003: malformed access widener: "
                    + exception.getMessage(), exception);
        }
    }

    private static final class MappingRemapper extends Remapper {
        private final TinyMappingIndex mappings;

        private MappingRemapper(TinyMappingIndex mappings) {
            this.mappings = mappings;
        }

        @Override
        public String map(String internalName) {
            return mappings.mapClass(internalName);
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            return mappings.mapField(owner, name, descriptor);
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            return mappings.mapMethod(owner, name, descriptor);
        }
    }
}
