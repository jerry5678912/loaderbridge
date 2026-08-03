package net.fabricmc.fabric.api.transfer.v1.storage;

import java.util.Objects;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;

/** Immutable object and component association used by item and fluid variants. */
public interface TransferVariant<O> {
    boolean isBlank();

    O getObject();

    DataComponentPatch getComponents();

    DataComponentMap getComponentMap();

    default boolean hasComponents() {
        return !getComponents().isEmpty();
    }

    default boolean componentsMatch(DataComponentPatch other) {
        return Objects.equals(getComponents(), other);
    }

    default boolean isOf(O object) {
        return getObject() == object;
    }

    default TransferVariant<O> withComponentChanges(DataComponentPatch changes) {
        throw new UnsupportedOperationException(
                "withComponentChanges is not supported by this TransferVariant");
    }
}
