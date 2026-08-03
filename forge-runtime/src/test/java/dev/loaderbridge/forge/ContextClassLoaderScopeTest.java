package dev.loaderbridge.forge;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContextClassLoaderScopeTest {
    @Test
    @SuppressWarnings("try")
    void exposesGameLoaderAndRestoresPreviousLoader() {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        ClassLoader game = new ClassLoader(previous) { };

        try (ContextClassLoaderScope ignored = ContextClassLoaderScope.open(game)) {
            assertThat(thread.getContextClassLoader()).isSameAs(game);
        }

        assertThat(thread.getContextClassLoader()).isSameAs(previous);
    }
}
