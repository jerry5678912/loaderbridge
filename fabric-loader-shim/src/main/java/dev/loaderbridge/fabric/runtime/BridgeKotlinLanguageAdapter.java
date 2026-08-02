package dev.loaderbridge.fabric.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;

/** Kotlin object/class adapter implemented from JVM conventions without bundling another Kotlin runtime. */
public final class BridgeKotlinLanguageAdapter implements LanguageAdapter {
    public static final BridgeKotlinLanguageAdapter INSTANCE = new BridgeKotlinLanguageAdapter();

    private BridgeKotlinLanguageAdapter() {}

    @Override
    public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException {
        String[] parts = value.split("::", -1);
        if (parts.length > 2 || parts[0].isBlank()) {
            throw new LanguageAdapterException("Invalid handle format: " + value);
        }
        Class<?> owner;
        try {
            owner = Class.forName(parts[0], true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new LanguageAdapterException(exception);
        }

        Object object = objectInstance(owner);
        if (parts.length == 1) {
            if (!type.isAssignableFrom(owner)) {
                throw new LanguageAdapterException("Class " + owner.getName() + " cannot be cast to "
                        + type.getName() + "!");
            }
            if (object != null) return type.cast(object);
            return BridgeDefaultLanguageAdapter.INSTANCE.create(mod, value, type);
        }
        if (object == null) {
            return BridgeDefaultLanguageAdapter.INSTANCE.create(mod, value, type);
        }

        String member = parts[1];
        List<Method> methods = Arrays.stream(owner.getMethods())
                .filter(method -> method.getName().equals(member) && !Modifier.isStatic(method.getModifiers()))
                .toList();
        Field property = findProperty(owner, member);
        if (property != null) {
            if (!methods.isEmpty()) {
                throw new LanguageAdapterException("Ambiguous " + value + " - refers to both field and method!");
            }
            if (!type.isAssignableFrom(property.getType())) {
                throw new LanguageAdapterException("Field " + value + " cannot be cast to " + type.getName() + "!");
            }
            try {
                if (!property.trySetAccessible()) {
                    throw new LanguageAdapterException("Field " + value + " cannot be accessed!");
                }
                return type.cast(property.get(object));
            } catch (IllegalAccessException exception) {
                throw new LanguageAdapterException("Field " + value + " cannot be accessed!", exception);
            }
        }
        if (!type.isInterface()) {
            throw new LanguageAdapterException("Cannot proxy method " + value + " to non-interface type "
                    + type.getName() + "!");
        }
        if (methods.isEmpty()) throw new LanguageAdapterException("Could not find " + value + "!");
        if (methods.size() > 1) {
            throw new LanguageAdapterException("Found multiple method entries of name " + value + "!");
        }
        try {
            MethodHandle handle = MethodHandles.lookup().unreflect(methods.getFirst()).bindTo(object);
            return type.cast(MethodHandleProxies.asInterfaceInstance(type, handle));
        } catch (IllegalAccessException | IllegalArgumentException exception) {
            throw new LanguageAdapterException("Failed to create method handle for " + value + "!", exception);
        }
    }

    private static Object objectInstance(Class<?> owner) throws LanguageAdapterException {
        try {
            Field instance = owner.getField("INSTANCE");
            return Modifier.isStatic(instance.getModifiers()) ? instance.get(null) : null;
        } catch (NoSuchFieldException ignored) {
            return null;
        } catch (IllegalAccessException exception) {
            throw new LanguageAdapterException(exception);
        }
    }

    private static Field findProperty(Class<?> owner, String name) {
        try {
            return owner.getDeclaredField(name);
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }
}
