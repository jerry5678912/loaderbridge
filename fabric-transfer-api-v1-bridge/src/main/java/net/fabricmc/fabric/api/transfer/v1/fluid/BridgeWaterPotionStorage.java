package net.fabricmc.fabric.api.transfer.v1.fluid;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ExtractionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;

final class BridgeWaterPotionStorage
        implements ExtractionOnlyStorage<FluidVariant>, SingleSlotStorage<FluidVariant> {
    private static final FluidVariant WATER = FluidVariant.of(Fluids.WATER);
    private final ContainerItemContext context;

    static BridgeWaterPotionStorage find(ContainerItemContext context) {
        return isWaterPotion(context) ? new BridgeWaterPotionStorage(context) : null;
    }

    private static boolean isWaterPotion(ContainerItemContext context) {
        PotionContents contents = context.getItemVariant().getComponentMap()
                .getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return context.getItemVariant().isOf(Items.POTION) && contents.is(Potions.WATER);
    }

    private BridgeWaterPotionStorage(ContainerItemContext context) {
        this.context = context;
    }

    private ItemVariant glassBottle() {
        var stack = context.getItemVariant().toStack();
        stack.remove(DataComponents.POTION_CONTENTS);
        return ItemVariant.of(Items.GLASS_BOTTLE, stack.getComponentsPatch());
    }

    @Override public long extract(FluidVariant resource, long maximum,
            TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maximum);
        if (isWaterPotion(context) && resource.equals(WATER)
                && maximum >= FluidConstants.BOTTLE
                && context.exchange(glassBottle(), 1, transaction) == 1) {
            return FluidConstants.BOTTLE;
        }
        return 0;
    }

    @Override public boolean isResourceBlank() { return getResource().isBlank(); }
    @Override public FluidVariant getResource() {
        return isWaterPotion(context) ? WATER : FluidVariant.blank();
    }
    @Override public long getAmount() {
        return isWaterPotion(context) ? FluidConstants.BOTTLE : 0;
    }
    @Override public long getCapacity() { return getAmount(); }
}
