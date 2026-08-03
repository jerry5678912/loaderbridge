package dev.loaderbridge.fabric.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
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
        List<Executable> executables = new ArrayList<>(Arrays.stream(owner.getDeclaredMethods())
                .filter(method -> method.getName().equals(member)).toList());
        if (member.equals("<init>")) {
            executables.addAll(Arrays.asList(owner.getDeclaredConstructors()));
        }
        try {
            Field field = owner.getDeclaredField(member);
            if (!Modifier.isStatic(field.getModifiers())) {
                throw new LanguageAdapterException("Field " + value + " must be static!");
            }
            if (!executables.isEmpty()) {
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
        if (executables.isEmpty()) {
            throw new LanguageAdapterException("Could not find " + value + "!");
        }
        if (executables.size() > 1) {
            throw new LanguageAdapterException("Found multiple method entries of name " + value + "!");
        }

        Executable executable = executables.getFirst();
        Object receiver = null;
        try {
            if (executable instanceof Method method
                    && !Modifier.isStatic(method.getModifiers())) {
                receiver = owner.getDeclaredConstructor().newInstance();
            }
            return adaptExecutable(executable, receiver, type);
        } catch (Exception exception) {
            throw new LanguageAdapterException(exception);
        }
    }

    static <T> T adaptExecutable(Executable executable, Object receiver, Class<T> type)
            throws NoSuchMethodException, IllegalAccessException {
        MethodHandle handle = reflectionHandle(executable, receiver);
        return type.cast(MethodHandleProxies.asInterfaceInstance(type, handle));
    }

    private static MethodHandle reflectionHandle(Executable executable, Object receiver)
            throws NoSuchMethodException, IllegalAccessException {
        if (!Modifier.isPublic(executable.getModifiers())
                || !Modifier.isPublic(executable.getDeclaringClass().getModifiers())) {
            throw new IllegalAccessException("Entrypoint member is not public: " + executable);
        }
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        if (executable instanceof Method method) {
            MethodHandle handle = lookup.findStatic(BridgeDefaultLanguageAdapter.class,
                    "invokeMethod", MethodType.methodType(
                            Object.class, Method.class, Object.class, Object[].class));
            handle = MethodHandles.insertArguments(handle, 0, method, receiver)
                    .asCollector(Object[].class, method.getParameterCount());
            return handle.asType(MethodType.methodType(
                    method.getReturnType(), method.getParameterTypes()));
        }
        Constructor<?> constructor = (Constructor<?>) executable;
        MethodHandle handle = lookup.findStatic(BridgeDefaultLanguageAdapter.class,
                "invokeConstructor", MethodType.methodType(
                        Object.class, Constructor.class, Object[].class));
        handle = MethodHandles.insertArguments(handle, 0, constructor)
                .asCollector(Object[].class, constructor.getParameterCount());
        return handle.asType(MethodType.methodType(
                constructor.getDeclaringClass(), constructor.getParameterTypes()));
    }

    private static Object invokeMethod(Method method, Object receiver, Object[] arguments)
            throws Throwable {
        try {
            return method.invoke(receiver, arguments);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static Object invokeConstructor(Constructor<?> constructor, Object[] arguments)
            throws Throwable {
        try {
            return constructor.newInstance(arguments);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }
}
