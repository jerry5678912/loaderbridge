package net.fabricmc.fabric.api.networking.v1;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;

public final class PacketByteBufs {
    private static final FriendlyByteBuf EMPTY = new FriendlyByteBuf(Unpooled.EMPTY_BUFFER);

    public static FriendlyByteBuf empty() { return EMPTY; }
    public static FriendlyByteBuf create() { return new FriendlyByteBuf(Unpooled.buffer()); }
    public static FriendlyByteBuf readBytes(ByteBuf buf, int length) { return new FriendlyByteBuf(checked(buf).readBytes(length)); }
    public static FriendlyByteBuf readSlice(ByteBuf buf, int length) { return new FriendlyByteBuf(checked(buf).readSlice(length)); }
    public static FriendlyByteBuf readRetainedSlice(ByteBuf buf, int length) { return new FriendlyByteBuf(checked(buf).readRetainedSlice(length)); }
    public static FriendlyByteBuf copy(ByteBuf buf) { return new FriendlyByteBuf(checked(buf).copy()); }
    public static FriendlyByteBuf copy(ByteBuf buf, int index, int length) { return new FriendlyByteBuf(checked(buf).copy(index, length)); }
    public static FriendlyByteBuf slice(ByteBuf buf) { return new FriendlyByteBuf(checked(buf).slice()); }
    public static FriendlyByteBuf retainedSlice(ByteBuf buf) { return new FriendlyByteBuf(checked(buf).retainedSlice()); }
    public static FriendlyByteBuf slice(ByteBuf buf, int index, int length) { return new FriendlyByteBuf(checked(buf).slice(index, length)); }
    public static FriendlyByteBuf retainedSlice(ByteBuf buf, int index, int length) { return new FriendlyByteBuf(checked(buf).retainedSlice(index, length)); }
    public static FriendlyByteBuf duplicate(ByteBuf buf) { return new FriendlyByteBuf(checked(buf).duplicate()); }
    public static FriendlyByteBuf retainedDuplicate(ByteBuf buf) { return new FriendlyByteBuf(checked(buf).retainedDuplicate()); }

    private static ByteBuf checked(ByteBuf buf) {
        return Objects.requireNonNull(buf, "ByteBuf cannot be null");
    }

    private PacketByteBufs() { }
}
