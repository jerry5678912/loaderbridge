package dev.loaderbridge.forge;

/** Temporarily exposes the Forge game layer to Fabric language adapters. */
final class ContextClassLoaderScope implements AutoCloseable {
    private final Thread thread;
    private final ClassLoader previous;

    private ContextClassLoaderScope(ClassLoader next) {
        thread = Thread.currentThread();
        previous = thread.getContextClassLoader();
        thread.setContextClassLoader(next);
    }

    static ContextClassLoaderScope open(ClassLoader next) {
        return new ContextClassLoaderScope(next);
    }

    @Override
    public void close() {
        thread.setContextClassLoader(previous);
    }
}
