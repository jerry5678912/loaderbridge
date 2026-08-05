package net.fabricmc.fabric.impl.item;

import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public final class DefaultItemComponentImpl {
    private static boolean registryReady;
    private DefaultItemComponentImpl() { }

    public static void modifyItemComponents() {
        DefaultItemComponentEvents.MODIFY.invoker().modify(ModifyContextImpl.INSTANCE);
    }

    public static synchronized void markRegistryReadyAndModify() {
        registryReady = true;
        modifyItemComponents();
    }

    public static synchronized void applyIfRegistryReady(
            DefaultItemComponentEvents.ModifyCallback callback) {
        if (registryReady) callback.modify(ModifyContextImpl.INSTANCE);
    }

    private enum ModifyContextImpl implements DefaultItemComponentEvents.ModifyContext {
        INSTANCE;

        @Override
        public void modify(Predicate<Item> itemPredicate,
                BiConsumer<DataComponentMap.Builder, Item> builderConsumer) {
            for (Item item : BuiltInRegistries.ITEM) {
                if (!itemPredicate.test(item)) continue;
                DataComponentMap.Builder builder = DataComponentMap.builder()
                        .addAll(item.components());
                builderConsumer.accept(builder, item);
                ((ItemExtensions) item).fabric_setDefaultComponents(builder.build());
            }
        }
    }
}
