package net.fabricmc.fabric.api.attachment.v1;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface AttachmentTarget {
    String NBT_ATTACHMENT_KEY = "fabric:attachments";

    default <A> A getAttached(AttachmentType<A> type) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    default <A> A getAttachedOrThrow(AttachmentType<A> type) {
        return Objects.requireNonNull(getAttached(type), "No value was attached");
    }

    default <A> A getAttachedOrSet(AttachmentType<A> type, A defaultValue) {
        Objects.requireNonNull(defaultValue, "default value cannot be null");
        A attached = getAttached(type);
        if (attached != null) return attached;
        setAttached(type, defaultValue);
        return defaultValue;
    }

    default <A> A getAttachedOrCreate(AttachmentType<A> type, Supplier<A> initializer) {
        A attached = getAttached(type);
        if (attached != null) return attached;
        A initialized = Objects.requireNonNull(initializer.get(),
                "initializer result cannot be null");
        setAttached(type, initialized);
        return initialized;
    }

    default <A> A getAttachedOrCreate(AttachmentType<A> type) {
        Supplier<A> initializer = type.initializer();
        if (initializer == null) {
            throw new IllegalArgumentException("Single-argument getAttachedOrCreate is reserved "
                    + "for attachment types with default initializers");
        }
        return getAttachedOrCreate(type, initializer);
    }

    default <A> A getAttachedOrElse(AttachmentType<A> type, A defaultValue) {
        A attached = getAttached(type);
        return attached == null ? defaultValue : attached;
    }

    default <A> A getAttachedOrGet(AttachmentType<A> type, Supplier<A> defaultValue) {
        Objects.requireNonNull(defaultValue, "default value supplier cannot be null");
        A attached = getAttached(type);
        return attached == null ? defaultValue.get() : attached;
    }

    default <A> A setAttached(AttachmentType<A> type, A value) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    default boolean hasAttached(AttachmentType<?> type) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    default <A> A removeAttached(AttachmentType<A> type) { return setAttached(type, null); }

    default <A> A modifyAttached(AttachmentType<A> type, UnaryOperator<A> modifier) {
        return setAttached(type, modifier.apply(getAttached(type)));
    }
}
