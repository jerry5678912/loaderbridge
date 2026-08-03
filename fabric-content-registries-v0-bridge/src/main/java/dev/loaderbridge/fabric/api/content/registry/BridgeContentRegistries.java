package dev.loaderbridge.fabric.api.content.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Shared backing state queried by public contracts and early vanilla Mixins. */
@SuppressWarnings("deprecation")
public final class BridgeContentRegistries {
    private static final FuelMap FUELS = new FuelMap();
    private static final CompostMap COMPOSTING = new CompostMap();
    private static final Map<Block, FlammableMap> FLAMMABILITY = new ConcurrentHashMap<>();
    private static final Map<Block, BlockState> FLATTENABLES = new ConcurrentHashMap<>();
    private static final Map<Block, Block> STRIPPABLES = new ConcurrentHashMap<>();
    private static final Map<Block, Block> OXIDIZABLES = new ConcurrentHashMap<>();
    private static final Map<Block, Block> WAXABLES = new ConcurrentHashMap<>();

    private BridgeContentRegistries() { }

    public static FuelRegistry fuels() { return FUELS; }
    public static CompostingChanceRegistry composting() { return COMPOSTING; }
    public static FlammableBlockRegistry flammability(Block fireBlock) {
        if (!(fireBlock instanceof FireBlock)) {
            throw new IllegalArgumentException("Block is not a FireBlock: " + fireBlock);
        }
        return FLAMMABILITY.computeIfAbsent(fireBlock, FlammableMap::new);
    }

    public static OptionalInt customFuel(ItemLike item) { return FUELS.custom(item); }
    public static void refreshTags() { FLAMMABILITY.values().forEach(FlammableMap::applyTags); }

    public static void registerFlattenable(Block input, BlockState flattened) {
        FLATTENABLES.put(input, flattened);
    }
    public static void registerStrippable(Block input, Block stripped) {
        STRIPPABLES.put(input, stripped);
    }
    public static void registerOxidizable(Block input, Block next) {
        OXIDIZABLES.put(input, next);
    }
    public static void registerWaxable(Block input, Block waxed) {
        WAXABLES.put(input, waxed);
    }

    public static Optional<BlockState> flattened(BlockState state) {
        return Optional.ofNullable(FLATTENABLES.get(state.getBlock()));
    }
    public static Optional<BlockState> stripped(BlockState state) {
        return Optional.ofNullable(STRIPPABLES.get(state.getBlock()))
                .map(block -> copySharedProperties(block.defaultBlockState(), state));
    }
    public static Optional<BlockState> waxed(BlockState state) {
        return Optional.ofNullable(WAXABLES.get(state.getBlock()))
                .map(block -> copySharedProperties(block.defaultBlockState(), state));
    }
    public static Optional<Block> next(Block block) {
        return Optional.ofNullable(OXIDIZABLES.get(block));
    }
    public static Optional<Block> previous(Block block) {
        return OXIDIZABLES.entrySet().stream().filter(entry -> entry.getValue() == block)
                .map(Map.Entry::getKey).findFirst();
    }
    public static Optional<Block> first(Block block) {
        Block current = block;
        Optional<Block> previous;
        while ((previous = previous(current)).isPresent()) current = previous.orElseThrow();
        return current == block ? Optional.empty() : Optional.of(current);
    }

    private static BlockState copySharedProperties(BlockState target, BlockState source) {
        BlockState result = target;
        for (Property<?> property : source.getProperties()) {
            result = copyProperty(result, source, property);
        }
        return result;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(
            BlockState target, BlockState source, Property<T> property) {
        return target.hasProperty(property)
                ? target.setValue(property, source.getValue(property)) : target;
    }

    private static final class FuelMap implements FuelRegistry {
        private final Map<ItemLike, Integer> items = new LinkedHashMap<>();
        private final Map<TagKey<Item>, Integer> tags = new LinkedHashMap<>();

        @Override public synchronized Integer get(ItemLike item) {
            OptionalInt custom = custom(item);
            return custom.isPresent() ? custom.getAsInt()
                    : AbstractFurnaceBlockEntity.getFuel().get(item.asItem());
        }
        private synchronized OptionalInt custom(ItemLike item) {
            Integer value = null;
            for (Map.Entry<TagKey<Item>, Integer> entry : tags.entrySet()) {
                if (item.asItem().builtInRegistryHolder().is(entry.getKey())) value = entry.getValue();
            }
            if (items.containsKey(item)) value = items.get(item);
            return value == null ? OptionalInt.empty() : OptionalInt.of(value);
        }
        @Override public synchronized void add(ItemLike item, Integer value) {
            items.put(java.util.Objects.requireNonNull(item), java.util.Objects.requireNonNull(value));
            AbstractFurnaceBlockEntity.invalidateCache();
        }
        @Override public synchronized void add(TagKey<Item> tag, Integer value) {
            tags.put(java.util.Objects.requireNonNull(tag), java.util.Objects.requireNonNull(value));
            AbstractFurnaceBlockEntity.invalidateCache();
        }
        @Override public void remove(ItemLike item) { add(item, 0); }
        @Override public void remove(TagKey<Item> tag) { add(tag, 0); }
        @Override public synchronized void clear(ItemLike item) {
            items.remove(item);
            AbstractFurnaceBlockEntity.invalidateCache();
        }
        @Override public synchronized void clear(TagKey<Item> tag) {
            tags.remove(tag);
            AbstractFurnaceBlockEntity.invalidateCache();
        }
    }

    private static final class CompostMap implements CompostingChanceRegistry {
        @Override public Float get(ItemLike item) {
            return ComposterBlock.COMPOSTABLES.getOrDefault(item.asItem(), 0.0F);
        }
        @Override public void add(ItemLike item, Float value) {
            ComposterBlock.COMPOSTABLES.put(item.asItem(), value.floatValue());
        }
        @Override public void add(TagKey<Item> tag, Float value) { unsupportedTags(); }
        @Override public void remove(ItemLike item) { ComposterBlock.COMPOSTABLES.removeFloat(item.asItem()); }
        @Override public void remove(TagKey<Item> tag) { unsupportedTags(); }
        @Override public void clear(ItemLike item) { unsupportedClear(); }
        @Override public void clear(TagKey<Item> tag) { unsupportedClear(); }
        private static void unsupportedTags() {
            throw new UnsupportedOperationException("Tags currently not supported!");
        }
        private static void unsupportedClear() {
            throw new UnsupportedOperationException(
                    "CompostingChanceRegistry operates directly on the vanilla map - clearing not supported!");
        }
    }

    private static final class FlammableMap implements FlammableBlockRegistry {
        private static final Entry REMOVED = new Entry(0, 0);
        private final FireBlock fire;
        private final Map<Block, Entry> blocks = new LinkedHashMap<>();
        private final Map<TagKey<Block>, Entry> tags = new LinkedHashMap<>();

        private FlammableMap(Block fire) { this.fire = (FireBlock) fire; }

        @Override public synchronized Entry get(Block block) {
            Entry value = null;
            for (Map.Entry<TagKey<Block>, Entry> entry : tags.entrySet()) {
                if (block.builtInRegistryHolder().is(entry.getKey())) value = entry.getValue();
            }
            if (blocks.containsKey(block)) value = blocks.get(block);
            if (value != null) return value;
            BlockState state = block.defaultBlockState();
            return new Entry(fire.getBurnOdds(state), fire.getIgniteOdds(state));
        }
        @Override public synchronized void add(Block block, Entry value) {
            blocks.put(block, value);
            apply(block, value);
        }
        @Override public synchronized void add(TagKey<Block> tag, Entry value) {
            tags.put(tag, value);
            applyTags();
        }
        @Override public void remove(Block block) { add(block, REMOVED); }
        @Override public void remove(TagKey<Block> tag) { add(tag, REMOVED); }
        @Override public synchronized void clear(Block block) { blocks.remove(block); }
        @Override public synchronized void clear(TagKey<Block> tag) { tags.remove(tag); }
        private synchronized void applyTags() {
            tags.forEach((tag, entry) -> net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getTag(tag).ifPresent(set -> set.forEach(holder -> apply(holder.value(), entry))));
            blocks.forEach(this::apply);
        }
        private void apply(Block block, Entry entry) {
            fire.setFlammable(block, entry.getBurnChance(), entry.getSpreadChance());
        }
    }
}
