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

/** Independently implemented default adapter matching Fabric Loader 0.16.14's public behavior. */
public final class BridgeDefaultLanguageAdapter implements LanguageAdapter {
    public static final BridgeDefaultLanguageAdapter INSTANCE = new BridgeDefaultLanguageAdapter();

    private BridgeDefaultLanguageAdapter() {}

    @Override
    public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException {
        String[] parts = value.split("::", -1);
        if (parts.length > 2 || parts[0].isBlank() || (parts.length == 2 && parts[1].isBlank())) {
            throw new LanguageAdapterException("Invalid handle format: " + value);
        }

        Class<?> owner;
        try {
            owner = Class.forName(parts[0], true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new LanguageAdapterException(exception);
        }

        if (parts.length == 1) {
            if (!type.isAssignableFrom(owner)) {
                throw new LanguageAdapterException("Class " + owner.getName() + " cannot be cast to "
                        + type.getName() + "!");
            }
            try {
                return type.cast(owner.getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException exception) {
                throw new LanguageAdapterException(exception);
            }
        }

        String member = parts[1];
        List<Method> methods = Arrays.stream(owner.getDeclaredMethods())
                .filter(method -> method.getName().equals(member))
                .toList();
        try {
            Field field = owner.getDeclaredField(member);
            if (!Modifier.isStatic(field.getModifiers())) {
                throw new LanguageAdapterException("Field " + value + " must be static!");
            }
            if (!methods.isEmpty()) {
                throw new LanguageAdapterException("Ambiguous " + value + " - refers to both field and method!");
            }
            if (!type.isAssignableFrom(field.getType())) {
                throw new LanguageAdapterException("Field " + value + " cannot be cast to " + type.getName() + "!");
            }
            return type.cast(field.get(null));
        } catch (NoSuchFieldException ignored) {
            // Continue with method adaptation.
        } catch (IllegalAccessException exception) {
            throw new LanguageAdapterException("Field " + value + " cannot be accessed!", exception);
        }

        if (!type.isInterface()) {
            throw new LanguageAdapterException("Cannot proxy method " + value + " to non-interface type "
                    + type.getName() + "!");
        }
        if (methods.isEmpty()) {
            throw new LanguageAdapterException("Could not find " + value + "!");
        }
        if (methods.size() > 1) {
            throw new LanguageAdapterException("Found multiple method entries of name " + value + "!");
        }

        Method method = methods.getFirst();
        Object receiver = null;
        try {
            if (!Modifier.isStatic(method.getModifiers())) {
                receiver = owner.getDeclaredConstructor().newInstance();
            }
            MethodHandle handle = MethodHandles.lookup().unreflect(method);
            if (receiver != null) handle = handle.bindTo(receiver);
            return type.cast(MethodHandleProxies.asInterfaceInstance(type, handle));
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            throw new LanguageAdapterException(exception);
        }
    }
}
