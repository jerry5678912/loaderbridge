package dev.loaderbridge.forge;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import net.minecraftforge.eventbus.api.Event;

/** Maps custom Fabric cooking recipe types into Forge's vanilla recipe-book tabs. */
final class FabricClientRecipeBookRegistration {
    private static final System.Logger LOGGER =
            System.getLogger(FabricClientRecipeBookRegistration.class.getName());
    private static final String REGISTER_EVENT =
            "net.minecraftforge.client.event.RegisterRecipeBookCategoriesEvent";
    private static final Tracker TRACKER = new Tracker();
    private static Event lastEvent;

    private FabricClientRecipeBookRegistration() {
    }

    static void captureBeforeEntrypoints(Event event) {
        TRACKER.begin(recipeTypeIds(event.getClass().getClassLoader()));
    }

    static void captureAfterEntrypoints(Event event) {
        TRACKER.complete(recipeTypeIds(event.getClass().getClassLoader()));
        LOGGER.log(System.Logger.Level.INFO,
                "LoaderBridge discovered Fabric recipe types: {0}", TRACKER.newRecipeTypes());
    }

    static synchronized boolean registerIfEvent(Event event) {
        if (!REGISTER_EVENT.equals(event.getClass().getName())) {
            return false;
        }
        if (lastEvent == event) {
            return true;
        }
        lastEvent = event;
        try {
            ClassLoader gameLoader = event.getClass().getClassLoader();
            Class<?> resourceLocation = gameLoader.loadClass(
                    "net.minecraft.resources.ResourceLocation");
            Class<?> registryClass = gameLoader.loadClass("net.minecraft.core.Registry");
            Class<?> recipeTypeClass = gameLoader.loadClass(
                    "net.minecraft.world.item.crafting.RecipeType");
            Class<?> cookingRecipeClass = gameLoader.loadClass(
                    "net.minecraft.world.item.crafting.AbstractCookingRecipe");
            Class<?> categoriesClass = gameLoader.loadClass(
                    "net.minecraft.client.RecipeBookCategories");
            Class<?> registries = gameLoader.loadClass(
                    "net.minecraft.core.registries.BuiltInRegistries");
            Object recipeTypes = registries.getField("RECIPE_TYPE").get(null);
            var parse = resourceLocation.getMethod("parse", String.class);
            var registryGet = registryClass.getMethod("get", resourceLocation);
            var category = cookingRecipeClass.getMethod("category");
            Object blocks = categoriesClass.getField("FURNACE_BLOCKS").get(null);
            Object food = categoriesClass.getField("FURNACE_FOOD").get(null);
            Object misc = categoriesClass.getField("FURNACE_MISC").get(null);
            Function<Object, Object> finder = recipe -> {
                if (!cookingRecipeClass.isInstance(recipe)) {
                    return null;
                }
                try {
                    return switch (((Enum<?>) category.invoke(recipe)).name()) {
                        case "BLOCKS" -> blocks;
                        case "FOOD" -> food;
                        default -> misc;
                    };
                } catch (IllegalAccessException | InvocationTargetException exception) {
                    throw new IllegalStateException(
                            "LB-RECIPE-002: failed to inspect a Fabric cooking recipe", exception);
                }
            };
            var register = event.getClass().getMethod(
                    "registerRecipeCategoryFinder", recipeTypeClass, Function.class);
            for (String id : TRACKER.newRecipeTypes()) {
                Object location = parse.invoke(null, id);
                Object recipeType = registryGet.invoke(recipeTypes, location);
                register.invoke(event, recipeType, finder);
            }
            LOGGER.log(System.Logger.Level.INFO,
                    "LoaderBridge registered {0} Fabric recipe-book category finders",
                    TRACKER.newRecipeTypes().size());
            return true;
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException
                | IllegalAccessException exception) {
            throw new IllegalStateException(
                    "LB-RECIPE-001: Forge recipe-book registration API is unavailable", exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException(
                    "LB-RECIPE-003: Forge rejected a Fabric recipe category finder",
                    exception.getCause());
        }
    }

    private static Set<String> recipeTypeIds(ClassLoader gameLoader) {
        try {
            Class<?> registries = gameLoader.loadClass(
                    "net.minecraft.core.registries.BuiltInRegistries");
            Class<?> registry = gameLoader.loadClass("net.minecraft.core.Registry");
            Object recipeTypes = registries.getField("RECIPE_TYPE").get(null);
            Collection<?> keys = (Collection<?>) registry.getMethod("keySet").invoke(recipeTypes);
            Set<String> ids = new LinkedHashSet<>();
            keys.forEach(key -> ids.add(key.toString()));
            return ids;
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException
                | IllegalAccessException exception) {
            throw new IllegalStateException(
                    "LB-RECIPE-004: Minecraft recipe type registry API is unavailable", exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException(
                    "LB-RECIPE-005: Minecraft rejected recipe type discovery",
                    exception.getCause());
        }
    }

    static final class Tracker {
        private Set<String> before = Set.of();
        private Set<String> added = Set.of();

        synchronized void begin(Collection<String> recipeTypeIds) {
            before = Set.copyOf(recipeTypeIds);
            added = Set.of();
        }

        synchronized void complete(Collection<String> recipeTypeIds) {
            LinkedHashSet<String> difference = new LinkedHashSet<>(recipeTypeIds);
            difference.removeAll(before);
            added = Set.copyOf(difference);
        }

        synchronized Set<String> newRecipeTypes() {
            return added;
        }
    }
}
