package dev.loaderbridge.forge;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.eventbus.api.Event;

/** Registers inventory models for items added directly by Fabric entrypoints. */
final class FabricClientModelRegistration {
    private static final String REGISTER_ADDITIONAL_EVENT =
            "net.minecraftforge.client.event.ModelEvent$RegisterAdditional";
    private static final String MODIFY_BAKING_EVENT =
            "net.minecraftforge.client.event.ModelEvent$ModifyBakingResult";
    private static final String BAKING_COMPLETED_EVENT =
            "net.minecraftforge.client.event.ModelEvent$BakingCompleted";
    private static final Tracker TRACKER = new Tracker();
    private static Event lastModelEvent;

    private FabricClientModelRegistration() {
    }

    static void captureBeforeEntrypoints(Event event) {
        TRACKER.begin(itemIds(event.getClass().getClassLoader()));
    }

    static void captureAfterEntrypoints(Event event) {
        TRACKER.complete(itemIds(event.getClass().getClassLoader()));
    }

    static synchronized boolean registerIfModelEvent(Event event) {
        String eventName = event.getClass().getName();
        if (!REGISTER_ADDITIONAL_EVENT.equals(eventName)
                && !MODIFY_BAKING_EVENT.equals(eventName)
                && !BAKING_COMPLETED_EVENT.equals(eventName)) {
            return false;
        }
        if (lastModelEvent == event) {
            return true;
        }
        lastModelEvent = event;
        try {
            ClassLoader gameLoader = event.getClass().getClassLoader();
            Class<?> resourceLocation = gameLoader.loadClass(
                    "net.minecraft.resources.ResourceLocation");
            Class<?> modelLocation = gameLoader.loadClass(
                    "net.minecraft.client.resources.model.ModelResourceLocation");
            var parse = resourceLocation.getMethod("parse", String.class);
            var withPrefix = resourceLocation.getMethod("withPrefix", String.class);
            var inventory = modelLocation.getMethod("inventory", resourceLocation);
            if (REGISTER_ADDITIONAL_EVENT.equals(eventName)) {
                var register = event.getClass().getMethod("register", modelLocation);
                for (String id : TRACKER.newItems()) {
                    Object location = parse.invoke(null, id);
                    Object itemResource = withPrefix.invoke(location, "item/");
                    register.invoke(event, inventory.invoke(null, itemResource));
                }
            } else if (MODIFY_BAKING_EVENT.equals(eventName)) {
                Map<Object, Object> models = castModelMap(
                        event.getClass().getMethod("getModels").invoke(event));
                for (String id : TRACKER.newItems()) {
                    Object location = parse.invoke(null, id);
                    Object logicalModel = inventory.invoke(null, location);
                    Object itemResource = withPrefix.invoke(location, "item/");
                    Object loadedModel = inventory.invoke(null, itemResource);
                    Object baked = models.get(loadedModel);
                    if (baked != null) {
                        models.put(logicalModel, baked);
                    }
                }
            } else {
                registerItemShapes(gameLoader, resourceLocation, modelLocation, parse, inventory);
            }
            return true;
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException
                | IllegalAccessException exception) {
            throw new IllegalStateException(
                    "LB-MODEL-001: Forge item model registration API is unavailable", exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException(
                    "LB-MODEL-002: Forge rejected a Fabric inventory model",
                    exception.getCause());
        }
    }

    private static void registerItemShapes(ClassLoader gameLoader, Class<?> resourceLocation,
            Class<?> modelLocation, java.lang.reflect.Method parse,
            java.lang.reflect.Method inventory)
            throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException,
            IllegalAccessException, InvocationTargetException {
        Class<?> minecraftClass = gameLoader.loadClass("net.minecraft.client.Minecraft");
        Class<?> itemClass = gameLoader.loadClass("net.minecraft.world.item.Item");
        Class<?> itemRendererClass = gameLoader.loadClass(
                "net.minecraft.client.renderer.entity.ItemRenderer");
        Class<?> itemModelShaperClass = gameLoader.loadClass(
                "net.minecraft.client.renderer.ItemModelShaper");
        Class<?> registryClass = gameLoader.loadClass("net.minecraft.core.Registry");
        Class<?> registries = gameLoader.loadClass(
                "net.minecraft.core.registries.BuiltInRegistries");
        Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
        Object itemRenderer = minecraftClass.getMethod("getItemRenderer").invoke(minecraft);
        Object shaper = itemRendererClass.getMethod("getItemModelShaper").invoke(itemRenderer);
        Object items = registries.getField("ITEM").get(null);
        var registryGet = registryClass.getMethod("get", resourceLocation);
        var register = itemModelShaperClass.getMethod("register", itemClass, modelLocation);
        for (String id : TRACKER.newItems()) {
            Object location = parse.invoke(null, id);
            Object item = registryGet.invoke(items, location);
            register.invoke(shaper, item, inventory.invoke(null, location));
        }
        itemModelShaperClass.getMethod("rebuildCache").invoke(shaper);
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> castModelMap(Object value) {
        return (Map<Object, Object>) value;
    }

    private static Set<String> itemIds(ClassLoader gameLoader) {
        try {
            Class<?> registries = gameLoader.loadClass(
                    "net.minecraft.core.registries.BuiltInRegistries");
            Class<?> registry = gameLoader.loadClass("net.minecraft.core.Registry");
            Object items = registries.getField("ITEM").get(null);
            Collection<?> keys = (Collection<?>) registry.getMethod("keySet").invoke(items);
            Set<String> ids = new LinkedHashSet<>();
            keys.forEach(key -> ids.add(key.toString()));
            return ids;
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException
                | IllegalAccessException exception) {
            throw new IllegalStateException(
                    "LB-MODEL-003: Minecraft item registry API is unavailable", exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException(
                    "LB-MODEL-004: Minecraft rejected item model discovery",
                    exception.getCause());
        }
    }

    static final class Tracker {
        private Set<String> before = Set.of();
        private Set<String> added = Set.of();

        synchronized void begin(Collection<String> itemIds) {
            before = Set.copyOf(itemIds);
            added = Set.of();
        }

        synchronized void complete(Collection<String> itemIds) {
            LinkedHashSet<String> difference = new LinkedHashSet<>(itemIds);
            difference.removeAll(before);
            added = Set.copyOf(difference);
        }

        synchronized Set<String> newItems() {
            return added;
        }
    }
}
