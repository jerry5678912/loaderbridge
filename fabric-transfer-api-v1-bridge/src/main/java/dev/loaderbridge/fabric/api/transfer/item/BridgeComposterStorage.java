package dev.loaderbridge.fabric.api.transfer.item;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ExtractionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Transactional Fabric-style storage for vanilla composters. */
public final class BridgeComposterStorage extends SnapshotParticipant<Float> {
    private static final float NONE = 0.0F;
    private static final float EXTRACT_BONE_MEAL = -1.0F;
    private static final Map<Level, Map<BlockPos, WeakReference<BridgeComposterStorage>>> WRAPPERS =
            new WeakHashMap<>();

    private final Level level;
    private final BlockPos position;
    private final Storage<ItemVariant> top = new TopStorage();
    private final Storage<ItemVariant> bottom = new BottomStorage();
    private float pendingAction;

    private BridgeComposterStorage(Level level, BlockPos position) {
        this.level = level;
        this.position = position.immutable();
    }

    public static Storage<ItemVariant> find(Level level, BlockPos position,
            BlockState state, net.minecraft.world.level.block.entity.BlockEntity blockEntity,
            Direction direction) {
        if (direction == null || direction.getAxis() != Direction.Axis.Y) return null;
        BridgeComposterStorage wrapper;
        synchronized (WRAPPERS) {
            Map<BlockPos, WeakReference<BridgeComposterStorage>> levelWrappers =
                    WRAPPERS.computeIfAbsent(level, ignored -> new HashMap<>());
            WeakReference<BridgeComposterStorage> reference =
                    levelWrappers.get(position);
            wrapper = reference == null ? null : reference.get();
            if (wrapper == null) {
                wrapper = new BridgeComposterStorage(level, position);
                levelWrappers.put(position.immutable(), new WeakReference<>(wrapper));
            }
        }
        return direction == Direction.UP ? wrapper.top : wrapper.bottom;
    }

    private BlockState state() {
        return level.getBlockState(position);
    }

    @Override protected Float createSnapshot() { return pendingAction; }
    @Override protected void readSnapshot(Float snapshot) { pendingAction = snapshot; }

    @Override
    protected void onFinalCommit() {
        BlockState state = state();
        if (!state.hasProperty(ComposterBlock.LEVEL)) {
            pendingAction = NONE;
            return;
        }
        if (pendingAction == EXTRACT_BONE_MEAL) {
            level.setBlockAndUpdate(position, state.setValue(ComposterBlock.LEVEL, 0));
            level.playSound(null, position, SoundEvents.COMPOSTER_EMPTY,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (pendingAction > 0.0F) {
            int oldLevel = state.getValue(ComposterBlock.LEVEL);
            boolean successful = oldLevel == 0
                    || level.getRandom().nextFloat() < pendingAction;
            if (successful) {
                int newLevel = oldLevel + 1;
                level.setBlockAndUpdate(position, state.setValue(ComposterBlock.LEVEL, newLevel));
                if (newLevel == 7) level.scheduleTick(position, state.getBlock(), 20);
            }
            level.levelEvent(1500, position, successful ? 1 : 0);
        }
        pendingAction = NONE;
    }

    private final class TopStorage implements InsertionOnlyStorage<ItemVariant> {
        @Override
        public long insert(ItemVariant resource, long maximum,
                TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maximum);
            BlockState state = state();
            if (maximum < 1 || pendingAction != NONE
                    || !state.hasProperty(ComposterBlock.LEVEL)
                    || state.getValue(ComposterBlock.LEVEL) >= 7) return 0;
            float chance = ComposterBlock.COMPOSTABLES.getFloat(resource.getItem());
            if (chance <= 0.0F) return 0;
            updateSnapshots(transaction);
            pendingAction = chance;
            return 1;
        }

        @Override public String toString() { return "ComposterStorage[" + position + "/top]"; }
    }

    private final class BottomStorage implements ExtractionOnlyStorage<ItemVariant>,
            SingleSlotStorage<ItemVariant> {
        private final ItemVariant boneMeal = ItemVariant.of(Items.BONE_MEAL);

        private boolean hasBoneMeal() {
            BlockState state = state();
            return pendingAction == NONE && state.hasProperty(ComposterBlock.LEVEL)
                    && state.getValue(ComposterBlock.LEVEL) == ComposterBlock.READY;
        }

        @Override
        public long extract(ItemVariant resource, long maximum,
                TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maximum);
            if (maximum < 1 || !boneMeal.equals(resource) || !hasBoneMeal()) return 0;
            updateSnapshots(transaction);
            pendingAction = EXTRACT_BONE_MEAL;
            return 1;
        }

        @Override public boolean isResourceBlank() { return boneMeal.isBlank(); }
        @Override public ItemVariant getResource() { return boneMeal; }
        @Override public long getAmount() { return hasBoneMeal() ? 1 : 0; }
        @Override public long getCapacity() { return 1; }
        @Override public String toString() { return "ComposterStorage[" + position + "/bottom]"; }
    }
}
