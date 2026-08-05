package net.fabricmc.fabric.api.transfer.v1.fluid;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class BridgeCauldronStorage extends SnapshotParticipant<BlockState>
        implements SingleSlotStorage<FluidVariant> {
    private record Location(Level world, BlockPos pos) { }
    private static final Map<Location, WeakReference<BridgeCauldronStorage>> CACHE =
            new HashMap<>();

    static synchronized BridgeCauldronStorage get(Level world, BlockPos pos) {
        CACHE.entrySet().removeIf(entry -> entry.getValue().get() == null);
        Location location = new Location(world, pos.immutable());
        WeakReference<BridgeCauldronStorage> reference = CACHE.get(location);
        BridgeCauldronStorage storage = reference == null ? null : reference.get();
        if (storage == null) {
            storage = new BridgeCauldronStorage(location);
            CACHE.put(location, new WeakReference<>(storage));
        }
        return storage;
    }

    private final Location location;
    private BlockState lastReleasedSnapshot;

    private BridgeCauldronStorage(Location location) {
        this.location = location;
    }

    private CauldronFluidContent currentContent() {
        CauldronFluidContent content = CauldronFluidContent.getForBlock(
                createSnapshot().getBlock());
        if (content == null) {
            throw new IllegalStateException("No registered cauldron at " + location);
        }
        return content;
    }

    private void updateLevel(CauldronFluidContent content, int level,
            TransactionContext transaction) {
        updateSnapshots(transaction);
        BlockState state = content.block.defaultBlockState();
        if (content.levelProperty != null) state = state.setValue(content.levelProperty, level);
        location.world.setBlock(location.pos, state, 0);
    }

    @Override public long insert(FluidVariant resource, long maximum,
            TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maximum);
        CauldronFluidContent inserted = CauldronFluidContent.getForFluid(resource.getFluid());
        if (inserted == null) return 0;
        int maximumLevels = saturatedInt(maximum / inserted.amountPerLevel);
        if (getAmount() == 0) {
            int levels = Math.min(maximumLevels, inserted.maxLevel);
            if (levels > 0) updateLevel(inserted, levels, transaction);
            return levels * inserted.amountPerLevel;
        }
        CauldronFluidContent current = currentContent();
        if (!resource.isOf(current.fluid)) return 0;
        int currentLevel = current.currentLevel(createSnapshot());
        int levels = Math.min(maximumLevels, current.maxLevel - currentLevel);
        if (levels > 0) updateLevel(current, currentLevel + levels, transaction);
        return levels * current.amountPerLevel;
    }

    @Override public long extract(FluidVariant resource, long maximum,
            TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maximum);
        CauldronFluidContent current = currentContent();
        if (!resource.isOf(current.fluid)) return 0;
        int currentLevel = current.currentLevel(createSnapshot());
        int levels = Math.min(saturatedInt(maximum / current.amountPerLevel), currentLevel);
        if (levels > 0) {
            if (levels == currentLevel) {
                updateSnapshots(transaction);
                location.world.setBlock(location.pos, Blocks.CAULDRON.defaultBlockState(), 0);
            } else {
                updateLevel(current, currentLevel - levels, transaction);
            }
        }
        return levels * current.amountPerLevel;
    }

    @Override public boolean isResourceBlank() { return getResource().isBlank(); }
    @Override public FluidVariant getResource() {
        return FluidVariant.of(currentContent().fluid);
    }
    @Override public long getAmount() {
        CauldronFluidContent content = currentContent();
        return content.currentLevel(createSnapshot()) * content.amountPerLevel;
    }
    @Override public long getCapacity() {
        CauldronFluidContent content = currentContent();
        return content.maxLevel * content.amountPerLevel;
    }

    @Override protected BlockState createSnapshot() {
        return location.world.getBlockState(location.pos);
    }
    @Override protected void readSnapshot(BlockState snapshot) {
        location.world.setBlock(location.pos, snapshot, 0);
    }
    @Override protected void releaseSnapshot(BlockState snapshot) {
        lastReleasedSnapshot = snapshot;
    }
    @Override protected void onFinalCommit() {
        BlockState state = createSnapshot();
        if (lastReleasedSnapshot != state) {
            location.world.setBlock(location.pos, lastReleasedSnapshot, 0);
            location.world.setBlockAndUpdate(location.pos, state);
        }
    }

    private static int saturatedInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
