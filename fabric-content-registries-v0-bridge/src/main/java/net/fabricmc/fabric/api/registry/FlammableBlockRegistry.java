package net.fabricmc.fabric.api.registry;

import dev.loaderbridge.fabric.api.content.registry.BridgeContentRegistries;
import net.fabricmc.fabric.api.util.Block2ObjectMap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Fabric-compatible fire behavior registry. */
public interface FlammableBlockRegistry extends Block2ObjectMap<FlammableBlockRegistry.Entry> {
    static FlammableBlockRegistry getDefaultInstance() {
        return getInstance(Blocks.FIRE);
    }

    static FlammableBlockRegistry getInstance(Block fireBlock) {
        return BridgeContentRegistries.flammability(fireBlock);
    }

    default void add(Block block, int burnChance, int spreadChance) {
        add(block, new Entry(burnChance, spreadChance));
    }

    default void add(TagKey<Block> tag, int burnChance, int spreadChance) {
        add(tag, new Entry(burnChance, spreadChance));
    }

    final class Entry {
        private final int burnChance;
        private final int spreadChance;

        public Entry(int burnChance, int spreadChance) {
            this.burnChance = burnChance;
            this.spreadChance = spreadChance;
        }

        public int getBurnChance() { return burnChance; }
        public int getSpreadChance() { return spreadChance; }

        @Override public boolean equals(Object other) {
            return other instanceof Entry entry && burnChance == entry.burnChance
                    && spreadChance == entry.spreadChance;
        }

        @Override public int hashCode() {
            return 31 * Integer.hashCode(burnChance) + Integer.hashCode(spreadChance);
        }
    }
}
