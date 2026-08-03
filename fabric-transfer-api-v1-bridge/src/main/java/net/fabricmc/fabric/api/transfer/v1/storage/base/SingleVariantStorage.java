package net.fabricmc.fabric.api.transfer.v1.storage.base;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Snapshot-backed storage that holds one transfer variant at a time. */
@SuppressWarnings("this-escape")
public abstract class SingleVariantStorage<T extends TransferVariant<?>>
        extends SnapshotParticipant<ResourceAmount<T>> implements SingleSlotStorage<T> {
    private static final Logger LOGGER =
            LoggerFactory.getLogger("fabric-transfer-api-v1/variant-storage");

    public T variant = getBlankVariant();
    public long amount;

    protected abstract T getBlankVariant();

    protected abstract long getCapacity(T variant);

    protected boolean canInsert(T variant) {
        return true;
    }

    protected boolean canExtract(T variant) {
        return true;
    }

    @Override
    public long insert(T insertedVariant, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(insertedVariant, maxAmount);
        if ((insertedVariant.equals(variant) || variant.isBlank())
                && canInsert(insertedVariant)) {
            long insertedAmount = Math.min(maxAmount,
                    getCapacity(insertedVariant) - amount);
            if (insertedAmount > 0) {
                updateSnapshots(transaction);
                if (variant.isBlank()) {
                    variant = insertedVariant;
                    amount = insertedAmount;
                } else {
                    amount += insertedAmount;
                }
                return insertedAmount;
            }
        }
        return 0;
    }

    @Override
    public long extract(T extractedVariant, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(extractedVariant, maxAmount);
        if (extractedVariant.equals(variant) && canExtract(extractedVariant)) {
            long extractedAmount = Math.min(maxAmount, amount);
            if (extractedAmount > 0) {
                updateSnapshots(transaction);
                amount -= extractedAmount;
                if (amount == 0) variant = getBlankVariant();
                return extractedAmount;
            }
        }
        return 0;
    }

    @Override
    public boolean isResourceBlank() {
        return variant.isBlank();
    }

    @Override
    public T getResource() {
        return variant;
    }

    @Override
    public long getAmount() {
        return amount;
    }

    @Override
    public long getCapacity() {
        return getCapacity(variant);
    }

    @Override
    protected ResourceAmount<T> createSnapshot() {
        return new ResourceAmount<>(variant, amount);
    }

    @Override
    protected void readSnapshot(ResourceAmount<T> snapshot) {
        variant = snapshot.resource();
        amount = snapshot.amount();
    }

    @Override
    public String toString() {
        return "SingleVariantStorage[%d %s]".formatted(amount, variant);
    }

    public static <T extends TransferVariant<?>> void readNbt(
            SingleVariantStorage<T> storage, Codec<T> codec, Supplier<T> fallback,
            CompoundTag nbt, HolderLookup.Provider wrapperLookup) {
        RegistryOps<Tag> ops = wrapperLookup.createSerializationContext(NbtOps.INSTANCE);
        DataResult<T> result = codec.parse(ops, nbt.get("variant"));
        if (result.error().isPresent()) {
            LOGGER.debug("Failed to load a TransferVariant from NBT: {}", result.error().get());
            storage.variant = fallback.get();
        } else {
            storage.variant = result.result().orElseGet(fallback);
        }
        storage.amount = nbt.getLong("amount");
    }

    public static <T extends TransferVariant<?>> void writeNbt(
            SingleVariantStorage<T> storage, Codec<T> codec, CompoundTag nbt,
            HolderLookup.Provider wrapperLookup) {
        RegistryOps<Tag> ops = wrapperLookup.createSerializationContext(NbtOps.INSTANCE);
        nbt.put("variant", codec.encode(storage.variant, ops, nbt)
                .getOrThrow(RuntimeException::new));
        nbt.putLong("amount", storage.amount);
    }
}
