package net.fabricmc.fabric.api.item.v1;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.impl.item.DefaultItemComponentImpl;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;

public final class DefaultItemComponentEvents {
    private static final Event<ModifyCallback> BACKING = EventFactory.createArrayBacked(
            ModifyCallback.class, callbacks -> context -> {
                for (ModifyCallback callback : callbacks) callback.modify(context);
            });
    public static final Event<ModifyCallback> MODIFY = new Event<>() {
        {
            invoker = context -> BACKING.invoker().modify(context);
        }

        @Override
        public void register(ModifyCallback listener) {
            BACKING.register(listener);
            DefaultItemComponentImpl.applyIfRegistryReady(listener);
        }

    };

    private DefaultItemComponentEvents() { }

    public interface ModifyContext {
        void modify(Predicate<Item> itemPredicate,
                BiConsumer<DataComponentMap.Builder, Item> builderConsumer);

        default void modify(Item item, Consumer<DataComponentMap.Builder> builderConsumer) {
            modify(Predicate.isEqual(item), (builder, ignored) -> builderConsumer.accept(builder));
        }

        default void modify(Collection<Item> items,
                BiConsumer<DataComponentMap.Builder, Item> builderConsumer) {
            modify(items::contains, builderConsumer);
        }
    }

    @FunctionalInterface
    public interface ModifyCallback {
        void modify(ModifyContext context);
    }
}
