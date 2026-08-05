package net.fabricmc.fabric.api.transfer.v1.fluid;

import java.util.Objects;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

/** Helpers for moving a single fluid variant between players and storages. */
public final class FluidStorageUtil {
    public static boolean interactWithFluidStorage(
            Storage<FluidVariant> storage, Player player, InteractionHand hand) {
        Storage<FluidVariant> handStorage =
                ContainerItemContext.forPlayerInteraction(player, hand).find(FluidStorage.ITEM);
        if (handStorage == null) return false;
        Item handItem = player.getItemInHand(hand).getItem();
        try {
            return moveWithSound(storage, handStorage, player, true, handItem)
                    || moveWithSound(handStorage, storage, player, false, handItem);
        } catch (Exception exception) {
            CrashReport report = CrashReport.forThrowable(exception, "Interacting with fluid storage");
            report.addCategory("Interaction details")
                    .setDetail("Player", player::toString)
                    .setDetail("Hand", hand)
                    .setDetail("Hand item", handItem::toString)
                    .setDetail("Fluid storage", () -> Objects.toString(storage, null));
            throw new ReportedException(report);
        }
    }

    private static boolean moveWithSound(Storage<FluidVariant> from,
            Storage<FluidVariant> to, Player player, boolean fill, Item handItem) {
        for (StorageView<FluidVariant> view : from) {
            if (view.isResourceBlank()) continue;
            FluidVariant resource = view.getResource();
            long maximum;
            try (Transaction test = Transaction.openOuter()) {
                maximum = view.extract(resource, Long.MAX_VALUE, test);
            }
            try (Transaction transfer = Transaction.openOuter()) {
                long accepted = to.insert(resource, maximum, transfer);
                if (accepted <= 0 || view.extract(resource, accepted, transfer) != accepted) {
                    continue;
                }
                transfer.commit();
                SoundEvent sound = fill
                        ? FluidVariantAttributes.getFillSound(resource)
                        : FluidVariantAttributes.getEmptySound(resource);
                if (resource.isOf(Fluids.WATER)) {
                    if (fill && handItem == Items.GLASS_BOTTLE) sound = SoundEvents.BOTTLE_FILL;
                    if (!fill && handItem == Items.POTION) sound = SoundEvents.BOTTLE_EMPTY;
                }
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        sound, SoundSource.BLOCKS, 1.0F, 1.0F);
                return true;
            }
        }
        return false;
    }

    private FluidStorageUtil() { }
}
