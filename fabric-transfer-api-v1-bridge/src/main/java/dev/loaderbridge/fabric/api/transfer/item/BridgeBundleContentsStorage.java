package dev.loaderbridge.fabric.api.transfer.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;

/** Transactional item storage backed by vanilla bundle contents. */
public final class BridgeBundleContentsStorage implements Storage<ItemVariant> {
    private final ContainerItemContext context;
    private final Item originalItem;
    private final List<BundleView> viewCache = new ArrayList<>();
    private List<StorageView<ItemVariant>> views = List.of();

    public BridgeBundleContentsStorage(ContainerItemContext context) {
        this.context = context;
        this.originalItem = context.getItemVariant().getItem();
    }

    private BundleContents contents() {
        return context.getItemVariant().getComponentMap().getOrDefault(
                DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
    }

    private boolean isStillValid() {
        return context.getItemVariant().getItem() == originalItem;
    }

    private boolean update(BundleContents contents, TransactionContext transaction) {
        ItemVariant updated = context.getItemVariant().withComponentChanges(
                DataComponentPatch.builder().set(DataComponents.BUNDLE_CONTENTS, contents).build());
        return context.exchange(updated, 1, transaction) == 1;
    }

    @Override
    public long insert(ItemVariant resource, long maximum, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maximum);
        if (!isStillValid() || !resource.getItem().canFitInsideContainerItems()) return 0;
        int requested = (int) Math.min(maximum, Integer.MAX_VALUE);
        BundleContents.Mutable mutable = new BundleContents.Mutable(contents());
        int inserted = mutable.tryInsert(resource.toStack(requested));
        return inserted > 0 && update(mutable.toImmutable(), transaction) ? inserted : 0;
    }

    @Override
    public long extract(ItemVariant resource, long maximum, TransactionContext transaction) {
        StoragePreconditions.notNegative(maximum);
        if (!isStillValid()) return 0;
        refreshViews();
        long extracted = 0;
        for (StorageView<ItemVariant> view : views) {
            extracted += view.extract(resource, maximum - extracted, transaction);
            if (extracted == maximum) break;
        }
        return extracted;
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        refreshViews();
        return views.iterator();
    }

    private void refreshViews() {
        int size = contents().size();
        if (views.size() == size) return;
        while (viewCache.size() < size) viewCache.add(new BundleView(viewCache.size()));
        views = Collections.unmodifiableList(new ArrayList<>(viewCache.subList(0, size)));
    }

    private final class BundleView implements StorageView<ItemVariant> {
        private final int index;

        private BundleView(int index) {
            this.index = index;
        }

        private ItemStack getStack() {
            return contents().size() > index ? contents().getItemUnsafe(index) : ItemStack.EMPTY;
        }

        @Override
        public long extract(ItemVariant resource, long maximum,
                TransactionContext transaction) {
            StoragePreconditions.notNegative(maximum);
            if (!isStillValid() || contents().size() <= index
                    || !resource.matches(getStack())) return 0;
            List<ItemStack> stacks = new ArrayList<>();
            contents().itemsCopy().forEach(stacks::add);
            int extracted = (int) Math.min(maximum, stacks.get(index).getCount());
            if (extracted <= 0) return 0;
            stacks.get(index).shrink(extracted);
            if (stacks.get(index).isEmpty()) stacks.remove(index);
            return update(new BundleContents(stacks), transaction) ? extracted : 0;
        }

        @Override public boolean isResourceBlank() { return getStack().isEmpty(); }
        @Override public ItemVariant getResource() { return ItemVariant.of(getStack()); }
        @Override public long getAmount() { return getStack().getCount(); }

        @Override
        public long getCapacity() {
            ItemStack stack = getStack();
            if (stack.isEmpty()) return 0;
            Fraction remaining = Fraction.ONE.subtract(contents().weight());
            Fraction unitWeight = new BundleContents(
                    List.of(stack.copyWithCount(1))).weight();
            return getAmount() + Math.max(remaining.divideBy(unitWeight).intValue(), 0);
        }

        @Override
        public String toString() {
            return "BundleContentsView[" + context.getItemVariant() + '#' + index + ']';
        }
    }
}
