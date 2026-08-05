package net.fabricmc.fabric.api.screenhandler.v1;

import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Menu type that creates its client menu from synchronized opening data. */
public class ExtendedScreenHandlerType<T extends AbstractContainerMenu, D> extends MenuType<T> {
    private final ExtendedFactory<T, D> factory;
    private final StreamCodec<? super RegistryFriendlyByteBuf, D> packetCodec;

    public ExtendedScreenHandlerType(ExtendedFactory<T, D> factory,
            StreamCodec<? super RegistryFriendlyByteBuf, D> packetCodec) {
        super(null, FeatureFlags.VANILLA_SET);
        this.factory = Objects.requireNonNull(factory, "screen handler factory cannot be null");
        this.packetCodec = Objects.requireNonNull(packetCodec, "packet codec cannot be null");
    }

    @Deprecated
    @Override
    public final T create(int syncId, Inventory inventory) {
        throw new UnsupportedOperationException(
                "Use ExtendedScreenHandlerType.create(int, PlayerInventory, PacketByteBuf)!");
    }

    public T create(int syncId, Inventory inventory, D data) {
        return factory.create(syncId, inventory, data);
    }

    public StreamCodec<? super RegistryFriendlyByteBuf, D> getPacketCodec() {
        return packetCodec;
    }

    @FunctionalInterface
    public interface ExtendedFactory<T extends AbstractContainerMenu, D> {
        T create(int syncId, Inventory inventory, D data);
    }
}
