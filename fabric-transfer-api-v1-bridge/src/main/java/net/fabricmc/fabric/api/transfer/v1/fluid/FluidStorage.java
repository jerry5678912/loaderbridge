package net.fabricmc.fabric.api.transfer.v1.fluid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.FullItemFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.BlankVariantView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Standard Fabric block and item lookups for fluid-variant storage. */
public final class FluidStorage {
    public static final BlockApiLookup<Storage<FluidVariant>, Direction> SIDED =
            BlockApiLookup.get(ResourceLocation.fromNamespaceAndPath(
                    "fabric", "sided_fluid_storage"), Storage.asClass(), Direction.class);
    public static final ItemApiLookup<Storage<FluidVariant>, ContainerItemContext> ITEM =
            ItemApiLookup.get(ResourceLocation.fromNamespaceAndPath(
                    "fabric", "fluid_storage"), Storage.asClass(), ContainerItemContext.class);
    public static final Event<CombinedItemApiProvider> GENERAL_COMBINED_PROVIDER =
            createCombinedEvent(false);

    static {
        SIDED.registerFallback((world, pos, state, blockEntity, direction) ->
                blockEntity instanceof SidedStorageBlockEntity provider
                        ? provider.getFluidStorage(direction) : null);
        ITEM.registerFallback((stack, context) -> GENERAL_COMBINED_PROVIDER.invoker().find(context));
        combinedItemApiProvider(Items.BUCKET).register(EmptyBucketStorage::new);
        GENERAL_COMBINED_PROVIDER.register(context -> {
            Item item = context.getItemVariant().getItem();
            if (item instanceof BucketItem bucket) {
                Fluid fluid = bucket.getFluid();
                if (fluid != Fluids.EMPTY && fluid.getBucket() == item) {
                    return new FullItemFluidStorage(context, Items.BUCKET,
                            FluidVariant.of(fluid), FluidConstants.BUCKET);
                }
            }
            return null;
        });
    }

    public static Event<CombinedItemApiProvider> combinedItemApiProvider(Item item) {
        ItemApiLookup.ItemApiProvider<Storage<FluidVariant>, ContainerItemContext> existing =
                ITEM.getProvider(item);
        if (existing == null) {
            CombinedProvider created = new CombinedProvider();
            ITEM.registerForItems(created, item);
            existing = ITEM.getProvider(item);
        }
        if (!(existing instanceof CombinedProvider combined)) {
            throw new IllegalStateException(
                    "An incompatible provider was already registered for item " + item
                            + ". Provider: " + existing + '.');
        }
        return combined.event;
    }

    private static Event<CombinedItemApiProvider> createCombinedEvent(boolean includeGeneral) {
        return EventFactory.createArrayBacked(CombinedItemApiProvider.class, listeners -> context -> {
            List<Storage<FluidVariant>> storages = new ArrayList<>();
            for (CombinedItemApiProvider listener : listeners) {
                Storage<FluidVariant> found = listener.find(context);
                if (found != null) storages.add(found);
            }
            if (!storages.isEmpty() && includeGeneral) {
                Storage<FluidVariant> general = GENERAL_COMBINED_PROVIDER.invoker().find(context);
                if (general != null) storages.add(general);
            }
            return storages.isEmpty() ? null : new CombinedStorage<>(storages);
        });
    }

    @FunctionalInterface
    public interface CombinedItemApiProvider {
        Storage<FluidVariant> find(ContainerItemContext context);
    }

    private static final class CombinedProvider implements
            ItemApiLookup.ItemApiProvider<Storage<FluidVariant>, ContainerItemContext> {
        private final Event<CombinedItemApiProvider> event = createCombinedEvent(true);
        @Override public Storage<FluidVariant> find(
                net.minecraft.world.item.ItemStack stack, ContainerItemContext context) {
            if (!context.getItemVariant().matches(stack)) {
                throw new IllegalArgumentException(
                        "Query stack " + stack + " and ContainerItemContext variant "
                                + context.getItemVariant() + " don't match.");
            }
            return event.invoker().find(context);
        }
    }

    private static final class EmptyBucketStorage
            implements InsertionOnlyStorage<FluidVariant> {
        private final ContainerItemContext context;
        private final List<StorageView<FluidVariant>> blankView = List.of(
                new BlankVariantView<>(FluidVariant.blank(), FluidConstants.BUCKET));

        private EmptyBucketStorage(ContainerItemContext context) { this.context = context; }

        @Override public long insert(FluidVariant resource, long maximum,
                TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maximum);
            if (!context.getItemVariant().isOf(Items.BUCKET)) return 0;
            Item fullBucket = resource.getFluid().getBucket();
            if (fullBucket instanceof BucketItem bucket
                    && bucket.getFluid() == resource.getFluid()
                    && maximum >= FluidConstants.BUCKET) {
                ItemVariant full = ItemVariant.of(
                        fullBucket, context.getItemVariant().getComponents());
                if (context.exchange(full, 1, transaction) == 1) {
                    return FluidConstants.BUCKET;
                }
            }
            return 0;
        }

        @Override public Iterator<StorageView<FluidVariant>> iterator() {
            return blankView.iterator();
        }
    }

    private FluidStorage() { }
}
