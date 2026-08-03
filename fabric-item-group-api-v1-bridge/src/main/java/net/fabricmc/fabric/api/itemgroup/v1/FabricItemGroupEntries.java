package net.fabricmc.fabric.api.itemgroup.v1;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/** Mutable Fabric view over a creative tab's parent and search entries. */
public class FabricItemGroupEntries implements CreativeModeTab.Output {
    private final CreativeModeTab.ItemDisplayParameters context;
    private final List<ItemStack> displayStacks;
    private final List<ItemStack> searchTabStacks;

    public FabricItemGroupEntries(CreativeModeTab.ItemDisplayParameters context,
            List<ItemStack> displayStacks, List<ItemStack> searchTabStacks) {
        this.context = Objects.requireNonNull(context, "context");
        this.displayStacks = Objects.requireNonNull(displayStacks, "displayStacks");
        this.searchTabStacks = Objects.requireNonNull(searchTabStacks, "searchTabStacks");
    }

    public CreativeModeTab.ItemDisplayParameters getContext() { return context; }
    public FeatureFlagSet getEnabledFeatures() { return context.enabledFeatures(); }
    public boolean shouldShowOpRestrictedItems() { return context.hasPermissions(); }
    public List<ItemStack> getDisplayStacks() { return displayStacks; }
    public List<ItemStack> getSearchTabStacks() { return searchTabStacks; }

    @Override
    public void accept(ItemStack stack, CreativeModeTab.TabVisibility visibility) {
        if (!stack.isItemEnabled(context.enabledFeatures())) return;
        checkStack(stack);
        if (visibility != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY) displayStacks.add(stack);
        if (visibility != CreativeModeTab.TabVisibility.PARENT_TAB_ONLY) searchTabStacks.add(stack);
    }

    public void prepend(ItemStack stack) {
        prepend(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }

    public void prepend(ItemStack stack, CreativeModeTab.TabVisibility visibility) {
        if (!stack.isItemEnabled(context.enabledFeatures())) return;
        checkStack(stack);
        if (visibility != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY) displayStacks.addFirst(stack);
        if (visibility != CreativeModeTab.TabVisibility.PARENT_TAB_ONLY) searchTabStacks.addFirst(stack);
    }

    public void prepend(ItemLike item) { prepend(new ItemStack(item)); }
    public void prepend(ItemLike item, CreativeModeTab.TabVisibility visibility) {
        prepend(new ItemStack(item), visibility);
    }

    public void addAfter(ItemLike after, ItemStack... stacks) { addAfter(after, Arrays.asList(stacks)); }
    public void addAfter(ItemStack after, ItemStack... stacks) { addAfter(after, Arrays.asList(stacks)); }
    public void addAfter(ItemLike after, ItemLike... items) { addAfter(after, stacks(items)); }
    public void addAfter(ItemStack after, ItemLike... items) { addAfter(after, stacks(items)); }
    public void addAfter(ItemLike after, Collection<ItemStack> stacks) {
        addAfter(after, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
    public void addAfter(ItemStack after, Collection<ItemStack> stacks) {
        addAfter(after, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
    public void addAfter(ItemLike after, Collection<ItemStack> stacks,
            CreativeModeTab.TabVisibility visibility) {
        addAfter(candidate -> candidate.is(after.asItem()), stacks, visibility);
    }
    public void addAfter(ItemStack after, Collection<ItemStack> stacks,
            CreativeModeTab.TabVisibility visibility) {
        addAfter(candidate -> ItemStack.isSameItemSameComponents(candidate, after), stacks, visibility);
    }
    public void addAfter(Predicate<ItemStack> predicate, Collection<ItemStack> stacks,
            CreativeModeTab.TabVisibility visibility) {
        insert(predicate, enabled(stacks), visibility, true);
    }

    public void addBefore(ItemLike before, ItemStack... stacks) { addBefore(before, Arrays.asList(stacks)); }
    public void addBefore(ItemStack before, ItemStack... stacks) { addBefore(before, Arrays.asList(stacks)); }
    public void addBefore(ItemLike before, ItemLike... items) { addBefore(before, stacks(items)); }
    public void addBefore(ItemStack before, ItemLike... items) { addBefore(before, stacks(items)); }
    public void addBefore(ItemLike before, Collection<ItemStack> stacks) {
        addBefore(before, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
    public void addBefore(ItemStack before, Collection<ItemStack> stacks) {
        addBefore(before, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
    public void addBefore(ItemLike before, Collection<ItemStack> stacks,
            CreativeModeTab.TabVisibility visibility) {
        addBefore(candidate -> candidate.is(before.asItem()), stacks, visibility);
    }
    public void addBefore(ItemStack before, Collection<ItemStack> stacks,
            CreativeModeTab.TabVisibility visibility) {
        addBefore(candidate -> ItemStack.isSameItemSameComponents(candidate, before), stacks, visibility);
    }
    public void addBefore(Predicate<ItemStack> predicate, Collection<ItemStack> stacks,
            CreativeModeTab.TabVisibility visibility) {
        insert(predicate, enabled(stacks), visibility, false);
    }

    private void insert(Predicate<ItemStack> anchor, Collection<ItemStack> stacks,
            CreativeModeTab.TabVisibility visibility, boolean after) {
        stacks.forEach(FabricItemGroupEntries::checkStack);
        if (visibility != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY) {
            insert(displayStacks, anchor, stacks, after);
        }
        if (visibility != CreativeModeTab.TabVisibility.PARENT_TAB_ONLY) {
            insert(searchTabStacks, anchor, stacks, after);
        }
    }

    private static void insert(List<ItemStack> target, Predicate<ItemStack> anchor,
            Collection<ItemStack> stacks, boolean after) {
        int found = -1;
        for (int index = 0; index < target.size(); index++) {
            if (anchor.test(target.get(index))) found = index;
        }
        if (found < 0) throw new IllegalArgumentException("Anchor stack is not in the item group");
        target.addAll(after ? found + 1 : found, stacks);
    }

    private Collection<ItemStack> enabled(Collection<ItemStack> stacks) {
        return stacks.stream().filter(stack -> stack.isItemEnabled(context.enabledFeatures())).toList();
    }

    private static List<ItemStack> stacks(ItemLike[] items) {
        return Arrays.stream(items).map(ItemStack::new).toList();
    }

    private static void checkStack(ItemStack stack) {
        if (stack.getCount() != 1) {
            throw new IllegalArgumentException("Item group stacks must have a count of exactly one");
        }
    }
}
